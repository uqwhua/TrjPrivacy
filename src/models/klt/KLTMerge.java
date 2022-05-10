package models.klt;

import models.glove.Formula;
import spatial.ComplexPoint;
import spatial.Grid;
import spatial.Trajectory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static models.glove.Merge.reshaping;
import static spatial.Grid.TotalCategoryNum;

public class KLTMerge {

    static Trajectory mergeTrace(final Trajectory a, final Trajectory b, final Grid grid,
                                 final int l_diversity, final float t_closeness, double[] poiDistribution) {
        List<ComplexPoint> longer, shorter;
        int nL, nS;
        if (a.get_length() > b.get_length()) {
            longer = a.get_pointSeq();
            shorter = b.get_pointSeq();
            nL = a.isMerged() ? a.get_mergedIds().size() : 1;
            nS = b.isMerged() ? b.get_mergedIds().size() : 1;
        } else {
            shorter = a.get_pointSeq();
            longer = b.get_pointSeq();
            nS = a.isMerged() ? a.get_mergedIds().size() : 1;
            nL = b.isMerged() ? b.get_mergedIds().size() : 1;
        }

        ComplexPoint[] once = mergeOnce(longer, shorter, nL, nS, grid, l_diversity, t_closeness, poiDistribution);

        //第一把合完，数组可能会有一些位置是null，相对应的index就说明，在短路径的相对应index上，那个点没有被合并过，我们要把这些点记录下来
        //相应的的不为null的位置，把他们的index作为key存入map当中，方便之后的二次合并
        List<ComplexPoint> unmerged = new ArrayList<>();
        List<ComplexPoint> merged = new ArrayList<>();
        for (int i = 0; i < once.length; i++) {     // once.length == shorter.length
            if (once[i] == null) {
                unmerged.add(shorter.get(i));
            } else {
                merged.add(once[i]);
            }
        }

        List<ComplexPoint> twice = mergeTwice(unmerged, merged, nS, nL, grid, l_diversity, t_closeness, poiDistribution);  // already non-null

        Trajectory m = new Trajectory();
        m.set_pointSeq(twice);
        m.set_mergedIds(a, b);
        m.set_K(a.get_K() + b.get_K());

        return reshaping(m);
    }

    private static List<ComplexPoint> mergeTwice(List<ComplexPoint> unmerged, List<ComplexPoint> merged,
                                                 final int nS, final int nL, final Grid grid,
                                                 final int l_diversity, final float t_closeness, final double[] poiDistribution) {

        ComplexPoint[] array = mergeOnce(unmerged, merged, nS, nL, grid, l_diversity, t_closeness, poiDistribution);
        for (int i = 0, idx = 0; i < array.length; i++, idx++) {
            if (array[i] != null) {
                merged.set(idx, array[i]);
            } else if (merged.get(i) == null) { // no new data, and it is originally null, remove it
                merged.remove(i);
                idx--;  // the merged is shorter than array now
            }
        }
        return merged;
    }

    private static ComplexPoint[] mergeOnce(final List<ComplexPoint> longer, final List<ComplexPoint> shorter,
                                            final int nL, final int nS,
                                            final Grid grid, final int l_diversity, final float t_closeness,
                                            final double[] poiDistribution) {
        //把长的 merge into 短的，所以合出来的结果一定是短的那条路径的长度
        int shortLen = shorter.size();
        ComplexPoint[] array = new ComplexPoint[shortLen];
        for (ComplexPoint pl : longer) {
            double minCost = Double.MAX_VALUE;
            int minIdx = -1;
            //长的list的一个点去和短的轨迹的所有点比较代价
            // compare every point in longer list with the shorter one
            for (int i = 0; i < shortLen; i++) {
//                double cost = KLTFormula.computeCost(pl, shorter.get(i), nL, nS, grid, MAX_GridNum);
                double cost = Formula.computeCost(pl, shorter.get(i), nL, nS);
                if (cost < minCost) {
                    minCost = cost;
                    minIdx = i;
                }
            }
            // if it has been merged, merge again
            if (array[minIdx] == null) {
                array[minIdx] = mergePoint(shorter.get(minIdx), pl, grid, l_diversity, t_closeness, poiDistribution);
            } else {
                array[minIdx] = mergePoint(array[minIdx], pl, grid, l_diversity, t_closeness, poiDistribution);
            }
        }
        return array;
    }

    // merge a and b to a new point
    private static ComplexPoint mergePoint(final ComplexPoint a, final ComplexPoint b, final Grid grid,
                                           final int l_diversity, final float t_closeness, double[] poiDistribution) {
        // the first time initialize the new point
        float lng = Math.min(a.getLongitude(), b.getLongitude());
        float lat = Math.min(a.getLatitude(), b.getLatitude());
        float deltaLng = Math.max(a.get_looseLongitude(), b.get_looseLongitude()) - lng;
        float deltaLat = Math.max(a.get_looseLatitude(), b.get_looseLatitude()) - lat;

        /* to satisfy l-diversity */
        Set<Integer> mergedGrids = new HashSet<>();
        int gid = (int) grid.get_gridId_by_lnglat(lng, lat);    // should >= 0
        int gid_Max = (int) grid.get_gridId_by_lnglat(lng + deltaLng, lat + deltaLat);
        int[] row_col = mergeTwoGrids(gid, gid_Max, mergedGrids);

        int[] totalCategory = new int[TotalCategoryNum];
        int diversity = 0;
        for (int g : mergedGrids) {
            diversity = updateDiversity(grid, totalCategory, diversity, g);
        }

        while (diversity < l_diversity) {
            int[] added = addGrids(mergedGrids, row_col, grid);
            if (added == null) {
                System.out.println("[ERROR] Cannot add new grids to satisfy l-diversity.");
                break;
            }
            for (int g : added) {
                diversity = updateDiversity(grid, totalCategory, diversity, g);
            }
        }

        /* to satisfy t-closeness */
        double[] curDistribution = getDistribution(totalCategory);
        double closeness = computeKLdivergence(curDistribution, poiDistribution);
        while (closeness > t_closeness) {

            int[] added = addGrids(mergedGrids, row_col, grid);
            if (added == null) {
                System.out.println("[ERROR] Cannot add new grids to satisfy l-diversity.");
                break;
            }
            for (int g : added) {
                diversity = updateDiversity(grid, totalCategory, diversity, g);
            }
            curDistribution = getDistribution(totalCategory);
            closeness = computeKLdivergence(curDistribution, poiDistribution);
        }

        return createNewPoint(a, b, row_col, grid);
    }

    private static ComplexPoint createNewPoint(ComplexPoint a, ComplexPoint b, int[] row_col, Grid grid) {

        /* to satisfy 2-anonymity in terms of temporal dimension */
        long atime = a.get_exactTime(), btime = b.get_exactTime();
        long time = Math.min(atime, btime);
        long deltaSecond = Math.max(a.get_looseTime(), b.get_looseTime()) - time;

        float[] lnglatMin = grid.get_lnglat_by_gridId(row_col[1], row_col[2]); // the center of the leftbottom grid
        float[] lnglatMax = grid.get_lnglat_by_gridId(row_col[0], row_col[3]); // the center of the rightup grid
        float deltaLng = lnglatMax[0] - lnglatMin[0];
        float deltaLat = lnglatMax[1] - lnglatMin[1];

        return new ComplexPoint(lnglatMin[0], lnglatMin[1], time, deltaLng, deltaLat, deltaSecond);
    }

    private static int updateDiversity(Grid grid, int[] totalCategory, int diversity, int targetGrid) {
        Integer[] categories = grid.get_category_by_gridId(targetGrid);
        if (categories != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i] > 0) {
                    if (totalCategory[i] == 0) {
                        diversity++;
                    }
                    totalCategory[i] += categories[i];
                }
            }
        }
        return diversity;
    }

    private static final int ROW_MIN = 0;
    private static final int ROW_MAX = 1;
    private static final int COL_MIN = 2;
    private static final int COL_MAX = 3;
    static int allColNum = 0;
    static int allRowNum = 0;

    private static int[] mergeTwoGrids(final int gidA, final int gidB, Set<Integer> mergedGrids) {
        int[] row_col = new int[4];
        row_col[ROW_MIN] = allRowNum;   // rowMinId
        row_col[ROW_MAX] = -1;                    // rowMaxId
        row_col[COL_MIN] = allColNum;   // colMinId
        row_col[COL_MAX] = -1;                    // colMaxId

        int row = gidA / allColNum;
        int col = gidA % allColNum;
        row_col[ROW_MIN] = Math.min(row, row_col[ROW_MIN]);
        row_col[ROW_MAX] = Math.max(row, row_col[ROW_MAX]);
        row_col[COL_MIN] = Math.min(col, row_col[COL_MIN]);
        row_col[COL_MAX] = Math.max(col, row_col[COL_MAX]);


        row = gidB / allColNum;
        col = gidB % allColNum;
        row_col[ROW_MIN] = Math.min(row, row_col[ROW_MIN]);
        row_col[ROW_MAX] = Math.max(row, row_col[ROW_MAX]);
        row_col[COL_MIN] = Math.min(col, row_col[COL_MIN]);
        row_col[COL_MAX] = Math.max(col, row_col[COL_MAX]);

        for (row = row_col[ROW_MIN]; row <= row_col[ROW_MAX]; row++) {
            for (col = row_col[COL_MIN]; col <= row_col[COL_MAX]; col++) {
                int gid = row * allColNum + col;
                mergedGrids.add(gid);
            }
        }
        return row_col;
    }

    private static int[] addGrids(Set<Integer> grid_ids, int[] row_col, Grid grid) {

        int[] gids = null;
        int row_num = row_col[ROW_MAX] - row_col[ROW_MIN] + 1;
        int col_num = row_col[COL_MAX] - row_col[COL_MIN] + 1;
        if (row_col[COL_MAX] + 1 < allColNum) { // right
            gids = new int[row_num];
            int idx = 0;
            for (int r = row_col[ROW_MIN]; r <= row_col[ROW_MAX]; r++) {
                gids[idx++] = r * allColNum + row_col[COL_MAX] + 1;
            }
            row_col[COL_MAX]++;
        } else if (row_col[COL_MIN] - 1 >= 0) {   // left
            gids = new int[row_num];
            int idx = 0;
            for (int r = row_col[ROW_MIN]; r <= row_col[ROW_MAX]; r++) {
                gids[idx++] = r * allColNum + row_col[COL_MIN] - 1;
            }
            row_col[COL_MIN]--;
        } else if (row_col[ROW_MIN] - 1 >= 0) {    // up
            gids = new int[col_num];
            int idx = 0;
            for (int c = row_col[COL_MIN]; c <= row_col[COL_MAX]; c++) {
                gids[idx++] = (row_col[ROW_MIN] - 1) * allColNum + c;
            }
            row_col[ROW_MIN]--;
        } else if (row_col[ROW_MAX] + 1 < allRowNum) {   // down
            gids = new int[col_num];
            int idx = 0;
            for (int c = row_col[COL_MIN]; c <= row_col[COL_MAX]; c++) {
                gids[idx++] = (row_col[ROW_MAX] + 1) * allColNum + c;
            }
            row_col[ROW_MAX]++;
        }

        if (gids != null) {
            for (int gid : gids) {
                grid_ids.add(gid);
            }
        }
        return gids;
    }

    private static double computeKLdivergence(double[] current, double[] global) {
        double KLd = 0, accretion;  // the incremental entropy
        float infinity = Float.MAX_VALUE;//无穷大
        //找出相对应poi类别的概率，如果找到了，就将accretion的值更新，并累加到相对熵上面；如果没找到，则增加了为无穷大
        for (int i = 1, len = current.length; i < len; i++) { // !!! No category's id is 0
            accretion = infinity;
            if (current[i] > 0 && global[i] > 0) {
                accretion = current[i] * Math.log(current[i] / global[i]);
            }
            KLd += accretion;
        }
        return KLd;
    }

    static double[] getDistribution(int[] countArray) {
        double sum = 0;
        for (Integer cnt : countArray) {
            sum += cnt;
        }
        double[] distribution = new double[countArray.length];
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = countArray[i] / sum;
        }
        return distribution;
    }
}
