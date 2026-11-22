package com.escapehub.team.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Μια ομάδα με το όνομα, τον μοναδικό κωδικό συμμετοχής και τα μέλη της (US-009, US-010).
 *
 * <p>Χρήση:
 * <pre>
 * Team team = Team.create("Escapers", 42L, "K7P3QX"); // 42 = id του δημιουργού
 * team.isCreator(42L);   // true
 * team.addMember(7L);    // μπαίνει νέο μέλος με ρόλο MEMBER
 * team.isMember(7L);     // true
 * </pre>
 */
public class Team {

    /** Ρόλος του χρήστη μέσα στην ομάδα. */
    public enum Role { CREATOR, MEMBER }

    /** Η σχέση ενός χρήστη με την ομάδα. */
    public record Member(long userId, Role role) { }

    private Long id; // το δίνει ο repository μετά το πρώτο save
    private final String name;
    private final String joinCode;
    private final List<Member> members = new ArrayList<>();

    private Team(String name, String joinCode) {
        this.name = name;
        this.joinCode = joinCode;
    }

    /** Δημιουργεί νέα ομάδα. Ο δημιουργός μπαίνει αυτόματα ως μέλος με ρόλο CREATOR. */
    public static Team create(String name, long creatorUserId, String joinCode) {
        Team team = new Team(name, joinCode);
        team.members.add(new Member(creatorUserId, Role.CREATOR));
        return team;
    }

    /** Ξαναφτιάχνει ομάδα που φορτώθηκε από την αποθήκευση. Για χρήση από repositories. */
    public static Team restore(Long id, String name, String joinCode, List<Member> members) {
        Team team = new Team(name, joinCode);
        team.id = id;
        team.members.addAll(members);
        return team;
    }

    /**
     * Προσθέτει νέο μέλος με ρόλο MEMBER.
     *
     * @throws IllegalStateException αν ο χρήστης είναι ήδη μέλος της ομάδας
     */
    public void addMember(long userId) {
        if (isMember(userId)) {
            throw new IllegalStateException("User " + userId + " is already a member");
        }
        members.add(new Member(userId, Role.MEMBER));
    }

    public Long getId() {
        return id;
    }

    /** Καλείται από τον repository μετά την αποθήκευση. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
     public void removeMember(long userId)
     {
         members.removeIf(member -> member.userId()==userId);

     }

    public String getJoinCode() {
        return joinCode;
    }

    /** Τα μέλη (μόνο για ανάγνωση). */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean isMember(long userId) {
        return members.stream().anyMatch(m -> m.userId() == userId);
    }

    public boolean isCreator(long userId) {
        return members.stream().anyMatch(m -> m.userId() == userId && m.role() == Role.CREATOR);
    }

}