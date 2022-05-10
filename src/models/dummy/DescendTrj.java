package models.dummy;

import spatial.Trajectory;

public class DescendTrj implements Comparable {
    Trajectory trj;
    double value;   // the similarity between realTrj and dummy

    DescendTrj(Trajectory _trj, double _s) {
        trj = _trj;
        value = _s;
    }

    Trajectory get_trajectory() {
        return trj;
    }

    double get_value() {
        return value;
    }

    @Override
    public int compareTo(Object obj) {
        if (obj instanceof DescendTrj a) {
            return Double.compare(a.value, this.value);      // if this.sim < a.sim, return 1, switch
            // it is descending ordering
        }
        throw new IllegalArgumentException("Error in DescendTrj.compareTo.");
    }
}

