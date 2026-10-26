package com.shipshape.identity;

public class EmailAlreadyTakenException extends RuntimeException{

    public EmailAlreadyTakenException() {
        super("Email is already registered");
    }

}
