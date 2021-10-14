public class Player implements Comparable<Player> {
    private final int id;
    private double rating;
    private double ratingDeviation;
    private double volatility;

    public Player(int id, double rating, double ratingDeviation, double volatility) {
        this.id = id;
        this.rating = rating;
        this.ratingDeviation = ratingDeviation;
        this.volatility = volatility;
    }

    public int getId() {
        return id;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public double getRatingDeviation() {
        return ratingDeviation;
    }

    public void setRatingDeviation(double ratingDeviation) {
        this.ratingDeviation = ratingDeviation;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public double getRatingG2() {
        return (rating - Glicko2.BASE_RATING) / Glicko2.RATING_DIVISOR;
    }

    public double getRatingDeviationG2() {
        return ratingDeviation / Glicko2.RATING_DIVISOR;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Player)) {
            return false;
        }

        Player other = (Player) obj;

        return this.id == other.getId();
    }

    @Override
    public int compareTo(Player o) {
        return Double.compare(this.rating, o.getRating());
    }
}
