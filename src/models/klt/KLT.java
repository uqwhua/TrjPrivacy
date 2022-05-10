package models.klt;

import shared.Utils;
import spatial.Grid;
import spatial.Trajectory;
import models.glove.Glove;
import models.glove.TrajectoryPair;

import java.util.*;

import static spatial.Grid.TotalCategoryNum;

public class KLT {

    public static void execute(final Vector<Trajectory> trajectories, final Grid grid,
                               final int k_anonymity, final int l_diversity, final float t_closeness,
                               Vector<Trajectory> anonymized_trajectories) {
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("\t\t Selected Model: KLT");
        System.out.println("\t\t Title:          Protecting trajectory from semantic attack considering k-anonymity, l-diversity, and t-closeness (2019)");
        System.out.printf( "\t\t Parameters:     1) k-anonymity = %d; 2) l-diversity = %d; 3) t-closeness = %.3f\n", k_anonymity, l_diversity, t_closeness);
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------\n");

        // transform
        int trj_num = trajectories.size();
        Vector<Trajectory> newTrajectories = new Vector<>();
        for(Trajectory trj: trajectories) {
            newTrajectories.add(new Trajectory(trj));
        }

        /* Computing stretch matrix */
        Queue<TrajectoryPair> stretchMatrix = new PriorityQueue<>();
        Glove.computeMatrix(newTrajectories, trj_num, stretchMatrix);

        Set<Integer> toBeAnonymized = new HashSet<>();
        for (int i = 0; i < trj_num; i++) {
            toBeAnonymized.add(newTrajectories.get(i).get_trajectoryId());
        }

        double[] poiDistribution = computeDistribution(grid);   // the global one to be compared

        KLTMerge.allColNum = (int) grid.numOfCellX_col;
        KLTMerge.allRowNum = (int) grid.numOfCellY_row;

        int k = k_anonymity;
        int nextId = trj_num;
        System.out.println("[PROGRESS] Merging trajectories based on pre-computed matrix: ");
        long startTimer = System.currentTimeMillis();
        while (!toBeAnonymized.isEmpty() && !stretchMatrix.isEmpty()) {

            TrajectoryPair minPair = stretchMatrix.poll(); // the pair with currently minimum value of stretch effort
                                            // a & b is going to be merged
            final int idA = minPair.getLeader();
            final int idB = minPair.getPartner();

            // a & b is going to be merged
            Trajectory a = newTrajectories.get(idA);
            Trajectory b = newTrajectories.get(idB);

            if (a == null || b == null) {   // one of the member has been merged
                continue;
            }

            Trajectory m = KLTMerge.mergeTrace(a, b, grid, l_diversity, t_closeness, poiDistribution);
            m.set_trajectoryId(nextId++);
            newTrajectories.add(m);

            // update the status of a and b
            newTrajectories.set(idA, null); toBeAnonymized.remove(idA);
            newTrajectories.set(idB, null); toBeAnonymized.remove(idB);

            stretchMatrix.removeIf(pair -> pair.getLeader() == idA || pair.getLeader() == idB
                    || pair.getPartner() == idA || pair.getPartner() == idB);

            boolean reduceK = Glove.updateMatrix(m, k, newTrajectories, toBeAnonymized, stretchMatrix);
            if(reduceK) {
                k--;
            }
            Utils.showProgress(trj_num - toBeAnonymized.size(), trj_num, "glove-merge");
        }
        System.out.println("100%");

        long timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for merging trajectories: %.3f s, %.3f min\n", timecost / 1000.0, timecost / (1000.0 * 60));

        if (!toBeAnonymized.isEmpty()) {
            System.out.printf("[ALERT] %d trajectories may not satisfy the %d-anonymity.\n", toBeAnonymized.size(), k_anonymity);
        }

        // transfer to anonymized trajectories
        for(Trajectory tj: newTrajectories){
            if(tj != null){
                anonymized_trajectories.add(tj);
            }
        }
        newTrajectories.clear();
        newTrajectories = null;
    }

    /* the POI distribution of the overall city (dataset) */
    private static double[] computeDistribution(final Grid grid) {
        Map<Integer, Integer[]> gridId2categories = grid.get_categories();
        int[] totalCategory = new int[TotalCategoryNum];
        for(int gid: gridId2categories.keySet()){
            Integer[] category = gridId2categories.get(gid);
            for(int i = 0; i < category.length; i++){
                if(category[i] != null) {
                    totalCategory[i] += category[i];
                }
            }
        }
        return KLTMerge.getDistribution(totalCategory);
    }
}
