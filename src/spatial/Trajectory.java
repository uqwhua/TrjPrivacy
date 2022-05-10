package spatial;

import java.util.*;

public class Trajectory {

    // basic thing, every model needs it
    private List<ComplexPoint> pointSeq;

    private final int userID;
    private int tripID;     // one user may have multiple trips

    // used for Mix-zone and Dummy
    private int pseudonymID;

    // only for k-anonymity model
    private int k_anonymity;
    private final Set<Integer> merged_ids;    // the merged userIDs (for linking attack evaluation)

    // default constructor
    public Trajectory(){
        pointSeq = new ArrayList<>();
        userID = -1;
        tripID = -1;
        pseudonymID = -1;
        k_anonymity = 1;
        merged_ids = new HashSet<>();
    }

    public Trajectory(final int uid, final int tid, List<ComplexPoint> seq) {
        pointSeq = seq;
        userID = uid;
        tripID = tid;
        pseudonymID = -1;
        k_anonymity = 1;
        merged_ids = new HashSet<>();
    }

    public Trajectory(final Trajectory trip) {
        pointSeq = new ArrayList<>(trip.pointSeq);
        userID = trip.userID;
        tripID = trip.tripID;
        pseudonymID = -1;
        k_anonymity = trip.k_anonymity;
        merged_ids = new HashSet<>(trip.merged_ids);
    }

    public int get_userId() {
        return userID;
    }

    public int get_trajectoryId() {
        return tripID;
    }

    public void set_trajectoryId(int tid) {
        tripID = tid;
    }

    public int get_pseudonym() {
        return pseudonymID;
    }

    public void set_pseudonym(int pid) {
        pseudonymID = pid;
    }

    public int get_length() {
        return pointSeq.size();
    }

    public ComplexPoint get_point_by_idx(int idx) {
        if (idx < pointSeq.size()) {
            return pointSeq.get(idx);
        }
        return null;
    }

    public List<ComplexPoint> get_pointSeq() {
        return pointSeq;
    }

    public void set_pointSeq(List<ComplexPoint> points) {
        pointSeq = points;
    }

    public boolean isMerged() {
        return !merged_ids.isEmpty();
    }

    public Set<Integer> get_mergedIds() {
        return merged_ids;
    }

    public int get_K() {
        return k_anonymity;
    }

    public void set_K(int _k) {
        k_anonymity = _k;
    }

    public void set_mergedIds(Trajectory a, Trajectory b) {
        if(a.isMerged()){
            merged_ids.addAll(a.merged_ids);
        }
        else {
            merged_ids.add(a.get_trajectoryId());
        }
        if(b.isMerged()){
            merged_ids.addAll(b.merged_ids);
        }
        else {
            merged_ids.add(b.get_trajectoryId());
        }
    }


    @Override
    public int hashCode() {
        int result = 0;
        result += result * 31 + tripID;
        result += result * 31 + pointSeq.size();
        for (ComplexPoint geoPoint : pointSeq) {
            result += result * 31 + geoPoint.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Trajectory trj))
            return false;

        if (trj.tripID != this.tripID || trj.get_length() != pointSeq.size())
            return false;

        for (int i = 0; i < pointSeq.size(); i++) {
            if (!pointSeq.get(i).equals(trj.get_point_by_idx(i))) {
                return false;
            }
        }
        return true;
    }
}
