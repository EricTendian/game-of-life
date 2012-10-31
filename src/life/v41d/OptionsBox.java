package life.v41d;

/**************************************************************
 OptionsBox.java
**************************************************************/

import java.awt.*;

class OptionsBox extends Frame
{
   private Panel howfarPanel;
   private TextField howfarText;

   private Panel clearonloadPanel;
   private Checkbox clearonloadCheck;

   private Panel buttons;
   private Button okBtn, cxBtn;
   private LifeCallback cb;

   OptionsBox(LifeCallback caller)
   {
      cb = caller;

      howfarPanel = new Panel();
      howfarPanel.add(new Label("Add How-Far Choice:"));
      howfarPanel.add(howfarText = new TextField(5));

      clearonloadPanel = new Panel();
      clearonloadCheck = new Checkbox("Clear on Open");
      clearonloadPanel.add(clearonloadCheck);

      buttons = new Panel();
      okBtn = new Button("OK");
      buttons.add(okBtn);
      cxBtn = new Button("Cancel");
      buttons.add(cxBtn);

      setLayout(new GridLayout(3,1));
      add(howfarPanel,0);
      add(clearonloadPanel,1);
      add(buttons,2);
   }

   public void enterData(boolean clear)
   {
      howfarText.setText("");
      clearonloadCheck.setState(clear);
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
            cb.callback(4,howfarText.getText());
            cb.callback(5,new Boolean(clearonloadCheck.getState()));
            dispose();
         }
      }

      return super.handleEvent(e);
   }
}
