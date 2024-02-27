package org.example;

import com.google.common.collect.*;
import com.sun.jna.platform.win32.WinDef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import okhttp3.*;
import org.checkerframework.checker.units.qual.A;
import org.example.data.entities.GameEntity;
import org.example.data.entities.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
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
import java.time.DateTimeException;
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
    private Long editedGameId;
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
            } else if (userStates.get(chatId).equals("registration_confirm_master")){
                registration(chatId, actualUser, messageText);
            } else if (userStates.get(chatId).contains("creating")){
                createGame(chatId, actualUser, messageText);
            } else if (userStates.get(chatId).contains("editing_games") && userStates.get(chatId).contains("control")){
                editMasterGame(chatId, update, actualUser, messageText);
            } else {
                sendMessage("Something went wrong.", chatId, null);
                showMenu(chatId, actualUser);
            }

        } else if (update.hasCallbackQuery()){
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String messageText = update.getCallbackQuery().getMessage().getText();
            emptyRecycleBin(chatId);
            userStates.putIfAbsent(chatId, "default");

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
            } else if (callData.equals("createGame") || callData.contains("creatingGameType")){
                if (callData.equals("createGame")) {
                    userStates.replace(chatId, "creating_game_name");
                    createGame(chatId, actualUser, messageText);
                } else if (callData.contains("creatingGameType")){
                    String gameType;
                    if (callData.equals("creatingGameTypeCampaign")){
                        gameType = "campaign";
                    } else {
                        gameType = "cneshot";
                    }
                    userStates.replace(chatId, "creating_game_date");
                    createGame(chatId, actualUser, gameType);
                }
            } else if (callData.equals("editGames") || callData.contains("editingMasterGame_") || callData.contains("editingGame")){
                if (callData.equals("editGames")){
                    showMasterGames(chatId, actualUser);
                } else if (callData.contains("editingMasterGame")){
                    editedGameId = Long.parseLong(callData.substring(18));
                    userStates.replace(chatId, "editing_games_action");
                    editMasterGame(chatId, update, actualUser, messageText);
                } else if (callData.contains("editingGame")){
                    if (callData.equals("editingGameName")){
                        userStates.replace(chatId, "editing_games_name");
                    } else if (callData.equals("editingGameDate")){
                        userStates.replace(chatId, "editing_games_date");
                    } else if (callData.equals("editingGameTime")){
                        userStates.replace(chatId, "editing_games_time");
                    } else if (callData.equals("editingGameType")){
                        userStates.replace(chatId, "editing_games_type");
                    }
                    editMasterGame(chatId, update, actualUser, messageText);
                }
            } else if (callData.equals("deletingGame")){
                userStates.replace(chatId, "editing_games_delete");
                editMasterGame(chatId, update, actualUser, messageText);
            } else if (callData.equals("deletingGameYes") || callData.equals("deletingGameNo")){
                if (callData.equals("deletingGameYes")){
                    userStates.replace(chatId, "editing_games_delete_control");
                    editMasterGame(chatId, update, actualUser, messageText);
                } else{
                    userStates.replace(chatId, "default");
                    showMenu(chatId, actualUser);
                }
            } else {
                sendMessage("Something went wrong.", chatId, null);
                showMenu(chatId, actualUser);
            }
        }
    }


    public void mainMenu(long chatId, User actualUser){
        userStates.replace(chatId, "default");

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
        sendMessage("      MENU      \n" +
                "----------------", chatId, markupInline);
    }
    public void registration(long chatId, User actualUser, String messageText){
        if (userStates.get(chatId).equals("registration_role")) {
           InlineKeyboardMarkup markupInLine = createMarkup(2, Map.of(0, "Master", 1, "Player"),
                   Map.of(0, "registerMaster", 1, "registerPlayer"));
           sendMessage("Choose your role please:", chatId, markupInLine);
        } else if (userStates.get(chatId).equals("registration_nickname")){
            String text = "Enter your nickname please:";
            try{
                userService.create(actualUser, true);
                userStates.replace(chatId, "registration_confirm_master");
            } catch (UserAlreadyRegisteredException e){
                text = e.getMessage();
                userStates.replace(chatId, "default");
            }
            sendMessage(text, chatId, null);
        } else if (userStates.get(chatId).equals("registration_confirm_player")){
            String text = "You are registered as a player now!";
            try{
                userService.create(actualUser, false);
            } catch (UserAlreadyRegisteredException e){
                text = e.getMessage();
            }
            sendMessage(text, chatId, null);
            userStates.replace(chatId, "default");
            showMenu(chatId, actualUser);
        } else if (userStates.get(chatId).equals("registration_confirm_master")){
            String text = "You are registered as a master now!";
            try{
                userService.setMasterNickname(actualUser, messageText);
                userStates.replace(chatId, "default");
            } catch (NicknameAlreadyExistsException | UserIsNotRegisteredException | UserIsNotMasterException e){
                text = e.getMessage();
            }
            sendMessage(text, chatId, null);
            if (userStates.get(chatId).equals("default")) {
                showMenu(chatId, actualUser);
            }
        }
    }
    public void deletingAccount(long chatId, User actualUser, Update update){
        if (userStates.get(chatId).equals("deleting_choice")) {
            String text = "Are you sure, you want to delete your account?";
            InlineKeyboardMarkup markupInline = createMarkup(2, Map.of(0, "YES", 1, "NO"),
                    Map.of(0, "deletingYes", 1, "deletingNo"));
            sendMessage(text, chatId, markupInline);
            userStates.replace(chatId, "deleting_answer");
        } else if (userStates.get(chatId).equals("deleting_answer")){
            String callData = update.getCallbackQuery().getData();
            if (callData.equals("deletingYes")){
                String answer = "Your account was deleted.";
                try{
                    userService.delete(actualUser);
                } catch (UserIsNotRegisteredException e){
                    answer = e.getMessage();
                }

                sendMessage(answer, chatId, null);
                userStates.replace(chatId,"default");
                showMenu(chatId, actualUser);
            }else if (callData.equals("deletingNo")){
                String answer = "Your account wasn't deleted.";
                sendMessage(answer, chatId, null);
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
        String text = "Hello "+actualUser.getFirstName()+", your master nickname is "+user.getMasterNickname()+"\n" +
                "THIS IS MAIN MENU";

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
        editGamesButton.setCallbackData("editGames");
        rowInLine1.add(deletingButton);
        rowInLine2.add(createGameButton);
        rowInLine2.add(editGamesButton);
        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);
        markupInline.setKeyboard(rowsInLine);
        sendMessage(text, chatID, markupInline);
    }
    public void createGame(long chatId, User actualUser, String messageText){
        if (userStates.get(chatId).equals("creating_game_name")){
            newGame = new GameEntity();
            sendMessage("Please choose a unique name for your game: ", chatId, null);
            userStates.replace(chatId, "creating_game_type");
        } else if (userStates.get(chatId).equals("creating_game_type")){
            if (!gameService.gameNameIsFree(messageText)){
                sendMessage("This game name is already used. Please choose other name: ", chatId, null);
            } else {
                newGame.setName(messageText);

                InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Campaign", 1,"One shot"),
                        Map.of(0, "creatingGameTypeCampaign", 1, "creatingGameTypeOneshot"));
                sendMessage("Please select a game type: ", chatId, markup);
                userStates.replace(chatId, "creating_game_date");
            }
        } else if (userStates.get(chatId).equals("creating_game_date")){
            if (messageText.equals("campaign")){
                newGame.setGameType(GameType.CAMPAIGN);
            } else {
                newGame.setGameType(GameType.ONESHOT);
            }
            sendMessage("Please choose a date for your game. Date must " +
                    "have format (dd.MM.yyyy) and be at least 1 week away but no more than 2 years away.", chatId, null);
            userStates.replace(chatId, "creating_game_time");
        } else if (userStates.get(chatId).equals("creating_game_time")){
            try {
                if (!DateTools.controlDate(messageText)) {
                    sendMessage("Bad date format or range. Please write date again: ", chatId, null);
                } else {
                    LocalDate date = DateTools.parseStringToLocalDate(messageText);
                    newGame.setDate(date);
                    sendMessage("Please choose a time four your game. Time must " +
                            "have format (HH:mm).", chatId, null);
                    userStates.replace(chatId, "creating_game_final");
                }
            }catch (DateTimeException e){
                sendMessage("Bad date format or range. Please write date again: ", chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_final")){
            LocalTime time;
            String message;
            try {
                time = TimeTools.parseStringToLocalTime(messageText);
                newGame.setTime(time);
                message = "Your game is successfully created!";
                UserEntity master = userService.getUserEntity(actualUser);
                gameService.create(newGame.getName(), newGame.getDate(), newGame.getTime(), master, newGame.getGameType());
                userStates.replace(chatId, "default");
                newGame = null;
                showMenu(chatId, actualUser);
            } catch (DateTimeException e){
                message = "Bad time format. Please write time again: ";
            } catch (UserIsNotRegisteredException e){
                message = "Something went wrong. UserIsNotRegisteredException happened.";
            }
            sendMessage(message, chatId, null);
        }
    }
    public void showMasterGames(long chatId, User actualUser){
        try {
            UserEntity master = userService.getUserEntity(actualUser);
            Set<GameEntity> masterGames = gameService.getAllGamesByMaster(master);
            String message = "Your games:";
            int numbering = 1;
            for (GameEntity game : masterGames){
                message += "\n"+numbering+")" +
                        "\nName: "+game.getName()+
                        "\nGame type: "+game.getGameType()+
                        "\nDate: "+DateTools.parseLocalDateToString(game.getDate())+
                        "\nTime: "+TimeTools.parseLocalTimeToString(game.getTime())+
                        "\nPlayers: "+game.getPlayers().size()+
                        "\n";
                numbering++;
            }
            message += "\nChoose a game you want to edit:";

            int numberingButtons = 0;
            int buttonAmount = masterGames.size();
            Map<Integer, String> buttonTexts = new HashMap<>();
            Map<Integer, String> callData = new HashMap<>();
            for (GameEntity game : masterGames){
                buttonTexts.put(numberingButtons, game.getName());
                callData.put(numberingButtons, "editingMasterGame_"+game.getId());
            }
            InlineKeyboardMarkup markupLine = createMarkup(buttonAmount, buttonTexts, callData);
            sendMessage(message, chatId, markupLine);
        } catch (UserIsNotRegisteredException e){
            sendMessage("Something went wrong. UserIsNotRegisteredException happened.", chatId, null);
        } catch (MasterHaveNoGamesException e){
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        }
    }
    public void editMasterGame(long chatId, Update update, User actualUser, String messageText){
        if (userStates.get(chatId).equals("editing_games_action")) {
            InlineKeyboardMarkup markupLine = createMarkup(5, Map.of(0, "Edit Name", 1, "Edit Date",
                    2, "Edit Time", 3, "Edit Type", 4, "Delete Game"), Map.of(0,
                    "editingGameName", 1, "editingGameDate", 2, "editingGameTime",
                    3, "editingGameType", 4, "deletingGame"));
            sendMessage("Please choose action: ", chatId, markupLine);
        } else if (userStates.get(chatId).equals("editing_games_name")){
            sendMessage("Please write to chat new name for a game: ", chatId, null);
            userStates.replace(chatId, "editing_games_name_control");
        } else if (userStates.get(chatId).equals("editing_games_date")){
            sendMessage("Please write to chat new date for a game: ", chatId, null);
            userStates.replace(chatId, "editing_games_date_control");
        } else if (userStates.get(chatId).equals("editing_games_time")){
            sendMessage("Please write to chat new time for a game: ", chatId, null);
            userStates.replace(chatId, "editing_games_time_control");
        } else if (userStates.get(chatId).equals("editing_games_type")){
            InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Campaign", 1 , "One shot"),
                    Map.of(0, "editingGameTypeCampaign", 1, "editingGameTypeOneshot"));
            sendMessage("Please select game type: ", chatId, markup);
            userStates.replace(chatId, "editing_games_type_control");
        } else if (userStates.get(chatId).equals("editing_games_delete")){
            InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Yes", 1, "No"),
                    Map.of(0, "deletingGameYes", 1, "deletingGameNo"));
            sendMessage("Are you sure, you want to delete this game?", chatId, markup);
        } else if (userStates.get(chatId).equals("editing_games_name_control")){
            if (gameService.gameNameIsFree(messageText)){
                try{
                    gameService.changeGameData("name", messageText, editedGameId);
                    sendMessage("Name was successfully changed", chatId, null);
                    editedGameId = null;
                    userStates.replace(chatId, "default");
                    showMenu(chatId, actualUser);
                } catch (NoSuchGameException | BadDataTypeException e){
                    e.printStackTrace();
                    sendMessage("Something went wrong, please try again.", chatId, null);
                }
            }else{
                sendMessage("This game name is already used. Please choose other name: ", chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_date_control")){
            try {
                if (DateTools.controlDate(messageText)) {
                    try {
                        gameService.changeGameData("date", messageText, editedGameId);
                        sendMessage("Date was successfully changed", chatId, null);
                        editedGameId = null;
                        userStates.replace(chatId, "default");
                        showMenu(chatId, actualUser);
                    } catch (NoSuchGameException | BadDataTypeException e) {
                        e.printStackTrace();
                        sendMessage("Something went wrong, please try again.", chatId, null);
                    }
                } else {
                    sendMessage("Bad date format or range. Please write date again: ", chatId, null);
                }
            } catch (DateTimeException e){
                sendMessage("Bad date format or range. Please write date again: ", chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_time_control")){
            try {
                TimeTools.parseStringToLocalTime(messageText);
                gameService.changeGameData("time", messageText, editedGameId);
                sendMessage("Time was successfully changed.", chatId, null);
                editedGameId = null;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (DateTimeParseException e){
                sendMessage("Bad time format. Please write time again: ", chatId, null);
            } catch (NoSuchGameException | BadDataTypeException e){
                e.printStackTrace();
                sendMessage("Something went wrong, please try again.", chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_type_control")){
            try {
                gameService.changeGameData("type", update.getCallbackQuery().getData(), editedGameId);
                sendMessage("Game type was successfully changed.", chatId, null);
                editedGameId = null;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Error: "+e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("editing_games_delete_control")){
            try {
                gameService.deleteGameById(editedGameId);
                editedGameId = null;
                userStates.replace(chatId, "default");
                sendMessage("Game was successfully deleted.", chatId, null);
                showMenu(chatId, actualUser);
            } catch (NoSuchGameException e){
                e.printStackTrace();
                sendMessage("Something went wrong, please try again.", chatId, null);
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
    private void sendMessage(String messageText, long chatId, InlineKeyboardMarkup markup){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        if (markup != null){
            message.setReplyMarkup(markup);
        }
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
    private InlineKeyboardMarkup createMarkup(int buttonAmount, Map<Integer, String> buttonTexts, Map<Integer, String> callData){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        int rowAmount;
        if (buttonAmount%2 == 0){
            rowAmount = buttonAmount/2;
        } else {
            rowAmount = ((buttonAmount-1)/2)+1;
        }
        for (int i = 0; i<rowAmount; i++){
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowsInLine.add(rowInLine);
        }
        for (int i = 0; i < buttonAmount; i++){
            List<InlineKeyboardButton> actualRow;
            if (i%2 == 0){
                actualRow = rowsInLine.get(i/2);
            } else {
                actualRow = rowsInLine.get((i-1)/2);
            }
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonTexts.get(i));
            button.setCallbackData(callData.get(i));
            actualRow.add(button);
        }
        markup.setKeyboard(rowsInLine);
        return markup;
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
