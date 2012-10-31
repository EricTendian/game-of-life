package life.v41d;

/**************************************************************
 RuleBox.java
**************************************************************/

import java.awt.*;

class RuleBox extends Frame
{
   private Panel inputbox;
   private Panel conway;
   private Panel coolroolz;
   private Panel buttons;
   private Button normBtn, okBtn, cxBtn;
   private LifeCallback cb;
   private Choice choice;
   private TextField text;

   RuleBox(LifeCallback caller)
   {
      cb = caller;

      inputbox = new Panel();
      inputbox.add(new Label("Rules: "));
      inputbox.add(text = new TextField(20));

      conway = new Panel();
      normBtn = new Button("Conway's Rules");
      conway.add(normBtn);

      buttons = new Panel();
      okBtn = new Button("OK");
      buttons.add(okBtn);
      cxBtn = new Button("Cancel");
      buttons.add(cxBtn);

      coolroolz = new Panel();
      coolroolz.add(new Label("Cool Rules: "));
      coolroolz.add(choice = new Choice());
      choice.addItem("23/3 (Life)");
      choice.addItem("23/36 (HighLife)");
      choice.addItem("/234 (lacy)");
      choice.addItem("01245678/34 (artsy)");
      choice.addItem("12345/3 (amazing)");
      choice.addItem("1234/37 (rats!)");
      choice.addItem("125678/367 (coagulation)");
      choice.addItem("45678/3 (coral)");
      choice.addItem("235678/3678 (ice crystals)");
      choice.addItem("5678/35678 (Dean's diamonds)");

      setLayout(new GridLayout(4,1));
      add(inputbox,0);
      add(coolroolz,1);
      add(conway,2);
      add(buttons,3);
   }

   public void enterRules(String rules)
   {
      text.setText(rules);
   }

   public boolean handleEvent(Event e)
   {
      if ((e.target == cxBtn && e.arg!=null)
            || e.id==Event.WINDOW_DESTROY)
      {
         dispose();
      }
      else if (e.target == okBtn
            || (e.target == text && e.id==Event.ACTION_EVENT))
      {
         if (e.arg != null)
         {
            cb.callback(1,text.getText());
            dispose();
         }
      }
      else if (e.target == normBtn)
      {
         if (e.arg != null)
         {
            text.setText("23/3");
            cb.callback(1,"23/3");
            dispose();
         }
      }
      else if (e.target == choice)
      {
         text.setText(choice.getSelectedItem());
      }

      return super.handleEvent(e);
   }
}
