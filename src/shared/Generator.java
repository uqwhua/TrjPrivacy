package shared;

import spatial.ComplexPoint;
import spatial.Trajectory;

import java.util.*;

public class Generator {

    // generate sample of real trajectory
    public static boolean[] randomlySamplePoints(final Trajectory trj, final double rate, Random rm){
        int len = trj.get_length();
        int neededNum = (int) Math.ceil(rate * len);
        if (neededNum >= len || neededNum <= 0){
            return null;
        }
        boolean[] selectedPoints = new boolean[len];
        int i = 0;
        while (neededNum > 0){
            boolean selected = false;
            if(rm.nextInt(len) < neededNum){
                ComplexPoint p = trj.get_point_by_idx(i);
                if(p != null) {
                    selected = true;
                    neededNum--;
                }
            }

            selectedPoints[i] = selected;
            len--;
            i++;
        }
        return selectedPoints;
    }

    public static int randomPseudonym(final int original_id, final Set<Integer> pre_ids, Random rm) {
        int id;
        do {
            id = rm.nextInt(Integer.MAX_VALUE); // return positive number starting from 0
        } while (id == original_id || pre_ids.contains(id));

        return id;
    }
}
