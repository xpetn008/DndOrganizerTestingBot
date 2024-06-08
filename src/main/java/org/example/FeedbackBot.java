package org.example;

import org.example.data.entities.FeedbackMessages;
import org.example.models.services.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Set;

@Component
public class FeedbackBot extends TelegramLongPollingBot {
    @Autowired
    private FeedbackService feedbackService;
    //Петрович, Вова, Суханов, Алекс, Падла, Антон
    private Set<Long> feedbackReceivers = Set.of(5206307557L, 1430757469L, 667567801L, 1119225555L, 769866268L, 842021062L);
    @Override
    public void onUpdateReceived(Update update) {

    }

    @Scheduled(fixedRate = 5000)
    public void checkNewFeedbacks(){
        List<FeedbackMessages> newFeedbacks = feedbackService.getNewFeedbacks();
        for (FeedbackMessages feedback : newFeedbacks){
            String messageText = "<b>НОВЫЙ ОТЗЫВ ОТ</b> @"+feedback.getSender() +
                    "\n<b>ТЕКСТ ОТЗЫВА:</b> "+feedback.getMessage();
            for (Long id : feedbackReceivers){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setParseMode("HTML");
                sendMessage.setChatId(id);
                sendMessage.setText(messageText);
                try{
                    execute(sendMessage);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public String getBotUsername() {
        return "DndOrganizerFeedbackBot";
    }
    @Override
    public String getBotToken(){
        return "7042083159:AAGv5LhgZ8BjQSEwp6XnGmcrOJ-icsu-2b8";
    }
}
