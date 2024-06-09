package org.example;

import com.google.common.collect.*;
import jakarta.transaction.Transactional;
import org.example.data.entities.FeedbackMessages;
import org.example.data.entities.GameEntity;
import org.example.data.entities.enums.GameLanguage;
import org.example.data.entities.enums.GameType;
import org.example.data.entities.UserEntity;
import org.example.models.exceptions.*;
import org.example.models.services.FeedbackService;
import org.example.models.services.GameService;
import org.example.models.services.PhotoService;
import org.example.models.services.UserService;
import org.example.tools.bot_tools.BadWordsFilter;
import org.example.tools.bot_tools.DateTools;
import org.example.tools.bot_tools.TextTools;
import org.example.tools.bot_tools.TimeTools;
import org.example.tools.code_tools.TraceTools;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.lang.reflect.Array;
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
    @Autowired
    private FeedbackService feedbackService;
    private String developerPassword = "scherbakov2k14";
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> devStates = new HashMap<>();
    private final Multimap<Long, Integer> botMessageRecycleBin = ArrayListMultimap.create();
    private final Map<Long, Integer> userMessageRecycleBin = new HashMap<>();
    private final Map<Long, Long> editedGamesIds = new HashMap<>();
    private final Map<Long, Long> disconnectedGamesIds = new HashMap<>();
    private final Map<Long, GameEntity> createdGamesIds = new HashMap<>();
    private final Map<Long, Set<org.example.tools.bot_tools.Message>> actualMessages = new HashMap<>();
    private final Map<Long, Deque<Set<org.example.tools.bot_tools.Message>>> oldMessages = new HashMap<>();
    private final Map<Long, Boolean> developerUsers = new HashMap<>();
    private final Map<Long, Set<String>> processedMediaGroups = new HashMap<>();
    private final Map<Long, Map<Integer, String>> pollCollection = new HashMap<>();
    private final BadWordsFilter filter = new BadWordsFilter();
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            handleMessageSystemOnUpdate(chatId);
            User actualUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText();
            userStates.putIfAbsent(chatId, "default");
            developerUsers.putIfAbsent(chatId, false);
            if (processedMediaGroups.get(chatId) != null){
                processedMediaGroups.remove(chatId);
            }
            if (developerUsers.containsKey(chatId) && developerUsers.get(chatId)){
                devStates.putIfAbsent(chatId, "default");
            } else {
                devStates.remove(chatId);
            }



            emptyBotRecycleBin(chatId);
            if (userMessageRecycleBin.get(chatId) != null) {
                emptyUserRecycleBin(chatId);
            }
            userMessageRecycleBin.put(chatId, update.getMessage().getMessageId());

            if (filter.containsBadWord(messageText)){
                Set<String> badWords = filter.returnBadWords(messageText);
                String text = "You text contains filthy language. Please remove those words from your text: ";
                for (String badWord : badWords){
                    text += badWord+", ";
                }
                text = text.substring(0, text.length()-2);
                text += ".";
                sendMessage(text, chatId, null, false, false);
                return;
            }

            if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals("/menu")) {
                showMenu(chatId, actualUser);
            } else if (userStates.get(chatId).contains("creating")) {
                createGame(chatId, actualUser, messageText, update);
            } else if (userStates.get(chatId).contains("editing_games") && userStates.get(chatId).contains("control")) {
                editMasterGame(chatId, update, actualUser, messageText, null);
            } else if (userStates.get(chatId).equals("waiting_for_feedback")){
                handleFeedback(messageText, actualUser, chatId);
            } else if (messageText.equals("/developerMode") || userStates.get(chatId).equals("turning_on_developer_mode") || messageText.startsWith("/dev-") || (devStates.containsKey(chatId) && (devStates.get(chatId).equals("default") || devStates.get(chatId).equals("waiting_for_new_photo")))){
                if (messageText.equals("/developerMode")) {
                    if (!developerUsers.get(chatId)) {
                        userStates.replace(chatId, "turning_on_developer_mode");
                        sendMessage("You want to turn on developer mode. Please write a developer password:", chatId, null, false, false);
                    } else {
                        sendMessage("Developer mode is turned off.", chatId, null, false, false);
                        developerUsers.put(chatId, false);
                    }
                } else if (userStates.get(chatId).equals("turning_on_developer_mode")){
                    if (messageText.equals(developerPassword)){
                        developerUsers.put(chatId, true);
                        sendMessage("Developer mode is on!", chatId, null, false, false);
                        userStates.replace(chatId, "default");
                    } else {
                        sendMessage("Wrong developer password.", chatId, null, false, false);
                        userStates.replace(chatId, "default");
                        showMenu(chatId, actualUser);
                    }
                }
                if (devStates.get(chatId) != null) {
                    developerCommands(chatId, update, actualUser);
                }
            } else if (messageText.equals("/ex")){
                handleExpiratedGames();
            } else {
                sendMessage("Something went wrong.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            }



        } else if (update.hasCallbackQuery()) {
            User actualUser = update.getCallbackQuery().getFrom();
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (!callData.equals("backButton")) {
                handleMessageSystemOnUpdate(chatId);
            }
            String messageText = update.getCallbackQuery().getMessage().getText();
            emptyBotRecycleBin(chatId);
            userStates.putIfAbsent(chatId, "default");
            String languageCode = callData.substring(callData.length() - 2);
            if (processedMediaGroups.get(chatId) != null){
                processedMediaGroups.remove(chatId);
            }



            if (callData.equals("register")) {
                registration(chatId, actualUser);
            } else if (callData.equals("delete") || callData.equals("deletingYes") || callData.equals("deletingNo")) {
                if (callData.equals("delete")) {
                    userStates.replace(chatId, "deleting_choice");
                }
                deletingAccount(chatId, actualUser, update);
            } else if (callData.equals("createGame") || callData.contains("creatingGameType") || callData.contains("creatingGameLanguage") || callData.contains("creatingGame")) {
                if (callData.equals("createGame")) {
                    userStates.replace(chatId, "creating_game_start");
                    createGame(chatId, actualUser, messageText, update);
                } else if (callData.contains("creatingGameType")) {
                    String gameType;
                    if (callData.equals("creatingGameTypeCampaign")) {
                        gameType = "campaign";
                    } else if (callData.equals("creatingGameTypeRealLifeGame")){
                        gameType = "realLifeGame";
                    } else {
                        gameType = "oneshot";
                    }
                    userStates.replace(chatId, "creating_game_language");
                    createGame(chatId, actualUser, gameType, update);
                } else if (callData.contains("creatingGameLanguage")) {
                    userStates.replace(chatId, "creating_game_price");
                    createGame(chatId, actualUser, languageCode, update);
                } else {
                    if (callData.equals("creatingGameYes")){
                        userStates.replace(chatId, "creating_game_final");
                        createGame(chatId, actualUser, "", update);
                    } else {
                        showMenu(chatId, actualUser);
                    }
                }
            } else if (callData.equals("editGames") || callData.contains("editingMasterGame_") || callData.contains("editingGame")) {
                if (callData.equals("editGames")) {
                    showMasterGames(chatId, actualUser);
                } else if (callData.contains("editingMasterGame")) {
                    userStates.replace(chatId, "editing_games_action");
                    editMasterGame(chatId, update, actualUser, messageText, null);
                    editedGamesIds.put(actualUser.getId(), Long.parseLong(callData.substring(18)));
                    System.out.println("Edited games put id: "+editedGamesIds.get(chatId));
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
                    } else if (callData.equals("editingGameRoleSystem")){
                        userStates.replace(chatId, "editing_games_rolesystem");
                    } else if (callData.equals("editingGameGenre")){
                        userStates.replace(chatId, "editing_games_genre");
                    } else if (callData.equals("editingGameLanguage")) {
                        userStates.replace(chatId, "editing_games_language");
                    } else if (callData.contains("editingGameLanguage") && userStates.get(chatId).contains("control")) {
                        messageText = languageCode;
                    } else if (callData.equals("editingGamePrice")) {
                        userStates.replace(chatId, "editing_games_price");
                    } else if (callData.equals("editingGameImage")){
                        userStates.replace(chatId, "editing_games_image");
                    }
                    editMasterGame(chatId, update, actualUser, messageText, null);
                }
            } else if (callData.equals("deletingGame")) {
                userStates.replace(chatId, "editing_games_delete");
                editMasterGame(chatId, update, actualUser, messageText, null);
            } else if (callData.equals("deletingGameYes") || callData.equals("deletingGameNo")) {
                if (callData.equals("deletingGameYes")) {
                    userStates.replace(chatId, "editing_games_delete_control");
                    editMasterGame(chatId, update, actualUser, messageText, null);
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
                    disconnectedGamesIds.put(actualUser.getId(), Long.parseLong(callData.split("_")[1]));
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
                sendPhotoMessageWithFilePath("<u>Notification of service rules</u> \n\n<b><i>Sir "+actualUser.getUserName()+", let me notify you of some rules that we want all of our players to understand to before you start using our service!</i></b> \n\nIt is very important to us that people come to the games they have registered for. If only two people don't come to a game for which four people have registered, the game will most likely not be able to happen at all and the participants will just have to go home. We do not want to allow this outcome to happen <u>under any circumstances</u>. \n\nTherefore, we have to restrict access to the service to players who register for games and then don't show up. \n\n<b>(!) Be aware that if you do not come to a game you registered for or cancel your registration for a game less than 3 days before the game, you will have your reputation reduced. If your reputation reaches 0, you will be banned. (!)</b> \n\nThank you for your understanding and have a good game!",
                        chatId, null, true, true, "d/soldier");
            } else if (callData.equals("aboutUs")){
                sendPhotoMessageWithFilePath("<b>Who are we? What kind of team are we?</b> \n\nWe are a small team of developers rallying around the idea of helping players from different cities or countries to gather and play tabletop role-playing games together. \n\nA huge number of people want to join this hobby, but can't find like-minded people in their city. A very common and very sad situation. \n\nAnd we will fix it! \n\nOur service is designed to help you find other players quickly, easily and without unnecessary movements. And we promise to do everything that depends on us, so that you can as soon as possible immerse yourself in the world of role-playing board games without unnecessary difficulties! \n\n<i>Regards,</i> \n<i>Dungeon Location team</i> \n\n*Above you can see our best photo.",
                        chatId, null, true, true, "d/ourTeam");
            } else if (callData.equals("backButton")) {
                Set<org.example.tools.bot_tools.Message> previousMessages = oldMessages.get(chatId).getFirst();
                oldMessages.get(chatId).removeFirst();
                for (org.example.tools.bot_tools.Message message : previousMessages) {
                    sendObjectMessage(message);
                }
                actualMessages.put(chatId, previousMessages);

            } else if (callData.equals("usernameException")){
                registration(chatId, actualUser);
            } else if (callData.equals("showMenu")){
                showMenu(chatId, actualUser);
            } else if (callData.equals("sendFeedback")){
                userStates.replace(chatId, "waiting_for_feedback");
                sendPhotoMessageWithFilePath("<u>Feedback</u>" +
                "\n\n<b>Please give us your opinion!</b>" +
                        "\n\nIn the process of testing, we are <b>actively</b> analyzing feedback from our testers. " +
                        "\n\nPlease write us <b>anything</b> you would like to say about our service! We will read your messages <b>literally instantly</b> by <b>our whole team</b>." +
                        "\n\nMaybe you have discovered a bug of some kind? You don't like some illustration we use? Did you find some menu unintuitive? " +
                        "\n\n<b>Write about it in the message box and send it to us!</b>", chatId, null, true, true,"d/waitress");
            } else if (callData.contains("masterFeedback")){
                Long gameId = Long.parseLong(callData.split("_")[0]);
                if (callData.contains("masterFeedbackFirst")){
                    if (callData.contains("masterFeedbackFirstYes")){
                        userStates.replace(chatId, "master_feedback_second");
                        masterFeedback(gameId, chatId);
                    } else {
                        userStates.replace(chatId, "default");
                        masterFeedback(gameId, chatId);
                        showMenu(chatId, actualUser);
                    }
                } else if (callData.contains("masterFeedbackSecond")){
                    if (callData.contains("masterFeedbackSecondYes")){
                        userStates.replace(chatId, "default");
                        masterFeedback(gameId, chatId);
                        showMenu(chatId, actualUser);
                    } else {
                        try {
                            if (gameService.getGameById(gameId).getPlayers().size() > 1) {
                                userStates.replace(chatId, "master_feedback_third");
                                masterFeedback(gameId, chatId);
                            } else {
                                userStates.replace(chatId, "default");
                                masterFeedback(gameId, chatId);
                            }
                        } catch (NoSuchGameException e){
                            standardError(e, chatId);
                        }
                    }
                }
            } else if (callData.equals("startCreatingGame")){
                userStates.replace(chatId, "creating_game_name");
                createGame(chatId, actualUser, messageText, update);
            } else {
                sendMessage("Something went wrong.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            }



        } else if (update.hasMessage() && update.getMessage().hasPhoto()){
            long chatId = update.getMessage().getChatId();
            handleMessageSystemOnUpdate(chatId);
            User actualUser = update.getMessage().getFrom();
            if (!processedMediaGroups.containsKey(chatId)) {
                processedMediaGroups.put(chatId, new HashSet<>());
            }
            if (developerUsers.get(chatId) != null){
                if (developerUsers.get(chatId)){
                    developerCommands(chatId, update, actualUser);
                }
            } if (userStates.get(chatId).equals("creating_game_control")){
                String mediaGroupId = update.getMessage().getMediaGroupId();
                if (mediaGroupId != null){
                    if (!processedMediaGroups.get(chatId).contains(mediaGroupId)){
                        sendMessage("You must send only one image!", chatId, null, false, false);
                        processedMediaGroups.get(chatId).add(mediaGroupId);
                    }
                } else {
                    if (userStates.get(chatId).equals("creating_game_control")) {
                        createGame(chatId, actualUser, update.getMessage().getCaption(), update);
                    }
                }
            } else if (userStates.get(chatId).contains("editing_games") && userStates.get(chatId).contains("control")) {
                List<PhotoSize> photo = update.getMessage().getPhoto();
                String photoId = photo.stream()
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
                    editMasterGame(chatId, update, actualUser, null, imageBytes);
                } catch (TelegramApiException | IOException e){
                    e.printStackTrace();
                }
            }
        } else if (update.hasPollAnswer()){
            long chatId = update.getPollAnswer().getUser().getId();
            handleMessageSystemOnUpdate(chatId);
            handlePollAnswer(update.getPollAnswer(), chatId);
            userStates.replace(chatId, "default");
            masterFeedback(null, chatId);
            showMenu(chatId, update.getPollAnswer().getUser());

        }
    }


    public void mainMenu(long chatId, User actualUser){
        userStates.replace(chatId, "default");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();

        InlineKeyboardButton registrationButton = new InlineKeyboardButton();
        registrationButton.setText("Accept");
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
        sendPhotoMessageWithFilePath("<u>Username access</u>\n" +
                "\n" +
                "<b><i>To start working in the Dungeon.Location bot, confirm that you accept that your Telegram username will be visible to other users within our service.</i></b>\n" +
                "\n" +
                "This is necessary for other users to be able to contact you.", chatId, markupInline, false, false, "d/writer");
    }
    public void registration(long chatId, User actualUser){
        String text = "You are successfully registered!";
        try{
            userService.create(actualUser);
            showMenu(chatId, actualUser);
        } catch (UserAlreadyRegisteredException | BadDataException e){
            text = e.getMessage();
            InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "Now I have a username"), Map.of(0, "usernameException"));
            sendPhotoMessageWithFilePath(text, chatId, markup, false, false, "d/instructions");
        }
    }
    public void deletingAccount(long chatId, User actualUser, Update update){
        if (userStates.get(chatId).equals("deleting_choice")) {
            String text = "Are you sure, you want to delete your account?";
            InlineKeyboardMarkup markupInline = createMarkup(2, Map.of(0, "YES", 1, "NO"),
                    Map.of(0, "deletingYes", 1, "deletingNo"));
            sendMessage(text, chatId, markupInline, false, false);
            userStates.replace(chatId, "deleting_answer");
        } else if (userStates.get(chatId).equals("deleting_answer")){
            String callData = update.getCallbackQuery().getData();
            if (callData.equals("deletingYes")){
                String answer = "Your account was deleted.";
                try {
                    if (userService.isMaster(actualUser)) {
                        Set<GameEntity> masterGames = gameService.getAllGamesByMaster(userService.getUserEntity(actualUser));
                        for (GameEntity game : masterGames) {
                            sendMessageToAllPlayersInGame("WARNING: A game " + game.getName() + ", leaded by master @" + game.getMaster().getUsername() + " was deleted!", game);
                        }
                    }
                } catch (UserIsNotRegisteredException | MasterHaveNoGamesException e){
                }
                try {
                    Set<GameEntity> gamesByPlayer = gameService.getAllGamesByPlayer(userService.getUserEntity(actualUser));
                    for (GameEntity game : gamesByPlayer) {
                        gameService.disconnectPlayer(userService.getUserEntity(actualUser), game);
                        sendMessage("ATTENTION: @"+actualUser.getUserName()+" have disconnected your game - " + game.getName() + " on " + DateTools.parseLocalDateToString(game.getDate()) + "! Players are now " + game.getPlayers().size() + "/" + game.getMaxPlayers(), gameService.getMasterTelegramId(game), null, false, false);
                    }
                } catch (UserHaveNoGamesExcpetion | UserIsNotRegisteredException | NoSuchGameException e){
                }
                try{
                    userService.delete(actualUser);
                } catch (UserIsNotRegisteredException e){
                    answer = e.getMessage();
                }

                sendMessage(answer, chatId, null, false, false);
                userStates.replace(chatId,"default");
                showMenu(chatId, actualUser);
            }else if (callData.equals("deletingNo")){
                String answer = "Your account wasn't deleted.";
                sendMessage(answer, chatId, null, false, false);
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
        sendMessage(text, chatID, markup, false, true);
    }
    public void createGame(long chatId, User actualUser, String messageText, Update update){
        createdGamesIds.putIfAbsent(actualUser.getId(), new GameEntity());
        GameEntity newGame = createdGamesIds.get(actualUser.getId());
        if (userStates.get(chatId).equals("creating_game_name")){
            try {
                if (gameService.canCreateNewGame(userService.getUserEntity(actualUser))) {
                    sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What will your game be called?</i></b> \n\nWrite the title in the message box and then submit it! \n\nExample title: \"<i>Dark Dungeon</i>\"",
                            chatId, null, false, false, "d/wizard");
                    userStates.replace(chatId, "creating_game_description");
                } else {
                    sendMessage("You already have maximum amount of created games, which is "+gameService.getMaximumGames()+". If you want to create new, please delete an old one.", chatId, null, false, false);
                    showMenu(chatId, actualUser);
                }
            }catch (UserIsNotRegisteredException e){
                standardError(e, chatId);
                showMenu(chatId, actualUser);
            }
        } else if (userStates.get(chatId).equals("creating_game_start")){
            InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "OK, start creating game"), Map.of(0, "startCreatingGame"));
            sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>Welcome to the game creation menu!</i></b> \n\nAfter registration, your game will appear in the list of scheduled games in the language region you specified when creating the game. \n\nWhen a player connects to the game, you will receive a message inside the bot, where the contact information of this player will be written, so you can immediately write to this person and negotiate with him about the details. \n\nAlso the contacts of the players who have joined the game will be indicated on the card of your game in the “My scheduled games” menu.",
                    chatId, markup, true, true, "d/wizard");
        } else if (userStates.get(chatId).equals("creating_game_description")){
            if (!gameService.gameNameIsFree(messageText)){
                sendMessage("This game name is already used. Please choose other name: ", chatId, null, false, false);
            } else {
                try {
                    newGame.setName(messageText);
                    sendPhotoMessageWithFilePath("<u>Game Creation </u> \n\n<b><i>Enter a description of the game</i></b> \n\nDescribe your game in the message box and send it! \n\nThe game description will be on the game card under the game name. It is usually one or two paragraphs long. The maximum length of the description is 650 characters. \n\nAn example of a good sized game description: \n<i>“You are hired to investigate a mysterious disappearance of people in a major city. The authorities are silent, people from the surrounding villages are in fear. The city itself is full of rumors, but how much are they connected to the incidents? Who is behind the missing people? And what's going on in the once calm and quiet town?”</i>",
                            chatId, null, false, false, "d/wizard");
                    userStates.replace(chatId, "creating_game_maxplayers");
                } catch (BadDataException e){
                    standardError(e, chatId);
                }
            }
        } else if (userStates.get(chatId).equals("creating_game_maxplayers")){
            try{
                newGame.setDescription(messageText);
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What is the maximum number of players that can come to the game?</i></b> \n\nWrite the number in the message and send it (2-10)!",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_rolesystem");
            }catch (BadDataException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_rolesystem")){
            try{
                newGame.setMaxPlayersByString(messageText);
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What roleplaying system are you going to use?</i></b> \n\nWrite the name of that roleplaying system and the edition number in the message box, then submit it.",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_genre");
            } catch (BadDataException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_genre")){
            try{
                newGame.setRoleSystem(messageText);
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What genre or genres will the game belong to?</i></b> \n\nWrite in the message box all the genres your game will belong to and submit it. \n\nFor example: “<i>Horror, Mystic, Dark Fantasy</i>”.",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_type");
            } catch (BadDataException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_type")){
            try {
                newGame.setGenre(messageText);
                InlineKeyboardMarkup markup = createMarkup(3, Map.of(0, "Campaign", 1, "One shot", 2, "LARP"),
                        Map.of(0, "creatingGameTypeCampaign", 1, "creatingGameTypeOneshot", 2, "creatingGameTypeRealLifeGame"));
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What format will the game be in? Will it be a one shoot or part of a campaign? Or will it be a LARP?</i></b> \n\nChoose one of the options!",
                        chatId, markup, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_language");
            } catch (BadDataException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_language")){
            if (messageText.equals("campaign")){
                newGame.setGameType(GameType.CAMPAIGN);
            } else if (messageText.equals("realLifeGame")){
                newGame.setGameType(GameType.REAL_LIFE_GAME);
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
            sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What language will the game be played in?</i></b> \n\nThis determines which language list your game will be in. Players looking for games in Czech will not see a game that will be played in English. \n\nChoose one of the options below.",
                    chatId, markup, false, false, "d/wizard");
        } else if (userStates.get(chatId).equals("creating_game_price")){
            try{
                newGame.setLanguage(GameLanguage.parseGameLanguage(messageText));
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>Now for the financial question! How much will the player have to pay to participate in the game?</i></b> \n\n<b><i>If you want the game to be free, specify a cost of 0.</i></b> \n\nWrite the amount in the message input field and send it.",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_date");
            }catch (BadDataTypeException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_date")){
            try {
                newGame.setPriceByString(messageText);
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>What date are you planning to play?</i></b> \n\nWrite the date in the message box and send it. It should be written in the format <b>dd.mm.yyyy</b> \n\nFor example: <i>30.12.2024</i>",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_time");
            } catch (BadDataException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("creating_game_time")){
            try {
                if (!DateTools.controlDate(messageText)) {
                    sendMessage("Bad date format or range. Please write date again: ", chatId, null, false, false);
                } else {
                    LocalDate date = DateTools.parseStringToLocalDate(messageText);
                    newGame.setDate(date);
                    sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\nWhat time do you plan to start the game? \n\nWrite the time in the message box and send it. The time should be written in the following format: <b>hh:mm</b> \n\nFor example: <i>13:00</i>",
                            chatId, null, false, false, "d/wizard");
                    userStates.replace(chatId, "creating_game_image");
                }
            }catch (DateTimeException e){
                sendMessage("Bad date format or range. Please write date again: ", chatId, null, false, false);
            }
        } else if (userStates.get(chatId).equals("creating_game_image")){
            try {
                LocalTime time;
                time = TimeTools.parseStringToLocalTime(messageText);
                newGame.setTime(time);
                sendPhotoMessageWithFilePath("<u>Game Creation</u> \n\n<b><i>Game cover image</i></b> \n\nYou can highlight your game by attaching an image to it. It will be located at the top of the game card. \n\nTo do this, you need to send us the image. To do this, click on the paper clip icon on the right side of the message box and then select “Photo or Video” in the window that appears. The image must be in JPEG or PNG format.",
                        chatId, null, false, false, "d/wizard");
                userStates.replace(chatId, "creating_game_control");
            } catch (DateTimeException e){
                sendMessage("Bad time format. Please write time again: ", chatId, null, false, false);
            }
        } else if (userStates.get(chatId).equals("creating_game_control")) {
            String photoId = update.getMessage().getPhoto().stream()
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
                newGame.setImage(imageBytes);
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
            }
            String message = "<u>Game Creation</u>"+
                    "\n\n<b>" + newGame.getName() + "</b>";
            message += "\n\n"+newGame.getDescription()+
                    "\n\n<b>Game type:</b> "+newGame.getGameType()+
                        "\n<b>Master:</b> @"+actualUser.getUserName()+
                        "\n<b>Language:</b> "+newGame.getLanguage().toFullString()+
                    "\n<b>Roleplaying system:</b> "+newGame.getRoleSystem()+
                    "\n<b>Genre:</b> "+newGame.getGenre()+
                    "\n<b>Date:</b> "+DateTools.parseLocalDateToString(newGame.getDate())+
                    "\n<b>Time:</b> "+TimeTools.parseLocalTimeToString(newGame.getTime())+
                    "\n<b>Price:</b> "+(newGame.getPrice()==0 ? "free" : newGame.getPrice()+",-")+
                    "\n<b>Players:</b> "+"0/"+newGame.getMaxPlayers()+
                    "\n";
            InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "That's right! (finish creating)", 1, "Cancel (return to menu)"), Map.of(0, "creatingGameYes", 1, "creatingGameNo"));
            sendPhotoMessage(message, chatId, markup, newGame.getImage());

        } else if (userStates.get(chatId).equals("creating_game_final")) {
            String message;
            try {
                message = "<u>Creating a game - Done!</u> \n\n<b><i>Done, your game has been successfully created!</i></b> \n\nDon't forget to look in the bot from time to time and check if someone has joined the game! \n\nYou can change the game parameters at any time by clicking the “Edit game” button in the “Show my games” menu.";
                UserEntity master = userService.getUserEntity(actualUser);
                gameService.create(newGame.getName(), newGame.getDate(), newGame.getTime(), master, newGame.getGameType(), newGame.getDescription(), newGame.getMaxPlayers(), newGame.getLanguage(), newGame.getPrice(), newGame.getImage(), newGame.getRoleSystem(), newGame.getGenre());
                userStates.replace(chatId, "default");
                sendPhotoMessageWithFilePath(message, chatId, null, false, false, "d/wizard");
                showMenu(chatId, actualUser);
            } catch (DateTimeException e) {
                message = "Bad time format. Please write time again: ";
                sendMessage(message, chatId, null, false, false);
            } catch (UserIsNotRegisteredException e) {
                message = "Something went wrong. UserIsNotRegisteredException happened.";
                sendMessage(message, chatId, null, false, false);
            } catch (BadDataException e) {
                message = e.getMessage();
                sendMessage(message, chatId, null, false, false);
                e.printStackTrace();
            }
            createdGamesIds.remove(chatId);
        }
    }
    public void showMasterGames(long chatId, User actualUser){
        try {
            UserEntity master = userService.getUserEntity(actualUser);
            Set<GameEntity> masterGames = gameService.getAllGamesByMaster(master);
            sendPhotoMessageWithFilePath("<u>Your scheduled games</u> \n\n<b><i>Below this message are all of your currently scheduled games!</i></b> \n\nClick on the “Edit game” button to change the parameters you set when you registered the game.",
                    chatId, null, true, true, "d/wizard");
            for (GameEntity game : masterGames){
                Set<UserEntity> players = game.getPlayers();
                String message = "<b>" + game.getName() + "</b>";
                message += "\n\n"+game.getDescription()+
                        "\n\n<b>Game type</b>: "+game.getGameType()+
                        "\n<b>Language:</b> "+game.getLanguage().toFullString()+
                        "\n<b>Roleplaying system:</b> "+game.getRoleSystem()+
                        "\n<b>Genre:</b> "+game.getGenre()+
                        "\n<b>Date:</b> "+DateTools.parseLocalDateToString(game.getDate())+
                        "\n<b>Time:</b> "+TimeTools.parseLocalTimeToString(game.getTime())+
                        "\n<b>Price:</b> "+(game.getPrice()==0 ? "free" : game.getPrice()+",-")+
                        "\n<b>Players:</b> "+players.size()+"/"+game.getMaxPlayers()+
                        "\n";
                        if (!players.isEmpty()){
                            for (UserEntity player : players){
                                message += "   - @"+player.getUsername()+"\n";
                            }
                        }
                InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "Edit game"), Map.of(0, "editingMasterGame_"+game.getId()));
                        System.out.println("Game id in markup: "+game.getId());
                sendPhotoMessageWithFilePath(message, chatId, markup, false, true,"game_"+game.getId());

            }
        } catch (UserIsNotRegisteredException e){
            standardError(e, chatId);
        } catch (MasterHaveNoGamesException e){
            sendMessage(e.getMessage(), chatId, null, false, false);
            showMenu(chatId, actualUser);
        }
    }
    public void editMasterGame(long chatId, Update update, User actualUser, String messageText, byte [] image){
        Long editedGameId = editedGamesIds.get(actualUser.getId());
        System.out.println("Edited game id: "+editedGameId);
        if (userStates.get(chatId).equals("editing_games_action")) {
            InlineKeyboardMarkup markupLine = createMarkup(12, createTwelveObjectMap("Edit Name", "Edit Date", "Edit Time", "Edit Type", "Edit Description",
                    "Edit Max Players", "Edit Roleplaying System", "Edit Genre", "Edit Language", "Edit Price",
                    "Edit Image", "Delete Game"), createTwelveObjectMap("editingGameName", "editingGameDate", "editingGameTime", "editingGameType",
                    "editingGameDescription", "editingGameMaxPlayers", "editingGameRoleSystem", "editingGameGenre",
                    "editingGameLanguage", "editingGamePrice", "editingGameImage", "deletingGame"));
            sendPhotoMessageWithFilePath("<u>Changing game parameters</u> <b><i>What parameter would you like to change?</i></b>", chatId, markupLine, true, true, "d/wizard");
        } else if (userStates.get(chatId).equals("editing_games_name")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new name for a game: ", chatId, null, false, false);
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
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please select game language: ", chatId, markup, false, false);
            userStates.replace(chatId, "editing_games_language_control");
        } else if (userStates.get(chatId).equals("editing_games_date")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new date for a game: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_date_control");
        } else if (userStates.get(chatId).equals("editing_games_time")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new time for a game: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_time_control");
        } else if (userStates.get(chatId).equals("editing_games_type")){
            InlineKeyboardMarkup markup = createMarkup(3, Map.of(0, "Campaign", 1 , "One shot", 2, "Real life game"),
                    Map.of(0, "editingGameTypeCampaign", 1, "editingGameTypeOneshot", 2, "editingGameTypeRealLifeGame"));
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please select game type: ", chatId, markup, false, false);
            userStates.replace(chatId, "editing_games_type_control");
        } else if (userStates.get(chatId).equals("editing_games_description")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new description for a game:", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_description_control");
        } else if (userStates.get(chatId).equals("editing_games_maxplayers")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new max amount of players(2-10):", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_maxplayers_control");
        } else if (userStates.get(chatId).equals("editing_games_rolesystem")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new role system name: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_rolesystem_control");
        } else if (userStates.get(chatId).equals("editing_games_genre")){
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new game genre: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_genre_control");
        } else if (userStates.get(chatId).equals("editing_games_price")) {
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please write to chat new price: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_price_control");
        } else if (userStates.get(chatId).equals("editing_games_image")) {
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Please send new image: ", chatId, null, false, false);
            userStates.replace(chatId, "editing_games_image_control");
        } else if (userStates.get(chatId).equals("editing_games_delete")){
            InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Yes", 1, "No"),
                    Map.of(0, "deletingGameYes", 1, "deletingGameNo"));
            sendGameCard(editedGameId, chatId, true);
            sendMessage("Are you sure, you want to delete this game?", chatId, markup, false, false);
        } else if (userStates.get(chatId).equals("editing_games_name_control")){
            if (gameService.gameNameIsFree(messageText)){
                try{
                    GameEntity editedGame = gameService.getGameById(editedGameId);
                    sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                            " was edited. New game name is "+messageText, editedGame);
                    gameService.changeGameData("name", messageText, editedGameId, null);
                    sendGameCard(editedGameId, chatId, false);
                    sendMessage("Name was successfully changed", chatId, null, false, false);
                    editedGamesIds.remove(actualUser.getId());
                    userStates.replace(chatId, "default");
                    showMenu(chatId, actualUser);
                } catch (NoSuchGameException | BadDataTypeException e){
                    standardError(e, chatId);
                }
            }else{
                sendMessage("This game name is already used. Please choose other name: ", chatId, null, false, false);
            }
        } else if (userStates.get(chatId).equals("editing_games_date_control")){
            try {
                if (DateTools.controlDate(messageText)) {
                    try {
                        GameEntity editedGame = gameService.getGameById(editedGameId);
                        sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                                " was edited. New game date is "+messageText, editedGame);
                        gameService.changeGameData("date", messageText, editedGameId, null);
                        sendGameCard(editedGameId, chatId, false);
                        sendMessage("Date was successfully changed", chatId, null, false, false);
                        editedGamesIds.remove(actualUser.getId());
                        userStates.replace(chatId, "default");
                        showMenu(chatId, actualUser);
                    } catch (NoSuchGameException | BadDataTypeException e) {
                        standardError(e, chatId);
                    }
                } else {
                    sendMessage("Bad date format or range. Please write date again: ", chatId, null, false, false);
                }
            } catch (DateTimeException e){
                sendMessage("Bad date format or range. Please write date again: ", chatId, null, false, false);
            }
        } else if (userStates.get(chatId).equals("editing_games_time_control")){
            try {
                TimeTools.parseStringToLocalTime(messageText);
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New game time is "+messageText, editedGame);
                gameService.changeGameData("time", messageText, editedGameId, null);
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Time was successfully changed.", chatId, null, false, false);
                editedGamesIds.remove(actualUser.getId());;
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (DateTimeParseException e){
                sendMessage("Bad time format. Please write time again: ", chatId, null, false, false);
            } catch (NoSuchGameException | BadDataTypeException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_type_control")){
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                String newGameType;
                switch (update.getCallbackQuery().getData()){
                    case "editingGameTypeCampaign": newGameType = "campaign"; break;
                    case "editingGameTypeRealLifeGame": newGameType = "realLifeGame"; break;
                    default: newGameType = "oneshot"; break;
                }
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New game type is " + newGameType + ".", editedGame);
                gameService.changeGameData("type", newGameType, editedGameId, null);
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game type was successfully changed.", chatId, null, false, false);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_language_control")) {
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New game language is "+ GameLanguage.parseGameLanguage(messageText.substring(messageText.length()-2)).toFullString()+".", editedGame);
                gameService.changeGameData("language", messageText, editedGameId, null);
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game language was successfully changed.", chatId, null, false, false);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_description_control")) {
            try{
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New game description: \n"+messageText, editedGame);
                gameService.changeGameData("description", messageText, editedGameId, null);
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game description was successfully changed.", chatId, null, false, false);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_maxplayers_control")){
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New max player amount is "+messageText, editedGame);
                gameService.changeGameData("maxPlayers", messageText, editedGameId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game max amount of players was successfully changed.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_rolesystem_control")){
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New role system is "+messageText, editedGame);
                gameService.changeGameData("roleSystem", messageText, editedGameId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game role system was successfully changed.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_genre_control")){
            try {
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. New game genre is "+messageText, editedGame);
                gameService.changeGameData("genre", messageText, editedGameId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game genre was successfully changed.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_price_control")){
            try{
                GameEntity editedGame = gameService.getGameById(editedGameId);
                if (!TextTools.containsOnlyNumbers(messageText)){throw new BadDataTypeException("Price can't contain other symbols than numbers.");}
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. "+(Long.parseLong(messageText) == 0 ? "Game is now free!" : "New price is "+messageText), editedGame);
                gameService.changeGameData("price", messageText, editedGameId, null);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Game price was successfully changed.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_image_control")){
            try{
                GameEntity editedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+editedGame.getName()+" leaded by master @"+editedGame.getMaster().getUsername()+
                        " was edited. Game has new image now.", editedGame);
                gameService.changeGameData("image", messageText, editedGameId, image);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendGameCard(editedGameId, chatId, false);
                sendMessage("Image was successfully changed.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (BadDataTypeException | NoSuchGameException e){
                standardError(e, chatId);
            }
        } else if (userStates.get(chatId).equals("editing_games_delete_control")){
            try {
                GameEntity deletedGame = gameService.getGameById(editedGameId);
                sendMessageToAllPlayersInGame("WARNING: A game "+deletedGame.getName()+", leaded by master @"+deletedGame.getMaster().getUsername()+" was deleted!", deletedGame);
                gameService.deleteGameById(editedGameId);
                editedGamesIds.remove(actualUser.getId());
                userStates.replace(chatId, "default");
                sendMessage("Game was successfully deleted.", chatId, null, false, false);
                showMenu(chatId, actualUser);
            } catch (NoSuchGameException e){
                standardError(e, chatId);
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
        sendMessage(text, chatId, markup, false, true);
    }
    public void showAllGamesByLanguage(long chatId, String language, User actualUser){
        if (userStates.get(chatId).equals("showing_games_select_language")) {
            InlineKeyboardMarkup markup = createButtonsByLanguages();
            sendPhotoMessageWithFilePath("<u>Select language</u>\n\n<u><i>What language will you play in?</i></u>",
                    chatId, markup, true, true, "d/mercenary");
        } else if (userStates.get(chatId).equals("showing_games_print")){
            try {
                Set<GameEntity> games = gameService.getAllGamesByLanguage(GameLanguage.parseGameLanguage(language));
                sendPhotoMessageWithFilePath("<u>List of scheduled games</u>\n\n<b><i>Brave mercenary, what mission will you decide to go on?</i></b> \n\nEach post below represents an <b>actual scheduled game</b>. To sign up for a game, click on the “Register for this game” button below the corresponding post. The game master will receive a link to your profile and will contact you after a short time.\n\nBelow this message you will see buttons for filtering. Use them to sort the games as you like.",
                        chatId, null, true, true, "d/mercenary");
                for (GameEntity game : games) {
                    String message = "<b>"+game.getName()+"</b>";
                    message += "\n\n" + game.getDescription() +
                            "\n\n<b>Game type:</b> " + game.getGameType() +
                            "\n<b>Master:</b> @" + game.getMaster().getUsername() +
                            "\n<b>Language:</b> "+game.getLanguage().toFullString()+
                            "\n<b>Role system:</b> "+game.getRoleSystem()+
                            "\n<b>Genre:</b> "+game.getGenre()+
                            "\n<b>Date:</b> " + DateTools.parseLocalDateToString(game.getDate()) +
                            "\n<b>Time:</b> " + TimeTools.parseLocalTimeToString(game.getTime()) +
                            "\n<b>Players:</b> " + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                            "\n<b>Price:</b> " + (game.getPrice()==0 ? "free" : game.getPrice()+",-")+
                            "\n";
                    InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "Join game"), Map.of(0, "choosingGameToJoin_"+game.getId()));
                    sendPhotoMessageWithFilePath(message, chatId, markup, false, true, "game_"+game.getId());
                }
            } catch (BadDataTypeException e) {
                standardError(e, chatId);
            } catch (NoSuchGameException e){
                sendMessage(e.getMessage(), chatId, null, false, false);
                showMenu(chatId, actualUser);
            }
        }
    }
    public void joinGame(long chatId, String gameId, User actualUser){
        try {
            GameEntity game = gameService.getGameById(Long.parseLong(gameId));
            UserEntity player = userService.getUserEntity(actualUser);
            gameService.joinPlayer(player, game);
            showMenu(chatId, actualUser);
            sendPhotoMessageWithFilePath("<u>Registration for the game - Done</u> \n\n<b><i>You have successfully registered for the game!</i></b> \n\nWe have notified the Master of this game and he will contact you in a short time.", chatId, null, false, false, "d/mercenary");
            sendMessage("ATTENTION: @"+actualUser.getUserName()+" have joined your game - "+game.getName()+" on "+DateTools.parseLocalDateToString(game.getDate())+"! Players are now "+game.getPlayers().size()+"/"+game.getMaxPlayers(), gameService.getMasterTelegramId(game), null, false, false);
        } catch (NumberFormatException e){
            standardError(e, chatId);
        } catch (NoSuchGameException | JoinGameException e){
            sendMessage(e.getMessage(), chatId, null, false, false);
            showMenu(chatId, actualUser);
        } catch (UserIsNotRegisteredException e){
            standardError(e, chatId);
        }
    }
    public void showPlayerGames(long chatId, User actualUser){
        try {
            UserEntity user = userService.getUserEntity(actualUser);
            Set<GameEntity> userGames = gameService.getAllGamesByPlayer(user);
            sendPhotoMessageWithFilePath("<u>List of games I'm registered in</u> \\n\\n<b><i>Below this message are the cards of all games in which you are currently registered as a player</i></b> \\n\\nHere you can specify when or where the game is being played if you have forgotten. \\n\\nIt is also here that you can cancel your registration for games that you can no longer attend. We kindly ask you not to do this at the last moment.",
                    chatId, null, true, true, "d/writer");
            for (GameEntity game : userGames){
                String message = "<b>" + game.getName() + "</b>";
                message += "\n\n" + game.getDescription() +
                        "\n\n<b>Game type:</b> " + game.getGameType() +
                        "\n<b>Master:</b> @" + game.getMaster().getUsername() +
                        "\n<b>Language:</b> "+game.getLanguage().toFullString()+
                        "\n<b>Role system:</b> "+game.getRoleSystem()+
                        "\n<b>Genre:</b> "+game.getGenre()+
                        "\n<b>Date:</b> " + DateTools.parseLocalDateToString(game.getDate()) +
                        "\n<b>Time:</b> " + TimeTools.parseLocalTimeToString(game.getTime()) +
                        "\n<b>Price:</b> " + (game.getPrice()==0 ? "free" : game.getPrice()+",-")+
                        "\n<b>Players:</b> " + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                        "\n";
                InlineKeyboardMarkup markup = createMarkup(1, Map.of(0, "Leave the game"), Map.of(0, "userGameListChoice_"+game.getId()));
                sendPhotoMessageWithFilePath(message, chatId, markup, false, true,"game_"+game.getId());
            }
        } catch (UserIsNotRegisteredException e){
            standardError(e, chatId);
        } catch (UserHaveNoGamesExcpetion e){
            sendMessage(e.getMessage(), chatId, null, false, false);
            showMenu(chatId, actualUser);
        }
    }
    public void disconnectGame(long chatId, User actualUser, Long gameId){
        try {
            GameEntity disconnectedGame = gameService.getGameById(gameId);
            if (userStates.get(chatId).equals("disconnecting_game_choice")) {
                InlineKeyboardMarkup markup = createMarkup(2, Map.of(0, "Yes", 1, "No"), Map.of(0, "disconnectingYes", 1, "disconnectingNo"));
                sendPhotoMessageWithFilePath("<b><i>Do you really want to leave this game?</i></b>", chatId, markup, false, false, "d/writer");
            } else if (userStates.get(chatId).equals("disconnecting_game_yes")) {
                try {
                    gameService.disconnectPlayer(userService.getUserEntity(actualUser), disconnectedGame);
                    showMenu(chatId, actualUser);
                    sendPhotoMessageWithFilePath("<b><i>You have left the game.</i></b>", chatId, null, false, false, "d/writer");
                    sendMessage("ATTENTION: @"+actualUser.getUserName()+" have left your game - "+disconnectedGame.getName()+" on "+DateTools.parseLocalDateToString(disconnectedGame.getDate())+"! Players are now "+disconnectedGame.getPlayers().size()+"/"+disconnectedGame.getMaxPlayers(), gameService.getMasterTelegramId(disconnectedGame), null, false, false);
                } catch (UserIsNotRegisteredException e) {
                    standardError(e, chatId);
                    showMenu(chatId, actualUser);
                }
            } else if (userStates.get(chatId).equals("disconnecting_game_no")) {
                showMenu(chatId, actualUser);
            }
        }catch (NoSuchGameException e){
            standardError(e, chatId);
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
                sendMessage("Images were successfully uploaded to database.", chatId, null, false, false);
                devStates.put(chatId, "default");
            } catch (DeveloperException e){
                sendMessage(e.getMessage(), chatId, null, false, false);
                devStates.put(chatId, "default");
            }

        } else if (messageText.equals("/dev-newPhoto") || (devStates.get(chatId).equals("waiting_for_new_photo") && !containsPhotos)) {
            if (messageText.equals("/dev-newPhoto")){
                sendMessage("Send a photos, which you want to add to database. Write names and descriptions for it." +
                        " It must look like - photoName. photoDescription; otherPhotoName. otherPhotoDescription", chatId, null, false, false);
                devStates.put(chatId, "waiting_for_new_photo");
                System.out.println("devState "+devStates.get(chatId));
            } else if (devStates.get(chatId).equals("waiting_for_new_photo")){
                sendMessage("No photos in message!", chatId, null, false, false);
                devStates.put(chatId, "default");
            }
        }
    }


    private void emptyBotRecycleBin(long chatId){
        for (Integer messageId : botMessageRecycleBin.get(chatId)){
            deleteMessage(chatId, messageId);
        }
        botMessageRecycleBin.removeAll(chatId);
    }
    private void emptyUserRecycleBin(long chatId){
        deleteMessage(chatId, userMessageRecycleBin.get(chatId));
        userMessageRecycleBin.remove(chatId);
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
    private Message sendMessage(String messageText, long chatId, InlineKeyboardMarkup markup, boolean addBackButton, boolean save){
        org.example.tools.bot_tools.Message actualMessage = new org.example.tools.bot_tools.Message(messageText, chatId, markup, null);
        if (addBackButton){
            addBackButtonToMessage(actualMessage);
        }
        if (save){
            handleMessageSystemWhenSending(chatId, actualMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(actualMessage.getChatId());
        sendMessage.setText(actualMessage.getText());
        sendMessage.setReplyMarkup(actualMessage.getMarkup());
        sendMessage.setParseMode("HTML");
        try{
            Message sentMessage = execute(sendMessage);
            botMessageRecycleBin.put(chatId, sentMessage.getMessageId());
            return sentMessage;
        }catch (TelegramApiException e){
            e.printStackTrace();
            return null;
        }
    }
    private void sendObjectMessage (org.example.tools.bot_tools.Message message){
        String messageText = message.getText();
        long chatId = message.getChatId();
        InlineKeyboardMarkup markup = message.getMarkup();
        byte [] photo = message.getPhoto();
        if (message.isMenu()){
            oldMessages.remove(chatId);
        }

        if (photo == null) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(messageText);
            sendMessage.setParseMode("HTML");
            if (markup != null) {
                sendMessage.setReplyMarkup(markup);
            }
            try {
                Message sentMessage = execute(sendMessage);
                botMessageRecycleBin.put(chatId, sentMessage.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(messageText);
            sendPhoto.setParseMode("HTML");
            sendPhoto.setPhoto(photoService.parseByteArrayToInputFile(photo));
            if (markup != null) {
                sendPhoto.setReplyMarkup(markup);
            }
            try {
                Message sentMessage = execute(sendPhoto);
                botMessageRecycleBin.put(chatId, sentMessage.getMessageId());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }
    }
    private void sendMessageToAllPlayersInGame(String messageText, GameEntity game){
        Set<UserEntity> players = game.getPlayers();
        for (UserEntity player : players){
            sendMessage(messageText, player.getTelegramId(), null, false, false);
        }
    }
    private void showMenu(long chatId, User actualUser){
        try {
            UserEntity user = userService.getUserEntity(actualUser);
            userStates.replace(chatId, "default");
            String text = "<u>Main menu</u>" +
                    "\n\n<b>Welcome to the main menu!</b>" +
                    "\n\nFrom here you can go to the list of scheduled games and sign up for one of them! " +
                    "\n\nIf you are a Master, from here you can go to the game creation menu and register your game! " +
                    "\n\nAlso we ask your attention for the <b>feedback button</b>! Now, in the process of testing, it is very important for us to know your opinion and we are actively analyzing feedback. You can write us <b>literally anything that you pay attention to</b>, we will read it immediately!";
            InlineKeyboardMarkup markup = createMarkup(8, Map.of(0, "Create game", 1, "Show my games",
                    2, "Join game", 3, "Games I've joined", 4, "Service rules", 5, "About us", 6, "Feedback", 7, "Delete account"), Map.of(0, "createGame", 1, "editGames",
                    2, "joinGame", 3, "myGames", 4, "faq", 5, "aboutUs", 6, "sendFeedback", 7, "delete"));
            sendPhotoMessageWithFilePath(text, chatId, markup, false, true, "d/waitress");
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
    private Map<Integer, String> createTwelveObjectMap (String first, String second, String third, String fourth, String fifth, String sixth, String seventh, String eight, String ninth, String tenth, String eleventh, String twelfth){
        Map<Integer, String> map = new HashMap<>();
        map.put(0, first);
        map.put(1, second);
        map.put(2, third);
        map.put(3, fourth);
        map.put(4, fifth);
        map.put(5, sixth);
        map.put(6, seventh);
        map.put(7, eight);
        map.put(8, ninth);
        map.put(9, tenth);
        map.put(10, eleventh);
        map.put(11, twelfth);
        return map;
    }
    private void addBackButtonToMessage(org.example.tools.bot_tools.Message message){
        if (message.getMarkup() != null) {
            List<List<InlineKeyboardButton>> rowsInLine = message.getMarkup().getKeyboard();
            List<InlineKeyboardButton> lastRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setCallbackData("backButton");
            backButton.setText("Back");
            lastRow.add(backButton);
            rowsInLine.add(lastRow);

        } else {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("Back");
            backButton.setCallbackData("backButton");
            row.add(backButton);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            message.setMarkup(markup);
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
    private Message sendPhotoMessageWithFilePath (String messageText, long chatId, InlineKeyboardMarkup markup, boolean addBackButton, boolean save, String photoFilePath){
        org.example.tools.bot_tools.Message actualMessage = new org.example.tools.bot_tools.Message(messageText, chatId, markup, null);
        if (addBackButton){
            addBackButtonToMessage(actualMessage);
        }
        if (photoFilePath.contains("d/")){
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(photoService.getPhoto(photoFilePath.split("/")[1]));
            actualMessage.setPhoto(photoService.getPhotoAsByteArray(photoFilePath.split("/")[1]));
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(messageText);
            sendPhoto.setReplyMarkup(actualMessage.getMarkup());
            sendPhoto.setParseMode("HTML");
            if (save){
                handleMessageSystemWhenSending(chatId, actualMessage);
            }
            try {
                Message message = execute(sendPhoto);
                botMessageRecycleBin.put(chatId, message.getMessageId());
                return message;
            } catch (TelegramApiException e) {
                e.printStackTrace();
                return null;
            }
        } else if (photoFilePath.contains("game")){
            SendPhoto sendPhoto = new SendPhoto();
            try {
                sendPhoto.setPhoto(gameService.getPhoto(Long.parseLong(photoFilePath.split("_")[1])));
                actualMessage.setPhoto(gameService.getPhotoAsByteArray(Long.parseLong(photoFilePath.split("_")[1])));
                sendPhoto.setCaption(messageText);
                sendPhoto.setParseMode("HTML");
                sendPhoto.setReplyMarkup(actualMessage.getMarkup());
                sendPhoto.setChatId(chatId);
                if (save){
                    handleMessageSystemWhenSending(chatId, actualMessage);
                }
                try{
                    Message message = execute(sendPhoto);
                    botMessageRecycleBin.put(chatId, message.getMessageId());
                    return message;
                } catch (TelegramApiException e){
                    e.printStackTrace();
                    return null;
                }
            } catch (NoSuchGameException e){
                standardError(e, chatId);
                return null;
            }
        } else {return  null;}
    }
    private Message sendPhotoMessage(String messageText, long chatId, InlineKeyboardMarkup markup, byte [] photo){
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(photoService.parseByteArrayToInputFile(photo));
        sendPhoto.setCaption(messageText);
        sendPhoto.setParseMode("HTML");
        sendPhoto.setChatId(chatId);
        sendPhoto.setReplyMarkup(markup);
        try {
            Message message = execute(sendPhoto);
            botMessageRecycleBin.put(chatId, message.getMessageId());
            return message;
        } catch (TelegramApiException e){
            e.printStackTrace();
            return null;
        }
    }
    private Message sendGameCard (Long gameId, long chatId, boolean addBackButton){
        try {
            GameEntity game = gameService.getGameById(gameId);
            Set<UserEntity> players = game.getPlayers();
            String messageText = "<b>" + game.getName() + "</b>";
            messageText += "\n<b>Game type</b>: "+game.getGameType()+
                    "\n<b>Language:</b> "+game.getLanguage().toFullString()+
                    "\n<b>Role system:</b> "+game.getRoleSystem()+
                    "\n<b>Genre:</b> "+game.getGenre()+
                    "\n<b>Date:</b> "+DateTools.parseLocalDateToString(game.getDate())+
                    "\n<b>Time:</b> "+TimeTools.parseLocalTimeToString(game.getTime())+
                    "\n<b>Price:</b> "+(game.getPrice()==0 ? "free" : game.getPrice())+
                    "\n<b>Description:</b> "+game.getDescription()+
                    "\n<b>Players:</b> "+players.size()+"/"+game.getMaxPlayers()+
                    "\n";
            if (!players.isEmpty()){
                for (UserEntity player : players){
                    messageText += "   - @"+player.getUsername()+"\n";
                }
            }
            return sendPhotoMessageWithFilePath(messageText, chatId, null, addBackButton, false, "game_"+game.getId());
        } catch (NoSuchGameException e){
            standardError(e, chatId);
            return null;
        }
    }
    private void standardError (Exception e, long chatId){
        sendMessage("Something went wrong!", chatId, null, false, false);
        sendMessage(e.getMessage(), chatId, null, false, false);
        if (TraceTools.traceContainsMethod("createGame")){
            sendMessage("Please send a correct version of this parameter.", chatId, null, false, false);
        }
    }
    private java.io.File handleMessagePhotoFile (String photoFilePath){
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(photoFilePath);
        java.io.File tempFile = null;
            try {
                // Создаем временный файл для хранения содержимого ресурса
                tempFile = java.io.File.createTempFile("photo", ".png");
                tempFile.deleteOnExit();

                // Записываем содержимое ресурса в временный файл
                try (FileOutputStream outStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = resourceStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return tempFile;
    }
    private Message sendPoll (long chatId, String messageText, List<String> options){
        SendPoll sendPoll = new SendPoll();
        sendPoll.setChatId(chatId);
        sendPoll.setAllowMultipleAnswers(true);
        sendPoll.setIsAnonymous(false);
        sendPoll.setQuestion(messageText);
        sendPoll.setOptions(options);

        try{
            Message sentMessage = execute(sendPoll);
            return sentMessage;
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
        return null;
    }
    private void handleFeedback (String messageText, User actualUser, long chatId){
        feedbackService.create(messageText, actualUser);
        showMenu(chatId, actualUser);
        sendPhotoMessageWithFilePath("<u>Feedback</u>\n\n<b><i>Thank you very much for your feedback!</i></b>\n\nIf you notice anything else, please write us about it too!",
                chatId, null, false, false, "d/waitress");
    }
    private void handlePollAnswer (PollAnswer pollAnswer, long chatId){
        try {
            Set<String> absentPlayers = new HashSet<>();
            Map<Integer, String> optionMap = pollCollection.get(chatId);
            for (Integer i : pollAnswer.getOptionIds()) {
                absentPlayers.add(optionMap.get(i).substring(1));
            }
            for (String username : absentPlayers) {
                UserEntity absentUser = userService.getUserByUsername(username);
                userService.addReport(absentUser);
            }

        } catch (UserIsNotRegisteredException e){
            standardError(e, chatId);
        }
        pollCollection.remove(chatId);
    }
    private void masterFeedback (Long gameId, long chatId){
        if (!userStates.get(chatId).equals("default")) {
            try {
                GameEntity game = gameService.getGameById(gameId);
                if (userStates.get(chatId).equals("master_feedback_second")) {
                    sendMessage("<b>Was every player present at game?</b>", chatId, createMarkup(2, Map.of(0, "Yes", 1, "No"), Map.of(0, game.getId() + "_masterFeedbackSecondYes",
                            1, game.getId() + "_masterFeedbackSecondNo")), false, false);
                } else if (userStates.get(chatId).equals("master_feedback_third")) {
                    List<String> players = new ArrayList<>();
                    for (UserEntity player : game.getPlayers()) {
                        players.add("@" + player.getUsername());
                    }
                    Map<Integer, String> optionMap = new HashMap<>();
                    for (int i = 0; i < players.size(); i++) {
                        optionMap.put(i, players.get(i));
                    }
                    pollCollection.put(chatId, optionMap);
                    sendPoll(chatId, "Who wasn't present?", players);
                }
            } catch (NoSuchGameException e) {
                standardError(e, chatId);
            }
        } else {
            sendMessage("Thank you for your feedback!", chatId, null, false, false);
        }
    }
    private void handleMessageSystemOnUpdate (long chatId){
        if (oldMessages.get(chatId) == null){
            oldMessages.put(chatId, new LinkedList<>());
        }
        if (actualMessages.get(chatId) != null) {
            if (!oldMessages.get(chatId).isEmpty()){
                for (org.example.tools.bot_tools.Message message : actualMessages.get(chatId)) {
                    message.setPreviousMessageSet(oldMessages.get(chatId).getFirst());
                    for (org.example.tools.bot_tools.Message previousMessage : message.getPreviousMessageSet()) {
                        System.out.println("Previous message text: "+previousMessage.getText());
                    }
                }
            }
            oldMessages.get(chatId).addFirst(actualMessages.get(chatId));
            actualMessages.remove(chatId);
        }
    }
    private void handleMessageSystemWhenSending (long chatId, org.example.tools.bot_tools.Message actualMessage){
        if (TraceTools.actualMessageIsMenu()){
            oldMessages.remove(chatId);
            actualMessage.setMenu(true);
        }
        if (actualMessages.get(chatId) == null){
            actualMessages.put(chatId, new HashSet<>());
        }
        actualMessages.get(chatId).add(actualMessage);
    }


    @Scheduled(cron = "0 0 12 * * ?")
    private void handleExpiratedGames(){
        Set<GameEntity> expiredGames = gameService.getAndSetExpiredGames();
        if (!expiredGames.isEmpty()){
            for (GameEntity game : expiredGames){
                sendMessage("<u>Your game expired</u>\n\nYour game \""+game.getName()+"\" expired and was deleted. Please answer a few questions." +
                        "\n\n<b>Was this game played?</b>", game.getMaster().getTelegramId(), createMarkup(2, Map.of(0, "Yes", 1, "No"), Map.of(0, game.getId()+"_masterFeedbackFirstYes",
                        1, game.getId()+"_masterFeedbackFirstNo")), false, false);
                sendMessageToAllPlayersInGame("<u>Game expired</u>\n\n<b>The game called \"" +game.getName()+ "\" expired and was deleted.</b>" +
                        "\n\nWe hope you've enjoyed the game!", game);
            }
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
        return "DungeonLocationBot";
    }
    @Override
    public String getBotToken(){
        return "7238028342:AAEuSVjDczxPCS8AzMgJXgbkUrRhzrpGDEc";
    }
}
