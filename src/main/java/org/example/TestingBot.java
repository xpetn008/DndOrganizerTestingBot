package org.example;

import com.sun.jna.platform.win32.WinDef;
import okhttp3.*;
import org.checkerframework.checker.units.qual.A;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.example.models.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestingBot extends TelegramLongPollingBot {
    @Autowired
    private UserService userService;
    private Map<Long, String> userStates = new HashMap<>();
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            userStates.putIfAbsent(chatId, "default");

            if (update.getMessage().getText().equals("/start")||update.getMessage().getText().equals("/menu")) {
                mainMenu(chatId, actualUser);
            }

        } else if (update.hasCallbackQuery()){
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callData.equals("register")){
                registration(chatId, actualUser);
            } else if (callData.equals("delete") || callData.equals("deletingYes") || callData.equals("deletingNo")){
                if (callData.equals("delete")){
                    userStates.replace(chatId, "deleting_choice");
                }
                deletingAccount(chatId, actualUser, update);
            }
        }
    }


    public void mainMenu(long chatId, User actualUser){
        userStates.replace(chatId, "default");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("      MENU      \n" +
                "----------------");


        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();

        InlineKeyboardButton registrationButton = new InlineKeyboardButton();
        registrationButton.setText("Registration");
        registrationButton.setCallbackData("register");

        InlineKeyboardButton deletingButton = new InlineKeyboardButton();
        deletingButton.setText("Delete account");
        deletingButton.setCallbackData("delete");


        if (!userService.isRegistered(actualUser)) {
            rowInLine1.add(registrationButton);
            rowsInLine.add(rowInLine1);
        } else {
            rowInLine1.add(deletingButton);
            rowsInLine.add(rowInLine1);
            rowsInLine.add(rowInLine2);

        }

        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline);

        try{
            Message sentMessage = execute(message);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    public void registration(long chatId, User actualUser){
        String answer = "You are registered.";
        try {
            userService.create(actualUser);
        } catch (UserAlreadyRegisteredException e){
            answer = e.getMessage();
        }
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(answer);
        try{
            Message sentMessage = execute(message);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
        mainMenu(chatId, actualUser);
    }
    public void deletingAccount(long chatId, User actualUser, Update update){
        if (userStates.get(chatId).equals("deleting_choice")) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Are you sure, you want to delete your account?");

            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();

            InlineKeyboardButton yesButton = new InlineKeyboardButton();
            yesButton.setText("YES");
            yesButton.setCallbackData("deletingYes");
            InlineKeyboardButton noButton = new InlineKeyboardButton();
            noButton.setText("NO");
            noButton.setCallbackData("deletingNo");

            rowInLine1.add(yesButton);
            rowInLine1.add(noButton);

            rowsInLine.add(rowInLine1);
            markupInline.setKeyboard(rowsInLine);
            message.setReplyMarkup(markupInline);
            try {
                Message sentMessage = execute(message);
                userStates.replace(chatId, "deleting_answer");
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (userStates.get(chatId).equals("deleting_answer")){
            String callData = update.getCallbackQuery().getData();
            if (callData.equals("deletingYes")){
                String answer = "Your account was deleted.";
                try{
                    userService.delete(actualUser);
                } catch (UserIsNotRegisteredException e){
                    answer = e.getMessage();
                }
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(answer);
                try{
                    Message sentMessage = execute(message);
                }catch (TelegramApiException e){
                    e.printStackTrace();
                }
                userStates.replace(chatId,"default");
                mainMenu(chatId, actualUser);
            }else if (callData.equals("deletingNo")){
                String answer = "Your account wasn't deleted.";
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(answer);
                try{
                    Message sentMessage = execute(message);
                }catch (TelegramApiException e){
                    e.printStackTrace();
                }
                userStates.replace(chatId,"default");
                mainMenu(chatId, actualUser);
            }
        }
    }

    private void deleteMessage(long chatId, int messageId){
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
