package org.example;

import com.google.common.collect.*;
import com.sun.jna.platform.win32.WinDef;
import okhttp3.*;
import org.checkerframework.checker.units.qual.A;
import org.example.models.exceptions.NicknameAlreadyExistsException;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotMasterException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.example.models.services.UserService;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.component.MultiMap;
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
import java.util.*;

@Component
public class TestingBot extends TelegramLongPollingBot {
    @Autowired
    private UserService userService;
    private Map<Long, String> userStates = new HashMap<>();
    private Multimap<Long, Integer> messageRecycleBin = ArrayListMultimap.create();
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            userStates.putIfAbsent(chatId, "default");

            emptyRecycleBin(chatId);
            if (update.getMessage().getText().equals("/start")||update.getMessage().getText().equals("/menu")) {
                mainMenu(chatId, actualUser);
            }
            if (userStates.get(chatId).equals("registration_confirm_master")){
                registration(chatId, actualUser, messageText);
            }

        } else if (update.hasCallbackQuery()){
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String messageText = update.getCallbackQuery().getMessage().getText();
            emptyRecycleBin(chatId);

            if (callData.equals("register") || callData.equals("registerMaster") || callData.equals("registerPlayer")){
                if (callData.equals("register")) {
                    userStates.replace(chatId, "registration_role");
                }else if (callData.equals("registerMaster")){
                    userStates.replace(chatId, "registration_nickname");
                }else if (callData.equals("registerPlayer")){
                    userStates.replace(chatId, "registration_confirm_player");
                }
                registration(chatId, actualUser, messageText);
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
            messageRecycleBin.put(chatId, sentMessage.getMessageId());
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    public void registration(long chatId, User actualUser, String messageText){
        if (userStates.get(chatId).equals("registration_role")) {
           SendMessage message = new SendMessage();
           message.setChatId(chatId);
           message.setText("Choose your role please:");

           InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
           List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();

           InlineKeyboardButton masterButton = new InlineKeyboardButton();
           masterButton.setText("Master");
           masterButton.setCallbackData("registerMaster");
           InlineKeyboardButton playerButton = new InlineKeyboardButton();
           playerButton.setText("Player");
           playerButton.setCallbackData("registerPlayer");

           rowInLine1.add(masterButton);
           rowInLine1.add(playerButton);
           rowsInLine.add(rowInLine1);
           markupInLine.setKeyboard(rowsInLine);
           message.setReplyMarkup(markupInLine);

           try {
                Message sentMessage = execute(message);
                messageRecycleBin.put(chatId, sentMessage.getMessageId());
           } catch (TelegramApiException e) {
                e.printStackTrace();
           }
        } else if (userStates.get(chatId).equals("registration_nickname")){
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Enter your nickname please:");

            try{
                userService.create(actualUser, true);
                userStates.replace(chatId, "registration_confirm_master");
            } catch (UserAlreadyRegisteredException e){
                message.setText(e.getMessage());
                userStates.replace(chatId, "default");
            }

            try {
                Message sentMessage = execute(message);
                messageRecycleBin.put(chatId, sentMessage.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (userStates.get(chatId).equals("registration_confirm_player")){
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("You are registered as a player now!");

            try{
                userService.create(actualUser, false);
            } catch (UserAlreadyRegisteredException e){
                message.setText(e.getMessage());
            }

            try {
                Message sentMessage = execute(message);
                messageRecycleBin.put(chatId, sentMessage.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            userStates.replace(chatId, "default");
            mainMenu(chatId, actualUser);
        } else if (userStates.get(chatId).equals("registration_confirm_master")){
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("You are registered as a master now!");

            try{
                userService.setMasterNickname(actualUser, messageText);
                userStates.replace(chatId, "default");
            } catch (NicknameAlreadyExistsException | UserIsNotRegisteredException | UserIsNotMasterException e){
                message.setText(e.getMessage());
            }
            try {
                Message sentMessage = execute(message);
                messageRecycleBin.put(chatId, sentMessage.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            if (userStates.get(chatId).equals("default")) {
                mainMenu(chatId, actualUser);
            }
        }
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
                messageRecycleBin.put(chatId, sentMessage.getMessageId());
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
                    messageRecycleBin.put(chatId, sentMessage.getMessageId());
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
                    messageRecycleBin.put(chatId, sentMessage.getMessageId());
                }catch (TelegramApiException e){
                    e.printStackTrace();
                }
                userStates.replace(chatId,"default");
                mainMenu(chatId, actualUser);
            }
        }
    }

    private void emptyRecycleBin(long chatId){
        for (Integer messageId : messageRecycleBin.get(chatId)){
            deleteMessage(chatId, messageId);
        }
        messageRecycleBin.removeAll(chatId);
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





    public void writeToManyPeople(List<Long> ids, String text){
        SendMessage message = new SendMessage();
        message.setText(text);

        for (Long userId : ids) {
            message.setChatId(userId.toString());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                // Обработка исключения, если сообщение не может быть отправлено пользователю с указанным id
            }
        }
    }
    public void writeToOnePerson(Long id, String text){
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(id.toString());
        try {
            execute(message);
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
