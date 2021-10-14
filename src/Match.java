public class Match {
    private final Player opponent;
    private final boolean won;

    public Match(Player opponent, boolean won) {
        this.opponent = opponent;
        this.won = won;
    }

    public Player getOpponent() {
        return opponent;
    }

    public boolean won() {
        return won;
    }
}
