package org.example.data.entities.enums;

import org.example.models.exceptions.BadDataTypeException;

public enum GameLanguage {
    CZ, RU, EN;

    public String toFullString(){
        return switch (this){
            case CZ -> "Czech";
            case RU -> "Russian";
            case EN -> "English";
        };
    }
    @Override
    public String toString(){
        return switch (this){
            case CZ -> "CZ";
            case RU -> "RU";
            case EN -> "EN";
        };
    }
    public static GameLanguage parseGameLanguage (String regionString) throws BadDataTypeException{
        regionString = regionString.toLowerCase();
        switch (regionString){
            case "cz" -> {
                return CZ;
            }
            case "ru" -> {
                return RU;
            }
            case "en" -> {
                return EN;
            }
            default -> throw new BadDataTypeException("Bad language!");
        }
    }
}
