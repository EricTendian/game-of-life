package life.v41d;

/**************************************************************
 LoadBox.java
**************************************************************/

import java.awt.*;
import java.io.*;
import java.net.*;

class LoadBox extends Frame
{
   private List list;
   private Panel buttons;
   private String cwd;
   private Button okBtn, cxBtn;
   private String choice=null;
   private LifeCallback cb;
   private boolean alreadyListed=false;

   LoadBox(LifeCallback caller)
   {
      cb = caller;
      list = new List(20,false);

      buttons = new Panel();
      okBtn = new Button("OK");
      buttons.add(okBtn);
      cxBtn = new Button("Cancel");
      buttons.add(cxBtn);

      setLayout(new BorderLayout());
      add("Center", list);
      add("South", buttons); 
   }

   public void listURL(String loc)
   {
      if (alreadyListed) return;

      pack();  // show a Load box so the user isn't
      show();  // waiting for a slow network

      try
      {
         String line;
         DataInputStream dis;

         cwd = new String(loc);
         if (!cwd.endsWith("/")) cwd += "/";

         URL url = new URL(cwd+"index.html");

         list.clear();
         list.addItem("Please wait...");
         dis = new DataInputStream(url.openStream());

         if ((line = dis.readLine())!=null) {
            list.delItem(0);
            list.addItem(line);
         }

         while ((line = dis.readLine())!=null) list.addItem(line);
      }
      catch(Exception e)
      {
         list.addItem(e.toString());
         return;
      }

      alreadyListed=true;

      // unfortunate kludge, to widen box to avoid horizontal scrollbar
      //Dimension d = LdBx.size();
      //if (d.width>0 && d.height>0)
      //    LdBx.resize(d.width+10, d.height);
   }

   public boolean handleEvent(Event e)
   {
      String sel;

      if ((e.target == cxBtn && e.arg!=null)
            || e.id==Event.WINDOW_DESTROY)
      {
         dispose();
      }
      else if ((e.target == okBtn || e.id==Event.ACTION_EVENT)
            && e.arg!=null)
      {
         sel=list.getSelectedItem();

         if (sel != null && sel.length()>0)
         {
            choice = cwd + list.getSelectedItem();
            if (!choice.toUpperCase().endsWith(".LIF"))
               choice+=".lif";
            cb.callback(0,choice);
         }

         dispose();
      }

      return super.handleEvent(e);
   }

   public synchronized void show()
   {
      int i = list.getSelectedIndex()-8;

      if (i<0) i=0;
      list.makeVisible(i);

      super.show();
   }
}
