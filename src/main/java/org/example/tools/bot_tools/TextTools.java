package org.example.tools.bot_tools;

import javax.print.DocFlavor;

public class TextTools {
    public static String makeTextBold (String text){
        return "<b>"+text+"</b>";
    }
    public static boolean containsOnlyNumbers (String text){
        String numbers = "0123456789";
        for (int i = 0; i<text.length(); i++){
            boolean accordance = false;
            for (int j = 0; j<numbers.length();j++){
                if (text.charAt(i) == numbers.charAt(j)){
                    accordance = true;
                }
            }
            if (!accordance){
                return false;
            }
        }
        return true;
    }
}
