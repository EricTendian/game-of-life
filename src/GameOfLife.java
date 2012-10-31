import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.MouseInputAdapter;
import java.util.Random;

public class GameOfLife extends Applet implements Runnable {
    int w;
    int h;
    //boolean grid[][];
    QuadTree<Integer, Boolean> tree;
    boolean started;
    Button b;
    Button clear;
    TextField rule;
    TextField speed;
    
    /**
     * initializes the applet and defines parameters
     * adds motionlisteners to the applet
     * so when you click a cell it changes state
     */
    public void init() {
        w = getWidth();
        h = getHeight();
        tree = new QuadTree<Integer, Boolean>();
        //grid = new boolean[(w-w%10)/10][(h-h%10)/10];
        setBackground(Color.WHITE);
        //add GUI objects
        b = new Button("Start");
        clear = new Button("Clear");
        rule = new TextField("B3/S23", 10);
        speed = new TextField("250", 4);
        add(b);
        add(clear);
        add(rule);
        add(speed);
        addMouseMotionListener(
                new MouseInputAdapter()  {
                    public void mouseDragged(MouseEvent e) {
                        if (e.isShiftDown()==true) adddellife(e.getX(), e.getY(), 2);
                        else adddellife(e.getX(), e.getY(), 1);
                    }
                });
        addMouseListener(
                new MouseInputAdapter()  {
                    public void mouseClicked(MouseEvent e) {
                        adddellife(e.getX(), e.getY(), 0);
                    }
                });
    }
    
    /*public boolean keyDown(Event e, int key) {
        if (key == Event.DOWN) started = !started;
        return true;
    }*/
    
    public boolean action(Event event, Object obj) {
        String stemp = obj.toString();

        if (stemp.equals("Start")) {
            started = true;
            b.setLabel("Stop");
            return true;
        } else if (stemp.equals("Stop")) {
            started = false;
            b.setLabel("Start");
            return true;
        } else if (stemp.equals("Clear")) {
            tree = new QuadTree();
            //grid = new boolean[(w-w%10)/10][(h-h%10)/10];
            return true;
        }
        return false;
    }
    
    public void update(Graphics g) {
        Image offI = createImage(w, h);
        Graphics offG = offI.getGraphics();
        offG.clearRect(0, 0, w, h);
        paint(offG);
        g.drawImage(offI, 0, 0, null);
    }
    
    public void paint(Graphics g) {
        g.clearRect(0, 0, w, h);
        
        //draw grid
        g.setColor(Color.LIGHT_GRAY);
        for (int i=0; i<((w-w%10)/10); i++) {
            g.drawLine(0, i*10, w, i*10);
            g.drawLine(i*10, 0, i*10, h);
        }
        
        //draw pattern
        Random r = new Random();
        g.setColor(Color.BLACK);
        //g.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
        /*for (int x = 0; x<((w-w%10)/10); x++) {
            for (int y = 0; y<((h-h%10)/10); y++) {
                if (grid[x][y]==true) g.fillRect((x*10)+1, (y*10)+1, 9, 9);
            }
        }*/
        Iterator iter = tree.new DFSIterator();
        QuadTree.Node curr = (QuadTree.Node) iter.get();
        while (curr!=null) {
            int x = Integer.parseInt(curr.x.toString());
            int y = Integer.parseInt(curr.y.toString());
            if (curr.value.equals(true)) g.fillRect((x*10)+1, (y*10)+1, 9, 9);
            iter.next();
            curr = (QuadTree.Node) iter.get();
        }
    }
    
    private void nextGeneration() {
        String r = rule.getText();
        String b = r.substring(r.indexOf("B")+1, r.indexOf("/"));
        String s = r.substring(r.indexOf("S")+1);
        
        //modify QuadTree
        Iterator iter = tree.new DFSIterator();
        QuadTree.Node curr = (QuadTree.Node) iter.get();
        while (curr!=null) {
            int x = Integer.parseInt(curr.x.toString());
            int y = Integer.parseInt(curr.y.toString());
            
            int a = 0;
            QuadTree.Node n;
            n = tree.find(x-10,y-10); //upper left
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x-10,y); //left
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x-10,y+10); //downer left
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x,y-10); //up
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x,y+10); //down
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x+10,y-10); //upper right
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x+10,y); //right
            if (n!=null && n.value.equals(true)) a++;
            n = tree.find(x+10,y+10); //downer right
            if (n!=null && n.value.equals(true)) a++;
            
            if (curr.value.equals(true)) {
                if(s.indexOf(Integer.toString(a))==-1) curr.value = false;
                else curr.value = true;
            } else {
                if(b.indexOf(Integer.toString(a))==-1) curr.value = false;
                else curr.value = true;
            }
            
            iter.next();
            curr = (QuadTree.Node) iter.get();
        }
    }
    
    private void adddellife(int x, int y, int state) {
        int i = x/10;
        int j = y/10;
        if (state==1) tree.insert(i, j, true);
        else if (state==2) {
            QuadTree.Node n = tree.find(i, j);
            if (n!=null) n.value = false;
        } else {
            QuadTree.Node n = tree.find(i, j);
            if (n!=null && n.value.equals(true)) n.value = false;
            else tree.insert(i, j, true);
        }
        
        /*if (i<grid.length && j<grid[0].length) {
            if (state==1) grid[i][j] = true;
            else if (state==2) grid[i][j] = false;
            else grid[i][j] = !grid[i][j];
        }*/
        repaint();
    }
    
    public void start() {
        Thread th = new Thread(this);
        th.start();
    }
    
    public void run() {
        while (true) {
            if (started==true) nextGeneration();
            repaint();
            try {
                if (speed.getText().length()>0) Thread.sleep(Integer.parseInt(speed.getText()));
            } catch (InterruptedException ex) {}
        }
    }
}