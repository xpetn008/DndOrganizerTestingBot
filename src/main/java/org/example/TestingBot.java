package org.example;

import com.google.common.collect.*;
import com.sun.jna.platform.win32.WinDef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import okhttp3.*;
import org.checkerframework.checker.units.qual.A;
import org.example.data.entities.GameEntity;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.NicknameAlreadyExistsException;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotMasterException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.example.models.services.GameService;
import org.example.models.services.UserService;
import org.example.tools.DateTools;
import org.example.tools.TimeTools;
import org.hibernate.Hibernate;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class TestingBot extends TelegramLongPollingBot {
    @Autowired
    private GameService gameService;
    @Autowired
    private UserService userService;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Multimap<Long, Integer> messageRecycleBin = ArrayListMultimap.create();
    private GameEntity newGame;
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            userStates.putIfAbsent(chatId, "default");

            emptyRecycleBin(chatId);
            if (update.getMessage().getText().equals("/start")||update.getMessage().getText().equals("/menu")) {
                showMenu(chatId, actualUser);
            }
            if (userStates.get(chatId).equals("registration_confirm_master")){
                registration(chatId, actualUser, messageText);
            } else if (userStates.get(chatId).contains("creating")){
                createGame(chatId, actualUser, messageText);
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
            } else if (callData.equals("createGame")){
                userStates.replace(chatId, "creating_game_name");
                createGame(chatId, actualUser, messageText);
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
            showMenu(chatId, actualUser);
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
                showMenu(chatId, actualUser);
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
                showMenu(chatId, actualUser);
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
                showMenu(chatId, actualUser);
            }
        }
    }


    public void masterMenu(long chatID, User actualUser){
        userStates.replace(chatID, "default");
        UserEntity user;
        try {
            user = userService.getUserEntity(actualUser);
        }catch (UserIsNotRegisteredException e){
            user = null;
            e.printStackTrace();
        }
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText("Hello "+actualUser.getFirstName()+", your master nickname is "+user.getMasterNickname()+"\n" +
                "THIS IS MAIN MENU");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();

        InlineKeyboardButton deletingButton = new InlineKeyboardButton();
        deletingButton.setText("Delete account");
        deletingButton.setCallbackData("delete");

        InlineKeyboardButton createGameButton = new InlineKeyboardButton();
        createGameButton.setText("New game");
        createGameButton.setCallbackData("createGame");

        InlineKeyboardButton editGamesButton = new InlineKeyboardButton();
        editGamesButton.setText("Edit games");
        editGamesButton.setCallbackData("editGame");

        rowInLine1.add(deletingButton);
        rowInLine2.add(createGameButton);
        rowInLine2.add(editGamesButton);
        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);

        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline);

        try{
            Message sentMessage = execute(message);
            messageRecycleBin.put(chatID, sentMessage.getMessageId());
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    public void createGame(long chatId, User actualUser, String messageText){
        if (userStates.get(chatId).equals("creating_game_name")){
            newGame = new GameEntity();
            sendMessage("Please choose a unique name for your game: ", chatId);
            userStates.replace(chatId, "creating_game_date");
        } else if (userStates.get(chatId).equals("creating_game_date")){
            if (!gameService.gameNameIsFree(messageText)){
                sendMessage("This game name is already used. Please choose other name: ", chatId);
            }else {
                newGame.setName(messageText);
                sendMessage("Please choose a date for your game. Date must " +
                        "have format (dd.MM.yyyy) and be at least 1 week away but no more than 2 years away.", chatId);
                userStates.replace(chatId, "creating_game_time");
            }
        } else if (userStates.get(chatId).equals("creating_game_time")){
            try {
                if (!DateTools.controlDate(messageText)) {
                    sendMessage("Bad date format or range. Please write date again: ", chatId);
                } else {
                    LocalDate date = DateTools.parseDate(messageText);
                    newGame.setDate(date);
                    sendMessage("Please choose a time four your game. Time must " +
                            "have format (HH:mm).", chatId);
                    userStates.replace(chatId, "creating_game_final");
                }
            }catch (DateTimeParseException e){
                sendMessage("Bad date format or range. Please write date again: ", chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_final")){
            LocalTime time;
            String message;
            try {
                time = TimeTools.parseTime(messageText);
                newGame.setTime(time);
                message = "Your game is successfully created!";
                UserEntity master = userService.getUserEntity(actualUser);
                gameService.create(newGame.getName(), newGame.getDate(), newGame.getTime(), master);
                userStates.replace(chatId, "default");
                newGame = null;
                showMenu(chatId, actualUser);
            } catch (DateTimeParseException e){
                message = "Bad time format. Please write time again: ";
            } catch (UserIsNotRegisteredException e){
                message = "Something went wrong. UserIsNotRegisteredException happened.";
            }
            sendMessage(message, chatId);
        }
    }
    public void showMasterGames(long chatId, User actualUser){

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
    private void sendMessage(String messageText, long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        try{
            Message sentMessage = execute(message);
            messageRecycleBin.put(chatId, sentMessage.getMessageId());
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    private void showMenu(long chatId, User actualUser){
        try{
            UserEntity user = userService.getUserEntity(actualUser);
            if (user.isMaster()){
                masterMenu(chatId, actualUser);
            } else {
                mainMenu(chatId, actualUser);
            }
        } catch (UserIsNotRegisteredException e){
            mainMenu(chatId, actualUser);
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
