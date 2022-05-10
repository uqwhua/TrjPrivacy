package models.mixzone.graph;

import spatial.Grid;
import spatial.SimplePoint;
import models.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// for POIs in road network
public class Graph {

    int start_idx;  // the id of vertices may start from 1 or 0
    int num_vertex;
    Vertex[] vertexList; // each vertex's adjacentList
    int[] firstEdgeId;        // 指向顶点的第一条边（链表）
    int num_ap;
    int time;

    int num_edge;
    Edge[] edgeList;    // key is edge-id
    int num_bg;

    static Rng rng = new Rng();
    Grid grid;  // will be initialized when the max and min lng/lat are determined
    Map<Long, Set<Integer>> gridIdx2vertices;

    // constructor
    public Graph() {
        start_idx = 0;
        num_vertex = 0;
        vertexList = null;
        firstEdgeId = null;
        num_ap = 0;
        time = 0;
        num_edge = 0;
        edgeList = null;
        num_bg = 0;
        gridIdx2vertices = new HashMap<>();
    }

    Graph(Graph graph) {
        start_idx = graph.start_idx;
        num_vertex = graph.num_vertex;
        num_ap = graph.num_ap;
        time = graph.time;
        vertexList = new Vertex[num_vertex + start_idx];
        firstEdgeId = new int[num_vertex + start_idx];
        if (start_idx == 1) {
            vertexList[0] = null;
            firstEdgeId[0] = -1;
        }
        for (int i = start_idx; i < num_vertex + start_idx; i++) {
            vertexList[i] = new Vertex(graph.vertexList[i]);
            firstEdgeId[i] = graph.firstEdgeId[i];
        }
        num_edge = graph.num_edge;
        num_bg = graph.num_bg;
        edgeList = new Edge[num_edge];
        for (int i = 0; i < num_edge; i++) {
            edgeList[i] = new Edge(graph.edgeList[i]);
        }

        grid = graph.grid;
        gridIdx2vertices = graph.gridIdx2vertices;
    }

    private int count_lines(final String filename) throws IOException {
        File file = new File(filename);
        int cnt = 0;
        if (file.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.readLine() != null) {
                cnt++;
            }
            br.close();
        }
        return cnt;
    }

    public void init_graph(final String v_file, final String e_file, final String delimiter,
                           final float lng_step, final float lat_step) throws IOException {
        num_vertex = count_lines(v_file);
        if (num_vertex == 0)
            return;
        System.out.println("[REPORT] # of vertices = " + num_vertex);

        if (vertexList == null) {
            vertexList = new Vertex[num_vertex + 1];    // in case the vertex-id starts from 1
            firstEdgeId = new int[num_vertex + 1];
        }
        BufferedReader br = new BufferedReader(new FileReader(v_file));
        String s;
        float lng_max = Float.NEGATIVE_INFINITY, lng_min = Float.POSITIVE_INFINITY;
        float lat_max = Float.NEGATIVE_INFINITY, lat_min = Float.POSITIVE_INFINITY;

        final int idPos = 0, latPos = 1, lngPos = 2;
        while ((s = br.readLine()) != null) {
            String[] tokens = s.split(delimiter);
            int vid = Integer.parseInt(tokens[idPos]);
            float lat = Float.parseFloat(tokens[latPos]);
            float lng = Float.parseFloat(tokens[lngPos]);
            vertexList[vid] = new Vertex(vid, lng, lat);
            lng_max = Math.max(lng_max, lng);
            lng_min = Math.min(lng_min, lng);
            lat_max = Math.max(lat_max, lat);
            lat_min = Math.min(lat_min, lat);
        }
        br.close();

        if (vertexList[0] == null) {
            start_idx = 1;
        }
        else {
            vertexList[num_vertex] = null;  // vertex-id starts from 0, so the last one is invalid
        }

        System.out.println("[PROGRESS] Construct a grid for the vertices in the graph ...");
        grid = new Grid(lng_min, lng_max, lat_min, lat_max, lng_step, lat_step);

        // initialize
        for (int i = 0; i < num_vertex + start_idx; i++) {
            firstEdgeId[i] = -1;
        }
        // read edge file, aka road segments
        // regard it as the undirected graph
        br = new BufferedReader(new FileReader(new File(e_file)));
        Set<Edge> edges = new HashSet<>();
        while ((s = br.readLine()) != null) {
            String[] tokens = s.split(delimiter);
            int u = Integer.parseInt(tokens[1]);
            int v = Integer.parseInt(tokens[2]);
            vertexList[u].add_neighbor(v);
            vertexList[v].add_neighbor(u);
            edges.add(new Edge(u, v));
        }
        br.close();
        num_edge = edges.size();
        System.out.println("[REPORT] # of edges = " + num_edge);

        edgeList = new Edge[num_edge];
        Iterator<Edge> itr = edges.iterator();
        for (int i = 0; i < num_edge && itr.hasNext(); i++) {
            Edge e = itr.next();
            edgeList[i] = new Edge(e.get_endpoint_head(), e.get_endpoint_tail(),
                    firstEdgeId[e.get_endpoint_head()], firstEdgeId[e.get_endpoint_tail()]);
            firstEdgeId[e.get_endpoint_head()] = i;
            firstEdgeId[e.get_endpoint_tail()] = i;
        }

        edges.clear();
        edges = null;
    }

    public void find_articulation_points() {
        int len = num_vertex + start_idx;
        int[] discovery = new int[len];
        int[] low_value = new int[len];
        int[] searchFirst = new int[len];   //指向顶点的第一条未搜索的边
        int[] children = new int[len];      //存储顶点的孩子数量

        if (firstEdgeId == null) {
            System.out.println("Error: firstEdgeId is null.");
            return;
        } else
            System.arraycopy(firstEdgeId, start_idx, searchFirst, start_idx, num_vertex);

        time = 0;

        // to find articulation points in DFS tree rooted with vertex 'i'
        for (int i = start_idx; i < len; i++) {
            if (vertexList[i] != null && discovery[i] == 0)
                APUtil(i, discovery, low_value, searchFirst, children);
        }

        // print articulation points
        for (int i = start_idx; i < len; i++) {
            Vertex u = vertexList[i];
            if (u.is_ap) {
                num_ap++;
            }
        }
        System.out.println("[REPORT] # of all articulation points: " + num_ap);

        // to remove bridge edges in the graph based on ap
        // so that the graph will transform to several disconnected components
        for (int i = start_idx; i < len; i++) {
            if (vertexList[i] != null && vertexList[i].is_ap) {
                int eid = firstEdgeId[i];
                while (eid != -1) {
                    Integer neighbor = edgeList[eid].find_another_endpoint(i);
                    if (vertexList[neighbor].is_ap && !edgeList[eid].is_bg) {
                        num_bg++;
                        edgeList[eid].is_bg = true;
                        vertexList[i].remove_neighbor(neighbor);
                        vertexList[neighbor].remove_neighbor(i);
                    }
                    eid = edgeList[eid].get_next(i);
                }
            }
        }

        System.out.println("[REPORT] # of bridge edges = " + num_bg);
    }

    /**
     * A recursive function that find articulation points using DFS traversal
     *
     * @param root        The vertex to be visited next
     * @param discovery   Stores discovery times of visited vertices, 记录节点u在DFS过程中被遍历到的次序号
     * @param low_value   记录节点u或u的子树通过非父子边追溯到最早的祖先节点
     * @param searchFirst 指向顶点的第一条未搜索的边
     * @param children    存储顶点的孩子数量
     */
    private void APUtil(int root, int[] discovery, int[] low_value, int[] searchFirst, int[] children) {
        int len = num_vertex + start_idx;
        int[] stack = new int[len];     // 存储当前被处理顶点的栈

        int top = 0;
        stack[top] = root;
        discovery[root] = low_value[root] = ++time;
        Edge edge;
        while (top >= 0) {
            int cur_vertex = stack[top];
            int eid = searchFirst[cur_vertex];
            if (eid != -1) {
                edge = edgeList[eid];
                searchFirst[cur_vertex] = edge.get_next(cur_vertex);
                int neighbor = edge.find_another_endpoint(cur_vertex);      // should be endpoint_head
                if (discovery[neighbor] == 0) {   // hasn't been visited
                    children[cur_vertex]++;     // become cur_vertex's child
                    stack[++top] = neighbor;    // push
                    low_value[neighbor] = discovery[neighbor] = ++time;
                } else {
                    low_value[cur_vertex] = Math.min(low_value[cur_vertex], discovery[neighbor]);
                }
            } else {  // no edge, aka no adjacent vertices
                if (top > 0) {
                    int u = stack[top - 1];
                    low_value[u] = Math.min(low_value[u], low_value[cur_vertex]);
                    if ((u != root && low_value[cur_vertex] >= discovery[u]) || (u == root && children[u] >= 2)) {
                        vertexList[u].is_ap = true;
                    }
                }
                top--;
            }
        }
    }

    /* Favor vertices with fewer neighbors */
    private Set<Integer> select() {
        Set<Integer> random = new HashSet<>();
        for (int i = start_idx; i < num_vertex + start_idx; i++) {
            if (vertexList[i] != null) {
                if (rng.probability() < 1.0 / (2 * vertexList[i].get_degree())) {
                    random.add(i);
                }
            }
        }
        return random;
    }

    boolean is_empty() {
        for (int i = start_idx; i < num_vertex + start_idx; i++) {
            if (vertexList[i] != null)
                return false;
        }
        return true;
    }

    void remove_vertex(int id) {
        if (id < num_vertex + start_idx) {
            vertexList[id] = null;
        }
    }

    public Set<Integer> find_mis() {
        Set<Integer> mis = new HashSet<>();
        Graph graph_copy = new Graph(this);
        while (!graph_copy.is_empty()) {
            Set<Integer> vertices = graph_copy.select();
            Set<Integer> to_be_removed = new HashSet<>();
            for (int uid : vertices) {
                Vertex u = graph_copy.vertexList[uid];
                if (u != null) {
                    // scan its adjacent neighbours
                    for (int j = 0, degree = u.get_degree(); j < degree; j++) {
                        int neighbor = u.get_neighbor_by_idx(j);
                        if (vertices.contains(neighbor) && !to_be_removed.contains(neighbor)) {
                            int v_degree = graph_copy.vertexList[neighbor].get_degree();
                            // remove the one with less neighbors
                            to_be_removed.add((degree < v_degree) ? uid : neighbor);
                        }
                    }
                }
            }

            vertices.removeAll(to_be_removed);
            mis.addAll(vertices);
            for (int vid : vertices) {
                Vertex u = graph_copy.vertexList[vid];
                if (u != null) {
                    // remove its all adjacent neighbours
                    for (int j = 0, degree = u.get_degree(); j < degree; j++) {
                        int v = u.get_neighbor_by_idx(j);
                        graph_copy.remove_vertex(v);
                    }
                    graph_copy.remove_vertex(vid);
                }
            }
        }

        System.out.println("[REPORT] The size of Maximal Independent Set = " + mis.size());
        return mis;
    }

    public int set_mix_zone(Set<Integer> mis, int k) {
        Set<Integer> candidates = new HashSet<>();
        for (int i = start_idx; i < num_vertex + start_idx; i++) {
            if (vertexList[i] != null) {
                // a vertex which is either an ap or excluded by the mis
                // will be the candidate of mix zones
                if (vertexList[i].is_ap || !mis.contains(i)) {
                    candidates.add(i);
                }
            }
        }

        Map<Integer, Integer> vertex2associations = new HashMap<>();
        for (int vid : candidates) {
            Vertex u = vertexList[vid];
            int association = 0;
            for (int j = 0, degree = u.get_degree(); j < degree; j++) {
                int neighbor = u.get_neighbor_by_idx(j);
                if (!candidates.contains(neighbor))
                    association++;
            }
            vertex2associations.put(vid, association);
        }

        // iteratively remove the vertex that
        // introduces the least number of pairwise association increment
        // from the mix zone candidate set
        while (candidates.size() > k) {
            int min_candidate = find_min(vertex2associations);
            Vertex u_remove = vertexList[min_candidate];
            candidates.remove(min_candidate);
            vertex2associations.remove(min_candidate);

            if (u_remove.is_ap) {
                int eid = firstEdgeId[min_candidate];
                while (eid != -1) {
                    // recover the adjacent relation between v and min_candidate
                    // as this vertex will not be the mix zone and this edge is not bridge edge
                    int v = edgeList[eid].find_another_endpoint(min_candidate);
                    u_remove.add_neighbor(v);
                    vertexList[v].add_neighbor(min_candidate);
                    edgeList[eid].is_bg = false;
                    eid = edgeList[eid].get_next(min_candidate);
                }
            }

            for (int i = 0, degree = u_remove.get_degree(); i < degree; i++) {
                int vid = u_remove.get_neighbor_by_idx(i);
                vertex2associations.computeIfPresent(vid, (key, value) -> value + 1);
            }
        }

        // print-out mix zones
//        if (Main.DEBUG) System.out.print("[REPORT] Final mix zones: ");
        int mmz = 0;
        for (int vid : candidates) {
            mmz++;
            vertexList[vid].is_mixzone = true;

            Vertex u = vertexList[vid];
            // for grid index
            long gid = grid.get_gridId_by_lnglat(u.longitude, u.latitude);
            Set<Integer> vids = gridIdx2vertices.compute(gid, (key, value) -> value == null ? new HashSet<>() : value);
            vids.add(vid);
        }

        System.out.println("[REPORT] # of valid grids that contain mix-zones = " + gridIdx2vertices.size());

        return mmz;
    }

    int find_min(Map<Integer, Integer> vertex2associations) {
        int min_value = Integer.MAX_VALUE;
        int min_id = -1;
        for (Map.Entry<Integer, Integer> entry : vertex2associations.entrySet()) {
            if (entry.getValue() < min_value) {
                min_id = entry.getKey();
                min_value = entry.getValue();
            }
        }
        return min_id;
    }

    public int cover_by_mixzone(final SimplePoint p, final float radius) {
        Vector<Long> grids = grid.get_gridIds_by_lnglat(p.getLongitude(), p.getLatitude());
        if (grids == null)
            return -1;
        int nearest_vid = -1;
        double min_dis = Double.MAX_VALUE;
        float lng = p.getLongitude();
        float lat = p.getLatitude();
        for (long gid : grids) {
            Set<Integer> vertices = gridIdx2vertices.get(gid);
            if (vertices != null) {
                for (int vid : vertices) {
                    double dis = SimplePoint.getDistance(lng, lat, vertexList[vid].longitude, vertexList[vid].latitude);
                    if (dis <= radius && dis < min_dis) {
                        min_dis = dis;
                        nearest_vid = vid;
                    }
                }
            }
        }
        return nearest_vid;
    }

}
