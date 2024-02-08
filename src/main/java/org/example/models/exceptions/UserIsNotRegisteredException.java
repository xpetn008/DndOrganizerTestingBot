package org.example.models.exceptions;

public class UserIsNotRegisteredException extends Exception{
    public UserIsNotRegisteredException (String message){
        super(message);
    }
}
