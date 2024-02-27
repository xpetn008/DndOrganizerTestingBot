package org.example;

import com.google.common.collect.*;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameRegion;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
import org.example.models.services.GameService;
import org.example.models.services.UserService;
import org.example.tools.DateTools;
import org.example.tools.TimeTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
            String regionCode = callData.substring(callData.length()-2);

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
            } else if (callData.equals("createGame") || callData.contains("creatingGameType") || callData.contains("creatingGameRegion")){
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
                    userStates.replace(chatId, "creating_game_region");
                    createGame(chatId, actualUser, gameType);
                } else if (callData.contains("creatingGameRegion")){
                    userStates.replace(chatId, "creating_game_date");
                    createGame(chatId, actualUser, regionCode);
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
                    } else if (callData.equals("editingGameDescription")){
                        userStates.replace(chatId, "editing_games_description");
                    } else if (callData.equals("editingGameMaxPlayers")){
                        userStates.replace(chatId, "editing_games_maxplayers");
                    } else if (callData.equals("editingGameRegion")){
                        userStates.replace(chatId, "editing_games_region");
                    } else if (callData.contains("editingGameRegion") && userStates.get(chatId).contains("control")){
                        messageText = regionCode;
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
            } else if (callData.contains("switchTo")){
                if (callData.equals("switchToMasterMenu")){
                    masterMenu(chatId, actualUser);
                } else {
                    playerMenu(chatId, actualUser);
                }
            } else if (callData.equals("joinGame") || callData.contains("choosingGame")){
                if (callData.equals("joinGame")) {
                    userStates.replace(chatId, "showing_games_select_region");
                    showAllGamesByRegion(chatId, messageText);
                } else if (callData.contains("choosingGame")){
                    if (callData.contains("choosingGameRegion")){
                        messageText = callData.substring(callData.length()-2);
                        userStates.replace(chatId, "showing_games_print");
                        showAllGamesByRegion(chatId, messageText);
                    }else if (callData.contains("choosingGameToJoin")){
                        String [] splittedCallData = callData.split("_");
                        String gameId = splittedCallData[1];
                        joinGame(chatId, gameId, actualUser);
                    }
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
        InlineKeyboardMarkup markup = createMarkup(4, Map.of(0, "New game", 1, "Edit games",
                2, "Player menu", 3, "Delete account"), Map.of(0, "createGame", 1, "editGames",
                2, "switchToPlayerMenu", 3, "delete"));
        sendMessage(text, chatID, markup);
    }
    public void createGame(long chatId, User actualUser, String messageText){
        if (userStates.get(chatId).equals("creating_game_name")){
            newGame = new GameEntity();
            sendMessage("Please choose a unique name for your game: ", chatId, null);
            userStates.replace(chatId, "creating_game_description");
        } else if (userStates.get(chatId).equals("creating_game_description")){
            if (!gameService.gameNameIsFree(messageText)){
                sendMessage("This game name is already used. Please choose other name: ", chatId, null);
            } else {
                try {
                    newGame.setName(messageText);
                    sendMessage("Please write a short description for your game: ", chatId, null);
                    userStates.replace(chatId, "creating_game_maxplayers");
                } catch (BadDataException e){
                    e.printStackTrace();
                    sendMessage(e.getMessage(), chatId, null);
                }
            }
        } else if (userStates.get(chatId).equals("creating_game_maxplayers")){
            try{
                newGame.setDescription(messageText);
                sendMessage("Please write max possible amount of players for your game", chatId, null);
                userStates.replace(chatId, "creating_game_type");
            }catch (BadDataException e){
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_type")){
            try {
                newGame.setMaxPlayersByString(messageText);
                InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Campaign", 1, "One shot"),
                        Map.of(0, "creatingGameTypeCampaign", 1, "creatingGameTypeOneshot"));
                sendMessage("Please select a game type: ", chatId, markup);
                userStates.replace(chatId, "creating_game_region");
            } catch (BadDataException e){
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_region")){
            if (messageText.equals("campaign")){
                newGame.setGameType(GameType.CAMPAIGN);
            } else {
                newGame.setGameType(GameType.ONESHOT);
            }
            int buttonAmount = GameRegion.values().length;
            Map<Integer, String> buttonTexts = new HashMap<>();
            Map<Integer, String> callData = new HashMap<>();
            for (int i = 0; i < buttonAmount; i++){
                buttonTexts.put(i, GameRegion.values()[i].toString());
                callData.put(i, "creatingGameRegion"+GameRegion.values()[i].toString());
            }
            InlineKeyboardMarkup markup = createMarkup(buttonAmount, buttonTexts, callData);
            sendMessage("Please select region:", chatId, markup);
        } else if (userStates.get(chatId).equals("creating_game_date")){
            try {
                newGame.setRegion(GameRegion.parseGameRegion(messageText));
                sendMessage("Please choose a date for your game. Date must " +
                        "have format (dd.MM.yyyy) and be at least 1 week away but no more than 2 years away.", chatId, null);
                userStates.replace(chatId, "creating_game_time");
            } catch (BadDataTypeException e){
                sendMessage("Something went wrong! Please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_time")){
            try {
                if (!DateTools.controlDate(messageText)) {
                    sendMessage("Bad date format or range. Please write date again: ", chatId, null);
                } else {
                    LocalDate date = DateTools.parseStringToLocalDate(messageText);
                    newGame.setDate(date);
                    sendMessage("Please choose a time for your game. Time must " +
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
                gameService.create(newGame.getName(), newGame.getDate(), newGame.getTime(), master, newGame.getGameType(), newGame.getDescription(), newGame.getMaxPlayers(), newGame.getRegion());
                userStates.replace(chatId, "default");
                newGame = null;
                showMenu(chatId, actualUser);
            } catch (DateTimeException e){
                message = "Bad time format. Please write time again: ";
            } catch (UserIsNotRegisteredException e){
                message = "Something went wrong. UserIsNotRegisteredException happened.";
            } catch (BadDataException e){
                message = e.getMessage();
                e.printStackTrace();
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
                        "\nRegion: "+game.getRegion().toFullString()+
                        "\nDate: "+DateTools.parseLocalDateToString(game.getDate())+
                        "\nTime: "+TimeTools.parseLocalTimeToString(game.getTime())+
                        "\nPlayers: "+game.getPlayers().size()+"/"+game.getMaxPlayers()+
                        "\nDescription: "+game.getDescription()+
                        "\n";
                numbering++;
            }
            message += "\nChoose a game you want to edit:";

            InlineKeyboardMarkup markupLine = createButtonsByGameSet(masterGames, "editingMasterGame");
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
            InlineKeyboardMarkup markupLine = createMarkup(8, Map.of(0, "Edit Name", 1, "Edit Date",
                    2, "Edit Time", 3, "Edit Type", 4, "Edit Description", 5, "Edit Max Players", 6, "Edit Region", 7, "Delete Game"), Map.of(0,
                    "editingGameName", 1, "editingGameDate", 2, "editingGameTime",
                    3, "editingGameType", 4, "editingGameDescription", 5, "editingGameMaxPlayers", 6 , "editingGameRegion", 7, "deletingGame"));
            sendMessage("Please choose action: ", chatId, markupLine);
        } else if (userStates.get(chatId).equals("editing_games_name")){
            sendMessage("Please write to chat new name for a game: ", chatId, null);
            userStates.replace(chatId, "editing_games_name_control");
        } else if (userStates.get(chatId).equals("editing_games_region")){
            int buttonAmount = GameRegion.values().length;
            Map<Integer, String> buttonTexts = new HashMap<>();
            Map<Integer, String> callData = new HashMap<>();
            for (int i = 0; i < buttonAmount; i++){
                buttonTexts.put(i, GameRegion.values()[i].toString());
                callData.put(i, "editingGameRegion"+GameRegion.values()[i].toString());
            }
            InlineKeyboardMarkup markup = createMarkup(buttonAmount, buttonTexts, callData);
            sendMessage("Please select game region: ", chatId, markup);
            userStates.replace(chatId, "editing_games_region_control");
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
        } else if (userStates.get(chatId).equals("editing_games_description")){
            sendMessage("Please write to chat new description for a game:", chatId, null);
            userStates.replace(chatId, "editing_games_description_control");
        } else if (userStates.get(chatId).equals("editing_games_maxplayers")){
            sendMessage("Please write to chat new max amount of players(2-10):", chatId, null);
            userStates.replace(chatId, "editing_games_maxplayers_control");
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
                    sendMessage(e.getMessage(), chatId, null);
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
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_type_control")){
            try {
                gameService.changeGameData("type", update.getCallbackQuery().getData(), editedGameId);
                sendMessage("Game type was successfully changed.", chatId, null);
                editedGameId = null;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("editing_games_region_control")) {
            try {
                gameService.changeGameData("region", messageText, editedGameId);
                sendMessage("Game region was successfully changed.", chatId, null);
                editedGameId = null;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("editing_games_description_control")) {
            try{
                gameService.changeGameData("description", messageText, editedGameId);
                sendMessage("Game description was successfully changed.", chatId, null);
                editedGameId = null;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_maxplayers_control")){
            try {
                gameService.changeGameData("maxPlayers", messageText, editedGameId);
                editedGameId = null;
                userStates.replace(chatId, "default");
                sendMessage("Game max amount of players was successfully changed.", chatId, null);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_delete_control")){
            try {
                gameService.deleteGameById(editedGameId);
                editedGameId = null;
                userStates.replace(chatId, "default");
                sendMessage("Game was successfully deleted.", chatId, null);
                showMenu(chatId, actualUser);
            } catch (NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        }
    }


    public void playerMenu(long chatId, User actualUser) {
        userStates.replace(chatId, "default");
        String text = "Hello, " + actualUser.getFirstName() + "\n" +
                "THIS IS MAIN MENU";
        int buttonAmount = 2;
        Map<Integer, String> buttonTexts = new HashMap<>(Map.of(0, "Join game", 1, "My games"));
        Map<Integer, String> callData = new HashMap<>(Map.of(0, "joinGame", 1, "myGames"));
        try {
            if (userService.isMaster(actualUser)){
                buttonTexts.put(2, "Master menu");
                buttonTexts.put(3, "Delete account");
                callData.put(2, "switchToMasterMenu");
                callData.put(3, "delete");
                buttonAmount = 4;
            } else {
                buttonTexts.put(2, "Delete account");
                callData.put(2, "delete");
                buttonAmount = 3;
            }
        } catch (UserIsNotRegisteredException e){
            buttonTexts.put(2, "Delete account");
            callData.put(2, "delete");
            buttonAmount = 3;
            e.printStackTrace();
        }
        InlineKeyboardMarkup markup = createMarkup(buttonAmount, buttonTexts, callData);
        sendMessage(text, chatId, markup);
    }
    public void showAllGamesByRegion(long chatId, String region){
        if (userStates.get(chatId).equals("showing_games_select_region")) {
            InlineKeyboardMarkup markup = createButtonsByRegions();
            sendMessage("Please choose region, you want to play in.", chatId, markup);
        } else if (userStates.get(chatId).equals("showing_games_print")){
            try {
                Set<GameEntity> games = gameService.getAllGamesByRegion(GameRegion.parseGameRegion(region));
                String message = "Games in " + GameRegion.parseGameRegion(region).toFullString();
                int numbering = 1;
                for (GameEntity game : games) {
                    message += "\n" + numbering + ")" +
                            "\nName: " + game.getName() +
                            "\nGame type: " + game.getGameType() +
                            "\nMaster: " + game.getMaster().getMasterNickname() +
                            "\nDate: " + DateTools.parseLocalDateToString(game.getDate()) +
                            "\nTime: " + TimeTools.parseLocalTimeToString(game.getTime()) +
                            "\nPlayers: " + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                            "\nDescription: " + game.getDescription() +
                            "\n";
                    numbering++;
                }
                InlineKeyboardMarkup markup = createButtonsByGameSet(games, "choosingGameToJoin");
                sendMessage(message, chatId, markup);
            } catch (BadDataTypeException | NoSuchGameException e) {
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        }
    }
    public void joinGame(long chatId, String gameId, User actualUser){
        try {
            GameEntity game = gameService.getGameById(Long.parseLong(gameId));
            UserEntity player = userService.getUserEntity(actualUser);
            gameService.joinPlayer(player, game);
            sendMessage("You were successfully joined!", chatId, null);
            showMenu(chatId, actualUser);
        } catch (NumberFormatException e){
            sendMessage("Something went wrong. Bad game id format.", chatId, null);
            e.printStackTrace();
        } catch (NoSuchGameException e){
            sendMessage("Something went wrong. Try again later.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
        } catch (UserIsNotRegisteredException e){
            sendMessage("Something went wrong. Try again later.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
        } catch (JoinGameException e){
            sendMessage(e.getMessage(), chatId, null);
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
                playerMenu(chatId, actualUser);
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
    private InlineKeyboardMarkup createButtonsByGameSet(Set<GameEntity> games, String callDataBeginning){
        int numberingButtons = 0;
        int buttonAmount = games.size();
        Map<Integer, String> buttonTexts = new HashMap<>();
        Map<Integer, String> callData = new HashMap<>();
        for (GameEntity game : games){
            buttonTexts.put(numberingButtons, game.getName());
            callData.put(numberingButtons, callDataBeginning+"_"+game.getId());
        }
        return createMarkup(buttonAmount, buttonTexts, callData);
    }
    private InlineKeyboardMarkup createButtonsByRegions(){
        GameRegion [] regions = GameRegion.values();
        Map<Integer, String> buttonTexts = new HashMap<>();
        Map<Integer, String> callData = new HashMap<>();
        for (int i = 0; i < regions.length; i++){
            buttonTexts.put(i, regions[i].toString().toUpperCase());
            callData.put(i, "choosingGameRegion_"+regions[i].toString());
        }
        return createMarkup(regions.length, buttonTexts, callData);
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
