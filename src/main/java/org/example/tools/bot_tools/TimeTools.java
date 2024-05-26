package org.example.tools.bot_tools;

import org.springframework.cglib.core.Local;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeTools {
    public static boolean controlTimeChars (String time){
        if (time.length() != 5){
            return false;
        } else {
            for (int i = 0; i < time.length(); i++){
                if (i == 0 || i == 1 || i == 3 || i == 4){
                    String numbers = "0123456789";
                    int controlNumber = 0;
                    for (int j = 0; j < numbers.length(); j++){
                        if (time.charAt(i) == numbers.charAt(j)){
                            controlNumber++;
                        }
                    }
                    if (controlNumber == 0){
                        return false;
                    }
                } else {
                    if (time.charAt(i) != ':'){
                        return false;
                    }
                }
            }
            return true;
        }
    }
    public static LocalTime parseStringToLocalTime (String timeString) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(timeString, formatter);
    }
    public static String parseLocalTimeToString (LocalTime time){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
}
