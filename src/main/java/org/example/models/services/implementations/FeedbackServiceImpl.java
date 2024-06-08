package org.example.models.services.implementations;

import jakarta.annotation.PostConstruct;
import org.example.data.entities.FeedbackMessages;
import org.example.data.repositories.FeedbackRepository;
import org.example.models.services.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import javax.sql.XADataSource;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;
    private Long lastId;
    @PostConstruct
    public void init(){
        lastId = feedbackRepository.findMaxId();
        if (lastId == null){
            lastId = 0L;
        }
    }
    @Override
    public List<FeedbackMessages> getNewFeedbacks (){
        List<FeedbackMessages> newFeedbacks = feedbackRepository.findNewFeedbacks(lastId);
        for (FeedbackMessages feedbackMessages : newFeedbacks){
            lastId = feedbackMessages.getId();
        }
        return newFeedbacks;
    }
    @Override
    public void create (String messageText, User actualUser){
        FeedbackMessages feedbackMessage = new FeedbackMessages();
        feedbackMessage.setSender(actualUser.getUserName());
        feedbackMessage.setMessage(messageText);
        feedbackRepository.save(feedbackMessage);
    }

    public Long getLastId() {
        return lastId;
    }

    public void setLastId(Long lastId) {
        this.lastId = lastId;
    }
}
