/**
 * QuadTree data structure for use in the Game of Life program.
 * @author ertendian
 */

public class QuadTree<Key extends Comparable, Value> {
    /**
     * Node subclass used for the QuadTree grid
     */
    public class Node {
        Key x, y;              // x- and y- coordinates
        Node NW, NE, SE, SW;   // four subtrees
        Value value;           // tree's data

        Node(Key x, Key y, Value value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }

    private Node root; //the root node in the tree
    
    /**
     * Inserts a point into the QuadTree grid.
     * @param x x-coordinate of the data
     * @param y y-coordinate of the data
     * @param value the data to be inserted into the QuadTree
     */
    public void insert(Key x, Key y, Value value) {
        root = insert(root, x, y, value);
    }
    
    /**
     * Auxiliary method for inserting a point into the QuadTree.
     * This is a recursive method
     * @param h parent node
     * @param x x-coordinate
     * @param y y-coordinate
     * @param value data in the point
     * @return the node that was inserted
     * @see QuadTree.insert(Key x, Key y, Value value)
     */
    private Node insert(Node up, Key x, Key y, Value value) {
        if (up == null) return new Node(x, y, value);
        // if (eq(x, up.x) && eq(y, up.y)) up.value = value;  // duplicate
        else if ( less(x, up.x) &&  less(y, up.y)) up.SW = insert(up.SW, x, y, value);
        else if ( less(x, up.x) && !less(y, up.y)) up.NW = insert(up.NW, x, y, value);
        else if (!less(x, up.x) &&  less(y, up.y)) up.SE = insert(up.SE, x, y, value);
        else if (!less(x, up.x) && !less(y, up.y)) up.NE = insert(up.NE, x, y, value);
        return up;
    }
    
    public Node find(Key x, Key y) {
        return find(root, x, y);
    }
    
    private Node find(Node up, Key x, Key y) {
        if (up == null) return null;
        if (eq(x, up.x) && eq(y, up.y)) return up;
        else if ( less(x, up.x) &&  less(y, up.y)) return find(up.SW, x, y);
        else if ( less(x, up.x) && !less(y, up.y)) return find(up.NW, x, y);
        else if (!less(x, up.x) &&  less(y, up.y)) return find(up.SE, x, y);
        else if (!less(x, up.x) && !less(y, up.y)) return find(up.NE, x, y);
        else return null;
    }
    
    public Node query2D(Interval2D<Key> rect) {
        return query2D(root, rect);
    }
    
    private Node query2D(Node n, Interval2D<Key> rect) {
        if (n == null) return null;
        Key xmin = (Key) rect.intervalX.low;
        Key ymin = (Key) rect.intervalY.low;
        Key xmax = (Key) rect.intervalX.high;
        Key ymax = (Key) rect.intervalY.high;
        if (rect.contains(n.x, n.y)) return n;
        if ( less(xmin, n.x) &&  less(ymin, n.y)) return query2D(n.SW, rect);
        if ( less(xmin, n.x) && !less(ymax, n.y)) return query2D(n.NW, rect);
        if (!less(xmax, n.x) &&  less(ymin, n.y)) return query2D(n.SE, rect);
        //if (!less(xmax, n.x) && !less(ymax, n.y)) return query2D(n.NE, rect);
        else return query2D(n.NE, rect);
    }
    
    /**
     * Checks if the first key is less than the second key.
     * @param k1 first key to be compared
     * @param k2 second key to be compared
     * @return true if k1 < k2
     */
    private boolean less(Key k1, Key k2) {
        return k1.compareTo(k2) <  0;
    }
    
    /**
     * Checks if the first key is equal to the second key.
     * @param k1 first key to be compared
     * @param k2 second key to be compared
     * @return true if k1==k2
     */
    private boolean eq(Key k1, Key k2) {
        return k1.compareTo(k2) == 0;
    }
    
    public class DFSIterator implements Iterator<Node> {
        private Node curr;
        private Queue<Node> q;

        public DFSIterator() {
            q = new Queue<Node>();
            if (root!=null) DFS(root);
            curr = q.dequeue();
        }

        public Node get() {
            if (isValid()) return curr;
            return null;
        }

        public boolean isValid() {
            return (curr!=null);
        }

        public void next() {
            if (isValid()) curr = q.dequeue();
        }
        
        private void DFS(Node n) {
            q.enqueue(n);
            if (n.NW!=null) DFS(n.NW);
            if (n.NE!=null) DFS(n.NE);
            if (n.SW!=null) DFS(n.SW);
            if (n.SE!=null) DFS(n.SE);
        }
    }
}