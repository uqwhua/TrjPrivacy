package models.glove;

import spatial.ComplexPoint;
import spatial.Trajectory;

import java.util.List;

public class Formula {

    public static final float Omega_Sigma_Tau = 0.5f;
    public static final float MAX_SPACE = 20;      // 20km
    public static final float MAX_MINUTE = 24 * 60;  // 24 hours for the check-in data, 8 hours for taxi trajectories
//    public static final long transfer = 60 * 1000;  // the factor between ms and minute

    // formula 10
    public static TrajectoryPair computeStretch(Trajectory trjA, Trajectory trjB) {
        TrajectoryPair pair = new TrajectoryPair(trjA.get_trajectoryId(), trjB.get_trajectoryId());
        int mA = trjA.get_length();
        int mB = trjB.get_length();
        int nA = trjA.isMerged() ? trjA.get_mergedIds().size() : 1;
        int nB = trjB.isMerged() ? trjB.get_mergedIds().size() : 1;
        float deltaAB = mA > mB ? Formula.minStretchEffort(trjA.get_pointSeq(), trjB.get_pointSeq(), nA, nB) :
                Formula.minStretchEffort(trjB.get_pointSeq(), trjA.get_pointSeq(), nB, nA);

        pair.setValue(deltaAB);   // the average of the efforts
        return pair;
    }

    private static float minStretchEffort(List<ComplexPoint> longer, List<ComplexPoint> shorter, int nL, int nS) {
        // for each sample in the longer fingerprint,
        // find the sample at minimum stretch effort in the shorter fingerprint
        float sum = 0;

        for (ComplexPoint pl : longer) {
            double min = Double.MAX_VALUE;
            for(ComplexPoint ps: shorter){
                float cost = computeCost(pl, ps, nL, nS);
                min = Math.min(cost, min);
            }
            sum += min;
        }
        return sum / longer.size();
    }

    // formula 1: determine the loss of accuracy in space and time induced by the merging of the two points
    public static float computeCost(ComplexPoint a, ComplexPoint b, int nA, int nB){
        float spaceSide = Omega_Sigma_Tau * spatialGeneralize(a, b, nA, nB);
        float timeSide = Omega_Sigma_Tau * temporalGeneralize(a, b, nA, nB);
        return spaceSide + timeSide;
    }

    // formula 2 & 4
    private static float spatialGeneralize(ComplexPoint a, ComplexPoint b, int nA, int nB) {
        float left_ab  = leftStretch_space(a ,b);
        float right_ab = rightStretch_space(a, b);
        float left_ba  = leftStretch_space(b, a);
        float right_ba = rightStretch_space(b, a);
        float space = ((left_ab + right_ab) * nA)/(nA+ nB) + ((left_ba + right_ba) * nB)/(nA+ nB);
        return space <= MAX_SPACE ? space / MAX_SPACE : 1f;
    }

    // formula 5
    private static float leftStretch_space(ComplexPoint a, ComplexPoint b) {
        float x = a.getLongitude() - Math.min(a.getLongitude(), b.getLongitude());
        float y = a.getLatitude() - Math.min(a.getLatitude(), b.getLatitude());
        return x + y;
    }

    // formula 6
    private static float rightStretch_space(ComplexPoint a, ComplexPoint b) {
        float deltaAx = a.get_looseLongitude();
        float deltaAy = a.get_looseLatitude();
        float deltaBx = b.get_looseLongitude();
        float deltaBy = b.get_looseLatitude();
        return ((Math.max(deltaAx, deltaBx)) - deltaAx) + ((Math.max(deltaAy, deltaBy)) - deltaAy);
    }

    // formula 3 & 7
    private static float temporalGeneralize(ComplexPoint a, ComplexPoint b, int nA, int nB) {
        float left_ab = leftStretch_time(a, b);
        float right_ab = rightStretch_time(a, b);
        float left_ba = leftStretch_time(b, a);
        float right_ba = rightStretch_time(b, a);
        float time = ((left_ab + right_ab) * nA) / (nA + nB) + ((left_ba + right_ba) * nB) / (nA + nB);
        return time <= MAX_MINUTE ? time / MAX_MINUTE : 1;
    }

    // formula 8
    private static float leftStretch_time(ComplexPoint a, ComplexPoint b) {
        long aTime = a.get_exactTime(), bTime = b.get_exactTime();  // sec
        long early = Math.min(aTime, bTime);
        return (aTime - early) / 60f;  // transfer to minute
    }

    // formula 9
    private static float rightStretch_time(ComplexPoint a, ComplexPoint b) {
        long deltaA = a.get_looseTime(), deltaB = b.get_looseTime();    // sec
        long late = Math.max(deltaA, deltaB);
        return (late - deltaA) / 60f;  // min
    }

}
