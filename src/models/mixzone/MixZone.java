package models.mixzone;

import spatial.ComplexPoint;
import spatial.Trajectory;
import models.mixzone.graph.Graph;
import shared.Generator;

import java.io.IOException;
import java.util.*;


// UTMP: Uniform traffic mix zone placement
// Title: Traffic-aware multiple mix zone placement for protecting location privacy
public class MixZone {

    /**
     * @param radius the distance threshold that a mix zone can cover a point
     * @param max_mz the max number of mix-zone
     */
    public static void execute(final Vector<Trajectory> ori_trajectories, final float radius, final int max_mz,
                               final int min_trj_len, final float lng_step, final float lat_step, final String roadNetworkFilename,
                               Vector<Trajectory> anonymized_trajectories) throws IOException {

        System.out.println("\n----------------------------------------------------------------------------------------------------------");
        System.out.println("\t\t Selected Model: Mix-Zone (Uniform traffic mix zone placement)                                ");
        System.out.println("\t\t Title:          Traffic-aware multiple mix zone placement for protecting location privacy (2012)");
        System.out.printf( "\t\t Parameters:     1) # of max mix-zones = %d; 2) distance threshold = %.3f\n", max_mz, radius);
        System.out.println("----------------------------------------------------------------------------------------------------------\n");

        /* -- Read POIs to construct graph G -- */
        long startTimer = System.currentTimeMillis();
        Graph g = initialize(roadNetworkFilename, lng_step, lat_step); // only for Beijing's Road network
        long timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for graph construction based on road network: %.3f s, %.3f min\n\n", timecost / 1000f, timecost / (1000.0 * 60));

        /* -- Step #1: Find articulation points -- */
        // DFS for G to find discover time i.d for each vertex
        assert g != null;
        startTimer = System.currentTimeMillis();
        g.find_articulation_points();
        timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for articulation points: %.3f s, %.3f min\n\n", timecost / 1000f, timecost / (1000.0 * 60));

        /* -- Step #2: Maximal independent set -- */
        startTimer = System.currentTimeMillis();
        Set<Integer> mis = g.find_mis();
        timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for maximal independent set: %.3f s, %.3f min\n\n", timecost / 1000f, timecost / (1000.0 * 60));

        /* -- Step #3: Maintain cost constraint-- */
        // vertices which are not included in mis will be the candidates of mix-zone
        System.out.printf("[PROGRESS] Setting %d mix zones ...\n", max_mz);
        startTimer = System.currentTimeMillis();
        int mmz = g.set_mix_zone(mis, max_mz);
        timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for %d mix-zone placement: %.3f s, %.3f min\n\n", mmz, timecost / 1000f, timecost / (1000.0 * 60));

        /* -- Anonymize the raw trajectories via mix zones -- */
        System.out.println("[PROGRESS] Start trajectory anonymization ...");
        startTimer = System.currentTimeMillis();
        MixZone.anonymize(ori_trajectories, g, radius, min_trj_len, anonymized_trajectories);
        timecost = System.currentTimeMillis() - startTimer;
        System.out.printf("[TIME-COST] for anonymization: %.3f s, %.3f min\n", timecost / 1000f, timecost / (1000.0 * 60));
    }

    private static Graph initialize(String roadNetworkFile, float lng_step, float lat_step) throws IOException {
        Graph g = new Graph();

        if (roadNetworkFile == null) {
            System.out.println("[ERROR] Illegal file of road network!");
            return null;
        }
        final String v_file = roadNetworkFile;
        final String e_file = roadNetworkFile.replace("vertices", "edges");
        g.init_graph(v_file, e_file, "\t", lng_step, lat_step);

        return g;
    }

    /**
     * split original trajectories based on mix-zones
     *
     * @param anonymized_trajectories the collection of shorter trajectories
     */
    private static void anonymize(final Vector<Trajectory> trajectories, final Graph graph, final float radius, final int min_trj_len,
                                  Vector<Trajectory> anonymized_trajectories) {
        Random rm = new Random();
        ArrayList<ComplexPoint> cur_pointSeq;
        Trajectory trj;
        int uid = -1, trj_id, pseudonym_id, pre_vid, vid;
        Set<Integer> existing_ids = new HashSet<>();
        for (Trajectory original_trj : trajectories) {
            if (original_trj.get_userId() != uid) {
                existing_ids.clear();   // the pseudonym ids of the same user cannot be duplicated
            }
            uid = original_trj.get_userId();
            trj_id = original_trj.get_trajectoryId();
            cur_pointSeq = new ArrayList<>();
            pre_vid = Integer.MIN_VALUE;

            for (int j = 0, len = original_trj.get_length(); j < len; j++) {
                ComplexPoint p = original_trj.get_point_by_idx(j);
                vid = graph.cover_by_mixzone(p, radius); // find the mix zone that this point went through
                if (vid != -1) {
                    pre_vid = (pre_vid >= 0) ? pre_vid : vid;   // NOTE that the valid vertex-id >= 0
                    if (vid != pre_vid && cur_pointSeq.size() >= min_trj_len) {
                        pseudonym_id = Generator.randomPseudonym(trj_id, existing_ids, rm);
                        existing_ids.add(pseudonym_id);
                        trj = new Trajectory(uid, trj_id, cur_pointSeq);
                        trj.set_pseudonym(pseudonym_id);
                        anonymized_trajectories.add(trj);

                        // next will be the new trajectory
                        cur_pointSeq = new ArrayList<>();
                    }
                }
                cur_pointSeq.add(p);
            }

            // the remaining pointSeq
            if (pre_vid >= 0 && !cur_pointSeq.isEmpty()) {
                pseudonym_id = Generator.randomPseudonym(trj_id, existing_ids, rm);
                trj = new Trajectory(uid, trj_id, cur_pointSeq);
                trj.set_pseudonym(pseudonym_id);
                anonymized_trajectories.add(trj);
            } else if (pre_vid < 0) {   // this trajectory didn't pass through any mix zone
                pseudonym_id = Generator.randomPseudonym(trj_id, existing_ids, rm);
                trj = new Trajectory(original_trj);
                trj.set_pseudonym(pseudonym_id);
                anonymized_trajectories.add(trj);
            }
        }
    }
}
