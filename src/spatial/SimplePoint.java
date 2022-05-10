package spatial;

public class SimplePoint {
    int pid;    // used in linking
    protected float longitude;
    protected float latitude;

    private final int category_id;

    public SimplePoint(final double lng, final double lat) {
        pid = -1;
        longitude = (float) lng;
        latitude = (float) lat;
        category_id = -1;
    }

    public SimplePoint(final float lng, final float lat, final int cid) {
        pid = -1;
        longitude = lng;
        latitude = lat;
        category_id = cid;
    }

    public SimplePoint(final int id, final float lng, final float lat) {
        pid = id;
        longitude = lng;
        latitude = lat;
        category_id = -1;
    }

    public SimplePoint(final SimplePoint p) {
        pid = p.pid;
        longitude = p.longitude;
        latitude = p.latitude;
        category_id = p.category_id;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public int getCategoryID() {
        return category_id;
    }

    public static double getDistance(SimplePoint p1, SimplePoint p2) {
        return getDistance(p1.longitude, p1.latitude, p2.longitude, p2.latitude);
    }

    public static double getDistance(float lng_p1, float lat_p1, float lng_p2, float lat_p2) {

        double EARTH_RADIUS = 6378.137; //km
        double a = rad(lng_p1) - rad(lng_p2);

        double radLatA = rad(lat_p1);
        double radLatB = rad(lat_p2);
        double b = radLatA - radLatB;

        double dis = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(b / 2), 2)
                + Math.cos(radLatA) * Math.cos(radLatB) * Math.pow(Math.sin(a / 2), 2)));

        dis = dis * EARTH_RADIUS;
        dis = Math.round(dis * 10000) / (double) 10000;

        return dis;
    }

    private static double rad(float d) {
        return d * Math.PI / 180.0;
    }

    @Override
    public int hashCode() {
        int result = 0;
        long tmp = Float.floatToIntBits(longitude);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Float.floatToIntBits(latitude);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimplePoint p))
            return false;

        return Float.compare(this.latitude, p.latitude) == 0
                && Float.compare(this.longitude, p.longitude) == 0;
    }

    public void set_ID(int vertexId) {
        pid = vertexId;
    }
}
