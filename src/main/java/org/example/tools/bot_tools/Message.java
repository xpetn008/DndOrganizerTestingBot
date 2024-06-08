package org.example.tools.bot_tools;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Set;

public class Message {
    private String text;
    private long chatId;
    private InlineKeyboardMarkup markup;
    private Set<Message> previousMessageSet;
    private byte [] photo;
    boolean menu;

    public Message (){}
    public Message (String text, long chatId, InlineKeyboardMarkup markup, byte[] photo){
        this.text = text;
        this.chatId = chatId;
        this.markup = markup;
        previousMessageSet = null;
        this.photo = photo;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public InlineKeyboardMarkup getMarkup() {
        return markup;
    }

    public void setMarkup(InlineKeyboardMarkup markup) {
        this.markup = markup;
    }



    public Set<Message> getPreviousMessageSet() {
        return previousMessageSet;
    }

    public void setPreviousMessageSet(Set<Message> previousMessageSet) {
        this.previousMessageSet = previousMessageSet;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public boolean isMenu() {
        return menu;
    }

    public void setMenu(boolean menu) {
        this.menu = menu;
    }
}
