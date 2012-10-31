package life.v41d;

/**************************************************************
 Life.java

 Conway's Game of Life (With Extra Caffeine).
 (c) 1996 by Alan Hensel, All Rights Reserved

 This version of Conway's Game of Life was designed with the
 following principles in mind:
 - To be FAST! (see LifeGen.java for super-optimized algorithm)
 - To have a huge universe:  1 million x 1 million.
 - To be user friendly.
 - To support the huge database of great patterns already out there.

 The class hierarchy is as follows:

                Life
               /    \
          LifeGUI   LifeGen
                          \
                         LifeCell


 Life:       Top-level threads and logic.
 LifeGUI:    The GUI.
 LifeGen:    The super-fast algorithm.
 LifeCell:   A 16x16 piece of the Life universe.

**************************************************************/

import java.applet.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;

class Life extends Panel implements Runnable, LifeCallback
{
   private boolean initFlag;
   private boolean dragState; // turn cells on while dragging
   private int origX, origY;  // Life universe coordinates at the center of the LifeGUI
   private LifeGUI f;         // the graphical field
   private LifeGen u;         // the universe itself
   private int drawX, drawY;
   private final static int REPAINT_DEFERRED   = -2, // do a full repaint when ready
                            REPAINT_UNEXPECTED = -1, // repaint from java environment
                            REPAINT_FULL       =  1, // full repaint (with flicker)
                            REPAINT_ONECELL    =  2, // repaint one cell
                            REPAINT_UPDATE     =  3; // update field
   private int updateState = REPAINT_UNEXPECTED;

   private Thread twin;
   private Button goBtn = null;
   private Choice goHowFar = null;
   private Button clearBtn = null;
   private Button speedBtn = null;
   private Button rulesBtn = null;
   private Button optionsBtn = null;
   private Button openBtn = null;
   private Label zoomLabel = null;
   private Choice zoomChoice = null;
   private Label blankLabel = null;
   private Label blankLabel2 = null;
   private Button quitBtn = null;
   private int zoom;
   private Scrollbar hbar=null, vbar=null;
   private int how_many, refresh_rate, skipping;
   private int default_refresh_rate, default_skipping; // reset on Clear
   private String load_param;
   private String set_rules;
   private boolean new_rules;
   private boolean clearonload=true;
   private Vector description = null;
   private int descriptionlen = 0, descriptionwid = 0;
   private LoadBox openBx;
   private SpeedBox spdBx;
   private RuleBox rulBx;
   private OptionsBox optBx;
   private DescribeBox descBx = null;
   private LifeQueue lifeQueue = null;
   private boolean realGoFlag; // remains true thru pit-stops; u.goFlag doesnt
   private Image offscreenImage;
   private Graphics offscreenGraphics;
   private Image countImage;
   private Graphics countGraphics;
   private FontMetrics countMetrics;
   private int countSpot=0;
   private int countWid=60;
   private int countY=0;
   private int countCY, countCH;
   private int countXOld = 0;
   private int countstrlenOld=0;
   private boolean isCountThere=false;
   private boolean editable=true;
   private boolean describe=true;
   private int scrollValue=0;
   public static Object mutex = new Object();
   private boolean gnoset=true;
   private int frameWidth, frameHeight;
   private int toolbarHeight=25;
   private boolean grids=true;
   private String fgcolor, bgcolor, gridcolor;
   private int scrollbarwidth;
   private String toolbar, howfarChoices;

   public void init()
   {
      // get the applet params

      zoom = LifeButton.zoom;
      load_param = LifeButton.load_param;
      refresh_rate = default_refresh_rate = LifeButton.refresh_rate;
      skipping = default_skipping = LifeButton.skipping;

      openBx = new LoadBox(this);
      openBx.setTitle("Patterns");

      if (LifeButton.rules != null) set_rules = new String(LifeButton.rules);
      else set_rules = null;
      rulBx = new RuleBox(this);
      rulBx.setTitle("Life Rules");
      rulBx.setResizable(false);

      spdBx = new SpeedBox(this);
      spdBx.setTitle("Life Speed");
      spdBx.setResizable(false);

      optBx = new OptionsBox(this);
      optBx.setTitle("Life Options");
      optBx.setResizable(false);

      fgcolor = LifeButton.fgcolor;
      bgcolor = "ffffff";  // this parameter is irrelevant now
      gridcolor=LifeButton.gridcolor;

      scrollbarwidth = LifeButton.scrollbarwidth;

      toolbar = LifeButton.toolbar;
      if (toolbar.length()==0)
         toolbarHeight=0;

      grids = LifeButton.grids;
      editable = LifeButton.editable;
      describe = LifeButton.describe;
      realGoFlag = LifeButton.autostart;

      howfarChoices = LifeButton.howfarChoices;

      initFlag=true;

      // set up the graphics:
      origX=0; origY=0;   // center of universe

      setupGUIfromFrame(zoom);

      gnoset=false;

      // set up the generate thread
      twin = new Thread(this);
      twin.setPriority(Thread.MAX_PRIORITY);
      twin.start();
   }

   private void setupGUIfromFrame(int z)
   {
      int fw, fh;

      frameWidth = size().width;
      frameHeight= size().height;

      fw=frameWidth-scrollbarwidth;
      if (fw<0) return;
      fh=frameHeight-toolbarHeight-scrollbarwidth+1;
      if (fh<0) return;
      fw-=(fw%32); fh-=(fh%32);

      f = new LifeGUI(fw,fh,z,toolbarHeight,grids);

      offscreenImage=createImage(fw+1, fh+1);
      offscreenGraphics=offscreenImage.getGraphics();

      f.setColors(fgcolor,bgcolor,gridcolor);
      f.setGraphicsContext(offscreenGraphics);
      drawComponents(toolbar, howfarChoices, scrollbarwidth, z);
   }

   public void paint(Graphics g)
   {
      if (gnoset==false)
      {
         f.clear();

         if (frameWidth != size().width
               || frameHeight!= size().height)
         {
            int cx = f.getCenterX();
            int cy = f.getCenterY();

            setupGUIfromFrame(f.getZoom());
            f.moveTo(cx, cy);
         }

         f.drawGrid();

         if (u!=null) f.updateAll(u);

         drawCount();
         g.drawImage(offscreenImage, 0, toolbarHeight, null);
         if (isCountThere)
            g.drawImage(countImage, countSpot, 0, null);
      }
   }

   public void removeAllComponents()
   {
      if (blankLabel2!=null) remove(blankLabel2);
      if (blankLabel!=null) remove(blankLabel);
      if (zoomLabel!=null) remove(zoomLabel);

      if (goHowFar!=null) remove(goHowFar);
      if (zoomChoice!=null) remove(zoomChoice);

      if (hbar!=null) remove(hbar);
      if (vbar!=null) remove(vbar);

      if (goBtn != null) remove(goBtn);
      if (clearBtn != null) remove(clearBtn);
      if (rulesBtn != null) remove(rulesBtn);
      if (speedBtn != null) remove(speedBtn);
      if (optionsBtn != null) remove(optionsBtn);
      if (openBtn != null) remove(openBtn);
      if (quitBtn != null) remove(quitBtn);

      blankLabel2 = blankLabel = zoomLabel = null;
      goHowFar = zoomChoice = null;
      hbar = vbar = null;
      goBtn = clearBtn = rulesBtn = optionsBtn = speedBtn =
         openBtn = quitBtn = null;
   }

   private void paintAsync(int ups)
   {
      updateState=ups;
      repaint();
   }

   private void paintLife(int ups)
   {
      synchronized(mutex)
      {
         updateState=ups;
         repaint();
         try {mutex.wait(200);}
         catch (InterruptedException ie) {}
      }
   }

   public void update(Graphics g)
   {
      int ups;

      synchronized(mutex)
      {
         if (updateState<0)  // DEFERRED or UNEXPECTED
         {
            updateState= REPAINT_DEFERRED;
            mutex.notifyAll();
            return;
         }
         ups=updateState;
         updateState= REPAINT_UNEXPECTED;

         switch(ups)
         {
            case REPAINT_UPDATE:
               if (u!=null) f.updateAll(u);

               drawCount();
               g.drawImage(offscreenImage, 0, toolbarHeight, null);
               if (isCountThere)
                  g.drawImage(countImage, countSpot, 0, null);
               break;

            case REPAINT_FULL:
               paint(g);
               break;

            case REPAINT_ONECELL:
               f.changeCell(drawX, drawY);
               f.drawCell(drawX, drawY);
               g.drawImage(offscreenImage, 0, toolbarHeight, null);
               break;
         }

         mutex.notifyAll();
      }
   }

   private void drawCount()
   {
      if (isCountThere)
      {
         String countstr = (new Integer(u==null?0:u.gencount)).toString();
         int countstrlen = countMetrics.stringWidth(countstr);
         int countX = countWid - countstrlen - 1;

         countGraphics.clearRect(countXOld, countCY,
               countstrlenOld, countCH); 
         countGraphics.drawString(countstr, countX, countY);

         countXOld=countX;
         countstrlenOld=countstrlen;
      }
   }

   private void drawComponents(String toolbar, String howfarChoices,
         int scrollbarwidth, int z)
   {
      Panel p;
      int oX = f.oX;
      int oY = f.oY;
      int w = f.getCellsAcross() * f.getPixelsPerCell();
      int h = f.getCellsDown() * f.getPixelsPerCell();
      int a, wid, ew, nb, bwid;
      StringTokenizer st;
      String buttonName;
      int howfarWid=80;
      int zoomLabelWid=56;
      int zoomWid=48;

      removeAllComponents();

      setLayout(null);

      st = new StringTokenizer(toolbar);

      ew=0; nb=1;  // 1 button required: "Quit"
      while (st.hasMoreTokens())
      {
         buttonName=st.nextToken();

         if (buttonName.equals("Go")) nb++;
         else if (buttonName.equals("HowFar")) ew+=howfarWid;
         else if (buttonName.equals("Clear")) nb++;
         else if (buttonName.equals("Rules")) nb++;
         else if (buttonName.equals("Speed")) nb++;
         else if (buttonName.equals("Options")) nb++;
         else if (buttonName.equals("Open")) nb++;
         else if (buttonName.equals("Zoom")) ew+=zoomLabelWid+zoomWid;
         else if (buttonName.equals("Count")) ew+=countWid;
      }

      bwid = (w+scrollbarwidth-ew)/nb;  // find a button width
      if (bwid<40) bwid=40; // minimum button width = 40
      if (bwid>100) bwid=100; // maximum button width = 100 

      st = new StringTokenizer(toolbar);

      a = 0;
      while (st.hasMoreTokens())
      {
         buttonName = st.nextToken();
         wid = bwid;  // default assumption: button width

         if (buttonName.equals("Go"))
         {
            if (realGoFlag) goBtn = newBtn("Stop", a, wid);
            else           goBtn = newBtn(" Go ", a, wid);
            if (u==null || u.isEmpty()) goBtn.disable();
         }
         else if (buttonName.equals("HowFar"))
         {
            wid = howfarWid;
            goHowFar = new Choice();
            StringTokenizer hfst=new StringTokenizer(howfarChoices);
            while (hfst.hasMoreTokens())
               goHowFar.addItem(hfst.nextToken());
            addToToolbar(goHowFar, a, wid);
         }
         else if (buttonName.equals("Clear")) {
            clearBtn = newBtn("Clear", a, wid);
         }
         else if (buttonName.equals("Rules")) {
            rulesBtn = newBtn("Rules", a, wid);
         }
         else if (buttonName.equals("Options")) {
            optionsBtn = newBtn("Options", a, wid);
         }
         else if (buttonName.equals("Speed")) {
            speedBtn = newBtn("Speed", a, wid);
         }
         else if (buttonName.equals("Open")) {
            openBtn = newBtn("Open", a, wid);
         }
         else if (buttonName.equals("Zoom"))
         {
            wid = zoomLabelWid+zoomWid;
            zoomLabel = new Label("Zoom:", Label.RIGHT);
            addToToolbar(zoomLabel, a, zoomLabelWid);

            zoomChoice = new Choice();
            for (int i=0; i<6; i++) zoomChoice.addItem(String.valueOf(i));
            zoomChoice.select(String.valueOf(z));

            addToToolbar(zoomChoice, a+zoomLabelWid, zoomWid);
         }
         else if (buttonName.equals("Count"))
         {
            wid = 0;
            countSpot = w+scrollbarwidth-countWid;

            countImage=createImage(countWid, toolbarHeight);
            countGraphics=countImage.getGraphics();
            countMetrics=countGraphics.getFontMetrics();
            countCH = countMetrics.getHeight();
            countY = (toolbarHeight + countCH)/2 - 1;
            countCY = toolbarHeight - countY;
            isCountThere=true;
            drawCount();
         }
         a += wid;
      }

      quitBtn = newBtn("Quit", a, bwid);
      a += bwid;

      if (goBtn==null) realGoFlag=true; // no Go button: start automatically

      if (scrollbarwidth>0)
      {   
         hbar = new Scrollbar(Scrollbar.HORIZONTAL);
         hbar.setValues(0,1,-15,16);
         hbar.reshape(oX-1, h+oY+toolbarHeight, w+1, scrollbarwidth-1);
         add(hbar);

         vbar = new Scrollbar(Scrollbar.VERTICAL);
         vbar.setValues(0,1,-15,16);
         vbar.reshape(w+oX, oY-1+toolbarHeight, scrollbarwidth-1, h+1);
         add(vbar);

         // fill in the scrollbar corner with blankness,
         // because Netscape paints it white.
         blankLabel2 = new Label("");
         blankLabel2.reshape(w+oX, h+oY+toolbarHeight,
               scrollbarwidth-1, scrollbarwidth-1);
         add(blankLabel2);
      }

      // fill in the extra toolbar space with blankness,
      // because Netscape paints it white.
      int spaceleft = w+scrollbarwidth-a-(isCountThere?countWid:0);
      if (spaceleft>0)
      {
         blankLabel = new Label("");
         addToToolbar(blankLabel, a,
               w+scrollbarwidth-a-(isCountThere?countWid:0));
      }
   }

   private Button newBtn(String name, int pos, int wid)
   {
      Button btn = new Button(name);
      addToToolbar(btn, pos, wid);
      return btn;
   }

   private void addToToolbar(Component c, int pos, int wid)
   {
      c.reshape(pos, 0, wid-1, toolbarHeight-1);
      add(c);
   }

   public void run()
   {
      boolean break4blap=false;
      boolean inaction=false;

      if (lifeQueue == null) lifeQueue = new LifeQueue();

      if ((twin != null) && initFlag)
      {
         // set up the internal algorithm:
         u = new LifeGen(set_rules);

         if (load_param != null) load(load_param);
         u.setRefresh(1000/refresh_rate);
         u.setSpeed(skipping);

         initFlag=false;
         paintLife(REPAINT_FULL);
      }

      u.goFlag = realGoFlag;

      while (twin != null)
      {
         if (new_rules)
         {
            u.setRules(set_rules);
            new_rules=false;
         }

         if (updateState==REPAINT_DEFERRED)
         {
            paintLife(REPAINT_FULL);
         }

         if (u.goFlag)
         {
            break4blap=u.generate(how_many, break4blap);

            if (how_many>0) realGoFlag=false;

            if (lifeQueue.length()==0)
            {
               paintLife(updateState==REPAINT_DEFERRED? REPAINT_FULL:
                                                        REPAINT_UPDATE);
            }
         }
         else
         {
            break4blap=false;

            if (goBtn!=null)
            {
               haltGoButton();
               if (u!=null)
               {
                  if (u.isEmpty()) goBtn.disable();
                  else if (goHowFar!=null &&
                        goHowFar.getSelectedItem().equals("-1") &&
                        u.backCorrect==false) goBtn.disable();
                  else goBtn.enable();
               }
               else
               {
                  goBtn.disable();
               }
            }

            try {Thread.currentThread().sleep(20);}
            catch (InterruptedException e) {}
         }

         processEvents();
      }
   }

   public boolean mouseDown(Event e, int x1, int y1)
   {
      int x,y;

      if (!editable) return true;

      if (u==null || f==null) return false;

      requestFocus();

      x = f.convertAppletToFieldX(x1);
      y = f.convertAppletToFieldY(y1);

      if (x<0 || x>=f.getCellsAcross()
       || y<0 || y>=f.getCellsDown())
      {
         return true;
      }

      dragState=!f.cellState(x,y);

      u.changeCell(f.convertFieldToUniverseX(x),
                   f.convertFieldToUniverseY(y),
                   dragState);

      goBtn.enable();
      drawX=x; drawY=y;
      paintAsync(REPAINT_ONECELL);

      return true;
   }

   public void drawLifeLine(int x1, int y1, int x2, int y2)
   {
      int shortdiff, longdiff;  // short dimension and long dimension

      int xdiff = Math.abs(x2-x1);
      int ydiff = Math.abs(y2-y1);

      boolean across = (xdiff > ydiff);
      if (across) {
         shortdiff = ydiff;
         longdiff = xdiff;
      } else {
         shortdiff = xdiff;
         longdiff = ydiff;
      }

      // determine direction
      int xright = (x2 > x1)? 1 : -1;
      int ydown  = (y2 > y1)? 1 : -1;

      // draw the line
      int x, y, j=0, wrap=0;
      for (int i=0; i<longdiff; i++)
      {
         if (across) {
            x = x1+(i*xright);
            y = y1+(j*ydown);
         } else {
            x = x1+(j*xright);
            y = y1+(i*ydown);
         }

         if (dragState!=f.cellState(x,y))
            u.changeCell(f.convertFieldToUniverseX(x),
                         f.convertFieldToUniverseY(y),
                         dragState);

         wrap += shortdiff;
         if (wrap >= longdiff) {
            wrap %= longdiff;
            j++;
         }
      }

      goBtn.enable();
      paintAsync(REPAINT_UPDATE);
   }

   public boolean mouseDrag(Event e, int x1, int y1)
   {
      int x,y;

      if (!editable) return true;

      if (u==null || f==null) return false;

      x = f.convertAppletToFieldX(x1);
      y = f.convertAppletToFieldY(y1);

      if (drawX==x && drawY==y) return true;

      if (x<0 || x>=f.getCellsAcross()
       || y<0 || y>=f.getCellsDown()) return true;

      drawLifeLine(drawX, drawY, x, y);
      drawX=x; drawY=y;

      return true;
   }

   private void processEvents()
   {
      Event e;
      int v;

      while ((e = (Event)lifeQueue.pull()) != null)
      {
         if (e.target instanceof Button)
         {
            if (e.id == Event.ACTION_EVENT)
               processButton((String)e.arg);
            else
               u.goFlag = realGoFlag;
         }
         else if (e.target == zoomChoice)
         {
            if (f.setZoom(Integer.parseInt(zoomChoice.getSelectedItem())))
            {
               paintLife(REPAINT_FULL);
            }
            u.goFlag = realGoFlag;
         }
         else if (e.target == goHowFar)
         {
            u.goFlag = realGoFlag;
         }
         else if (e.target instanceof Scrollbar)
         {
            v=0;
            switch(e.id)
            {
               case Event.SCROLL_LINE_UP:
                  v= -1;
                  break;

               case Event.SCROLL_PAGE_UP:
                  v=-f.getCellsDown()/2;
                  if (v==0) v=1;
                  break;

               case Event.SCROLL_LINE_DOWN:
                  v=1;
                  break;

               case Event.SCROLL_PAGE_DOWN:
                  v=f.getCellsDown()/2;
                  if (v==0) v=1;
                  break;

               case Event.SCROLL_ABSOLUTE:
                  // AWT can't handle the kind of scrolling I'd like.
                  // It reports absolute scrollbar position, but does
                  // not deliver an event when the thumb is released.
                  // So how do I know when to return the thumb to the
                  // middle? If I do it every time, the scroll amounts
                  // are all wrong. There does not appear to be any
                  // workaround. So, no absolute scrolling.
                  v=0;
                  hbar.setValue(0);
                  vbar.setValue(0);
                  break;
            }

            if (v!=0)
            {
               if (e.target == hbar)
               {
                  origX += v;
                  hbar.setValue(0);
               }
               else
               {
                  origY += v;
                  vbar.setValue(0);
               }

               f.moveTo(origX,origY);
               u.freshenView();

               paintLife(REPAINT_FULL);
            }

            u.goFlag = realGoFlag;
         }
      }
   }

   private void haltGoButton()
   {
      if (goBtn!=null && !goBtn.getLabel().equals(" Go "))
         goBtn.setLabel(" Go ");
      realGoFlag=false;
      if (u!=null) u.goFlag=false;
   }

   private void processButton(String buttonPressed)
   {
      if (buttonPressed.equals(" Go "))
      {
         if (goHowFar!=null)
         {
            String howFar = goHowFar.getSelectedItem();

            if (howFar.startsWith("+")) howFar = howFar.substring(1);

            try {
               how_many = Integer.parseInt(howFar);
            }catch (NumberFormatException e) {
               how_many = 0;
            }
         }
         else how_many = 0;

         if (how_many!=1 && how_many!=-1) goBtn.setLabel("Stop");
         u.goFlag = realGoFlag = true;
      }
      else if (buttonPressed.equals("Stop")) {
         haltGoButton();
         paintLife(REPAINT_UPDATE);
      }
      else if (buttonPressed.equals("Clear")) {
         skipping=default_skipping;
         u.setSpeed(skipping);
         refresh_rate=default_refresh_rate;
         u.setRefresh(1000/refresh_rate);

         clear();
      }
      else if (buttonPressed.equals("Open")) {
         openBx.listURL(LifeButton.loaddir);
         haltAndShow(openBx);
      }
      else if (buttonPressed.equals("Rules")) {
         rulBx.enterRules(u.getRules());
         haltAndShow(rulBx);
      }
      else if (buttonPressed.equals("Speed")) {
         spdBx.enterData(refresh_rate, skipping);
         haltAndShow(spdBx);
      }
      else if (buttonPressed.equals("Options")) {
         optBx.enterData(clearonload);
         haltAndShow(optBx);
      }
      else if (buttonPressed.equals("Quit")) {
         quit();
      }
   }

   private void haltAndShow(Frame box)
   {
      haltGoButton();
      box.pack();
      box.show();
   }

   public void clear()
   {
      haltGoButton();
      goBtn.disable();
      u.clear();
      origX = origY = 0;
      f.moveTo(0,0);
      if (descBx != null) descBx.dispose();
      descBx = null;
      paintLife(REPAINT_FULL);
   }

   public void quit()
   {
      if (openBx != null) openBx.dispose();
      if (rulBx != null) rulBx.dispose();
      if (spdBx != null) spdBx.dispose();
      if (optBx != null) optBx.dispose();
      if (descBx != null) descBx.dispose();
      ((Frame)getParent()).dispose();
      twin = null;
   }

   public boolean handleEvent(Event e)
   {
      if (lifeQueue==null) lifeQueue = new LifeQueue();

      if (e.target instanceof Scrollbar
            || e.target instanceof Choice
            || e.target instanceof Button)
      {
         lifeQueue.push(e);
         if (u!=null) u.goFlag=false;
         return true;
      }
      else if (e.target instanceof Frame)
      {
         if (e.id == Event.WINDOW_DESTROY) {
            quit();
         }
      }

      return super.handleEvent(e);
   }


   public void setRules(String rules)
   {
      // coming from the RuleBox!!!

      set_rules = new String(rules);
      new_rules = true;
      haltGoButton();
      u.goFlag = false;
   }

   private boolean readLife105(DataInputStream lifefile) throws IOException
   {
      String s;
      char c;
      int x=0, y=0, i, i1, i1e, i2, i2e;
      String rulestring = null;

      descriptionlen = descriptionwid=0;
      description = new Vector();

      while ((s=lifefile.readLine())!=null)
      {
         if (s.startsWith("#D"))
         {
            StringTokenizer st = new StringTokenizer(s);

            st.nextToken();

            for (i=2; i<s.length() && s.charAt(i)==' '; i++);

            addDescriptionLine(s.substring(i));
         }
         else if (s.startsWith("#P"))
         {
            StringTokenizer st = new StringTokenizer(s);

            st.nextToken();  // #P
            x = Integer.parseInt(st.nextToken()) + origX;
            y = Integer.parseInt(st.nextToken()) + origY;

         }
         else if (s.startsWith(".") || s.startsWith("*")
               || s.startsWith("o") || s.startsWith("O"))
         {
            for (i=0; i<s.length(); i++)
            {
               c=s.charAt(i);
               if (c=='*' || c=='o' || c=='O') u.changeCell(x+i,y,true);
            }
            y++;
         }
         else if (s.startsWith("#N"))
         {
            rulestring = new String("23/3");

         }
         else if (s.startsWith("#R"))
         {
            StringTokenizer st = new StringTokenizer(s);

            st.nextToken();  // #R
            rulestring = st.nextToken();
         }
         else if (s.startsWith("#S"))
         {
            String spd;
            StringTokenizer st = new StringTokenizer(s);
            st.nextToken();
            spd = st.nextToken();
            if (spd!=null)
            {
               skipping=Integer.parseInt(spd);
               if (u!=null) u.setSpeed(skipping);
            }
         }
      }

      if (rulestring!=null && rulestring.length()>0
            && !rulestring.equals(set_rules))
      {
         set_rules=rulestring;
         u.setRules(rulestring);
      }

      goBtn.enable();
      return true;
   }

   private boolean readRLE(String firstLine, DataInputStream lifefile)
      throws IOException
   {
      String s;
      char c;
      int n, leftX=0, x=0, y=0;
      String rulestring = "23/3";  // default RLE is Conway's rules
      boolean done = false;

      descriptionlen = descriptionwid=0;
      description = new Vector();

      StringTokenizer stcomma = new StringTokenizer(firstLine, ",");
      while (stcomma.hasMoreTokens())
      {
         String t = stcomma.nextToken();
         StringTokenizer stequal = new StringTokenizer(t, "= ");
         String tokenType = stequal.nextToken();
         String tokenValue = stequal.nextToken();

         if (tokenType.equals("x"))
            leftX = x = origX-(Integer.parseInt(tokenValue)/2);
         else if (tokenType.equals("y"))
            y = origY-(Integer.parseInt(tokenValue)/2);
         else if (tokenType.equals("rule") || tokenType.equals("rules"))
            rulestring = tokenValue;
         else if (tokenType.equals("skip"))
            u.setSpeed((skipping = Integer.parseInt(tokenValue)));
         else if (tokenType.equals("fps"))
            u.setRefresh(1000/(refresh_rate=Integer.parseInt(tokenValue)));
      }

      while (!done)
      {
         s = lifefile.readLine();
         if (s==null) break;  // this is actually an error

         for (int i=0; i<s.length(); i++)
         {
            c = s.charAt(i);
            n=0;
            while (c>='0' && c<='9' && i<s.length())
            {
               n = n*10 +((int)(c-'0'));
               i++;
               c = s.charAt(i);
            }
            if (n==0) n=1;

            if (c=='b') x+=n;
            else if (c=='o') {
               for (int j=0; j<n; j++) u.changeCell(x+j, y, true);
               x+=n;
            }
            else if (c=='$') {
               x=leftX;
               y+=n;
            }
            else if (c=='!') done=true;
         }
      }

      if (rulestring!=null && rulestring.length()>0
            && !rulestring.equals(set_rules))
      {
         set_rules=rulestring;
         u.setRules(rulestring);
      }

      if (done) // '!' reached; anything after that is a comment
      {
         while ((s=lifefile.readLine())!=null)
            addDescriptionLine(s);
      }

      goBtn.enable();
      return true;
   }

   private void addDescriptionLine(String newDescLine)
   {
      description.addElement(newDescLine);
      if (newDescLine.length()>descriptionwid)
         descriptionwid = newDescLine.length();
      descriptionlen++;
   }

   public boolean load(String fn)
   {
      URL urly;
      DataInputStream lifefile;

      try {urly = new URL(fn);}
      catch (MalformedURLException mue)
      {
         //            setMsg("Malformed URL: " + fn);
         return false;
      }
      catch (SecurityException se) {
         return false;
      }

      try
      {
         lifefile = new DataInputStream(urly.openStream());

         String firstLine = lifefile.readLine();

         if (firstLine.startsWith("#Life 1.05"))
         {
            readLife105(lifefile);
            paintLife(REPAINT_FULL);
            return true;
         }
         else if (firstLine.startsWith("x"))
         {
            // now, it is just a guess that this is an RLE file.
            readRLE(firstLine, lifefile);
            paintLife(REPAINT_FULL);
            return true;
         }
         else return false;
      }
      catch (IOException ioe) {
         //            setMsg("Couldn't open " + fn);
      }
      catch (SecurityException se) {
         return false;
      }

      return true;
   }

   public void callback(int i, Object o)
   {
      switch (i)
      {
         case 0:
            if (clearonload) clear();

            boolean loaded = load((String)o);

            if (loaded && describe && descriptionlen>0)
            {
               if (descBx!=null) descBx.dispose();
               descBx = new DescribeBox(descriptionlen+1,
                     descriptionwid+1);
               for (int j=0; j<descriptionlen; j++)
                  descBx.addLine(new String(
                           (String)(description.elementAt(j))));
               descBx.addLine(new String(" "));

               descBx.pack();
               descBx.setTitle("Description of "+(String)o);
               descBx.show();
            }
            break;
         case 1:
            setRules((String)o);
            break;
         case 2:
            try {
               refresh_rate=Integer.parseInt((String)o);
               if (u!=null) u.setRefresh(1000/refresh_rate);
               default_refresh_rate=refresh_rate;
            }catch (NumberFormatException e){}
            break;
         case 3:
            try {
               skipping=Integer.parseInt((String)o);
               if (u!=null) u.setSpeed(skipping);
               default_skipping=skipping;
            }catch (NumberFormatException e){}
            break;
         case 4:
            try {
               if (Integer.parseInt((String)o)!=0) {
                  goHowFar.addItem((String)o);
                  goHowFar.select((String)o);
               }
            }catch (NumberFormatException e){}
            break;
         case 5:
            clearonload = ((Boolean)o).booleanValue();
            break;
      }
   }
}