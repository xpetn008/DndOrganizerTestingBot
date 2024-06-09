package org.example.data.entities.enums;

import org.example.models.exceptions.BadDataTypeException;

public enum GameType {
    CAMPAIGN, ONESHOT, REAL_LIFE_GAME;
    @Override
    public String toString() {
        return switch (this) {
            case CAMPAIGN -> "Campaign";
            case ONESHOT -> "One Shot";
            case REAL_LIFE_GAME -> "LARP";
        };
    }
    public static GameType parseGameType (String typeString) throws BadDataTypeException{
        if (typeString.contains("campaign")){
            return GameType.CAMPAIGN;
        } else if (typeString.contains("oneshot")){
            return GameType.ONESHOT;
        } else if (typeString.contains("realLifeGame")){
            return GameType.REAL_LIFE_GAME;
        } else {
            throw new BadDataTypeException("Bad game type!");
        }
    }
}
