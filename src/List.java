public class List<E> {

    private class Node {
	public E data;
	public Node next;

	public Node(E data) {
	    this.data = data;
	}

	public Node(E data, Node next) {
	    this.data = data;
	    this.next = next;
	}
    }

    private int size;
    private Node first;
    private Node last;

    public List() {
	size = 0;
	first = null;
	last = null;
    }

    public int size() {
	return size;
    }
	    
    public int find(E data) {
	Node cursor = first;
	int counter = 0;
	boolean found = false;

	while (cursor != null && !found) {
	    counter++;
	    if (cursor.data != data) 
		cursor = cursor.next;
	    else
		found = true;
	}

	if (found)
	    return counter;
	else
	    return 0;
    }

    public void insertAtFront(E data) {
	first = new Node(data,first);
	if (last==null)
	    last = first;
	size++;
    }

    public void insertAtEnd(E data) {
	Node tmp = new Node(data);

	size++;
	if (last == null) {
	    first = tmp;
	    last = tmp;
	} else {
	    last.next = tmp;
	    last = tmp;
	}
    }

    public E removeFromFront() {
	E tmp;

	if (first == null) 
	    return null;
	else {
	    tmp = first.data;
	    first = first.next;
	    size--;
	    if (first == null)
		last = null;
	    return tmp;
	}
    }
}
