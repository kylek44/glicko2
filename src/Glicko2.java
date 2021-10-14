import java.io.*;
import java.util.*;

public class Glicko2 {
    public static final double BASE_RATING = 1500;
    public static final double RATING_DIVISOR = 173.7178;

    private static final double TAU = 0.7;
    private static final double EPSILON = 0.000001;
    private static final double BASE_RATING_DEVIATION = 350;
    private static final double BASE_VOLATILITY = 0.06;
    private static final double WIN = 1.0;
    private static final double LOSS = 0;

    public static Map<Integer, MatchSet> loadMatches(String filename, Map<Integer, Player> players) {
        Map<Integer, MatchSet> map = new HashMap<>();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(filename));
            String line;

            in.readLine();
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split(",");
                if (!map.containsKey(Integer.parseInt(tokens[0]))) {
                    map.put(Integer.parseInt(tokens[0]), new MatchSet(players.get(Integer.parseInt(tokens[0])), 50));
                }
                if (!map.containsKey(Integer.parseInt(tokens[1]))) {
                    map.put(Integer.parseInt(tokens[1]), new MatchSet(players.get(Integer.parseInt(tokens[1])), 50));
                }
                map.get(Integer.parseInt(tokens[0])).addMatch(new Match(players.get(Integer.parseInt(tokens[1])), true));
                map.get(Integer.parseInt(tokens[1])).addMatch(new Match(players.get(Integer.parseInt(tokens[0])), false));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    public static Map<Integer, Player> loadPlayers(String filename) {
        Map<Integer, Player> map = new HashMap<>();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(filename));
            String line;

            while ((line = in.readLine()) != null) {
                String[] tokens = line.split(",");
                if (!map.containsKey(Integer.parseInt(tokens[0]))) {
                    map.put(Integer.parseInt(tokens[0]), new Player(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    public static double estimatedVariance(MatchSet matchSet) {
        Player player = matchSet.getPlayer();
        List<Match> matches = matchSet.getMatches();
        double playerRating = player.getRatingG2();
        double v = 0;

        for (Match m : matches) {
            double opponentRating = m.getOpponent().getRatingG2();
            double opponentRatingDeviation = m.getOpponent().getRatingDeviationG2();
            v += Math.pow(calculateG(opponentRatingDeviation), 2) * calculateE(playerRating, opponentRating, opponentRatingDeviation) * (1 - calculateE(playerRating, opponentRating, opponentRatingDeviation));
        }

        return v == 0 ? 0 : 1 / v;
    }

    public static double calculateDelta(MatchSet matchSet) {
        Player player = matchSet.getPlayer();
        List<Match> matches = matchSet.getMatches();
        double playerRating = player.getRatingG2();
        double v = estimatedVariance(matchSet);
        double sum = calculateSumChange(matches, playerRating);
        return v * sum;
    }

    public static double calculateSumChange(List<Match> matches, double playerRating) {
        double sum = 0;

        for (Match m: matches) {
            double s = m.won() ? WIN : LOSS;
            double opponentRating = m.getOpponent().getRatingG2();
            double opponentRatingDeviation = m.getOpponent().getRatingDeviationG2();
            sum += calculateG(opponentRatingDeviation) * (s - calculateE(playerRating, opponentRating, opponentRatingDeviation));
        }

        return sum;
    }

    public static double calculateG(double ratingDeviation) {
        return 1 / Math.sqrt(1 + (3 * (ratingDeviation * ratingDeviation)) / (Math.PI * Math.PI));
    }

    public static double calculateE(double playerRating, double opponentRating, double opponentRatingDeviation) {
        return 1 / (1 + Math.exp(-1 * calculateG(opponentRatingDeviation) * (playerRating - opponentRating)));
    }

    public static double calculateF(double x, double delta, double ratingDeviation, double variance, double a) {
        double p1 = Math.exp(x) * ((delta * delta) - (ratingDeviation * ratingDeviation) - variance - Math.exp(x));
        double p2 = 2 * Math.pow(((ratingDeviation * ratingDeviation) + variance + Math.exp(x)), 2);
        double p3 = (x - a) / (TAU * TAU);
        return p1 / p2 - p3;
    }

    public static double calculateNewVolatility(MatchSet matchSet) {
        Player player = matchSet.getPlayer();
        double ratingDeviation = player.getRatingDeviationG2();
        double volatility = player.getVolatility();
        double a = Math.log(volatility * volatility);
        double A = a;
        double B = 0;
        double delta = calculateDelta(matchSet);
        double variance = estimatedVariance(matchSet);

        if ((delta * delta) > (ratingDeviation * ratingDeviation + variance)) {
            B = Math.log(delta * delta - ratingDeviation * ratingDeviation - variance);
        } else {
            double k = 1;
            double f = calculateF((a - k * TAU), delta, ratingDeviation, variance, a);
            while (f < 0) {
                k++;
                f = calculateF((a - k * TAU), delta, ratingDeviation, variance, a);
            }
            B = a - k * TAU;
        }

        double fA = calculateF(A, delta, ratingDeviation, variance, a);
        double fB = calculateF(B, delta, ratingDeviation, variance, a);

        while (Math.abs(B - A) > EPSILON) {
            double C = A + (A - B) * fA / (fB - fA);
            double fC = calculateF(C, delta, ratingDeviation, variance, a);
            if (fC * fB < 0) {
                A = B;
                fA = fB;
            } else {
                fA = fA / 2;
            }
            B = C;
            fB = fC;
        }

        return Math.exp(A / 2);
    }

    public static double calculatePreRatingDeviation(MatchSet matchSet) {
        double ratingDeviation = matchSet.getPlayer().getRatingDeviationG2();
        double volatility = calculateNewVolatility(matchSet);
        return Math.sqrt((ratingDeviation * ratingDeviation) + (volatility * volatility));
    }

    public static double calculateNewRatingDeviation(MatchSet matchSet) {
        double preRatingDeviation = calculatePreRatingDeviation(matchSet);
        double variance = estimatedVariance(matchSet);
        return 1 / Math.sqrt((1 / (preRatingDeviation * preRatingDeviation)) + (1 / variance));
    }

    public static double calculateNewRating(MatchSet matchSet) {
        Player player = matchSet.getPlayer();
        List<Match> matches = matchSet.getMatches();
        double playerRating = player.getRatingG2();
        double newRatingDeviation = calculateNewRatingDeviation(matchSet);
        double sum = calculateSumChange(matches, playerRating);
        return playerRating + (newRatingDeviation * newRatingDeviation) * sum;
    }

    public static void update(Map<Integer, Player> players, Map<Integer, MatchSet> matchSets) {
        Map<Integer, Rating> newRatings = new HashMap<>(players.size());

        for (int i : matchSets.keySet()) {
            double newVolatility = calculateNewVolatility(matchSets.get(i));
            double newRatingDeviation = calculateNewRatingDeviation(matchSets.get(i));
            double newRating = calculateNewRating(matchSets.get(i));
            newRatings.put(i, new Rating(newRating, newRatingDeviation, newVolatility));
        }

        for (int i : newRatings.keySet()) {
            players.get(i).setRating(RATING_DIVISOR * newRatings.get(i).getRating() + BASE_RATING);
            players.get(i).setRatingDeviation(RATING_DIVISOR * newRatings.get(i).getRatingDeviation());
            players.get(i).setVolatility(newRatings.get(i).getVolatility());
        }
    }

    public static void main(String[] args) throws IOException {
        Map<Integer, Player> players = loadPlayers("players.csv");
        Map<Integer, MatchSet> matchSets = loadMatches("data.csv", players);
        update(players, matchSets);
        List<Player> ps = new ArrayList<>(players.values());
        ps.sort(Collections.reverseOrder());
        BufferedWriter out = new BufferedWriter(new FileWriter("ratings0.7.csv"));
        for (Player p : ps) {
            out.write(String.format("%d,%f", p.getId(), p.getRating()));
            out.newLine();
        }
        out.close();
    }
}
