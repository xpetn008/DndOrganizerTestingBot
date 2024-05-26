package org.example.data.entities;

import com.sun.jna.platform.win32.WinDef;
import jakarta.persistence.*;
import org.example.tools.bot_tools.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Entity
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "text")
    private String text;
    @OneToOne
    @JoinColumn (name = "photo_id")
    private PhotoEntity photo;

    public MessageEntity(){}
    public MessageEntity(String name, String text, PhotoEntity photo){
        this.name = name;
        this.text = text;
        this.photo = photo;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public PhotoEntity getPhoto() {
        return photo;
    }

    public void setPhoto(PhotoEntity photo) {
        this.photo = photo;
    }
}
