package org.example.models.exceptions;

public class UserAlreadyRegisteredException extends Exception{
    public UserAlreadyRegisteredException (String message){
        super(message);
    }
}
