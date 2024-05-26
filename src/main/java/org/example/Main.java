package org.example;

import jakarta.annotation.PostConstruct;
import org.example.models.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Timer;
import java.util.TimerTask;
@EnableScheduling
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        /*Исправить кнопку Back - можно вернуться только на одно сообщение назад
        изменить на дерево сообщений до главного меню
        добавить обновление последнего сообщения каждый раз, сейчас при переходе на предъидущее сообщение несколько раз добавляеться несколько
        кнопок Back
         */

        SpringApplication.run(Main.class, args);
    }
    @PostConstruct
    public void init() throws TelegramApiException{
        telegram();
    }
    public void telegram() throws TelegramApiException{
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(testingBot);
    }
    @Autowired
    private TestingBot testingBot;


}