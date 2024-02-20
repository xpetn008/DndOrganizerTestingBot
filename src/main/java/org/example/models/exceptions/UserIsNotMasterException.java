package org.example.models.exceptions;

public class UserIsNotMasterException extends Exception{
    public UserIsNotMasterException (String message){
        super(message);
    }
}
