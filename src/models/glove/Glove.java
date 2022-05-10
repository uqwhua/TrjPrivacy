package models.glove;

import shared.Utils;
import spatial.Trajectory;
import java.util.*;

public class Glove {

    public static void execute(final Vector<Trajectory> trajectories, final int k_anonymity,
                               Vector<Trajectory> anonymized_trajectories) {
        System.out.println("\n------------------------------------------------------------------------------------------------------");
        System.out.println("\t\t Selected Model: GLOVE");
        System.out.println("\t\t Title:          Hiding mobile traffic fingerprints with GLOVE (2015)");
        System.out.printf( "\t\t Parameters:     k-anonymity = %d\n", k_anonymity);
        System.out.println("------------------------------------------------------------------------------------------------------\n");

        final int trj_num = trajectories.size();
        Vector<Trajectory> newTrajectories = new Vector<>();
        for(Trajectory trj: trajectories)
            newTrajectories.add(new Trajectory(trj));

        /* Computing stretch matrix ahead */
        Queue<TrajectoryPair> stretchMatrix = new PriorityQueue<>();
        computeMatrix(newTrajectories, trj_num, stretchMatrix);

        mergeTrajectorySet(newTrajectories, trj_num, k_anonymity, stretchMatrix);
        stretchMatrix.clear();
        stretchMatrix = null;

        // transfer to anonymized trajectories
        for (Trajectory tj : newTrajectories) {
            if (tj != null) {
                anonymized_trajectories.add(tj);
            }
        }
        newTrajectories.clear();
        newTrajectories = null;
    }

    private static void mergeTrajectorySet(Vector<Trajectory> newTrajectories, final int trj_num, final int k_anonymity,
                                           Queue<TrajectoryPair> stretchMatrix) {
        Set<Integer> toBeAnonymized = new HashSet<>();
        for (int i = 0; i < trj_num; i++) {
            toBeAnonymized.add(newTrajectories.get(i).get_trajectoryId());
        }

        int nextId = trj_num;
        int k = k_anonymity;
        System.out.print("[PROGRESS] Merging trajectories based on pre-computed costs: ");
        long startTimer = System.currentTimeMillis();
        while (toBeAnonymized.size() >= k && !stretchMatrix.isEmpty()) {

            TrajectoryPair minPair = stretchMatrix.poll(); // the pair with currently minimum value of stretch effort

            final int idA = minPair.getLeader();
            final int idB = minPair.getPartner();

            // a & b is going to be merged
            Trajectory a = newTrajectories.get(idA), b = newTrajectories.get(idB);
            if (a == null || b == null) {   // one of the member has been merged
                continue;
            }

            Trajectory m = Merge.mergeTrace(a, b);
            m.set_trajectoryId(nextId++);
            newTrajectories.add(m);

            // update the status of a and b
            newTrajectories.set(idA, null); toBeAnonymized.remove(idA);
            newTrajectories.set(idB, null); toBeAnonymized.remove(idB);

            // all relevant pairs should be erased
            stretchMatrix.removeIf(pair -> pair.getLeader() == idA || pair.getLeader() == idB
                    || pair.getPartner() == idA || pair.getPartner() == idB);

            // new trajectory should be computed with others for further merging
            boolean reduceK = updateMatrix(m, k, newTrajectories, toBeAnonymized, stretchMatrix);
            if (reduceK) {
                k--;
            }
            Utils.showProgress(trj_num - toBeAnonymized.size(), trj_num, "glove-merge");
        }
        System.out.println("100%");

        long timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for merging trajectories: %.3f s, %.3f min\n\n",
                timecost / 1000.0, timecost / (1000.0 * 60));

        if (!toBeAnonymized.isEmpty()) {
            System.out.printf("[ALERT] %d trajectories may not satisfy the %d-anonymity.\n", toBeAnonymized.size(), k_anonymity);
        }
    }

    public static boolean updateMatrix(final Trajectory newTrj, final int k_anonymity, final Vector<Trajectory> newTrajectories,
                                       Set<Integer> toBeAnonymized, Queue<TrajectoryPair> stretchMatrix) {
        if (newTrj.get_K() < k_anonymity) {
            toBeAnonymized.add(newTrj.get_trajectoryId());
            if (toBeAnonymized.size() >= k_anonymity) {
                for (int id : toBeAnonymized) {
                    if (id != newTrj.get_trajectoryId()) {
                        Trajectory trj = newTrajectories.get(id);
                        if (trj != null) {
                            TrajectoryPair stretch = Formula.computeStretch(newTrj, trj);
                            stretchMatrix.add(stretch);
                        }
                    }
                }
            }
            else {
                return true;
            }
        }
        return false;
    }

    // compute the pairwise merge cost
    // !!! Note that this step is very time-consuming
    public static void computeMatrix(final Vector<Trajectory> newTrajectories, final int total_trj,
                                     Queue<TrajectoryPair> stretchMatrix) {
        System.out.print("[PROGRESS] Computing Trajectory-wise merge cost matrix: ");

        long startTimer = System.currentTimeMillis();
        for (int i = 0; i < total_trj - 1; i++) {
            Trajectory trj = newTrajectories.get(i);
            for (int j = i + 1; j < total_trj; j++) {
                TrajectoryPair pair = Formula.computeStretch(trj, newTrajectories.get(j));
                stretchMatrix.add(pair);
            }
            if(i > 0)
                Utils.showProgress(i, total_trj, "");
        }
        System.out.println("100%");

        long timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for computing stretch matrix: %.3f s, %.3f min\n\n", timecost / 1000.0, timecost / (1000.0 * 60));
    }
}


