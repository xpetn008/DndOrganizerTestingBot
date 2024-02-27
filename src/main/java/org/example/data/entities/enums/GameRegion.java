package org.example.data.entities.enums;

import org.example.models.exceptions.BadDataTypeException;

public enum GameRegion {
    CZ, RU;

    public String toFullString(){
        return switch (this){
            case CZ -> "Czech Republic";
            case RU -> "Russian Federation";
        };
    }
    @Override
    public String toString(){
        return switch (this){
            case CZ -> "CZ";
            case RU -> "RU";
        };
    }
    public static GameRegion parseGameRegion (String regionString) throws BadDataTypeException{
        regionString = regionString.toLowerCase();
        switch (regionString){
            case "cz" -> {
                return CZ;
            }
            case "ru" -> {
                return RU;
            }
            default -> throw new BadDataTypeException("Bad region!");
        }
    }
}
