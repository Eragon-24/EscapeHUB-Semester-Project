package com.escapehub.gameplay.domain;
import java.util.ArrayList;
import java.util.List;

public class Checker
{
    public enum ItemType {              //εκανα τα αντικειμενα για
        KEY1,                            // την ωρα σε enums,
        KEY2,                            // αυτο μπορει να αλλαξει οταν
        KEY3                            //κανουμε κανονικο inventory
    }

    static List<ItemType> inventory = new ArrayList<>();  //ψευτικη λιστα inventory για ελεγχο αν υπαρχει το κλειδι

    public static boolean IsExitReady()
    {
        return inventory.contains(ItemType.KEY1);//επιστρεφει true/false αναλογα αν υπαρχει το κλειδι
        //return inventory.containsAll(List.of(ItemType.KEY1, ItemType.KEY2, ItemType.KEY3)); σε περιπτωση που μπορει να χρειαστουν περισσοτερα κλειδια του ενος
    }
}
