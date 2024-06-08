package org.example.models.services;

import org.example.data.entities.FeedbackMessages;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

public interface FeedbackService {
    void create (String messageText, User actualUser);
    List<FeedbackMessages> getNewFeedbacks();

}
