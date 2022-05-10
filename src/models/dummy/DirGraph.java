package models.dummy;

import spatial.ComplexPoint;
import spatial.SimplePoint;
import spatial.Trajectory;

import java.util.*;

public class DirGraph {
    Vector<SimplePoint> vertexList;
    Map<Integer, Set<Integer>> adjVertices;
    Vector<Set<Integer>> time2vids;

    public DirGraph() {
        vertexList = new Vector<>();
        adjVertices = new HashMap<>();
        time2vids = new Vector<>();
    }

    // idx represents where the fromPoint located in realTrj (time sequential)
    // so, toPoint is idx+1
    public void add_edge(SimplePoint fromPoint, SimplePoint toPoint, int time) {
        Set<Integer> set_from = new HashSet<>();
        boolean from_new = true;
        if(time < time2vids.size()){
            set_from = time2vids.get(time);
            from_new = false;
        }

        int fromIdx = vertexList.indexOf(fromPoint);
        if (fromIdx == -1) {
            fromIdx = vertexList.size();
            vertexList.add(new SimplePoint(fromPoint.getLongitude(), fromPoint.getLatitude()));
        }
        Set<Integer> neighbors = adjVertices.compute(fromIdx, (k, v) -> v == null ? new HashSet<>() : v);

        int toIdx = vertexList.indexOf(toPoint);
        if (toIdx == -1) {
            toIdx = vertexList.size();
            vertexList.add(new SimplePoint(toPoint.getLongitude(), toPoint.getLatitude()));
        }
        neighbors.add(toIdx);
        adjVertices.put(fromIdx, neighbors);

        set_from.add(fromIdx);
        if(from_new)
            time2vids.add(set_from);
    }

    public List<Trajectory> DFS(SimplePoint startPoint, Trajectory real_trj, int loc_anonymity, int k_anonymity) {
        int start_idx = vertexList.indexOf(startPoint);
        if (start_idx == -1) {
            System.out.printf("[ERROR] No point %s in the graph.\n", startPoint.toString());
            return null;
        }

        int vertexNum = vertexList.size();
        int length = real_trj.get_length();

        final int FirstTimeSlot = 1;
        int[][] visitCount = new int[length][];    // the start point is fixed
        for(int i = FirstTimeSlot; i < length; i++){
            visitCount[i] = new int[vertexNum];
            Arrays.fill(visitCount[i], 0);
        }

        List<Trajectory> allPaths = new ArrayList<>();
        int trjNum = 0;
        int minTrjNum = (int) Math.ceil((1.0 / time2vids.firstElement().size()) * k_anonymity * loc_anonymity);
        int exe = 0, maxTry = (time2vids.size() > 1 ? time2vids.get(FirstTimeSlot).size() : 10) * k_anonymity;
        Trajectory trj;
        Stack<Integer> stack;
        Set<Integer> vids, adjs, intersection;
        int pre_point, pid, minCnt;
        while (trjNum < minTrjNum && exe++ < maxTry){
            stack = new Stack<>();
            stack.push(start_idx);
            for(int t = FirstTimeSlot; t < length; t++){
                pre_point = stack.peek();
                adjs = adjVertices.get(pre_point);
                intersection = new HashSet<>(adjs);
                if(t < length -1) {         // the last point is someone's neighbor
                    vids = time2vids.get(t);
                    intersection.retainAll(vids);
                }
                // select one point that had been rarely selected before
                pid = -1;
                minCnt = Integer.MAX_VALUE;
                for(int p: intersection){
                    if(visitCount[t][p] < minCnt){
                        minCnt = visitCount[t][p];
                        pid = p;
                    }
                    else if(visitCount[t][p] == minCnt){
                        pid = (adjVertices.getOrDefault(p, new HashSet<>()).size() > adjVertices.getOrDefault(pid, new HashSet<>()).size()) ? p : pid;  // prefer the one with more neighbours
                    }
                }
                if(pid == -1){
                    if(t == FirstTimeSlot){
                        return null;
                    }
                    time2vids.get(t-1).remove(pre_point);
                    stack.pop();
                    t -= 2;
                }
                else {
                    visitCount[t][pid]++;
                    stack.push(pid);
                }
            }
            // has found enough points to be a dummy trajectory
            if(stack.size() == length) {
                trj = new Trajectory(real_trj.get_userId(), real_trj.get_trajectoryId(), transform(stack));  // dummy, the tripID doesn't work anymore
                allPaths.add(trj);
                trjNum = allPaths.size();
            }

            if(exe == maxTry && trjNum < minTrjNum){
                int cnt = 0;
                for(int t = FirstTimeSlot; t <length-1; t++){
                    long tmp = Arrays.stream(visitCount[FirstTimeSlot]).filter(c -> c > 0).count();  // # of visited vertices at time-1
                    if(tmp >= time2vids.get(t).size()){
                        cnt++;
                    }
                }
                if(cnt >= length - 2){
                    break;
                }
                else {
                    maxTry *= 2;
                }
            }
        }

        return allPaths;
    }

    private int compareAndTransform(Stack<Integer> path, List<ComplexPoint> pointSeq, List<SimplePoint> newSeq) {
        int diff = 0;
        Stack<Integer> reverse = new Stack<>();
        while (!path.isEmpty()){
            reverse.add(path.pop());
        }
        int idx;
        int i = 0;
        SimplePoint p;
        while (!reverse.isEmpty()){
            idx = reverse.pop();
            p = vertexList.get(idx);
            newSeq.add(p);
            if(!p.equals(pointSeq.get(i++))){
                diff++;
            }
            path.push(idx);  // recover
        }
        return diff;
    }

    private ArrayList<ComplexPoint> transform(Stack<Integer> path) {
        ArrayList<ComplexPoint> seq = new ArrayList<>();
        Stack<Integer> reverse = new Stack<>();
        while (!path.isEmpty()){
            reverse.add(path.pop());
        }
        int idx;
        while (!reverse.isEmpty()){
            idx = reverse.pop();
            seq.add(new ComplexPoint(vertexList.get(idx)));
            path.push(idx);  // recover
        }
        return seq;
    }

}
