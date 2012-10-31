package life.v41d;

/**************************************************************
 LifeRules.java
 
 This class exists to convert a rule string (i.e. "23/3") into
 an array of 512 booleans representing the 3x3 neighborhood.
 The bits, in order of 123456789, are laid out in order:
 
 123
 456
 789
 
 so that, for example, the hex number 0x1ca would represent
 
 ***
 ..*
 .*.
 
 (c) Alan Hensel, Aug 2000. All Rights Reserved.
**************************************************************/

class LifeRules
{
   private boolean[] ruleArray = new boolean[512];
   private String ruleString = null;

   public LifeRules()
   {
   }

   /** After convertRules has been called, a slightly cleaner
    * version of the rule string can be obtained with this method.
    */
   public String getRules()
   {
      return ruleString;
   }

   /** The main event of this class: get a rule array laid out
    * according to the human-readable string.
    */
   public boolean[] convertRules(String rule)
   {
      for (int i=0; i<512; i++) ruleArray[i] = false;

      rule = rule.toLowerCase();
      if (rule.indexOf('(') >= 0)
         rule = rule.substring(0, rule.indexOf('('));
      int slashIndex = rule.indexOf('/');
      if (slashIndex < 0) return null;  // no slash!
      String leftRules = rule.substring(0, slashIndex);
      String rightRules = rule.substring(slashIndex+1);

      // determine which set of digits is actually survival, birth
      boolean bs = (leftRules.indexOf('b') >= 0
                || rightRules.indexOf('s') >= 0);

      setRules(leftRules, !bs);
      setRules(rightRules, bs);

      ruleString = rule;

      return ruleArray;
   }

   /** Set half of the rules.
    * survival = true for survival, false for birth.
    */
   private void setRules(String rules, boolean survival)
   {
      rules += " ";  // for safety (1-character peekaheads occurring)

      for (int i=0; i<rules.length(); i++)
      {
         int num = rules.charAt(i) - 0x30;
         if ((num > 0 && num <= 9) || (num == 0 && survival))
         {
            char peekAhead = rules.charAt(i+1);

            if (peekAhead < 'a')
               setTotalisticNeighborhood(num, survival);

            boolean b = true;
            if (peekAhead == '-')
            {
               b = false; // invert meaning of chars that follow
               i++;
               peekAhead = rules.charAt(i+1);
            }

            while (peekAhead >= 'a' && peekAhead <= 'z')
            {
               setSymmetricalNeighborhood(num, survival, peekAhead, b);
               i++;
               peekAhead = rules.charAt(i+1);
            }
         }
      }
   }


   private int flipBits(int x)
   {
      return ((x & 0x07)<<6) | ((x & 0x1c0)>>>6) | (x & 0x38);
   }

   private int rotateBits90Clockwise(int x)
   {
      return
         ((x & 0x04) << 6)
         | ((x & 0x20) << 2)
         | ((x &0x100) >>> 2)
         | ((x & 0x02) << 4)
         |  (x & 0x10)
         | ((x & 0x80) >>> 4)
         | ((x & 0x01) << 2)
         | ((x & 0x08) >>> 2)
         | ((x & 0x40) >>> 6);
   }

   // non-totalistic rule extensions
   private String[] rule_letters =
   {
      "ce",
      "ceaikv",
      "ceaikvjqry",
      "ceaikvjqrytwz"
   };

   private int[][] rule_neighborhoods =
   {
      { 001,  002},  // octal numbers
      { 005,  012, 003, 050,  041, 0104},
      {0105,  052, 013, 007, 0142,  015,  016, 0106,  051, 0141},
      {0505, 0252, 017, 055, 0143, 0107, 0152, 0146,  053, 0145, 0151, 0116, 0154}
   };

   /** set Rule array by neighborhood type (of 51 possibilities) specified
    * by number of neighbors (num) and a letter of the alphabet (c).
    * survival = true if survival rule, false if birth.
    * b = true if adding, false if suppressing survival or birth.
    */
   private void setSymmetricalNeighborhood(int num, boolean survival,
         char c, boolean b)
   {
      if (num == 0 || num == 8) {   // duh, no combos for homogeneous bits
         setTotalisticNeighborhood(num, survival);
         return;
      }

      int xorbit = 0;
      int nIndex = num-1;
      if (nIndex > 3) {
         nIndex = 6-nIndex;
         xorbit = 0x1ef;
      }

      int letterIndex = rule_letters[nIndex].indexOf(c);
      if (letterIndex == -1) return;

      int x = rule_neighborhoods[nIndex][letterIndex] ^ xorbit;
      if (survival) x |= 0x10;

      setSymmetricalNeighborhood(x, b);
   }

   /** set Rule array by 9-bit neighborhood x.
    * b = true if adding, false if suppressing survival or birth.
    */
   private void setSymmetricalNeighborhood(int x, boolean b)
   {
      int y = x;

      for (int i=0; i<4; i++) {
         ruleArray[y] = b;
         y = rotateBits90Clockwise(y);
      }
      y = flipBits(y);
      for (int i=0; i<4; i++) {
         ruleArray[y] = b;
         y = rotateBits90Clockwise(y);
      }
   }

   /** set Rule array by number of neighbors.
    * survival = true for survival rule, false for birth rule.
    */
   private void setTotalisticNeighborhood(int num, boolean survival)
   {
      int mask = survival?0x10:0;

      for (int i=0; i<512; i+=32)
      {
         for (int j=0; j<16; j++)
         {
            int neighbors = 0;
            int nborhood = i+j;
            while (nborhood > 0) {  
               neighbors += (nborhood&1);
               nborhood>>>=1;
            }
            if (num == neighbors)
               ruleArray[i+j+mask] = true;
         }
      }
   }
}
