package com.escapehub.gameplay.domain;
import java.util.ArrayList;
import java.util.List;

public class Checker
{
    static List<String> inventory = new ArrayList<>();  //ψευτικη λιστα inventory για ελεγχο αν υπαρχει το κλειδι
    public static boolean IsExitReady()
    {
        return inventory.contains("key");   //επιστρεφει true/false αναλογα αν υπαρχει το κλειδι
    }
}
