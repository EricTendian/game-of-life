package life.v41d;

/********************************************************************
 * LifeButton
 *
 * This is the button that appears in the browser window to start
 * the Life applet.
 *
 * This object also gets all of the applet parameters and makes them
 * available to the rest of the Life program.
 ********************************************************************/

import java.awt.*;
import java.applet.*;

public class LifeButton extends Applet
{
   private static String VERSIONINFO = "Version 0.41d";
   private String tmp;

   // Life applet parameters
   static int zoom;
   static String load_param;
   static int refresh_rate;
   static int skipping;
   static String loaddir;
   static String rules;
   static String fgcolor;
   static String gridcolor;
   static int scrollbarwidth;
   static String toolbar;
   static boolean grids;
   static boolean editable;
   static boolean describe;
   static boolean autostart;
   static String howfarChoices;
   static boolean clearRectBroken=false;

   static java.net.URL codebase;
   static String url;

   private int appletWidth, appletHeight;

   public String getAppletInfo() {
      return "Conway's Game of Life "+VERSIONINFO+" by Alan Hensel";
   }

   public void getParameters()
   {
      tmp = getParameter("windowwidth");
      if (tmp!=null) appletWidth = Integer.parseInt(tmp);
      else appletWidth = 608;

      tmp = getParameter("windowheight");
      if (tmp!=null) appletHeight = Integer.parseInt(tmp);
      else appletHeight = 448;

      // get the applet params relevant to running Life

      tmp = getParameter("zoom");
      if (tmp != null) zoom = Integer.parseInt(tmp);
      else zoom = 2;

      tmp = getParameter("refreshrate");
      if (tmp != null) refresh_rate = Integer.parseInt(tmp);
      else refresh_rate=20;
      if (refresh_rate>100) refresh_rate=100; // 100 frames/sec max

      tmp = getParameter("skip");
      if (tmp != null)
      {
         if (tmp.equals("warp")) skipping = 0;
         else 
            skipping = Integer.parseInt(tmp);
      }
      else skipping=1;

      loaddir = getParameter("loaddir");
      if (loaddir == null) loaddir = "p";
      loaddir = url + loaddir;

      load_param = getParameter("open");
      if (load_param!=null && load_param.length()>0)
         load_param = url + load_param;

      rules = getParameter("rules");

      fgcolor = getParameter("fgcolor");
      gridcolor=getParameter("gridcolor");

      tmp = getParameter("scrollbarwidth");
      if (tmp != null) scrollbarwidth = Integer.parseInt(tmp);
      else scrollbarwidth=24;

      toolbar = getParameter("toolbar");
      if (toolbar==null)
         toolbar="Open Go HowFar Clear Speed Rules Options Zoom Count";

      tmp = getParameter("grids");
      if (tmp != null && tmp.equals("false")) grids = false;
      else grids = true;

      tmp = getParameter("editable");
      if (tmp != null && tmp.equals("false")) editable = false;
      else editable = true;

      tmp = getParameter("describe");
      if (tmp != null && tmp.equals("false")) describe = false;
      else describe = true;

      tmp = getParameter("autostart");
      if (tmp != null) if (tmp.equals("true")) autostart = true;
      else autostart = false;

      howfarChoices = getParameter("howfarchoices");
      if (howfarChoices==null)
         howfarChoices="forever +1 -1";
   }

   public void init()
   {
      codebase = getCodeBase();
      url = codebase.toString();

      // check java version. Shouldn't be necessary (write once, run
      // everywhere, right? hah!)
      String javaVendor = System.getProperty("java.vendor");
      String javaVersion = System.getProperty("java.version");
      // put that info in java console for future debugging purposes.
      System.out.println(getAppletInfo()+"\non "+javaVendor+
            "\nversion "+javaVersion);
      // and then determine the value of the clearRectBroken flag.
      if (javaVendor.startsWith("Netscape") &&
            (javaVersion.startsWith("1.0") ||
             (javaVersion.startsWith("1.1") &&
              javaVersion.charAt(javaVersion.length()-1) < '5')))
      {
         clearRectBroken = true;  // Netscape Java less than 1.1.5
      }

      // Dammit! Microsoft deals with this in a broken way.
      // Gives me a security exception unless I do the following.
      if (javaVendor.startsWith("Microsoft") &&
            url.startsWith("http://www.mindspring.net"))
      {
         url = "http://www.mindspring.com/~alanh/life/";
      }

      String buttonName = getParameter("buttonname");
      if (buttonName == null) buttonName = "Enjoy Life";
      add(new Button(buttonName));
   }

   public boolean handleEvent(Event e)
   {
      if (e.target instanceof Button)
      {
         if (e.id == Event.ACTION_EVENT)
         {
            getParameters();
            LifeFrame f1 = new LifeFrame(
                  "Game of Life Applet, " + VERSIONINFO);
            f1.startLife(appletWidth, appletHeight);
         }
      }

      return super.handleEvent(e);
   }
}
