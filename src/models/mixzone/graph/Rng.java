package models.mixzone.graph;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public class Rng {
    private final RealDistribution distribution;

    Rng() {
        distribution = new UniformRealDistribution(0, 1);
        distribution.reseedRandomGenerator(64925784252L);
    }

    double probability() {
        return distribution.sample();
    }
}
