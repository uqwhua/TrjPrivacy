package models.glove;

public class TrajectoryPair implements Comparable {
    int leaderId;       // the smaller one
    int partnerId;   // the larger one
    float value;

    public TrajectoryPair(final int idA, final int idB) {
        leaderId = idA;
        partnerId = idB;
    }

    public TrajectoryPair(final int idA, final int idB, final float v) {
        leaderId = idA;
        partnerId = idB;
        value = v;
    }

    public int getLeader() {
        return leaderId;
    }

    public int getPartner() {
        return partnerId;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        value = v;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrajectoryPair a) {
            return (a.getLeader() == this.leaderId && a.getPartner() == this.partnerId)
                    || (a.getLeader() == this.partnerId && a.getPartner() == this.leaderId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Math.min(leaderId, partnerId);
        result += 31 * Math.min(leaderId, partnerId);
        return result;
    }

    @Override
    public int compareTo(Object obj) {
        if (obj instanceof TrajectoryPair a) {
            return Double.compare(this.value, a.value);      // if this.sim > a.sim, return 1
            // it is ascending ordering
        }

        throw new IllegalArgumentException("Error in TrajectoryPair.compareTo.");
    }


    @Override
    public String toString() {
        return "(" + leaderId + ", " + partnerId + ", " + value + ")";
    }

    public boolean contains(int i) {
        return leaderId == i || partnerId == i;
    }

}
