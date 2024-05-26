package org.example.tools.bot_tools;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class Message {
    private String text;
    private long chatId;
    private InlineKeyboardMarkup markup;
    private boolean menu;
    private Message previousMessage;

    public Message (){}
    public Message (String text, long chatId, InlineKeyboardMarkup markup, boolean menu){
        this.text = text;
        this.chatId = chatId;
        this.markup = markup;
        this.menu = menu;
        previousMessage = null;
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

    public boolean isMenu() {
        return menu;
    }

    public void setMenu(boolean menu) {
        this.menu = menu;
    }

    public Message getPreviousMessage() {
        return previousMessage;
    }

    public void setPreviousMessage(Message previousMessage) {
        if (menu){
            this.previousMessage = null;
        } else {
            this.previousMessage = previousMessage;
        }
    }
}
