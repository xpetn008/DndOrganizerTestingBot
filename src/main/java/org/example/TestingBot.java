package org.example;

import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TestingBot extends TelegramLongPollingBot {
    @Autowired
    private UserService userService;
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (update.getMessage().getText().equals("/start")) {
                SendMessage message = new SendMessage();
                message.setText("Hello "+actualUser.getFirstName());
                message.setChatId(chatId);

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                InlineKeyboardButton registrationButton = new InlineKeyboardButton();
                registrationButton.setText("Registration");
                registrationButton.setCallbackData("register");

                rowInLine.add(registrationButton);
                rowsInLine.add(rowInLine);

                markupInline.setKeyboard(rowsInLine);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }else if (update.hasCallbackQuery()){
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callData.equals("register")){
                String answer = "You are registered!!!!!!";
                try {
                    userService.create(actualUser);
                } catch (UserAlreadyRegisteredException e){
                    answer = e.getMessage();
                }

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(answer);
                try{
                    execute(message);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "DndOrganizerTestingBot";
    }
    @Override
    public String getBotToken(){
        return "6982861837:AAENHCWO2Br87FXm0r3Jdb7sucaFd6mxwP4";
    }
}
