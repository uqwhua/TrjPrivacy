package models.mixzone.graph;

import java.util.HashSet;
import java.util.Set;

public class Edge {
    private final int endpoint_head;
    private final int endpoint_tail;
    private final int head_next;
    private final int tail_next;   //指向同一个弧尾的下一条边
    protected boolean is_bg;  // bridge edge

    Edge(int head, int tail) {
        endpoint_head = head;
        endpoint_tail = tail;
        head_next = tail_next = -1;
        is_bg = false;
    }

    Edge(int head, int tail, int h_next, int t_next) {
        endpoint_head = head;
        endpoint_tail = tail;
        head_next = h_next;
        tail_next = t_next;
        is_bg = false;
    }

    Edge(Edge e) {
        endpoint_head = e.endpoint_head;
        endpoint_tail = e.endpoint_tail;
        head_next = e.head_next;
        tail_next = e.tail_next;
        is_bg = e.is_bg;
    }

    public int get_endpoint_head() {
        return endpoint_head;
    }

    public int get_endpoint_tail() {
        return endpoint_tail;
    }

    public int find_another_endpoint(int vid) {
        if (endpoint_head == vid)
            return endpoint_tail;
        else if (endpoint_tail == vid)
            return endpoint_head;
        else
            return -1;
    }

    @Override
    public int hashCode() {
        Set<Integer> set = new HashSet<>();
        set.add(endpoint_head);
        set.add(endpoint_tail);
        return set.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof Edge e))
            return false;
        if (o == this)
            return true;

        // only care about two endpoints
        return endpoint_head == e.endpoint_head && endpoint_tail == e.endpoint_tail ||
                endpoint_head == e.endpoint_tail && endpoint_tail == e.endpoint_head;
    }

    public int get_next(int vid) {
        if (vid == endpoint_head)
            return head_next;
        else if (vid == endpoint_tail)
            return tail_next;
        else
            return -1;
    }
}
