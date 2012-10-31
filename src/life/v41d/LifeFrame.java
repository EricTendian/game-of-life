package life.v41d;

/********************************************************************
 * LifeFrame
 ********************************************************************/

import java.awt.*;

class LifeFrame extends Frame implements Runnable
{
   protected Life viva;
   protected int appletWidth, appletHeight;

   public LifeFrame(String title)
   {
      super(title);
   }

   public void startLife(int w, int h)
   {
      appletWidth = w;
      appletHeight= h;

      Thread th = new Thread(this);
      th.start();
   }

   public void run()
   {
      resize(appletWidth,appletHeight);
      show();
      setCursor(Cursor.WAIT_CURSOR);

      viva = new Life();

      add("Center", viva);
      show();
      setCursor(Cursor.DEFAULT_CURSOR);

      viva.init();
   }

   public boolean handleEvent(Event e)
   {
      if(e.id == Event.WINDOW_DESTROY)
      {
         viva.handleEvent(e);
         return true;
      }

      return super.handleEvent(e);
   }
}

