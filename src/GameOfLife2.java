//////////////////////////////////////////////////////////
/* Bezier Surface, Copyright 2001-2010 Ryoichi Mizuno   */
/* ryoichi[at]mizuno.org                                */
/* Dept. of Complexity Science and Engineering          */
/* at The University of Tokyo                           */
//////////////////////////////////////////////////////////

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

public class GameOfLife2 extends Applet implements Runnable {

    int cells = 52, size = 10, generation = 0, population, newborncell = 0;
    Thread th = null;
    Image buf_i;
    Graphics buf_g;
    int w, h;
    boolean life[][] = new boolean[cells][cells];
    boolean next[][] = new boolean[cells][cells];
    boolean remain[][] = new boolean[cells][cells];
    boolean startflag = false;
    Button bt1, bt2;

    public void init() {
        w = getSize().width;
        h = getSize().height;
        buf_i = createImage(w, h);
        buf_g = buf_i.getGraphics();
        setLayout(new FlowLayout(1, 20, h - 60));
        add(bt1 = new Button("start"));
        add(bt2 = new Button("clear"));
        bt1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                startstopgame();
            }
        });
        bt2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearcell();
            }
        });
        addMouseListener(
                new MouseAdapter() {

                    public void mouseClicked(MouseEvent e) {
                        adddellife(e.getX(), e.getY());
                    }
                });
        addMouseMotionListener(
                new MouseMotionAdapter() {

                    public void mouseMoved(MouseEvent e) {
                        showcellstatus(e.getX(), e.getY());
                    }
                });
        setBackground(Color.white);
    }

    public void update(Graphics g) {
        this.paint(g);
    }

    public void paint(Graphics g) {
        int i, j;
        init_b();
        drawgrid();
        drawinfo();
        for (i = 0; i < cells; i++) {
            for (j = 0; j < cells; j++) {
                if (remain[i][j]) {
                    buf_g.setColor(Color.blue);
                } else {
                    buf_g.setColor(Color.green);
                }
                if (life[i][j]) {
                    buf_g.fill3DRect(i * size, j * size, size, size, true);
                }
            }
        }
        g.drawImage(buf_i, 0, 0, this);
    }

    public void init_b() {
        buf_g.setColor(Color.white);
        buf_g.fillRect(0, 0, w, h);
        buf_g.setColor(Color.black);
    }

    public void start() {
        if (th == null) {
            th = new Thread(this);
            th.start();
        }
    }

    public void run() {
        int i, j, a;
        while (true) {
            try {
                if (startflag) {
                    population = 0;
                    for (i = 0; i < cells; i++) {
                        for (j = 0; j < cells; j++) {
                            a = 0;
                            if (life[(i + cells - 1) % cells][j]) {
                                a++; //left
                            }
                            if (life[(i + 1) % cells][j]) {
                                a++; //right
                            }
                            if (life[i][(j + cells - 1) % cells]) {
                                a++; //up
                            }
                            if (life[i][(j + 1) % cells]) {
                                a++; //down
                            }
                            if (life[(i + cells - 1) % cells][(j + cells - 1) % cells]) {
                                a++; //upper left
                            }
                            if (life[(i + 1) % cells][(j + cells - 1) % cells]) {
                                a++; //upper right
                            }
                            if (life[(i + cells - 1) % cells][(j + 1) % cells]) {
                                a++; //downer left
                            }
                            if (life[(i + 1) % cells][(j + 1) % cells]) {
                                a++; //downer right
                            }
                            if (life[i][j]) {
                                if (a == 2 || a == 3) {
                                    next[i][j] = true;
                                    population++;
                                } else {
                                    next[i][j] = false;
                                }
                            } else {
                                if (a == 3) {
                                    next[i][j] = true;
                                    population++;
                                } else {
                                    next[i][j] = false;
                                }
                            }
                        }
                    }
                    judgeremain();
                    nextgeneration();
                    generation++;
                    repaint();
                }
                th.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    public void stop() {
        if (th != null) {
            //th.stop();
            th = null;
        }
    }

    public void judgeremain() {
        int i, j;
        newborncell = 0;
        for (i = 0; i < cells; i++) {
            for (j = 0; j < cells; j++) {
                if (life[i][j] == next[i][j]) {
                    remain[i][j] = true;
                } else {
                    remain[i][j] = false;
                    if (next[i][j]) {
                        newborncell++;
                    }
                }
            }
        }
    }

    public void nextgeneration() {
        int i, j;
        for (i = 0; i < cells; i++) {
            for (j = 0; j < cells; j++) {
                life[i][j] = next[i][j];
            }
        }
    }

    public void drawgrid() {
        int i;
        buf_g.setColor(Color.gray);
        for (i = 0; i <= cells; i++) {
            buf_g.drawLine(0, i * size, size * cells, i * size);
            buf_g.drawLine(i * size, 0, i * size, size * cells);
        }
    }

    public void drawinfo() {
        int remaincell;
        String info[] = new String[3];
        String detail[] = new String[2];
        String label[] = new String[2];
        int number;
        buf_g.setColor(Color.green);
        buf_g.fill3DRect(w - 110, 60, size, size, true);
        buf_g.setColor(Color.blue);
        buf_g.fill3DRect(w - 110, 80, size, size, true);
        label[0] = "new born cell";
        label[1] = "remain cell";
        info[0] = "generation: " + generation;
        info[1] = "population: " + population;
        info[2] = "percentage: " + (int) ((double) population / Math.pow((double) cells, 2) * 100);
        detail[0] = "new born cell: " + newborncell;
        remaincell = population - newborncell;
        detail[1] = "remain cell: " + remaincell;
        buf_g.setColor(Color.black);
        buf_g.drawString("Java game of life", w - 110, 30);
        for (number = 0; number < 2; number++) {
            buf_g.drawString(label[number], w - 90, 70 + number * 20);
        }
        for (number = 0; number < 3; number++) {
            buf_g.drawString(info[number], w - 110, 130 + number * 20);
        }
        for (number = 0; number < 2; number++) {
            buf_g.drawString(detail[number], w - 110, 200 + number * 20);
        }
        if (newborncell < 0 || remaincell < 0) {
            buf_g.setColor(Color.red);
            buf_g.drawString("bug occured!", w - 110, 250);
        }
    }

    public void adddellife(int px, int py) {
        int i, j;
        i = (px - px % 10) / size;
        j = (py - py % 10) / size;
        if (life[i][j]) {
            life[i][j] = false;
        } else {
            life[i][j] = true;
        }
        repaint();
    }

    public void showcellstatus(int px, int py) {
        int x, y;
        String cellstat;
        x = (px - px % 10) / size + 1;
        y = (py - py % 10) / size + 1;
        if (x <= cells && y <= cells) {
            cellstat = x + ", " + y + ", " + life[x - 1][y - 1];
            showStatus(cellstat);
        }
    }

    public void startstopgame() {
        if (startflag) {
            startflag = false;
            bt1.setLabel("start");
        } else {
            startflag = true;
            bt1.setLabel("stop");
        }
    }

    public void clearcell() {
        int i, j;
        if (startflag) {
            startflag = false;
            bt1.setLabel("start");
        }
        for (i = 0; i < cells; i++) {
            for (j = 0; j < cells; j++) {
                life[i][j] = false;
            }
        }
        generation = 0;
        population = 0;
        newborncell = 0;
        repaint();
    }
}