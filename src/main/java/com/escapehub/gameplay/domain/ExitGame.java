package com.escapehub.gameplay.domain;

public class ExitGame
{

    private boolean completed=false;

    public boolean exitGame() {
        if (Checker.IsExitReady())
        {
            completed = true;
            return completed; //εφοσον υπαρχει το κλειδι τελειωνει το παιχνιδι
        }
        else
        {
            return completed;
        }
    }
}