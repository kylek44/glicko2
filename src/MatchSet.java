import java.util.ArrayList;
import java.util.List;

public class MatchSet {
    private final Player player;
    private final List<Match> matches;

    public MatchSet(Player player, int size) {
        this.player = player;
        this.matches = new ArrayList<>(size);
    }

    public Player getPlayer() {
        return player;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void addMatch(Match match) {
        matches.add(match);
    }
}
