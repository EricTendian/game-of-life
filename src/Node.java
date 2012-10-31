public class Node<Key extends Comparable, Value> {
    Key x, y;              // x- and y- coordinates
    Node NW, NE, SE, SW;   // four subtrees
    Value value;           // associated data
    
    Node(Key x, Key y, Value value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }
}