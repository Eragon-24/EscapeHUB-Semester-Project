package com.escapehub.gameplay.application;


public class GameSessionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GameSessionNotFoundException(final String message) {
        super(message);
    }
}