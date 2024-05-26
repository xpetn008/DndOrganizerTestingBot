package org.example.data.entities;

import jakarta.persistence.*;

@Entity
public class PhotoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "photo_name")
    private String photoName;
    @Column(name = "photo_description")
    private String photoDescription;
    @Lob
    @Column(name = "photo", columnDefinition = "MEDIUMBLOB")
    private byte [] photo;

    public PhotoEntity(){}
    public PhotoEntity(String photoName, String photoDescription, byte [] photo){
        this.photoName = photoName;
        this.photoDescription = photoDescription;
        this.photo = photo;
    }

    public Long getId() {
        return id;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getPhotoDescription() {
        return photoDescription;
    }

    public void setPhotoDescription(String photoDescription) {
        this.photoDescription = photoDescription;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
}
