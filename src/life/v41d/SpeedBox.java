package life.v41d;

/**************************************************************
 SpeedBox.java
**************************************************************/

import java.awt.*;

class SpeedBox extends Frame 
{
   private static String WARP_TEXT = "warp";

   private Panel refreshPanel;
   private TextField refreshText;

   private Panel speedPanel;
   private TextField speedText;

   private Panel warpPanel;
   private Button warpBtn;
   private Button dontSkipBtn;

   private Panel buttons;
   private Button okBtn, cxBtn;
   private LifeCallback cb;

   SpeedBox(LifeCallback caller)
   {
      cb = caller;

      refreshPanel = new Panel();
      refreshPanel.add(new Label("Frames/sec (fps):"));
      refreshPanel.add(refreshText = new TextField(8));

      speedPanel = new Panel();
      speedPanel.add(new Label("Gens/Frame (skip):"));
      speedPanel.add(speedText = new TextField(8));

      warpPanel = new Panel();
      warpBtn = new Button("Warp Speed");
      warpPanel.add(warpBtn);
      dontSkipBtn = new Button("Don't Skip");
      warpPanel.add(dontSkipBtn);

      buttons = new Panel();
      okBtn = new Button("OK");
      buttons.add(okBtn);
      cxBtn = new Button("Cancel");
      buttons.add(cxBtn);

      setLayout(new GridLayout(4,1));
      add(refreshPanel,0);
      add(speedPanel,1);
      add(warpPanel,2);
      add(buttons,3);
   }

   public void enterData(int refresh, int speed)
   {
      refreshText.setText(Integer.toString(refresh));

      if (speed==0) speedText.setText(WARP_TEXT);
      else          speedText.setText(Integer.toString(speed));
   }

   public boolean handleEvent(Event e)
   {
      if ((e.target == cxBtn && e.arg!=null)
            || e.id==Event.WINDOW_DESTROY)
      {
         dispose();
      }
      else if (e.target == okBtn)
      {
         if (e.arg != null)
         {
            cb.callback(2,refreshText.getText());
            if (speedText.getText().equals(WARP_TEXT))
               cb.callback(3,"0");
            else
               cb.callback(3,speedText.getText());
            dispose();
         }
      }
      else if (e.target == warpBtn)
      {
         if (e.arg != null) speedText.setText(WARP_TEXT);
      }
      else if (e.target == dontSkipBtn)
      {
         if (e.arg != null) speedText.setText("1");
      }

      return super.handleEvent(e);
   }
}
