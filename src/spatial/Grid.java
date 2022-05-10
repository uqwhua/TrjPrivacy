package spatial;

import java.util.*;

import static spatial.SimplePoint.getDistance;

public class Grid {
    private float latitude_max;
    private float latitude_min;
    private float longitude_max;
    private float longitude_min;

    private float latitude_step;     // the given step length
    private float longitude_step;

    public float horizontal_size;
    public float vertical_size;

    public long numOfCellX_col;   // longitude
    public long numOfCellY_row;   // latitude

    Map<Long, Set<SimplePoint>> gridIdx2Nodes;

    // for KLT
    Map<Integer, Integer[]> gridIdx2category;
    public static int TotalCategoryNum;

    // set spatial range
    public Grid(float lng_min, float lng_max, float lat_min, float lat_max, float lng_step, float lat_step) {
        setBoundary(lng_min, lng_max, lat_min, lat_max, lng_step, lat_step);
    }

    public void setBoundary(float lng_min, float lng_max, float lat_min, float lat_max, float lng_step, float lat_step) {
        latitude_step = lat_step;
        longitude_step = lng_step;

        longitude_min = (float) Math.floor(lng_min);
        longitude_max = (float) Math.ceil(lng_max);
        latitude_min = (float) Math.floor(lat_min);
        latitude_max = (float) Math.ceil(lat_max);

        numOfCellX_col = (long) Math.ceil((longitude_max - longitude_min) / lng_step);      // how many cell in one row
        numOfCellY_row = (long) Math.ceil((latitude_max - latitude_min) / lat_step);      // how many row in total

        double up = SimplePoint.getDistance(longitude_min, latitude_max, longitude_max, latitude_max);
        double down = SimplePoint.getDistance(longitude_min, latitude_min, longitude_max, latitude_min);
        double left = SimplePoint.getDistance(longitude_min, latitude_min, longitude_min, latitude_max);
        double right = SimplePoint.getDistance(longitude_max, latitude_min, longitude_max, latitude_max);
        double updown = (up + down) / 2.0;
        double leftright = (left + right) / 2.0;

        horizontal_size = (float) (updown / numOfCellX_col);
        vertical_size = (float) (leftright / numOfCellY_row);

//        showStatistics();
    }

    private void showStatistics(){
        double up = getDistance(longitude_min, latitude_max, longitude_max, latitude_max);
        double down = getDistance(longitude_min, latitude_min, longitude_max, latitude_min);
        double left = getDistance(longitude_min, latitude_min, longitude_min, latitude_max);
        double right = getDistance(longitude_max, latitude_min, longitude_max, latitude_max);
        double updown = (up + down) / 2;
        double leftright = (left + right) / 2;
        double horizontal_size = updown / numOfCellX_col;
        double vertical_size = leftright / numOfCellY_row;


        System.out.println("*******************************************************************");
        System.out.println("[Report Grid Index]");
        System.out.printf("\tlng_max = %.3f, lng_min = %.3f, lat_max = %.3f, lat_min = %.3f\n", longitude_max, longitude_min, latitude_max, latitude_min);
        System.out.printf("\tGrids = %d * %d\n", numOfCellY_row, numOfCellX_col);
        System.out.printf("\tup = %.3f, down = %.3f, avg = %.3f km\n", up, down, updown);
        System.out.printf("\tleft = %.3f, right = %.3f, avg = %.3f km\n", left, right, leftright);
        System.out.println("[Unit length of a grid]");
        System.out.printf("\tupdown/n_X_col = %.3f\n", horizontal_size);
        System.out.printf("\tleftright/n_Y_row = %.3f\n", vertical_size);
        System.out.println("*******************************************************************");
    }

    public void addPoints(Set<SimplePoint> points) {
        gridIdx2Nodes = new HashMap<>();
        gridIdx2category = new HashMap<>();
        for (SimplePoint p : points) {
            long gid = get_gridId_by_lnglat(p.getLongitude(), p.getLatitude());
            int a = (int) gid;
            if (a < 0) {
                System.out.println("ERROR: exceed integer length.");
                return;
            }

            // index of grid2points
            Set<SimplePoint> ps = gridIdx2Nodes.compute(gid, (k, v) -> v == null ? new HashSet<>() : v);
            ps.add(p);

            // index of grid2category
            int cid = p.getCategoryID();
            if (cid > 0) {
                Integer[] categories = gridIdx2category.compute((int) gid, (k, v) -> {
                    if (v == null) {
                        Integer[] tmp = new Integer[TotalCategoryNum];
                        Arrays.fill(tmp, 0);
                        return tmp;
                    } else {
                        return v;
                    }
                });

                categories[cid]++;
//                gridIdx2category.put((int) gid, categories);
            }
        }
    }

    public Set<SimplePoint> get_POIs_by_gridId(long gid) {
        return gridIdx2Nodes.get(gid);
    }

    public Map<Integer, Integer[]> get_categories() {
        return gridIdx2category;
    }

    public Integer[] get_category_by_gridId(int gid) {
        return gridIdx2category.get(gid);
    }

    public long get_gridId_by_lnglat(float lng, float lat) {
        long idx_row = (long) Math.floor((latitude_max - lat) / latitude_step);
        long idx_col = (long) Math.floor((lng - longitude_min) / longitude_step);
        long gridIdx = idx_row * numOfCellX_col + idx_col;

        if (is_invalid(gridIdx)) {    // invalid gridIdx
            return -1;
        }
        return gridIdx;
    }

    public Vector<Long> get_gridIds_by_lnglat(float lng, float lat) {
        long gridIdx = get_gridId_by_lnglat(lng, lat);
        if (gridIdx == -1)
            return null;
        else
            return surrounding(gridIdx, true);
    }

    private boolean is_invalid(long gridIdx) {
        return gridIdx < 0 || gridIdx >= numOfCellX_col * numOfCellY_row;
    }

    public Vector<Long> surrounding(long gid, boolean itself) {
        Vector<Long> results = new Vector<>();
        if (itself) {
            results.add(gid);
        }
        if (!is_invalid(gid - 1)) { // left
            results.add(gid - 1);
        }
        if (!is_invalid(gid + 1)) { // right
            results.add(gid + 1);
        }
        if (!is_invalid(gid - numOfCellX_col)) {  // up
            results.add(gid - numOfCellX_col);
        }
        if (!is_invalid(gid - numOfCellX_col - 1)) {
            results.add(gid - numOfCellX_col - 1);
        }
        if (!is_invalid(gid - numOfCellX_col + 1)) {
            results.add(gid - numOfCellX_col + 1);
        }
        if (!is_invalid(gid + numOfCellX_col)) {  // down
            results.add(gid + numOfCellX_col);
        }
        if (!is_invalid(gid + numOfCellX_col - 1)) {  // down
            results.add(gid + numOfCellX_col - 1);
        }
        if (!is_invalid(gid + numOfCellX_col + 1)) {  // down
            results.add(gid + numOfCellX_col + 1);
        }

        return results;
    }

    public float[] get_lnglat_by_gridId(int row, int col) {
        float[] lnglat = new float[2];
        lnglat[0] = longitude_min + (col + 0.5f) * longitude_step;
        lnglat[1] = latitude_max - (row + 0.5f) * latitude_step;
        return lnglat;  // the center of the grid
    }
}
