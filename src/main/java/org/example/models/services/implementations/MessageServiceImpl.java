package org.example.models.services.implementations;

import org.example.data.entities.MessageEntity;
import org.example.data.entities.PhotoEntity;
import org.example.data.repositories.MessageRepository;
import org.example.models.exceptions.BadDataException;
import org.example.models.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageRepository messageRepository;

    @Override
    public void create(String name, String text, PhotoEntity photo) throws BadDataException {
        try {
            getByName(name);
            throw new BadDataException("This message name is already used.");
        } catch (BadDataException e) {
            MessageEntity newMessage = new MessageEntity(name, text, photo);
            messageRepository.save(newMessage);
        }
    }
    @Override
    public MessageEntity getByName (String name) throws BadDataException {
        Optional<MessageEntity> message = messageRepository.findByName(name);
        if (message.isPresent()){
            return message.get();
        } else {
            throw new BadDataException("Message was not found.");
        }
    }
}
