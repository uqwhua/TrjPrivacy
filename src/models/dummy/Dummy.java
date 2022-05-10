package models.dummy;

import spatial.*;
import shared.FileOutput;
import shared.Generator;
import shared.Utils;

import java.io.IOException;
import java.util.*;


public class Dummy {
    /**
     * Dummy-Based Trajectory Privacy Protection
     * to achieve the (p,k)-anonymity
     * default setting:
     *
     * @param ratio_EL      the paper limits # of exposure locations, aka |EL| within [1, 4]
     * @param k_anonymity   range [3, 9]
     * @param loc_anonymity aka p, location anonymity: [6, 27]
     */
    public static void execute(final Vector<Trajectory> trajectories, final Grid grid, final double ratio_EL,
                               final int k_anonymity, final int loc_anonymity, final int min_length,
                               Vector<Trajectory> anonymized_trajectories, final String outputFile) throws IOException {

        System.out.println("\n------------------------------------------------------------------------------------------------------");
        System.out.println("\t\t Selected Model: DUMMY");
        System.out.println("\t\t Title:          Dummy-Based Trajectory Privacy Protection Against Exposure Location Attacks (2019)");
        System.out.printf( "\t\t Parameters:     1) k-anonymity = %d; 2) location-anonymity = %d; 3) exposure ratio = %.2f\n", k_anonymity, loc_anonymity, ratio_EL);
        System.out.println("------------------------------------------------------------------------------------------------------");
        System.out.print("\n[PROGRESS] ");

        Random rm = new Random();
        Trajectory trj;

        List<Trajectory> anonymized_trjs;
        int i = 0, total = trajectories.size(), suppress = 0;
        double removeRatio = 0;
        Iterator<Trajectory> itr = trajectories.iterator();

        long startTimer, totalTime = 0;
        while (itr.hasNext() && i++ <= total) {     // !!! for some datasets, trajectories.size() > need_obj
            trj = itr.next();
            final int trjLen = trj.get_length();
            if (trjLen * ratio_EL < 1 || trjLen < min_length || (max_trj_length > 0 && trjLen > max_trj_length)) {
                suppress++;
                continue;
            }

            Utils.showProgress(i, total, "Dummy");

            startTimer = System.currentTimeMillis();

            // exposure locations sampled from true trajectories
            boolean[] EL = Generator.randomlySamplePoints(trj, ratio_EL, rm); // the list of exposure locations

            anonymized_trjs = new ArrayList<>();
            double ratio = generateDummy(trj, EL, grid, loc_anonymity, k_anonymity, anonymized_trjs);

            totalTime += (System.currentTimeMillis() - startTimer);

            if (ratio < 0) {
                suppress++;
                itr.remove();
            }
            else {
                removeRatio += ratio;
                if (outputFile.equals(""))
                    anonymized_trajectories.addAll(anonymized_trjs);
                else
                    FileOutput.outputAnonymizedTrj(outputFile, anonymized_trjs);
            }

            // clear up for this round
            anonymized_trjs.clear();
        }
        // end of scanning all trajectories
        System.out.println("100%");

        System.out.printf("\n[TIME-COST] for generating dummy trajectories: %.3f s, %.3f min\n",
                totalTime / 1000.0, totalTime / (1000.0 * 60));

        // printout statistics
        System.out.printf("\n[REPORT] avg ratio of removed points = %.5f\n ", removeRatio / (total - suppress));
        if (suppress > 0)
            System.out.println("\t # of suppressed trajectories = " + suppress);
    }

    /**
     * DTC: Generating Dummy Trajectory Candidate Set
     *
     * @param loc_anonymity threshold, aka the min # of a point's candidates
     */
    private static double generateDummy(final Trajectory realTrj, final boolean[] EL, final Grid grid,
                                        final int loc_anonymity, final int k_anonymity,
                                        List<Trajectory> results) {

        /* -- (1) generate dummy locations based on grids and POIs -- */
        Vector<Set<SimplePoint>> dummy_locations = generateDummyLocations(realTrj, EL, grid, loc_anonymity);
        if (dummy_locations.isEmpty() || dummy_locations.stream().mapToInt(Set::size).sum() == 0) {
            return -1;
        }
        int elNum = 0;
        for (boolean b : EL)
            elNum += (b ? 1 : 0);

        /* -- (2) generate directed graph */
        DirGraph g = generateDirGraph(realTrj, dummy_locations);

        /* -- (3) construct connected path on the graph as the dummy trajectories -- */
        Queue<DescendTrj> candidates = new PriorityQueue<>();
        Set<SimplePoint> startPoints = dummy_locations.firstElement(); // the candidate starting points of all dummy trajectories
        int len = realTrj.get_length();
        for (SimplePoint p : startPoints) {
            // execute DFS on the graph to find all trajectories starting from point p
            List<Trajectory> candidatePaths = g.DFS(p, realTrj, loc_anonymity, k_anonymity);
            if (candidatePaths != null) {
                for (Trajectory trj : candidatePaths) {
                    double sim = Calculate.trajSimilarity(realTrj, trj);
                    double diff = Calculate.trajDifference(realTrj, trj, elNum);
                    if (diff > 0) {
                        candidates.add(new DescendTrj(trj, sim * diff));    // descending order of the score
//                        updateLocationSet(trj, selected_locations);
                    }
                }
            }
        }

//        Queue<DescendTrj> queue = new PriorityQueue<>(candidates);
//        candidates.clear();
//        while (!queue.isEmpty()) {
//            DescendTrj trj = queue.poll();
//            double div = Calculate.computeDiversity(trj.get_trajectory(), selected_locations); // update the score
//            if (div > 0) {
//                trj.set_value(trj.get_value() * div);
//                candidates.add(trj);
//            }
//        }

        while (results.size() < k_anonymity - 1 && !candidates.isEmpty()) {
            Trajectory trj = candidates.poll().get_trajectory();     // the trj with max score/similarity
            results.add(trj);
            updateCandidates(results, candidates, elNum);       // update the ordering of candidates
        }
        results.add(realTrj);   // itself

        // check k-anonymity
        if (results.size() < k_anonymity) {
            return -1;
        }

        // check location anonymity
        boolean[] toBeRemove = new boolean[len];
        int num_remove = checkLocationAnonymity(results, EL, realTrj, loc_anonymity, toBeRemove);
        if (num_remove == len - elNum) {
            return -1;
        }

        // remove the unsafe points that cannot satisfy the location anonymity
        // the first one is the real trajectory
        Random rm = new Random();
        finalizeDummy(results, toBeRemove, realTrj, len, rm);

        return  num_remove * 1.0 / len;
    }

    private static void updateCandidates(final List<Trajectory> currentPickedTrjs, Queue<DescendTrj> remainTrjs, final int elNum) {
        Queue<DescendTrj> tmp = new PriorityQueue<>(remainTrjs);
        remainTrjs.clear();
        int totalPick = currentPickedTrjs.size();
        while (!tmp.isEmpty()) {
            DescendTrj trj = tmp.poll();
            double avgDiff = 0;
            for (Trajectory pickedTrj : currentPickedTrjs) {
                avgDiff += Calculate.trajDifference(pickedTrj, trj.get_trajectory(), elNum);
            }
            remainTrjs.add(new DescendTrj(trj.get_trajectory(), trj.get_value() * (avgDiff / totalPick)));
        }
    }

    private static int checkLocationAnonymity(final List<Trajectory> results, final boolean[] EL,
                                              final Trajectory realTrj, final int loc_anonymity, boolean[] toBeRemove) {
        Vector<Set<ComplexPoint>> pointSets = new Vector<>();
        Set<ComplexPoint> points;
        int len = realTrj.get_length();
        for (Trajectory trj : results) {
            // count the diversity of each time's location set
            for (int i = 0; i < len; i++) {
                if (i >= pointSets.size()) {    // hasn't met it till now
                    points = new HashSet<>();
                    points.add(trj.get_point_by_idx(i));
                    pointSets.add(points);
                } else {
                    points = pointSets.get(i);
                    points.add(trj.get_point_by_idx(i));
                    pointSets.set(i, points);
                }
            }
        }

        // check location anonymity
        int num_remove = 0;
        for (int i = 0; i < len; i++) {
            if (pointSets.get(i).size() < loc_anonymity && !EL[i]) {
                toBeRemove[i] = true;
                num_remove++;
            }
        }
        return num_remove;
    }

    private static void finalizeDummy(List<Trajectory> results, final boolean[] toBeRemove, Trajectory realTrj, final int real_len, Random rm) {
        Set<Integer> ids = new HashSet<>(); // no reuse
        for (int j = 0, num = results.size(); j < num; j++) {
            Trajectory trj = results.get(j);
            int pseudonym_id = Generator.randomPseudonym(realTrj.get_trajectoryId(), ids, rm);
            trj.set_pseudonym(pseudonym_id);
            ids.add(pseudonym_id);

            int len = real_len;
            List<ComplexPoint> pointSeq = trj.get_pointSeq();
            for (int i = 0, removed = 0; i < len; ) {
                if (toBeRemove[i + removed]) {
                    pointSeq.remove(i);
                    len--;
                    removed++;
                } else {
                    if (j < num - 1) {  // the last one is the real trajectory
                        ComplexPoint p = pointSeq.get(i);
                        if (p.get_exactTime() < 0) {
                            p.set_time(realTrj.get_point_by_idx(i).get_exactTime());
                        }
                    }
                    i++;
                }
            }
        }
        ids.clear();
        ids = null;
    }


    private static void updateLocationSet(final Trajectory trj, Vector<Map<ComplexPoint, Integer>> selected_locations) {
        Map<ComplexPoint, Integer> point2cnt;
        for (int i = 0, len = trj.get_length(); i < len; i++) {
            ComplexPoint p = trj.get_point_by_idx(i);
            if (i < selected_locations.size()) {
                point2cnt = selected_locations.get(i);
                point2cnt.compute(p, (k, v) -> v == null ? 1 : ++v);
            } else {
                point2cnt = new HashMap<>();
                point2cnt.put(p, 1);
                selected_locations.add(point2cnt);
            }
        }
    }

    private static DirGraph generateDirGraph(final Trajectory realTrj, Vector<Set<SimplePoint>> dummy_locations) {
        DirGraph g = new DirGraph();  // directed graph
//        double v_max = Calculate.get_max_speed(realTrj.get_pointSeq());
        for (int i = 0, len = realTrj.get_length(); i < len - 1; i++) {
            ComplexPoint p_cur = realTrj.get_point_by_idx(i);
            ComplexPoint p_next = realTrj.get_point_by_idx(i + 1);
            Set<SimplePoint> dummies_cur = dummy_locations.get(i);
            dummies_cur.add(p_cur);  // add itself
            Set<SimplePoint> dummies_next = dummy_locations.get(i + 1);
            dummies_next.add(p_next);

//            long t_diff = getTimeDiff(p_next, p_cur);
            double realDist = SimplePoint.getDistance(p_next, p_cur);
            Iterator<SimplePoint> itr = dummies_cur.iterator();
            while (itr.hasNext()) {
                SimplePoint sp_cur = itr.next();
                boolean valid_head = false;
                for (SimplePoint sp_next : dummies_next) {
                    if (!sp_next.equals(sp_cur)) {
                        double d = SimplePoint.getDistance(sp_cur, sp_next);
                        // judges the spatio-temporal reachability between adjacent locations
//                    if (getDistance(p_i, p) / v_max <= t_diff || t_diff == 0) {
                        if (d <= (1 + epsilon) * realDist && d >= (1 - epsilon) * realDist) {
                            g.add_edge(sp_cur, sp_next, i); // directed, from p_i to p
                            valid_head = true;      // at least one edge starts from point p_cur
                        }
                    }
                }
                if (!valid_head) {
                    itr.remove();
                }
            }
            // there is no valid edge starting from p_cur, should not happen
            if (dummies_cur.isEmpty()) {
                g.add_edge(p_cur, p_next, i);
                dummies_cur.add(p_cur);
            }
        }

        return g;
    }

    /**
     * DLC: Generating Dummy Location Candidate Set
     *
     * @param loc_anonymity location anonymity threshold, aka the min # of a point's dummy candidates
     */
    private static Vector<Set<SimplePoint>> generateDummyLocations(final Trajectory realTrj, boolean[] EL,
                                                                   final Grid grid, final int loc_anonymity) {
        Vector<Set<SimplePoint>> candidates = new Vector<>();
        final int bound = Math.max(min_anchor_points, loc_anonymity);
        Random rm = new Random();
        for (int i = 0, len = realTrj.get_length(); i < len; i++) {
            ComplexPoint p_cur = realTrj.get_point_by_idx(i);
            Set<SimplePoint> candi = new HashSet<>();

            if (!EL[i]) {    // we always generate enough dummy for the first point
                Vector<Long> gids = grid.get_gridIds_by_lnglat(p_cur.getLongitude(), p_cur.getLatitude());
                if (gids != null) {     // if gids == null, this point is out of grid range
                    /* v2: each grid cell will have several qualified candidates */

                    while (candi.size() < bound && !gids.isEmpty()) {
                        int idx = rm.nextInt(gids.size());  // randomly select one grid cell and the inside anchors
                        long gid = gids.get(idx);
                        Set<SimplePoint> anchorPoints = grid.get_POIs_by_gridId(gid);
                        if (anchorPoints != null) {
                            for (SimplePoint p : anchorPoints) {
                                if (SimplePoint.getDistance(p_cur, p) <= anchor_dist_bound) {
                                    candi.add(p);
                                }
                            }
                        }
                        gids.remove(idx);
                    }
                    if (candi.size() < loc_anonymity) {
                        candi.clear();
                        EL[i] = true;   // it becomes an exposure point
                    }
                }
            }

            candidates.add(candi);
        }

        return candidates;
    }

    private static final float epsilon = 0.5f;
    private static final float anchor_dist_bound = 0.5f;    // how close two points are can be a candidate
    private static final int min_anchor_points = 5;
    private static final int max_trj_length = 20000;    // due to the high sampling rate of Geolife
}

