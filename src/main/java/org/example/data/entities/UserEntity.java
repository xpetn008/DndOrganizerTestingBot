package org.example.data.entities;

import jakarta.persistence.*;

@Entity
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username")
    private String username;
    @Column(name = "telegram_id")
    private Long telegramId;
    @Column(name = "master")
    private boolean master;
    @Column(name = "master_nickname")
    private String masterNickname;

    public UserEntity (){}
    public UserEntity (String username, Long telegramId, boolean master){
        this.username = username;
        this.telegramId = telegramId;
        this.master = master;
    }
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public String getMasterNickname() {
        return masterNickname;
    }

    public void setMasterNickname(String masterNickname) {
        this.masterNickname = masterNickname;
    }
}
