package spatial;

public class ComplexPoint extends SimplePoint{
    public static boolean needDelta = false;

    /* latitude: 0.000001标准经度 = 0.11131955m*/
    private static final float INIT_dxy = 0.001f; // 100 meter = 0.1km
    private static final long INIT_dt = 60;   // 60 second

    long exactTimestamp = -1;    // unit: second

    // for Glove and KLT, to specify a larger range
    long deltaSecond = -1;
    float deltaLongitude = -1;
    float deltaLatitude = -1;

    public ComplexPoint(float lng, float lat, long t) {
        super(lng, lat);
        exactTimestamp = t;
        if(needDelta) {
            deltaSecond = INIT_dt;
            deltaLongitude = INIT_dxy;
            deltaLatitude = INIT_dxy;
        }
    }

    public ComplexPoint(float lng, float lat, long t, float dlng, float dlat, long dt) {
        super(lng, lat);
        exactTimestamp = t;
        deltaLongitude = dlng;
        deltaLatitude = dlat;
        deltaSecond = dt;
    }

    public ComplexPoint(SimplePoint point) {
        super(point.longitude, point.latitude);
    }

    public long get_exactTime() {
        return exactTimestamp;
    }

    public void set_time(long time) {
        exactTimestamp = time;
    }

    // only for Glove and KLT with delta information
    public long get_deltaSecond() {
        return deltaSecond;
    }

    public void set_deltaSecond(long dt) {
        deltaSecond = dt;
    }

    public long get_looseTime() {
        return exactTimestamp + deltaSecond;
    }

    public float get_deltaLongitude() {
        return deltaLongitude;
    }

    public float get_deltaLatitude() {
        return deltaLatitude;
    }

    public float get_looseLongitude() {
        return longitude + deltaLongitude;
    }

    public float get_looseLatitude() {
        return latitude + deltaLatitude;
    }
}
