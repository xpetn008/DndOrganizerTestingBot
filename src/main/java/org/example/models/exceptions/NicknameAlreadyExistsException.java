package org.example.models.exceptions;

public class NicknameAlreadyExistsException extends Exception{
    public NicknameAlreadyExistsException (String message){
        super(message);
    }
}
