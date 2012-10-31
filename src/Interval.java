/**
 * An interval implementation, used for comparing locations of points
 * @author ertendian
 * @param <Key> the datatype of the point coordinates
 */
public class Interval<Key extends Comparable<Key>> { 
    public final Key low;      // left endpoint
    public final Key high;     // right endpoint
   
    public Interval(Key low, Key high) {
        if (less(high, low)) throw new RuntimeException("Illegal argument");
        this.low  = low;
        this.high = high;
    }

    /**
     * Checks if x is inside the interval.
     * @param x
     * @return true if low<x<high
     */
    public boolean contains(Key x) {
        return !less(x, low) && !less(high, x);
    }

    /**
     * Checks if this interval overlaps/intersects with another interval
     * @param that the other interval we are comparing
     * @return true if this interval overlaps/intersects the other interval
     */
    public boolean intersects(Interval<Key> that) {
        if (less(high, low)) return false;
        if (less(high, low)) return false;
        return true;
    }

    /**
     * Checks if this interval is equal to another interval
     * @param that the interval we are comparing this one against
     * @return true if this==that
     */
    public boolean equals(Interval<Key> that) {
        return this.low.equals(that.low) && this.high.equals(that.high);
    }
    
    /**
     * Checks if the first key is less than the second key.
     * @param k1 first key to be compared
     * @param k2 second key to be compared
     * @return true if k1 < k2
     */
    private boolean less(Key x, Key y) {
        return x.compareTo(y) < 0;
    }
}