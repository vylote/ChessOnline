package model;

public class GameState {
    public static State currentState = State.MENU;
    public static void setState (State newState) {
        currentState = newState;
    }
}
