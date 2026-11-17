package com.escapehub.gameplay.domain;

public class ExitGame
{
    public void exitGame() {
        if (Checker.IsExitReady()) {
            System.out.println("end of the game!"); //εφοσον υπαρχει το κλειδι τελειωνει το παιχνιδι
        }
    }
}