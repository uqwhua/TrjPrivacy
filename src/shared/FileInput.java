package shared;

import spatial.*;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class FileInput {

    // considering Geolife is a long-period data (five years)
    // it would be better to do the segmentation first (split or not)
    public static String getInputFilename(final String inputFolder, final String dataset, final String variant) {
        String filename = inputFolder;
        String fDB = formalizeName(dataset);
        if (fDB.equals("T-Drive")) {
            filename += variant;
        }
        // todo: process the Geolife if needed
        filename += ".csv";
        return filename;
    }

    public static double readTrajectory(final String inputFilename, Vector<Trajectory> trajectories) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFilename));
        List<ComplexPoint> pointSeq = new ArrayList<>();
        ComplexPoint point, pre_point = null;
        String s;

        final int trajPos = 0, uidPos = 1, timePos = 2, lngPos = 3, latPos = 4;
        int currTripID = -1, prevTripID = -1;
        int currUserID = -1, prevUserID = -1;
        double length = 0;
        boolean firstLine = true;

        while ((s = br.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }

            // format: trj-id, user-id, timestamp, longitude, latitude
            String[] tokens = s.split(",");
            currUserID = Integer.parseInt(tokens[uidPos]);
            currTripID = Integer.parseInt(tokens[trajPos]);

            // the end of the records belonging to pre_uid
            if (prevTripID >= 0 && prevTripID != currTripID) {
                Trajectory trj = new Trajectory(prevUserID, prevTripID, pointSeq);    // it's only one day data
                trajectories.add(trj);
                length += pointSeq.size();
                pointSeq = new ArrayList<>();
            }
            prevTripID = currTripID;
            prevUserID = currUserID;

            // process current point

            float lng = Float.parseFloat(tokens[lngPos]);
            float lat = Float.parseFloat(tokens[latPos]);
            long timestamp = Long.parseLong(tokens[timePos]);   // unit: second
            point = new ComplexPoint(lng, lat, timestamp);

            // no duplicate continuous point
            if (pre_point == null || !pre_point.equals(point)) {    // to avoid duplicate continuous points
                pre_point = point;
                pointSeq.add(point);
            }
            // else it is the same as the pre point, no need to insert
        }
        br.close();

        // the last one
        if (!pointSeq.isEmpty()) {
            Trajectory trj = new Trajectory(currUserID, currTripID, pointSeq);    // it's only one day data
            trajectories.add(trj);
            length += pointSeq.size();
        }

        return length / trajectories.size();
    }

    public static Grid readBeijingPOIs(final String poi_file, final float lng_step, final float lat_step) throws IOException {

        Category.initialize();
        Grid.TotalCategoryNum = Category.getNum();

        if (poi_file != null && !poi_file.isEmpty()) {
            File file = new File(poi_file);
            if (file.exists() && file.isFile()) {
                float lng_max = Float.NEGATIVE_INFINITY, lng_min = Float.POSITIVE_INFINITY;
                float lat_max = Float.NEGATIVE_INFINITY, lat_min = Float.POSITIVE_INFINITY;
                Set<SimplePoint> pois = new HashSet<>();
                Map<String, Integer> extraCategory2ID = new HashMap<>();
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(read);
                String s;
                // format: POI_name, categoryID, address, telephone, longitude, latitude
                while ((s = br.readLine()) != null) {
                    String[] tokens = s.split(",");
                    String subCategoryCode = tokens[1];
                    int cid = Category.getCategoryID(subCategoryCode);

                    // just in case
                    if (cid == -1) {  // should be useless now
                        final int nextExtraID = Grid.TotalCategoryNum + extraCategory2ID.size();
                        cid = extraCategory2ID.compute(subCategoryCode, (k, v) -> v == null ? nextExtraID : v);
                    }

                    float lng = Float.parseFloat(tokens[4]);
                    float lat = Float.parseFloat(tokens[5]);
                    SimplePoint p = new SimplePoint(lng, lat, cid);
                    pois.add(p);

                    lng_max = Math.max(lng_max, lng);
                    lng_min = Math.min(lng_min, lng);
                    lat_max = Math.max(lat_max, lat);
                    lat_min = Math.min(lat_min, lat);
                }
                br.close();
                Grid grid = new Grid(lng_min, lng_max, lat_min, lat_max, lng_step, lat_step);
                grid.addPoints(pois);
                return grid;
            } else {
                System.out.println("[ERROR] Cannot find the POI file.");
            }
        }
        return null;
    }

    public static String formalizeName(final String inputName) {
        String rtn = inputName;
        switch (inputName.toLowerCase()) {
            // model
            case "mixzone"          -> rtn = "Mixzone";
            case "dummy"            -> rtn = "Dummy";
            case "glove"            -> rtn = "Glove";
            case "klt"              -> rtn = "KLT";
            case "dpt"              -> rtn = "DPT";
            case "w4m"              -> rtn = "W4M";
            case "adatrace", "ada"  -> rtn = "AdaTrace";
            case "sc", "sigclosure" -> rtn = "SigClosure";
            case "fdp", "frequencydp" -> rtn = "FrequencyDP";

            // dataset
            case "geolife"          -> rtn = "Geolife";
            case "tdrive", "t-drive" -> rtn = "T-Drive";
        }
        return rtn;
    }
}
