package com.escapehub;

public class ExitGame
{
    public void exitGame() {
        if (Checker.ExitReady()) {
            System.out.println("end of the game!"); //εφοσον υπαρχει το κλειδι τελειωνει το παιχνιδι
        }
    }
}