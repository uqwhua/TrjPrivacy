package models.mixzone.graph;

import java.util.LinkedList;

public class Vertex {
    private final int id;
    private final LinkedList<Integer> adjacentList;
    protected boolean is_mixzone = false;
    protected boolean is_ap = false;
    protected float longitude;
    protected float latitude;

    Vertex(final int _id, final float _lng, final float _lat){
        id = _id;
        adjacentList = new LinkedList<>();
        longitude = _lng;
        latitude = _lat;
    }

    Vertex(Vertex v){
        id = v.id;
        adjacentList = new LinkedList<>(v.adjacentList);
        is_mixzone = v.is_mixzone;
        is_ap = v.is_ap;
        longitude = v.longitude;
        latitude = v.latitude;
    }

    int get_degree(){
        return adjacentList.size();
    }

    int get_neighbor_by_idx(int idx){
        if(idx >= adjacentList.size()){
            return -1;
        }
        return adjacentList.get(idx);
    }

    void add_neighbor(int vid){
        if(!adjacentList.contains(vid)){
            adjacentList.push(vid);
        }
    }

    public void remove_neighbor(Integer id) {
        adjacentList.remove(id);
    }
}
