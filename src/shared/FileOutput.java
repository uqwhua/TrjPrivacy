package shared;

import spatial.ComplexPoint;
import spatial.Trajectory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static shared.FileInput.formalizeName;

public class FileOutput {

    /* output the anonymized results to files */
    public static void outputAnonymization(final String model, final String filename, final Vector<Trajectory> anonymized_trajectories) throws IOException {
        File file = new File(filename);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        String head = "user-id,trj-id,pseudonym-id,time-second,longitude,latitude";
        if(ComplexPoint.needDelta){
            head += ",deltaSecond,deltaLng,deltaLat,merged-TIDs";
        }
        bw.append(head + "\n");

        StringBuilder sb = new StringBuilder();
        for (Trajectory trj : anonymized_trajectories) {
            List<Integer> mergedIDs = new ArrayList<>();
            if(trj.isMerged()) {
                mergedIDs.addAll(trj.get_mergedIds());
                mergedIDs.sort(Integer::compareTo);
            }
            for (int i = 0, len = trj.get_length(); i < len; i++) {
                ComplexPoint p = trj.get_point_by_idx(i);

                /* format: user-id, trj-id, pseudonym-id, time, longitude, latitude [deltaMinute, deltaLng, deltaLat, merged-ids] */
                sb.append(trj.get_userId() + "," + trj.get_trajectoryId() + "," + trj.get_pseudonym() + ",");

                // point information
                sb.append(p.get_exactTime() + "," + p.getLongitude() + "," + p.getLatitude());

                if(ComplexPoint.needDelta){
                    sb.append("," + p.get_deltaSecond() + "," + p.get_deltaLongitude() + "," + p.get_deltaLatitude());
                    mergedIDs.forEach(id -> sb.append("," + id));
//                    for(int id: mergedIDs) {
//                        sb.append("," + id);
//                    }
                }
                sb.append("\n");
            }
            bw.append(sb.toString());
            sb.setLength(0);    //reset
        }
        bw.close();
    }

    // only for dummy
    public static void outputAnonymizedTrj(final String outputFile, final List<Trajectory> anonymized_trjs) throws IOException {
        File file = new File(outputFile);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if(!file.exists()) {
            BufferedWriter bw = new BufferedWriter(new PrintWriter(file));
            bw.append("user-id,trj-id,pseudonym-id,time-second,longitude,latitude\n");  // the header of the file
            bw.close();
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true)));
        for(Trajectory trj: anonymized_trjs){
            int uid = trj.get_userId();
            int tid = trj.get_trajectoryId();
            int pid = trj.get_pseudonym();
            for (int i = 0, len = trj.get_length(); i < len; i++) {
                ComplexPoint p = trj.get_point_by_idx(i);

                /* format: user-id, trj-id, pseudonym-id, time, longitude, latitude */
                bw.append(uid + "," + tid + "," + pid + ",");

                // point information
                bw.append(p.get_exactTime() + "," + p.getLongitude() + "," + p.getLatitude() + "\n");
            }
        }
        bw.close();
    }

    public static String getOutputPrefix(final String dataset, final String variant) {
        String prefix = "";
        String fDB = formalizeName(dataset);
        if(fDB.equals("T-Drive")) {
            prefix = variant.replace("/", "_") + "_"; // e.g., T-Drive_sample_1800_
        }
        return prefix;
    }
}