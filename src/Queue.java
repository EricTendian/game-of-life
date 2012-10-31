public class Queue<E> {
    private List<E> l;//
    
    public Queue() {
        l = new List<E>();
    }
    
    public void enqueue(E data) {
        l.insertAtEnd(data);
    }
    
    public E dequeue() {
        return l.removeFromFront();
    }
    
    public E front() {
        E first = l.removeFromFront();
        if (first!=null) l.insertAtFront(first);
        return first;
    }
    
    public int size() {
        return l.size();
    }
}
