package models.dummy;

import spatial.ComplexPoint;
import spatial.SimplePoint;
import spatial.Trajectory;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Calculate {

    static double trajDifference(final Trajectory real_trj, final Trajectory trj, final int elNum) {
        int diff = 0, len = real_trj.get_length();
        ComplexPoint rp, p;
        for (int i = 0; i < len; i++) {
            rp = real_trj.get_point_by_idx(i);
            p = trj.get_point_by_idx(i);
            if (!p.equals(rp)) {
                diff++;
            }
        }
        return diff * 1.0 / (len - elNum);
    }

    static double locationDiversity(final Trajectory trj, final Vector<Map<ComplexPoint, Integer>> selected_locations) {
        int len = trj.get_length();
        double diversity = 0;
        for (int i = 0; i < len; i++) {
            Map<ComplexPoint, Integer> map = selected_locations.get(i);
            int num = map.size();   // # of points at this time
            Integer cnt = map.get(trj.get_point_by_idx(i));
            if (cnt != null && cnt > 0) {
                diversity += (1.0 / cnt) * (1.0 / num); // contribution
            }
        }
        if (diversity > 0)
            return diversity;// / len;
        else
            return -1;
    }


    static double trajSimilarity(final Trajectory realTrj, final Trajectory trj) {
        double sim = 0;
        int len = realTrj.get_length();
        double avg = 0;
        double[] pairDist = new double[len];
        for (int i = 0; i < len; i++) {
            pairDist[i] = SimplePoint.getDistance(realTrj.get_point_by_idx(i), trj.get_point_by_idx(i));
            avg += pairDist[i];
        }
        avg /= len;
        for (double dist: pairDist) {
            sim += Math.pow(dist - avg, 2);
        }
        return 1.0 / Math.sqrt(sim / len);
    }

    // a and b should be the same length
    static double trajectoryDist(Trajectory a, Trajectory b) {
        double dis = 0;
        for (int i = 0, len = a.get_length(); i < len; i++) {
            dis += SimplePoint.getDistance(a.get_point_by_idx(i), b.get_point_by_idx(i));
        }
        return dis;
    }

    static double get_max_speed(final List<ComplexPoint> pointSeq) {
        double v_max = Double.MIN_VALUE;
        ComplexPoint p_pre, p_cur;
        for (int i = 1, len = pointSeq.size(); i < len; i++) {
            p_pre = pointSeq.get(i - 1);
            p_cur = pointSeq.get(i);
            double dis = SimplePoint.getDistance(p_cur, p_pre);
            long t_diff = p_cur.get_exactTime() - p_pre.get_exactTime();            // unit: second
            if (t_diff > 0) {
                double v = dis / t_diff;
                v_max = Math.max(v, v_max);
            }
        }
        return v_max;
    }
}
