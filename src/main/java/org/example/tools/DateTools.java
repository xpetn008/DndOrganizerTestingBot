package org.example.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTools {
    public static boolean controlDateChars (String date){   //@param - Date String type (dd.MM.yyyy)
        if (date.length() != 10){                           //returns - true if date is dd.mm.yyyy type
            return false;                                   //        - false if date is other type
        } else {
            for (int i = 0; i < date.length(); i++) {
                if (i == 0 || i == 1 || i == 3 || i == 4 || i == 6 || i == 7 || i == 8 || i == 9){
                    String numbers = "0123456789";
                    int controlNumber = 0;
                    for (int j = 0; j < numbers.length(); j++){
                        if (date.charAt(i) == numbers.charAt(j)){
                            controlNumber++;
                        }
                    }
                    if (controlNumber == 0){
                        return false;
                    }
                } else {
                    if (date.charAt(i) != '.'){
                        return false;
                    }
                }
            }
            return true;
        }
    }
    public static boolean controlDate (String dateString){          //@param - Date String type dd.MM.yyyy
        if (controlDateChars(dateString)){                          //controls date chars, parsing date to LocalDate variable and controls if date is at least 1 week away, but no more than 2 years away
            LocalDate date = parseStringToLocalDate(dateString);                 //returns true or false
            LocalDate currentDate = LocalDate.now();
            LocalDate nextWeek = currentDate.plusWeeks(1);
            LocalDate afterTwoYears = currentDate.plusYears(2);
            if (date.isAfter(nextWeek) && date.isBefore(afterTwoYears)){
                return true;
            }else{
                return false;
            }
        } else {
            return false;
        }
    }
    public static LocalDate parseStringToLocalDate(String dateString){                           //@param - Date String type dd.MM.yyyy
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");    //returns - LocalDate variable type dd.MM.yyyy
        return LocalDate.parse(dateString, formatter);
    }
    public static String parseLocalDateToString(LocalDate date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }
}
