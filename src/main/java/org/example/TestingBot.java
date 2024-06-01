package org.example;

import com.google.common.collect.*;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
import org.example.models.services.GameService;
import org.example.models.services.PhotoService;
import org.example.models.services.UserService;
import org.example.tools.bot_tools.BadWordsFilter;
import org.example.tools.bot_tools.DateTools;
import org.example.tools.bot_tools.TimeTools;
import org.example.tools.code_tools.TraceTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    @Autowired
    private PhotoService photoService;
    private String developerPassword = "scherbakov2k14";
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> devStates = new HashMap<>();
    private final Multimap<Long, Integer> messageRecycleBin = ArrayListMultimap.create();
    private final Map<Long, Integer> pinnedMessages = new HashMap<>();
    private final Map<Long, Long> editedGamesIds = new HashMap<>();
    private final Map<Long, Long> disconnectedGamesIds = new HashMap<>();
    private final Map<Long, GameEntity> createdGamesIds = new HashMap<>();
    private final Map<Long, org.example.tools.bot_tools.Message> actualMessages = new HashMap<>();
    private final Map<Long, Set<org.example.tools.bot_tools.Message>> oldMessageCollections = new HashMap<>();
    private final Map<Long, Boolean> developerUsers = new HashMap<>();
    private final BadWordsFilter filter = new BadWordsFilter();
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            userStates.putIfAbsent(chatId, "default");
            developerUsers.putIfAbsent(chatId, false);
            pinnedMessages.remove(chatId);
            if (developerUsers.containsKey(chatId) && developerUsers.get(chatId)){
                devStates.putIfAbsent(chatId, "default");
            } else {
                devStates.remove(chatId);
            }


            emptyRecycleBin(chatId);

            if (filter.containsBadWord(messageText)){
                Set<String> badWords = filter.returnBadWords(messageText);
                String text = "You text contains filthy language. Please remove those words from your text: ";
                for (String badWord : badWords){
                    text += badWord+", ";
                }
                text = text.substring(0, text.length()-2);
                text += ".";
                sendMessage(text, chatId, null);
                return;
            }

            if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals("/menu")) {
                showMenu(chatId, actualUser);
            } else if (userStates.get(chatId).contains("creating")) {
                createGame(chatId, actualUser, messageText);
            } else if (userStates.get(chatId).contains("editing_games") && userStates.get(chatId).contains("control")) {
                editMasterGame(chatId, update, actualUser, messageText);
            } else if (messageText.equals("/developerMode") || userStates.get(chatId).equals("turning_on_developer_mode") || messageText.startsWith("/dev-") || (devStates.containsKey(chatId) && (devStates.get(chatId).equals("default") || devStates.get(chatId).equals("waiting_for_new_photo")))){
                if (messageText.equals("/developerMode")) {
                    if (!developerUsers.get(chatId)) {
                        userStates.replace(chatId, "turning_on_developer_mode");
                        sendMessage("You want to turn on developer mode. Please write a developer password:", chatId, null);
                    } else {
                        sendMessage("Developer mode is turned off.", chatId, null);
                        developerUsers.put(chatId, false);
                    }
                } else if (userStates.get(chatId).equals("turning_on_developer_mode")){
                    if (messageText.equals(developerPassword)){
                        developerUsers.put(chatId, true);
                        sendMessage("Developer mode is on!", chatId, null);
                        userStates.replace(chatId, "default");
                    } else {
                        sendMessage("Wrong developer password.", chatId, null);
                        userStates.replace(chatId, "default");
                        showMenu(chatId, actualUser);
                    }
                }
                if (devStates.get(chatId) != null) {
                    developerCommands(chatId, update, actualUser);
                }
            } else if (messageText.equals("/sendPic")){
                sendPhotoMessage(messageText, chatId, null, "database");
            } else {
                sendMessage("Something went wrong.", chatId, null);
                showMenu(chatId, actualUser);
            }

        } else if (update.hasCallbackQuery()) {
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String messageText = update.getCallbackQuery().getMessage().getText();
            emptyRecycleBin(chatId);
            userStates.putIfAbsent(chatId, "default");
            String languageCode = callData.substring(callData.length() - 2);

            if (callData.equals("register")) {
                registration(chatId, actualUser);
            } else if (callData.equals("delete") || callData.equals("deletingYes") || callData.equals("deletingNo")) {
                if (callData.equals("delete")) {
                    userStates.replace(chatId, "deleting_choice");
                }
                deletingAccount(chatId, actualUser, update);
            } else if (callData.equals("createGame") || callData.contains("creatingGameType") || callData.contains("creatingGameLanguage")) {
                if (callData.equals("createGame")) {
                    userStates.replace(chatId, "creating_game_name");
                    createGame(chatId, actualUser, messageText);
                } else if (callData.contains("creatingGameType")) {
                    String gameType;
                    if (callData.equals("creatingGameTypeCampaign")) {
                        gameType = "campaign";
                    } else {
                        gameType = "cneshot";
                    }
                    userStates.replace(chatId, "creating_game_language");
                    createGame(chatId, actualUser, gameType);
                } else if (callData.contains("creatingGameLanguage")) {
                    userStates.replace(chatId, "creating_game_price");
                    createGame(chatId, actualUser, languageCode);
                }
            } else if (callData.equals("editGames") || callData.contains("editingMasterGame_") || callData.contains("editingGame")) {
                if (callData.equals("editGames")) {
                    showMasterGames(chatId, actualUser);
                } else if (callData.contains("editingMasterGame")) {
                    userStates.replace(chatId, "editing_games_action");
                    editMasterGame(chatId, update, actualUser, messageText);
                    editedGamesIds.put(actualUser.getId(), Long.parseLong(callData.substring(18)));
                } else if (callData.contains("editingGame")) {
                    if (callData.equals("editingGameName")) {
                        userStates.replace(chatId, "editing_games_name");
                    } else if (callData.equals("editingGameDate")) {
                        userStates.replace(chatId, "editing_games_date");
                    } else if (callData.equals("editingGameTime")) {
                        userStates.replace(chatId, "editing_games_time");
                    } else if (callData.equals("editingGameType")) {
                        userStates.replace(chatId, "editing_games_type");
                    } else if (callData.equals("editingGameDescription")) {
                        userStates.replace(chatId, "editing_games_description");
                    } else if (callData.equals("editingGameMaxPlayers")) {
                        userStates.replace(chatId, "editing_games_maxplayers");
                    } else if (callData.equals("editingGameLanguage")) {
                        userStates.replace(chatId, "editing_games_language");
                    } else if (callData.contains("editingGameLanguage") && userStates.get(chatId).contains("control")) {
                        messageText = languageCode;
                    } else if (callData.equals("editingGamePrice")) {
                        userStates.replace(chatId, "editing_games_price");
                    }
                    editMasterGame(chatId, update, actualUser, messageText);
                }
            } else if (callData.equals("deletingGame")) {
                userStates.replace(chatId, "editing_games_delete");
                editMasterGame(chatId, update, actualUser, messageText);
            } else if (callData.equals("deletingGameYes") || callData.equals("deletingGameNo")) {
                if (callData.equals("deletingGameYes")) {
                    userStates.replace(chatId, "editing_games_delete_control");
                    editMasterGame(chatId, update, actualUser, messageText);
                } else {
                    userStates.replace(chatId, "default");
                    showMenu(chatId, actualUser);
                }
            } else if (callData.contains("switchTo")) {
                if (callData.equals("switchToMasterMenu")) {
                    masterMenu(chatId, actualUser);
                } else {
                    playerMenu(chatId, actualUser);
                }
            } else if (callData.equals("joinGame") || callData.contains("choosingGame")) {
                if (callData.equals("joinGame")) {
                    userStates.replace(chatId, "showing_games_select_language");
                    showAllGamesByLanguage(chatId, messageText, actualUser);
                } else if (callData.contains("choosingGame")) {
                    if (callData.contains("choosingGameLanguage")) {
                        messageText = callData.substring(callData.length() - 2);
                        userStates.replace(chatId, "showing_games_print");
                        showAllGamesByLanguage(chatId, messageText, actualUser);
                    } else if (callData.contains("choosingGameToJoin")) {
                        String[] splittedCallData = callData.split("_");
                        String gameId = splittedCallData[1];
                        joinGame(chatId, gameId, actualUser);
                    }
                }
            } else if (callData.equals("myGames") || callData.contains("userGameListChoice") || callData.contains("disconnecting")) {
                if (callData.equals("myGames")) {
                    showPlayerGames(chatId, actualUser);
                } else if (callData.contains("userGameListChoice")) {
                    disconnectedGamesIds.put(actualUser.getId(), Long.parseLong(callData.substring(callData.length() - 2)));
                    userStates.replace(chatId, "disconnecting_game_choice");
                    disconnectGame(chatId, actualUser, disconnectedGamesIds.get(actualUser.getId()));
                } else if (callData.contains("disconnecting")) {
                    if (callData.contains("Yes")) {
                        userStates.replace(chatId, "disconnecting_game_yes");
                    } else {
                        userStates.replace(chatId, "disconnecting_game_no");
                    }
                    disconnectGame(chatId, actualUser, disconnectedGamesIds.get(actualUser.getId()));
                }
            } else if (callData.equals("faq")){
                sendMessage("FAQ will be here ;)", chatId, createMarkup(1, Map.of(0, "Back"), Map.of(0, "showMenu")));
            } else if (callData.equals("aboutUs")){
                sendMessage("Information about us will be here soon ;)", chatId, createMarkup(1, Map.of(0, "Back"), Map.of(0, "showMenu")));
            } else if (callData.equals("backButton")) {
                org.example.tools.bot_tools.Message previousMessage = actualMessages.get(chatId).getPreviousMessage();
                sendObjectMessage(previousMessage);
                actualMessages.put(chatId, previousMessage);
            } else if (callData.equals("showMenu")){
                showMenu(chatId, actualUser);
            } else {
                sendMessage("Something went wrong.", chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (update.hasMessage() && update.getMessage().hasPhoto()){
            long chatId = update.getMessage().getChatId();
            User actualUser = update.getMessage().getFrom();

            if (developerUsers.get(chatId)){
                developerCommands(chatId, update, actualUser);
            } else {
                sendMessage("Something went wrong.", chatId, null);
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
    public void registration(long chatId, User actualUser){
        String text = "You are successfully registered!";
        try{
            userService.create(actualUser);
        } catch (UserAlreadyRegisteredException | BadDataException e){
            text = e.getMessage();
        }
        sendMessage(text, chatId, null);
        showMenu(chatId, actualUser);
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
                try {
                    if (userService.isMaster(actualUser)) {
                        Set<GameEntity> masterGames = gameService.getAllGamesByMaster(userService.getUserEntity(actualUser));
                        for (GameEntity game : masterGames) {
                            sendMessageToAllPlayersInGame("WARNING: A game " + game.getName() + ", leaded by master " + game.getMaster().getUsername() + " was deleted!", game);
                        }
                    }
                } catch (UserIsNotRegisteredException | MasterHaveNoGamesException e){
                }
                try {
                    Set<GameEntity> gamesByPlayer = gameService.getAllGamesByPlayer(userService.getUserEntity(actualUser));
                    for (GameEntity game : gamesByPlayer) {
                        gameService.disconnectPlayer(userService.getUserEntity(actualUser), game);
                        sendMessage("ATTENTION: Some player have disconnected your game - " + game.getName() + " on " + DateTools.parseLocalDateToString(game.getDate()) + "! Players are now " + game.getPlayers().size() + "/" + game.getMaxPlayers(), gameService.getMasterTelegramId(game), null);
                    }
                } catch (UserHaveNoGamesExcpetion | UserIsNotRegisteredException | NoSuchGameException e){
                }
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
        String text = "Hello "+actualUser.getFirstName()+", your master nickname is "+user.getUsername()+"\n" +
                "THIS IS MAIN MENU";
        InlineKeyboardMarkup markup = createMarkup(5, Map.of(0, "New game", 1, "Edit games",
                2, "Player menu", 3, "Edit Profile", 4, "Delete account"), Map.of(0, "createGame", 1, "editGames",
                2, "switchToPlayerMenu", 3, "editingProfile", 4, "delete"));
        sendMessage(text, chatID, markup);
    }
    public void createGame(long chatId, User actualUser, String messageText){
        createdGamesIds.putIfAbsent(actualUser.getId(), new GameEntity());
        GameEntity newGame = createdGamesIds.get(actualUser.getId());
        if (userStates.get(chatId).equals("creating_game_name")){
            try {
                if (gameService.canCreateNewGame(userService.getUserEntity(actualUser))) {
                    sendMessage("Please choose a unique name for your game: ", chatId, null);
                    userStates.replace(chatId, "creating_game_description");
                } else {
                    sendMessage("You already have maximum amount of created games, which is "+gameService.getMaximumGames()+". If you want to create new, please delete an old one.", chatId, null);
                    showMenu(chatId, actualUser);
                }
            }catch (UserIsNotRegisteredException e){
                sendMessage("Something went wrong. Please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
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
                userStates.replace(chatId, "creating_game_language");
            } catch (BadDataException e){
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_language")){
            if (messageText.equals("campaign")){
                newGame.setGameType(GameType.CAMPAIGN);
            } else {
                newGame.setGameType(GameType.ONESHOT);
            }
            int buttonAmount = GameLanguage.values().length;
            Map<Integer, String> buttonTexts = new HashMap<>();
            Map<Integer, String> callData = new HashMap<>();
            for (int i = 0; i < buttonAmount; i++){
                buttonTexts.put(i, GameLanguage.values()[i].toString());
                callData.put(i, "creatingGameLanguage"+ GameLanguage.values()[i].toString());
            }
            InlineKeyboardMarkup markup = createMarkup(buttonAmount, buttonTexts, callData);
            sendMessage("Please select language:", chatId, markup);
        } else if (userStates.get(chatId).equals("creating_game_price")){
            try{
                newGame.setLanguage(GameLanguage.parseGameLanguage(messageText));
                sendMessage("Please write a price for a game (in CZK): ", chatId, null);
                userStates.replace(chatId, "creating_game_date");
            }catch (BadDataTypeException e){
                sendMessage("Something went wrong! Please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("creating_game_date")){
            try {
                newGame.setPriceByString(messageText);
                sendMessage("Please choose a date for your game. Date must " +
                        "have format (dd.MM.yyyy) and be at least 1 week away but no more than 2 years away.", chatId, null);
                userStates.replace(chatId, "creating_game_time");
            } catch (BadDataException e){
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
                gameService.create(newGame.getName(), newGame.getDate(), newGame.getTime(), master, newGame.getGameType(), newGame.getDescription(), newGame.getMaxPlayers(), newGame.getLanguage(), newGame.getPrice());
                userStates.replace(chatId, "default");

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
            createdGamesIds.remove(chatId);
        }
    }
    public void showMasterGames(long chatId, User actualUser){
        try {
            UserEntity master = userService.getUserEntity(actualUser);
            Set<GameEntity> masterGames = gameService.getAllGamesByMaster(master);
            sendMessage("<b>YOUR GAMES LIST</b>", chatId, null);
            for (GameEntity game : masterGames){
                Set<UserEntity> players = game.getPlayers();
                String message = "<b>" + game.getName() + "</b>";
                message += "\nGame type: "+game.getGameType()+
                        "\nLanguage: "+game.getLanguage().toFullString()+
                        "\nDate: "+DateTools.parseLocalDateToString(game.getDate())+
                        "\nTime: "+TimeTools.parseLocalTimeToString(game.getTime())+
                        "\nPrice: "+(game.getPrice()==0 ? "free" : game.getPrice())+
                        "\nDescription: "+game.getDescription()+
                        "\nPlayers: "+players.size()+"/"+game.getMaxPlayers()+
                        "\n";
                        if (!players.isEmpty()){
                            for (UserEntity player : players){
                                message += "   - @"+player.getUsername()+"\n";
                            }
                        }
                InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "Edit game"), Map.of(0, "editingMasterGame_"+game.getId()));
                sendMessage(message, chatId, markup);

            }
        } catch (UserIsNotRegisteredException e){
            sendMessage("Something went wrong. Please try again.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
        } catch (MasterHaveNoGamesException e){
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        }
    }
    public void editMasterGame(long chatId, Update update, User actualUser, String messageText){
        Long editedGameId = editedGamesIds.get(actualUser.getId());
        if (userStates.get(chatId).equals("editing_games_action")) {
            InlineKeyboardMarkup markupLine = createMarkup(9, Map.of(0, "Edit Name", 1, "Edit Date",
                    2, "Edit Time", 3, "Edit Type", 4, "Edit Description", 5, "Edit Max Players", 6, "Edit Language",
                    7, "Edit Price", 8, "Delete Game"), Map.of(0,
                    "editingGameName", 1, "editingGameDate", 2, "editingGameTime",
                    3, "editingGameType", 4, "editingGameDescription", 5, "editingGameMaxPlayers", 6 , "editingGameLanguage",
                    7, "editingGamePrice", 8, "deletingGame"));
            Message sentMessage = sendMessage("Please choose action: ", chatId, markupLine);
            pinnedMessages.put(chatId, sentMessage.getMessageId());
        } else if (userStates.get(chatId).equals("editing_games_name")){
            sendMessage("Please write to chat new name for a game: ", chatId, null);
            userStates.replace(chatId, "editing_games_name_control");
        } else if (userStates.get(chatId).equals("editing_games_language")){
            int buttonAmount = GameLanguage.values().length;
            Map<Integer, String> buttonTexts = new HashMap<>();
            Map<Integer, String> callData = new HashMap<>();
            for (int i = 0; i < buttonAmount; i++){
                buttonTexts.put(i, GameLanguage.values()[i].toString());
                callData.put(i, "editingGameLanguage"+ GameLanguage.values()[i].toString());
            }
            InlineKeyboardMarkup markup = createMarkup(buttonAmount, buttonTexts, callData);
            sendMessage("Please select game language: ", chatId, markup);
            userStates.replace(chatId, "editing_games_language_control");
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
        } else if (userStates.get(chatId).equals("editing_games_price")) {
            sendMessage("Please write to chat new price: ", chatId, null);
            userStates.replace(chatId, "editing_games_price_control");
        } else if (userStates.get(chatId).equals("editing_games_delete")){
            InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Yes", 1, "No"),
                    Map.of(0, "deletingGameYes", 1, "deletingGameNo"));
            sendMessage("Are you sure, you want to delete this game?", chatId, markup);
        } else if (userStates.get(chatId).equals("editing_games_name_control")){
            if (gameService.gameNameIsFree(messageText)){
                try{
                    GameEntity editedGame = gameService.getGameById(editedGameId);
                    sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                            " was edited. New game name is "+messageText, editedGame);
                    gameService.changeGameData("name", messageText, editedGameId);
                    sendMessage("Name was successfully changed", chatId, null);
                    editedGamesIds.remove(actualUser.getId());
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
                        GameEntity editedGame = gameService.getGameById(editedGameId);
                        sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                                " was edited. New game date is "+messageText, editedGame);
                        gameService.changeGameData("date", messageText, editedGameId);
                        sendMessage("Date was successfully changed", chatId, null);
                        editedGamesIds.remove(actualUser.getId());
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
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. New game time is "+messageText, editedGame);
                gameService.changeGameData("time", messageText, editedGameId);
                sendMessage("Time was successfully changed.", chatId, null);
                editedGamesIds.remove(actualUser.getId());;
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
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. New game type is " + (update.getCallbackQuery().getData().equals("editingGameTypeCampaign") ? "Campaign" : "One Shot") + ".", editedGame);
                gameService.changeGameData("type", update.getCallbackQuery().getData(), editedGameId);
                sendMessage("Game type was successfully changed.", chatId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("editing_games_language_control")) {
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. New game language is "+ GameLanguage.parseGameLanguage(messageText.substring(messageText.length()-2)).toFullString()+".", editedGame);
                gameService.changeGameData("language", messageText, editedGameId);
                sendMessage("Game language was successfully changed.", chatId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("editing_games_description_control")) {
            try{
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. New game description: \n"+messageText, editedGame);
                gameService.changeGameData("description", messageText, editedGameId);
                sendMessage("Game description was successfully changed.", chatId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_maxplayers_control")){
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. New max player amount is "+messageText, editedGame);
                gameService.changeGameData("maxPlayers", messageText, editedGameId);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendMessage("Game max amount of players was successfully changed.", chatId, null);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_price_control")){
            try{
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master "+editedGame.getMaster().getUsername()+
                        " was edited. "+(Long.parseLong(messageText) == 0 ? "Game is now free!" : "New price is "+messageText), editedGame);
                gameService.changeGameData("price", messageText, editedGameId);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendMessage("Game price was successfully changed.", chatId, null);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
            }
        } else if (userStates.get(chatId).equals("editing_games_delete_control")){
            try {
                GameEntity deletedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+deletedGame.getName()+", leaded by master "+deletedGame.getMaster().getUsername()+" was deleted!", deletedGame);
                gameService.deleteGameById(editedGameId);
                editedGamesIds.remove(actualUser.getId());
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
                buttonTexts.put(3, "Edit Profile");
                buttonTexts.put(4, "Delete account");
                callData.put(2, "switchToMasterMenu");
                callData.put(3, "editingProfile");
                callData.put(4, "delete");
                buttonAmount = 5;
            } else {
                buttonTexts.put(2, "Editing Profile");
                buttonTexts.put(3, "Delete account");
                callData.put(2, "editingProfile");
                callData.put(3, "delete");
                buttonAmount = 4;
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
    public void showAllGamesByLanguage(long chatId, String language, User actualUser){
        if (userStates.get(chatId).equals("showing_games_select_language")) {
            InlineKeyboardMarkup markup = createButtonsByLanguages();
            sendMessage("Please choose language of the game.", chatId, markup);
        } else if (userStates.get(chatId).equals("showing_games_print")){
            try {
                Set<GameEntity> games = gameService.getAllGamesByLanguage(GameLanguage.parseGameLanguage(language));
                String message = "Games in " + GameLanguage.parseGameLanguage(language).toFullString();
                int numbering = 1;
                for (GameEntity game : games) {
                    message += "\n" + numbering + ")" +
                            "\nName: " + game.getName() +
                            "\nGame type: " + game.getGameType() +
                            "\nMaster: @" + game.getMaster().getUsername() +
                            "\nDate: " + DateTools.parseLocalDateToString(game.getDate()) +
                            "\nTime: " + TimeTools.parseLocalTimeToString(game.getTime()) +
                            "\nPlayers: " + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                            "\nPrice: " + (game.getPrice()==0 ? "free" : game.getPrice())+
                            "\nDescription: " + game.getDescription() +
                            "\n";
                    numbering++;
                }
                InlineKeyboardMarkup markup = createButtonsByGameSet(games, "choosingGameToJoin");
                sendMessage(message, chatId, markup);
            } catch (BadDataTypeException e) {
                sendMessage("Something went wrong, please try again.", chatId, null);
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            } catch (NoSuchGameException e){
                sendMessage(e.getMessage(), chatId, null);
                showMenu(chatId, actualUser);
            }
        }
    }
    public void joinGame(long chatId, String gameId, User actualUser){
        try {
            GameEntity game = gameService.getGameById(Long.parseLong(gameId));
            UserEntity player = userService.getUserEntity(actualUser);
            gameService.joinPlayer(player, game);
            sendMessage("You were successfully joined!", chatId, null);
            sendMessage("ATTENTION: A new player have joined your game - "+game.getName()+" on "+DateTools.parseLocalDateToString(game.getDate())+"! Players are now "+game.getPlayers().size()+"/"+game.getMaxPlayers(), gameService.getMasterTelegramId(game), null);
            showMenu(chatId, actualUser);
        } catch (NumberFormatException e){
            sendMessage("Something went wrong. Bad game id format.", chatId, null);
            e.printStackTrace();
        } catch (NoSuchGameException | JoinGameException e){
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        } catch (UserIsNotRegisteredException e){
            sendMessage("Something went wrong. Try again later.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
        }
    }
    public void showPlayerGames(long chatId, User actualUser){
        try {
            UserEntity user = userService.getUserEntity(actualUser);
            Set<GameEntity> userGames = gameService.getAllGamesByPlayer(user);
            String message = "You've join to: ";
            int numbering = 1;
            for (GameEntity game : userGames){
                message += "\n"+numbering+")"+
                        "\nName: " + game.getName() +
                        "\nGame type: " + game.getGameType() +
                        "\nMaster: @" + game.getMaster().getUsername() +
                        "\nDate: " + DateTools.parseLocalDateToString(game.getDate()) +
                        "\nTime: " + TimeTools.parseLocalTimeToString(game.getTime()) +
                        "\nPrice: " + (game.getPrice()==0 ? "free" : game.getPrice())+
                        "\nDescription: " + game.getDescription() +
                        "\nPlayers: " + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                        "\n";
                numbering++;
            }
            message += "\nChoose a game you want to disconnect";

            InlineKeyboardMarkup markup = createButtonsByGameSet(userGames, "userGameListChoice");
            sendMessage(message, chatId, markup);

        } catch (UserIsNotRegisteredException e){
            sendMessage("Something went wrong! Please try again.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        } catch (UserHaveNoGamesExcpetion e){
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        }
    }
    public void disconnectGame(long chatId, User actualUser, Long gameId){
        try {
            GameEntity disconnectedGame = gameService.getGameById(gameId);
            if (userStates.get(chatId).equals("disconnecting_game_choice")) {
                InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Yes", 1, "No"), Map.of(0, "disconnectingYes", 1, "disconnectingNo"));
                sendMessage("Do you really want to leave this game?", chatId, markup);
            } else if (userStates.get(chatId).equals("disconnecting_game_yes")) {
                try {
                    gameService.disconnectPlayer(userService.getUserEntity(actualUser), disconnectedGame);
                    sendMessage("You were disconnected.", chatId, null);
                    sendMessage("ATTENTION: Some player have disconnected your game - "+disconnectedGame.getName()+" on "+DateTools.parseLocalDateToString(disconnectedGame.getDate())+"! Players are now "+disconnectedGame.getPlayers().size()+"/"+disconnectedGame.getMaxPlayers(), gameService.getMasterTelegramId(disconnectedGame), null);
                    showMenu(chatId, actualUser);
                } catch (UserIsNotRegisteredException e) {
                    sendMessage("Something went wrong! Please try again later.", chatId, null);
                    sendMessage(e.getMessage(), chatId, null);
                    showMenu(chatId, actualUser);
                }
            } else if (userStates.get(chatId).equals("disconnecting_game_no")) {
                showMenu(chatId, actualUser);
            }
        }catch (NoSuchGameException e){
            sendMessage("Something went wrong! Please try again later.", chatId, null);
            sendMessage(e.getMessage(), chatId, null);
            showMenu(chatId, actualUser);
        }
    }


    public void developerCommands(long chatId, Update update, User actualUser) {
        boolean containsPhotos = update.getMessage().hasPhoto();
        System.out.println(containsPhotos);
        String messageText;
        if (containsPhotos){
            messageText = update.getMessage().getCaption();
        } else {
            messageText = update.getMessage().getText();
        }

        if (containsPhotos){
            List<byte[]> receivedImages = new ArrayList<>();
            List<PhotoSize> photos = update.getMessage().getPhoto();
            System.out.println("PhotoSize collection size: "+photos.size());
            String photoId = photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getFileId();
                GetFile getFileMethod = new GetFile();
                getFileMethod.setFileId(photoId);
                try {
                    org.telegram.telegrambots.meta.api.objects.File file = execute(getFileMethod);
                    InputStream inputStream = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath()).openStream();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] imageBytes = outputStream.toByteArray();
                    receivedImages.add(imageBytes);
                } catch (TelegramApiException | IOException e){
                    e.printStackTrace();
                }

            System.out.println("Received images collection size: "+receivedImages.size());
            try {
                photoService.newPhotosFromMessage(messageText, receivedImages);
                sendMessage("Images were successfully uploaded to database.", chatId, null);
                devStates.put(chatId, "default");
            } catch (DeveloperException e){
                sendMessage(e.getMessage(), chatId, null);
                devStates.put(chatId, "default");
            }

        } else if (messageText.equals("/dev-newPhoto") || (devStates.get(chatId).equals("waiting_for_new_photo") && !containsPhotos)) {
            if (messageText.equals("/dev-newPhoto")){
                sendMessage("Send a photos, which you want to add to database. Write names and descriptions for it." +
                        " It must look like - photoName. photoDescription; otherPhotoName. otherPhotoDescription", chatId, null);
                devStates.put(chatId, "waiting_for_new_photo");
                System.out.println("devState "+devStates.get(chatId));
            } else if (devStates.get(chatId).equals("waiting_for_new_photo")){
                sendMessage("No photos in message!", chatId, null);
                devStates.put(chatId, "default");
            }
        }
    }


    private void emptyRecycleBin(long chatId){
        int pinnedMessageId = 0;
        for (Integer messageId : messageRecycleBin.get(chatId)){
            if (pinnedMessages.get(chatId) != null) {
                if (pinnedMessages.get(chatId).equals(messageId)) {
                    pinnedMessageId = messageId;
                }
            }else {
                System.out.println("Message id: "+messageId);
                deleteMessage(chatId, messageId);
            }
        }
        messageRecycleBin.removeAll(chatId);
        messageRecycleBin.put(chatId, pinnedMessageId);
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
    private Message sendMessage(String messageText, long chatId, InlineKeyboardMarkup markup){
        org.example.tools.bot_tools.Message actualMessage = new org.example.tools.bot_tools.Message(messageText, chatId, markup, TraceTools.actualMessageIsMenu());
        SendMessage sendMessage = new SendMessage();
        if (markup != null) {
            if (TraceTools.actualMessageIsMenu()){
                actualMessages.put(chatId, actualMessage);
                oldMessageCollections.remove(chatId);
            } else {
                org.example.tools.bot_tools.Message oldMessage = actualMessages.get(chatId);
                actualMessage.setPreviousMessage(oldMessage);
                addBackButtonToMessage(actualMessage);
                actualMessages.put(chatId, actualMessage);
                if (oldMessageCollections.containsKey(chatId)){
                    oldMessageCollections.get(chatId).add(oldMessage);
                } else {
                    Set<org.example.tools.bot_tools.Message> messageSet = new HashSet<>();
                    messageSet.add(oldMessage);
                    oldMessageCollections.put(chatId, messageSet);
                }
            }
            sendMessage.setReplyMarkup(actualMessage.getMarkup());
        }
        sendMessage.setChatId(actualMessage.getChatId());
        sendMessage.setText(actualMessage.getText());
        sendMessage.setParseMode("HTML");
        try{
            Message sentMessage = execute(sendMessage);
            messageRecycleBin.put(chatId, sentMessage.getMessageId());
            return sentMessage;
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
        return null;
    }
    private void sendObjectMessage (org.example.tools.bot_tools.Message message){
        String messageText = message.getText();
        long chatId = message.getChatId();
        InlineKeyboardMarkup markup = message.getMarkup();
        if (message.isMenu()){
            oldMessageCollections.remove(chatId);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);
        if (markup != null){
            sendMessage.setReplyMarkup(markup);
        }
        try{
            Message sentMessage = execute(sendMessage);
            messageRecycleBin.put(chatId, sentMessage.getMessageId());
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    private void sendMessageToAllPlayersInGame(String messageText, GameEntity game){
        Set<UserEntity> players = game.getPlayers();
        for (UserEntity player : players){
            sendMessage(messageText, player.getTelegramId(), null);
        }
    }
    private void showMenu(long chatId, User actualUser){
        try {
            UserEntity user = userService.getUserEntity(actualUser);
            userStates.replace(chatId, "default");
            String text = "Hello, " + user.getUsername() + ".\n" +
                    "MAIN MENU";
            InlineKeyboardMarkup markup = createMarkup(6, Map.of(0, "Create game", 1, "Show my games",
                    2, "Join game", 3, "Games I've joined", 4, "FAQ", 5, "About us"), Map.of(0, "createGame", 1, "editGames",
                    2, "joinGame", 3, "myGames", 4, "faq", 5, "aboutUs"));
            sendMessage(text, chatId, markup);
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
            numberingButtons++;
        }
        return createMarkup(buttonAmount, buttonTexts, callData);
    }
    private InlineKeyboardMarkup createButtonsByLanguages(){
        GameLanguage[] languages = GameLanguage.values();
        Map<Integer, String> buttonTexts = new HashMap<>();
        Map<Integer, String> callData = new HashMap<>();
        for (int i = 0; i < languages.length; i++){
            buttonTexts.put(i, languages[i].toString().toUpperCase());
            callData.put(i, "choosingGameLanguage_"+languages[i].toString());
        }
        return createMarkup(languages.length, buttonTexts, callData);
    }
    private void addBackButtonToMessage(org.example.tools.bot_tools.Message message){
        List<List<InlineKeyboardButton>> rowsInLine = message.getMarkup().getKeyboard();
        boolean selectOptionsWhenCreatingOrEditingGame = message.getText().contains("language") || message.getText().contains("game type");
        boolean containsBackButton = false;
        boolean containsNoButton = false;
        for (List row : rowsInLine){
            for (Object actual : row){
                InlineKeyboardButton button = (InlineKeyboardButton) actual;
                if (button.getText().equals("Back")){
                    containsBackButton = true;
                } else if (button.getText().equalsIgnoreCase("no")){
                    containsNoButton = true;
                }
            }
        }
        if (!containsBackButton && !containsNoButton && !TraceTools.traceContainsMethod("registration") && !selectOptionsWhenCreatingOrEditingGame) {
            List<InlineKeyboardButton> lastRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setCallbackData("backButton");
            backButton.setText("Back");
            lastRow.add(backButton);
            rowsInLine.add(lastRow);
        }
    }
    private void removeBackButtonFromMarkup (InlineKeyboardMarkup markup){
        List<List<InlineKeyboardButton>> rowsInLine = markup.getKeyboard();
        boolean containsBackButton = false;
        for (List row : rowsInLine){
            for (Object actual : row){
                InlineKeyboardButton button = (InlineKeyboardButton) actual;
                if (button.getText().equals("Back")){
                    containsBackButton = true;
                }
            }
        }
        if (containsBackButton){
            rowsInLine.remove(rowsInLine.size()-1);
        }
    }
    private void printAllButtonsTexts (InlineKeyboardMarkup markup){
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        System.out.println("Markup buttons texts:");
        for (List<InlineKeyboardButton> row : keyboard){
            for (InlineKeyboardButton button : row){
                System.out.println(button.getText());
            }
        }
    }
    private void sendPhotoMessage (String messageText, long chatId, InlineKeyboardMarkup markup, String photoFilePath){
        if (photoFilePath.equals("database")){
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(photoService.getPhoto());
            sendPhoto.setChatId(chatId);
            try {
                Message message = execute(sendPhoto);
                messageRecycleBin.put(chatId, message.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(new File(photoFilePath)));
            sendPhoto.setCaption(messageText);
            sendPhoto.setChatId(chatId);
            sendPhoto.setReplyMarkup(markup);
            try {
                Message message = execute(sendPhoto);
                messageRecycleBin.put(chatId, message.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    @Scheduled(fixedRate = 3600000, initialDelay = 6000)
    private void setUpcomingExpiredGames (){
        gameService.setUpcomingExpiredGames();
    }
    @Scheduled(fixedRate = 60000, initialDelay = 12000)
    private void removeExpiredGames (){
        try {
            for (GameEntity game : gameService.getUpcomingExpiredGames()){
                sendMessageToAllPlayersInGame("Game - "+game.getName()+" was expired and removed.", game);
                sendMessage("Game - "+game.getName()+", leaded by you, was expired and removed.", game.getMaster().getTelegramId(), null);
            }
            gameService.removeExpiredGames();
        } catch(NoSuchGameException e){

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
