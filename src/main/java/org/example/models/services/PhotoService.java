package org.example.models.services;

import org.example.models.exceptions.DeveloperException;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.List;

public interface PhotoService {
    void newPhoto(String name, String description, byte[] photo);
    void newPhotoCollection (List<String> names, List<String> descriptions, List<byte[]> photos) throws DeveloperException;
    void newPhotosFromMessage (String message, List<byte[]> photos) throws DeveloperException;
    InputFile getPhoto ();
}
