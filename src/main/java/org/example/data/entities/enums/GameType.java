package org.example.data.entities.enums;

import org.example.models.exceptions.BadDataTypeException;

public enum GameType {
    CAMPAIGN, ONESHOT;
    @Override
    public String toString() {
        return switch (this) {
            case CAMPAIGN -> "Campaign";
            case ONESHOT -> "One Shot";
        };
    }
    public static GameType parseGameType (String typeString) throws BadDataTypeException{
        typeString = typeString.toLowerCase();
        if (typeString.contains("campaign")){
            return GameType.CAMPAIGN;
        } else if (typeString.contains("oneshot")){
            return GameType.ONESHOT;
        } else {
            throw new BadDataTypeException("Bad game type!");
        }
    }
}
