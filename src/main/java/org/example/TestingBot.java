package org.example;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TestingBot extends TelegramLongPollingBot {
    @Override
    public void onUpdateReceived(Update update) {

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
