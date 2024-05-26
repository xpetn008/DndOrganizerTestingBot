package org.example.models.services;

import org.example.data.entities.MessageEntity;
import org.example.data.entities.PhotoEntity;
import org.example.models.exceptions.BadDataException;

public interface MessageService {
    void create (String name, String text, PhotoEntity photo) throws BadDataException;
    MessageEntity getByName (String name) throws BadDataException;
}
