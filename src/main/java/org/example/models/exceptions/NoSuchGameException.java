package org.example.models.exceptions;

public class NoSuchGameException extends Exception{
    public NoSuchGameException(String message){
        super(message);
    }
}
