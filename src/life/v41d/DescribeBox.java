package life.v41d;

/**************************************************************
 DescribeBox.java
**************************************************************/

import java.awt.*;

class DescribeBox extends Frame
{
   private TextArea description;
   private Panel buttons;
   private Button okBtn;

   DescribeBox(int rows, int cols)
   {
      description = new TextArea(rows, cols);
      description.setEditable(false);

      buttons = new Panel();
      okBtn = new Button("OK");
      buttons.add(okBtn);

      setLayout(new BorderLayout());
      add("Center", description);
      add("South", buttons);
   }

   public void addLine(String line)
   {
      description.appendText(line+"\n");
   }

   public boolean handleEvent(Event e)
   {
      if ((e.target == okBtn && e.arg!=null)
        || e.id==Event.WINDOW_DESTROY)
      {
         dispose();
      }

      return super.handleEvent(e);
   }
}
