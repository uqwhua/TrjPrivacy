package models.glove;

import spatial.ComplexPoint;
import spatial.Trajectory;

import java.util.*;

public class Merge {

    public static Trajectory mergeTrace(Trajectory a, Trajectory b) {
        List<ComplexPoint> longer, shorter;
        int nL, nS;
        if(a.get_length() > b.get_length()){
            longer = a.get_pointSeq();
            shorter = b.get_pointSeq();
            nL = a.isMerged() ? a.get_mergedIds().size() : 1;
            nS = b.isMerged() ? b.get_mergedIds().size() : 1;
        }
        else {
            shorter = a.get_pointSeq();
            longer = b.get_pointSeq();
            nS = a.isMerged() ? a.get_mergedIds().size() : 1;
            nL = b.isMerged() ? b.get_mergedIds().size() : 1;
        }

        ComplexPoint[] once = mergeOnce(longer, shorter, nL, nS);

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

        List<ComplexPoint> twice = mergeTwice(unmerged, merged, nS, nL);  // already non-null
//        List<DeltaPoint> listWithoutNulls = twice.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());

        Trajectory m = new Trajectory();
        m.set_pointSeq(twice);
        m.set_mergedIds(a, b);
        m.set_K(a.get_K() + b.get_K());

        return reshaping(m);
    }

    private static List<ComplexPoint> mergeTwice(List<ComplexPoint> unmerged, List<ComplexPoint> merged, int nL, int nS) {
        ComplexPoint[] array = mergeOnce(unmerged, merged, nL, nS);
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
                                     final int nL, final int nS) {
        //把长的 merge into 短的，所以合出来的结果一定是短的那条路径的长度
        int shortLen = shorter.size();
        ComplexPoint[] array = new ComplexPoint[shortLen];
        for (ComplexPoint pl : longer) {
            double minCost = Double.MAX_VALUE;
            int minIdx = -1;
            //长的或者说旧的list的一个点去和短的或者说新的轨迹的所有点比较代价
            // compare every point in longer list with the shorter one
            for (int i = 0; i < shortLen; i++) {
                double cost = Formula.computeCost(pl, shorter.get(i), nL, nS);
                if (cost < minCost) {
                    minCost = cost;
                    minIdx = i;
                }
            }
            // if it has been merged, merge again
            array[minIdx] = (array[minIdx] == null) ? mergePoint(shorter.get(minIdx), pl) : mergePoint(array[minIdx], pl);
        }
        return array;
    }

    // merge a and b to a new point
    public static ComplexPoint mergePoint(final ComplexPoint a, final ComplexPoint b) {
        float lng = Math.min(a.getLongitude(), b.getLongitude());
        float lat = Math.min(a.getLatitude(), b.getLatitude());
        long aTime = a.get_exactTime(), bTime = b.get_exactTime();  // sec
        long time = Math.min(aTime, bTime);

        long deltaSecond = Math.max(a.get_looseTime(), b.get_looseTime()) - time;
        float deltaLng = Math.max(a.get_looseLongitude(), b.get_looseLongitude()) - lng;
        float deltaLat = Math.max(a.get_looseLatitude(), b.get_looseLatitude()) - lat;

        return new ComplexPoint(lng, lat, time, deltaLng, deltaLat, deltaSecond);
    }

    // only related to temporal sorting
    public static Trajectory reshaping(Trajectory m) {
        List<ComplexPoint> points = m.get_pointSeq();
        points.sort(Comparator.comparingLong(ComplexPoint::get_exactTime));     // sort by the start time of each point

        //需要插点的位置和点，key是位置，value是点
        Map<Integer, ComplexPoint> map = new TreeMap<>();
        ComplexPoint one, two;
        for (int i = 0, len = points.size(); i < len - 1; i++) {
            one = points.get(i);
            two = points.get(i + 1);

            long oneEndTime = one.get_looseTime();  //sec
            long twoStartTime = two.get_exactTime();

            if (oneEndTime > twoStartTime) {
                // only update the delta time of the front point
                one.set_deltaSecond(twoStartTime - one.get_exactTime());    // update
                long deltaSec, twoEndTime = two.get_looseTime();
                if(oneEndTime < twoEndTime) {   // just partial overlapping
                    two.set_time(oneEndTime);
                    two.set_deltaSecond(twoEndTime - oneEndTime);

                    deltaSec = oneEndTime - twoStartTime;
                }
                else if(oneEndTime > twoEndTime){   // one completely covers two in the timeline, two actually becomes the middle point
                    two.set_time(twoEndTime);
                    two.set_deltaSecond(oneEndTime - twoEndTime);

                    deltaSec = two.get_deltaSecond();
                }
                else {  // if (oneEndTime == twoEndTime) no need to change the latter one, just modify the front one
                    continue;
                }

                // to insert a middle point between them
                final long middleTime = twoStartTime;
                final float middleLng = Math.min(one.getLongitude(), two.getLongitude());
                final float middleLat = Math.min(one.getLatitude(), two.getLatitude());
                final float deltaLng = (Math.max(one.get_looseLongitude(), two.get_looseLongitude()) - middleLng);
                final float deltaLat = (Math.max(one.get_looseLatitude(), two.get_looseLatitude()) - middleLat);

                ComplexPoint p = new ComplexPoint(middleLng, middleLat, middleTime, deltaLng, deltaLat, deltaSec);
                map.put(i + 1, p); //record the position to be inserted
            }
        }

        int extra = 0;
        for(int pos: map.keySet()){
            points.add(pos + extra, map.get(pos));
            extra++;
        }
        m.set_pointSeq(points); // new sequence
        return m;
    }
}
