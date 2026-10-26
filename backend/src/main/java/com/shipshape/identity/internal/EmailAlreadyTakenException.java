package com.shipshape.identity.internal;

public class EmailAlreadyTakenException extends RuntimeException{

    public EmailAlreadyTakenException() {
        super("Email is already registered");
    }
    
}
