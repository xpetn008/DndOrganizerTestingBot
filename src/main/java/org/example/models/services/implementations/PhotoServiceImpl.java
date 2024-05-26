package org.example.models.services.implementations;

import org.example.data.entities.PhotoEntity;
import org.example.data.repositories.PhotoRepository;
import org.example.models.exceptions.DeveloperException;
import org.example.models.services.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PhotoServiceImpl implements PhotoService {
    @Autowired
    private PhotoRepository photoRepository;
    @Override
    public void newPhoto (String name, String description, byte[] photo){
        PhotoEntity newPhoto = new PhotoEntity(name, description, photo);
        photoRepository.save(newPhoto);
    }
    @Override
    public void newPhotoCollection (List<String> names, List<String> descriptions, List<byte[]> photos) throws DeveloperException {
        System.out.println("Collections: "+names.size()+" "+descriptions.size()+" "+photos.size());
        if (names.size() == descriptions.size() && descriptions.size() == photos.size()){
            for (int i = 0; i < names.size(); i++){
                newPhoto(names.get(i), descriptions.get(i), photos.get(i));
            }
        } else {
            throw new DeveloperException("Collections are not equal! Mistake in message text is possible.");
        }
    }
    @Override
    public void newPhotosFromMessage(String message, List<byte[]> photos) throws DeveloperException {
        List<String> names = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();

        String [] partsArray = message.split("; ");
        for (String part : partsArray){
            String [] nameAndDescription = part.split(". ");
            String name = nameAndDescription[0];
            String description = nameAndDescription[1];
            names.add(name);
            descriptions.add(description);
        }
        newPhotoCollection(names, descriptions, photos);
    }
    @Override
    public InputFile getPhoto(){
        byte [] photo = photoRepository.findById(Long.parseLong("1")).orElseThrow().getPhoto();
        InputStream inputStream = new ByteArrayInputStream(photo);
        InputFile photoFile = new InputFile();
        photoFile.setMedia(inputStream, "photo.jpg");
        return photoFile;
    }
}
