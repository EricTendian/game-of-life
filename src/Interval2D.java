/*
 * 2D interval class, a primitive version of a rectangle
 * used for checking which region a point is in
 * @author ertendian
 */
public class Interval2D<Key extends Comparable> { 
    public Interval intervalX;   // x-interval
    public Interval intervalY;   // y-interval
   
    public Interval2D(Interval intervalX, Interval intervalY) {
        this.intervalX = intervalX;
        this.intervalY = intervalY;
    }

    /**
     * Checks to see if this 2D interval and another 2D interval, b, are equal
     * @param b other 2D interval to compare against
     * @return true if this==b
     */
    public boolean intersects(Interval2D<Key> b) {
        if (intervalX.intersects(b.intervalX)) return true;
        if (intervalY.intersects(b.intervalY)) return true;
        return false;
    }

    /**
     * Checks if a point is inside the 2D interval.
     * @param x x-coord of point
     * @param y y-coord of point
     * @return true if this 2D interval contains the point
     */
    public boolean contains(Key x, Key y) {
        return intervalX.contains(x) && intervalY.contains(y);
    }
}