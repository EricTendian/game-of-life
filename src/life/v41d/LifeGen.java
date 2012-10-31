package life.v41d;

/**************************************************************
 LifeGen.java

 Life Generate, implementing a Java-enabled Life Staggerstep
 Algorithm Class, an extremely efficient implementation of
 Conway's classic Game of Life.

 The universe is divided up into 16x16 blocks, allocated as
 LifeCell structures (class instances).  Naturally, most of this
 "virtual universe" is empty, and is thus left unallocated.  This
 is a great way to gain both speed and universe size.  By pointer
 manipulation we link blocks that contain life, and can so carry
 on to the next generation.

 Already, note a slight difficulty:  It isn't enough that blocks
 know their neighbors, because a block could come to life next
 to a neighbor it didn't know existed.  The solution here is that
 upon allocation, each block must register itself in a hash table,
 and then set its neighbors according to the hash table entries,
 and then notify its neighbors to set their neighbor pointers to it.

 All of that hassle is enough to warrant blocks as large as 16x16.

 These 16x16 blocks are subdivided into 8x8 functional units ("cages").
 Each cage can go through states:  Living, morgue, and hibernating.
 In the morgue and hibernating stages, LifeGen skips over them; they
 require no processing.  Morgue means there is no life inside; it is
 completely blank.  Hibernating means that the life inside is stable,
 and so requires no processing.

 If any of the 4 cages in a block is in the living state, the block
 is kept in the Living list (LifeGen processes it).  If all 4 cages
 are in the morgue state, the LifeGen algorithm puts it in the Morgue
 list, and it becomes a candidate for deallocation.  If at least one
 block is in hibernation and none are living, the block is put in
 the Hibernating list.

 The state transitions are as follows:

 (allocation) ----> living
                     ^  ^
                     |  |
    hibernating <----+  +----> morgue ----> (deallocation)

 Allocation and deallocation apply to full 16x16 blocks only.  The
 8x8 cages set a flag.

 Within the 8x8 cages are 4x4 lookup units.  These 16-bit entities
 are represented by a "short".  A lookup table is employed to
 calculate the inner 2x2 group of the next generation of the 4x4 unit.
 Thus, the internal algorithm zips ahead 4 cells at a time, in an
 unrolled loop which solves a whole cage in one fell swoop.

 One caveat.  The inner 2x2 is offset by 1 pixel in each direction.
 This implies some bit-manipulation....  But all of the complexity
 has been shifted to the display process (in LifeField), which, it
 has been assumed, will be called only a certain number of times per
 second (not to  exceed the capabilities of the human eye by too great
 a margin).

 This complexity shift has been accomplished by introducing a
 "staggerstep":  On even numbered generations (marked "p"), the block
 starts on a cell coordinate of (16x, 16y), while on odd numbered
 generations (marked "q"), the block starts on a cell coordinate of
 (16x+1, 16y+1).

 (c) Alan Hensel, Nov 1995 - Apr 1996. All Rights Reserved.
**************************************************************/

class LifeGen
{
   private LifeCell living;      // beginning of list of live blocks
   private LifeCell hibernating; // p1 or p2 blocks
   private LifeCell morgue;      // empty blocks not ready to deallocate
   private LifeCell caretaker;
   public  LifeCell display;   // list of blocks in the viewing area

   private LifeHash hashTable;

   public boolean qCycle;  // false = p side now, true  = q side now.
   public boolean backCorrect;

   public int gencount = 0;   // current generation count
   private int countdown_gen; // number of generations before scheduled stop
   public boolean goFlag = false;

   private LifeRules lifeRules = new LifeRules();

   private short[] crunch = new short[65536];  // p --> q rule table
   private short[] munch  = new short[65536];  // p <-- q rule table

   //  The Crunch and Munch tables crunch&munch a 4x4 block to find
   //  the 2x2 inner result.  Four results are stored in each
   //  entry.
   //
   //  This requires some bit-masking to uncover the results, but
   //  the alternative is 4 separate tables.

   // display parameters:
   public  long interval=100; // milliseconds per frame (1000/(frames per second))
   public  long speed;   // if >0, max number of generations per blap
   private long last_blap_interval; // length of time for displaying 1 generation
   private long last_timestamp; // timestamp at the beginning of a string of unblapped generations
   private long last_gencount;  // generation when above timestamp was taken
   private long predicted_blap_interval;
   private long last_gen_interval = 0;
   private long timenow;

   private int statecheck, statediff;

   /*-------------------------------------------------*/

   LifeGen(String rules)  // constructor
   {
      gencount=0;

      qCycle = backCorrect=false;

      living=hibernating=morgue=display=caretaker=null;

      hashTable = new LifeHash();

      if (rules==null) setRules("23/3");   // Conway's rule is default
      else setRules(rules);
   }

   LifeGen()  // constructor
   {
      this(null);   // default rules
   }

   /**
    * setRules(String)
    *
    * set Rule[] table by string
    */
   public void setRules(String rulestring)
   {
      // Easter Egg for Just Friends rule
      if (rulestring.toLowerCase().equals("justfriends")) {
         rulestring = "12/2-a";
      }

      this.setRules(lifeRules.convertRules(rulestring));
   }

   public String getRules()
   {
      return lifeRules.getRules();
   }

   private void generate_p()
   {
      LifeCell cnext;
      LifeCell cS, cSE, cE;
      int cSpstate, cSEpstate, cEpstate;
      int x,y,xp, yp;
      int xor1,xor2,xor3;
      int ix00, ix02, ix10, ix12,   // 4x4 neighborhoods
      ix01, ix03, ix11, ix13,   // extracted from a
      ix20, ix22, ix30, ix32,   // full 8x8 block,
      ix21, ix23, ix31, ix33;   // for table lookups.
      short n0, n1, n2, n3;         // table lookup results

      //System.out.println("\n*** P -> Q\n");

      // For each cage:
      for (LifeCell c=living; c!=null; c=cnext)
      {
         cnext=c.Next;

         cS=c.S;
         cE=c.E;
         cSE=c.SE;

         if (cS != null) cSpstate=cS.pstate;
         else cSpstate= -1;

         if (cE != null) cEpstate=cE.pstate;
         else cEpstate= -1;

         if (cSE!= null) cSEpstate=cSE.pstate;
         else cSEpstate= -1;

         if ((c.pstate & 0x08080808)==0x08080808 && (cSpstate & 0x02000200)==0x02000200
               &&(cEpstate & 0x04040000)==0x04040000 && (cSEpstate& 0x01000000)==0x01000000)
         {
            if ((c.pstate & 0x80808080)==0x80808080 && (cSpstate & 0x20002000)==0x20002000
                  &&(cEpstate & 0x40400000)==0x40400000 && (cSEpstate& 0x10000000)==0x10000000)
            {
               c.flags |= 0xc000;
               if ((c.flags & 0xf800)==0xf000)
               {
                  killCage(c);
               }
               c.qstate = 0xffffffff;
            }
            else
            {
               c.flags |= 0x4000;
               if ((c.flags & 0x5800)==0x5000)
               {
                  tranquilizeCage(c);
               }
               c.qstate |= 0x0f0f0f0f;
            }
            c.flags &= 0xf7ff;  // Reset the Rattling bit
         }
         else
         {
            c.flags &= 0x07ff;
            x = c.x<<4; y = c.y<<4;
            xp = (x+16);  //pin
            yp = (y+16);  //pin

            if ((c.pstate & 0x08020401) != 0x08020401)  // first 8x8 active
            {
               ix00= (int)(c.p[0]) & 0xffff;   // darn signed arithmetic!
               ix10= (int)(c.p[1]) & 0xffff;
               ix20= (int)(c.p[2]) & 0xffff;
               ix30= (int)(c.p[3]) & 0xffff;

               ix02= (ix00 & 0x00ff) | (ix10 & 0xff00);
               ix12= (ix10 & 0x00ff) | (c.p[8] & 0xff00);
               ix22= (ix20 & 0x00ff) | (ix30 & 0xff00);
               ix32= (ix30 & 0x00ff) | (c.p[10] & 0xff00);

               ix01= (ix00 & 0x0f0f) | (ix20 & 0xf0f0);
               ix11= (ix10 & 0x0f0f) | (ix30 & 0xf0f0);
               ix21= (ix20 & 0x0f0f) | (c.p[4] & 0xf0f0);
               ix31= (ix30 & 0x0f0f) | (c.p[5] & 0xf0f0);

               ix03= (ix01 & 0x00ff) | (ix11 & 0xff00);
               ix13= (ix12 & 0x0f0f) | (ix32 & 0xf0f0);
               ix23= (ix21 & 0x00ff) | (ix31 & 0xff00);
               ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0) | (c.p[12] & 0xf000);

               n0 = (short)(
                     (0xf000 & crunch[ix00])
                     | (0x0f00 & crunch[ix01])
                     | (0x00f0 & crunch[ix02])
                     | (0x000f & crunch[ix03]));

               n1 = (short)(
                     (0xf000 & crunch[ix10])
                     | (0x0f00 & crunch[ix11])
                     | (0x00f0 & crunch[ix12])
                     | (0x000f & crunch[ix13]));

               n2 = (short)(
                     (0xf000 & crunch[ix20])
                     | (0x0f00 & crunch[ix21])
                     | (0x00f0 & crunch[ix22])
                     | (0x000f & crunch[ix23]));

               n3 = (short)(
                     (0xf000 & crunch[ix30])
                     | (0x0f00 & crunch[ix31])
                     | (0x00f0 & crunch[ix32])
                     | (0x000f & crunch[ix33]));

               // qstate bitmap
               // My 8x8 | Ver.2x8 | Hor.8x2 | Corner

               xor1 = c.q[1] ^ n1;
               xor2 = c.q[2] ^ n2;
               xor3 = c.q[3] ^ n3;

               if ((xor3 & 0x000f) == 0)
               {
                  if ((n3 & 0x000f) == 0)         // SE 2x2 corner
                     c.qstate |= 0x11000000;    // morgue & hiber
                  else
                     c.qstate |= 0x01000000;    // just hibernation

                  if (((xor2 | xor3) & 0x0f0f) == 0)   // S 8x2 Horizontal border
                  {
                     if (((n2 | n3) & 0x0f0f) == 0)
                        c.qstate |= 0x22000000;
                     else
                        c.qstate |= 0x02000000;
                  }
                  else
                  {
                     c.qstate &= 0x55ffffff;
                  }

                  if (((xor1 | xor3) & 0x00ff) == 0)   // E 2x8 Vertical border
                  {
                     if (((n1 | n3) & 0x00ff) == 0)
                        c.qstate |= 0x44000000;
                     else
                        c.qstate |= 0x04000000;

                     if ((xor1 | xor2 | xor3 | (c.q[0] ^ n0)) == 0)   // whole 8x8 block
                     {
                        if (n0==0 && n1==0 && n2==0 && n3==0)
                           c.qstate |= 0x88000000;
                        else
                           c.qstate |= 0x08000000;
                     }
                     else
                     {
                        c.qstate &= 0x77ffffff;
                     }
                  }
                  else
                  {
                     c.qstate &= 0x33ffffff;
                  }
               }
               else
               {
                  c.qstate &= 0x00ffffff;
               }

               c.q[0]=n0; c.q[2]=n2;
               c.q[1]=n1; c.q[3]=n3;
            }
            else
            {
               c.qstate |= 0x0f000000;

               if ((c.q[3] & 0x000f) == 0)         // SE 2x2 corner
                  c.qstate |= 0x11000000;    // morgue & hiber

               if (((c.q[2] | c.q[3]) & 0x0f0f) == 0)
                  c.qstate |= 0x22000000;

               if (((c.q[1] | c.q[3]) & 0x00ff) == 0)
                  c.qstate |= 0x44000000;

               if (c.q[0]==0 && c.q[1]==0 && c.q[2]==0 && c.q[3]==0)
                  c.qstate |= 0x88000000;
            }

            if ((c.pstate & 0x00080004) != 0x00080004
                  ||(cSpstate & 0x02000100) != 0x02000100) // second 8x8 (lower left):
                  {
                     ix00= (int)(c.p[4]) & 0xffff;
                     ix10= (int)(c.p[5]) & 0xffff;
                     ix20= (int)(c.p[6]) & 0xffff;
                     ix30= (int)(c.p[7]) & 0xffff;

                     ix02= (ix00 & 0x00ff) | (ix10 & 0xff00);
                     ix12= (ix10 & 0x00ff) | (c.p[12] & 0xff00);
                     ix22= (ix20 & 0x00ff) | (ix30 & 0xff00);
                     ix32= (ix30 & 0x00ff) | (c.p[14] & 0xff00);

                     ix01= (ix00 & 0x0f0f) | (ix20 & 0xf0f0);
                     ix11= (ix10 & 0x0f0f) | (ix30 & 0xf0f0);

                     if (cS != null) // there's a Southern neighbor (hi y'all!)
                     {
                        ix21= (ix20 & 0x0f0f) | (cS.p[0] & 0xf0f0);
                        ix31= (ix30 & 0x0f0f) | (cS.p[1] & 0xf0f0);
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0) | (cS.p[8] & 0xf000);
                     }
                     else
                     {
                        ix21= (ix20 & 0x0f0f);
                        ix31= (ix30 & 0x0f0f);
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0);
                     }

                     ix03= (ix01 & 0x00ff) | (ix11 & 0xff00);
                     ix13= (ix12 & 0x0f0f) | (ix32 & 0xf0f0);
                     ix23= (ix21 & 0x00ff) | (ix31 & 0xff00);

                     n0 = (short)(
                           (0xf000 & crunch[ix00])
                           | (0x0f00 & crunch[ix01])
                           | (0x00f0 & crunch[ix02])
                           | (0x000f & crunch[ix03]));

                     n1 = (short)(
                           (0xf000 & crunch[ix10])
                           | (0x0f00 & crunch[ix11])
                           | (0x00f0 & crunch[ix12])
                           | (0x000f & crunch[ix13]));

                     n2 = (short)(
                           (0xf000 & crunch[ix20])
                           | (0x0f00 & crunch[ix21])
                           | (0x00f0 & crunch[ix22])
                           | (0x000f & crunch[ix23]));

                     n3 = (short)(
                           (0xf000 & crunch[ix30])
                           | (0x0f00 & crunch[ix31])
                           | (0x00f0 & crunch[ix32])
                           | (0x000f & crunch[ix33]));


                     xor1 = c.q[5] ^ n1;
                     xor2 = c.q[6] ^ n2;
                     xor3 = c.q[7] ^ n3;

                     if ((xor3 & 0x000f) == 0)
                     {
                        if ((n3 & 0x000f) == 0)         // SE 2x2 corner
                           c.qstate |= 0x00110000;    // morgue & hiber
                        else
                           c.qstate |= 0x00010000;    // just hibernation

                        if (((xor2 | xor3) & 0x0f0f) == 0)   // S 8x2 Horizontal border
                        {
                           if (((n2 | n3) & 0x0f0f) == 0)
                              c.qstate |= 0x00220000;
                           else
                              c.qstate |= 0x00020000;
                        }
                        else
                        {
                           c.qstate &= 0xff55ffff;
                           if (cS != null) {
                              rattleCage(cS);
                           }
                           else {
                              allocateCage(x,yp);
                              cS=c.S;
                           }
                        }

                        if (((xor1 | xor3) & 0x00ff) == 0)   // E 2x8 Vertical border
                        {
                           if (((n1 | n3) & 0x00ff) == 0)
                              c.qstate |= 0x00440000;
                           else
                              c.qstate |= 0x00040000;

                           if ((xor1 | xor2 | xor3 | (c.q[4] ^ n0)) == 0)   // whole 8x8 block
                           {
                              if (n0==0 && n1==0 && n2==0 && n3==0)
                                 c.qstate |= 0x00880000;
                              else
                                 c.qstate |= 0x00080000;
                           }
                           else
                           {
                              c.qstate &= 0xff77ffff;
                           }
                        }
                        else
                        {
                           c.qstate &= 0xff33ffff;
                        }
                     }
                     else
                     {
                        c.qstate &= 0xff00ffff;

                        if (cS != null) {
                           rattleCage(cS);
                        }
                        else {
                           allocateCage(x,yp);
                           cS=c.S;
                        }
                     }

                     c.q[4]=n0; c.q[6]=n2;
                     c.q[5]=n1; c.q[7]=n3;
                  }
            else
            {
               c.qstate |= 0x000f0000;

               if ((c.q[7] & 0x000f) == 0)
                  c.qstate |= 0x00110000;

               if (((c.q[6] | c.q[7]) & 0x0f0f) == 0)
                  c.qstate |= 0x00220000;

               if (((c.q[5] | c.q[7]) & 0x00ff) == 0)
                  c.qstate |= 0x00440000;

               if (c.q[4]==0 && c.q[5]==0 && c.q[6]==0 && c.q[7]==0)
                  c.qstate |= 0x00880000;
            }

            if ((c.pstate & 0x00000802) != 0x00000802
                  ||(cEpstate & 0x04010000) != 0x04010000) // third 8x8 (upper right):
                  {
                     ix00= (int)(c.p[8]) & 0xffff;
                     ix10= (int)(c.p[9]) & 0xffff;
                     ix20= (int)(c.p[10])& 0xffff;
                     ix30= (int)(c.p[11])& 0xffff;

                     ix02= (ix00 & 0x00ff) | (ix10 & 0xff00);
                     ix22= (ix20 & 0x00ff) | (ix30 & 0xff00);

                     ix01= (ix00 & 0x0f0f) | (ix20 & 0xf0f0);
                     ix11= (ix10 & 0x0f0f) | (ix30 & 0xf0f0);
                     ix21= (ix20 & 0x0f0f) | (c.p[12] & 0xf0f0);
                     ix31= (ix30 & 0x0f0f) | (c.p[13] & 0xf0f0);

                     if (cE != null)
                     {
                        ix12= (ix10 & 0x00ff) | (cE.p[0] & 0xff00);
                        ix32= (ix30 & 0x00ff) | (cE.p[2] & 0xff00);
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0) | (cE.p[4] & 0xf000);
                     }
                     else
                     {
                        ix12= (ix10 & 0x00ff);
                        ix32= (ix30 & 0x00ff);
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0);
                     }

                     ix03= (ix01 & 0x00ff) | (ix11 & 0xff00);
                     ix13= (ix12 & 0x0f0f) | (ix32 & 0xf0f0);
                     ix23= (ix21 & 0x00ff) | (ix31 & 0xff00);

                     n0 = (short)(
                           (0xf000 & crunch[ix00])
                           | (0x0f00 & crunch[ix01])
                           | (0x00f0 & crunch[ix02])
                           | (0x000f & crunch[ix03]));

                     n1 = (short)(
                           (0xf000 & crunch[ix10])
                           | (0x0f00 & crunch[ix11])
                           | (0x00f0 & crunch[ix12])
                           | (0x000f & crunch[ix13]));

                     n2 = (short)(
                           (0xf000 & crunch[ix20])
                           | (0x0f00 & crunch[ix21])
                           | (0x00f0 & crunch[ix22])
                           | (0x000f & crunch[ix23]));

                     n3 = (short)(
                           (0xf000 & crunch[ix30])
                           | (0x0f00 & crunch[ix31])
                           | (0x00f0 & crunch[ix32])
                           | (0x000f & crunch[ix33]));


                     xor1 = c.q[9] ^ n1;
                     xor2 = c.q[10] ^ n2;
                     xor3 = c.q[11] ^ n3;

                     if ((xor3 & 0x000f) == 0)
                     {
                        if ((n3 & 0x000f) == 0)         // SE 2x2 corner
                           c.qstate |= 0x00001100;    // morgue & hiber
                        else
                           c.qstate |= 0x00000100;    // just hibernation

                        if (((xor2 | xor3) & 0x0f0f) == 0)   // S 8x2 Horizontal border
                        {
                           if (((n2 | n3) & 0x0f0f) == 0)
                              c.qstate |= 0x00002200;
                           else
                              c.qstate |= 0x00000200;
                        }
                        else
                        {
                           c.qstate &= 0xffff55ff;
                        }

                        if (((xor1 | xor3) & 0x00ff) == 0)   // E 2x8 Vertical border
                        {
                           if (((n1 | n3) & 0x00ff) == 0)
                              c.qstate |= 0x00004400;
                           else
                              c.qstate |= 0x00000400;

                           if ((xor1 | xor2 | xor3 | (c.q[8] ^ n0)) == 0)   // whole 8x8 block
                           {
                              if (n0==0 && n1==0 && n2==0 && n3==0)
                                 c.qstate |= 0x00008800;
                              else
                                 c.qstate |= 0x00000800;

                           }
                           else
                           {
                              c.qstate &= 0xffff77ff;
                           }
                        }
                        else
                        {
                           c.qstate &= 0xffff33ff;

                           if (cE != null) {
                              rattleCage(cE);
                           }
                           else {
                              allocateCage(xp,y);
                              cE=c.E;
                           }
                        }
                     }
                     else
                     {
                        c.qstate &= 0xffff00ff;

                        if (cE != null) {
                           rattleCage(cE);
                        }
                        else {
                           allocateCage(xp,y);
                           cE=c.E;
                        }
                     }

                     c.q[8]=n0;  c.q[10]=n2;
                     c.q[9]=n1;  c.q[11]=n3;
                  }
            else
            {
               c.qstate |= 0x00000f00;

               if ((c.q[11] & 0x000f) == 0)
                  c.qstate |= 0x00001100;

               if (((c.q[10] | c.q[11]) & 0x0f0f) == 0)
                  c.qstate |= 0x00002200;

               if (((c.q[9] | c.q[11]) & 0x00ff) == 0)
                  c.qstate |= 0x00004400;

               if (c.q[8]==0 && c.q[9]==0 && c.q[10]==0 && c.q[11]==0)
                  c.qstate |= 0x00008800;
            }

            if ((c.pstate & 0x00000008) != 0x00000008
                  ||(cSpstate & 0x00000200) != 0x00000200
                  ||(cEpstate & 0x00040000) != 0x00040000
                  ||(cSEpstate& 0x01000000) != 0x01000000)   // fourth 8x8 (lower right):
                  {
                     ix00= (int)(c.p[12]) & 0xffff;
                     ix10= (int)(c.p[13]) & 0xffff;
                     ix20= (int)(c.p[14]) & 0xffff;
                     ix30= (int)(c.p[15]) & 0xffff;

                     ix02= (ix00 & 0x00ff) | (ix10 & 0xff00);
                     ix22= (ix20 & 0x00ff) | (ix30 & 0xff00);

                     ix01= (ix00 & 0x0f0f) | (ix20 & 0xf0f0);
                     ix11= (ix10 & 0x0f0f) | (ix30 & 0xf0f0);

                     if (cS != null) {
                        ix21= (ix20 & 0x0f0f) | (cS.p[8] & 0xf0f0);
                        ix31= (ix30 & 0x0f0f) | (cS.p[9] & 0xf0f0);
                     }
                     else {
                        ix21= (ix20 & 0x0f0f);
                        ix31= (ix30 & 0x0f0f);
                     }

                     if (c.E != null) {
                        ix12= (ix10 & 0x00ff) | (cE.p[4] & 0xff00);
                        ix32= (ix30 & 0x00ff) | (cE.p[6] & 0xff00);
                     }
                     else {
                        ix12= (ix10 & 0x00ff);
                        ix32= (ix30 & 0x00ff);
                     }

                     ix03= (ix01 & 0x00ff) | (ix11 & 0xff00);
                     ix13= (ix12 & 0x0f0f) | (ix32 & 0xf0f0);
                     ix23= (ix21 & 0x00ff) | (ix31 & 0xff00);

                     if (cSE != null) {
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0) | (cSE.p[0] & 0xf000);
                     }
                     else {
                        ix33= (ix32 & 0x0f0f) | (ix31 & 0x00f0);
                     }

                     n0 = (short)(
                           (0xf000 & crunch[ix00])
                           | (0x0f00 & crunch[ix01])
                           | (0x00f0 & crunch[ix02])
                           | (0x000f & crunch[ix03]));

                     n1 = (short)(
                           (0xf000 & crunch[ix10])
                           | (0x0f00 & crunch[ix11])
                           | (0x00f0 & crunch[ix12])
                           | (0x000f & crunch[ix13]));

                     n2 = (short)(
                           (0xf000 & crunch[ix20])
                           | (0x0f00 & crunch[ix21])
                           | (0x00f0 & crunch[ix22])
                           | (0x000f & crunch[ix23]));

                     n3 = (short)(
                           (0xf000 & crunch[ix30])
                           | (0x0f00 & crunch[ix31])
                           | (0x00f0 & crunch[ix32])
                           | (0x000f & crunch[ix33]));


                     xor1 = c.q[13] ^ n1;
                     xor2 = c.q[14] ^ n2;
                     xor3 = c.q[15] ^ n3;

                     if ((xor3 & 0x000f) == 0)
                     {
                        if ((n3 & 0x000f) == 0)         // SE 2x2 corner
                           c.qstate |= 0x00000011;    // morgue & hiber
                        else
                           c.qstate |= 0x00000001;    // just hibernation

                        if (((xor2 | xor3) & 0x0f0f) == 0)   // S 8x2 Horizontal border
                        {
                           if (((n2 | n3) & 0x0f0f) == 0)
                              c.qstate |= 0x00000022;
                           else
                              c.qstate |= 0x00000002;
                        }
                        else
                        {
                           c.qstate &= 0xffffff55;

                           if (cS != null) {
                              rattleCage(cS);
                           }
                           else {
                              allocateCage(x,yp);
                              cS=c.S;
                           }

                        }

                        if (((xor1 | xor3) & 0x00ff) == 0)   // E 2x8 Vertical border
                        {
                           if (((n1 | n3) & 0x00ff) == 0)
                              c.qstate |= 0x00000044;
                           else
                              c.qstate |= 0x00000004;

                           if ((xor1 | xor2 | xor3 | (c.q[12] ^ n0)) == 0)   // whole 8x8 block
                           {
                              if (n0==0 && n1==0 && n2==0 && n3==0)
                                 c.qstate |= 0x00000088;
                              else
                                 c.qstate |= 0x00000008;
                           }
                           else
                           {
                              c.qstate &= 0xffffff77;
                           }
                        }
                        else
                        {
                           c.qstate &= 0xffffff33;

                           if (cE != null) {
                              rattleCage(cE);
                           }
                           else {
                              allocateCage(xp,y);
                              cE=c.E;
                           }
                        }
                     }
                     else
                     {
                        c.qstate &= 0xffffff00;

                        if (cS != null) {
                           rattleCage(cS);
                        }
                        else {
                           allocateCage(x,yp);
                           cS=c.S;
                        }

                        if (cE != null) {
                           rattleCage(cE);
                        }
                        else {
                           allocateCage(xp,y);
                           cE=c.E;
                        }

                        if (cSE != null) {
                           rattleCage(cSE);
                        }
                        else {
                           allocateCage(xp,yp);
                           cSE=c.SE;
                        }
                     }

                     c.q[12]=n0; c.q[14]=n2;
                     c.q[13]=n1; c.q[15]=n3;
                  }
            else
            {
               c.qstate |= 0x0000000f;

               if ((c.q[15] & 0x000f) == 0)
                  c.qstate |= 0x00000011;

               if (((c.q[14] | c.q[15]) & 0x0f0f) == 0)
                  c.qstate |= 0x00000022;

               if (((c.q[13] | c.q[15]) & 0x00ff) == 0)
                  c.qstate |= 0x00000044;

               if (c.q[12]==0 && c.q[13]==0 && c.q[14]==0 && c.q[15]==0)
                  c.qstate |= 0x00000088;
            }
         }
         if (!backCorrect) c.qstate=0;
      }
      gencount++;
      backCorrect=true;
      return;
   }

   private void generate_q()
   {
      LifeCell c, cnext;
      LifeCell cN, cNW, cW;
      int cNqstate, cNWqstate, cWqstate;
      int x, y, xm, ym;
      int xor0, xor1, xor2;
      int ix00, ix02, ix10, ix12,   // 4x4 neighborhoods
      ix01, ix03, ix11, ix13,   // extracted from a
      ix20, ix22, ix30, ix32,   // full 8x8 block,
      ix21, ix23, ix31, ix33;   // for table lookups.
      short n0, n1, n2, n3;         // table lookup results

      //System.out.println("\n*** Q -> P\n");

      // For each cage:
      for (c=living; c!=null; c=cnext)
      {
         cnext=c.Next;

         cN=c.N;
         cW=c.W;
         cNW=c.NW;

         if (cN != null) cNqstate=cN.qstate;
         else cNqstate= -1;

         if (cW != null) cWqstate=cW.qstate;
         else cWqstate= -1;

         if (cNW!= null) cNWqstate=cNW.qstate;
         else cNWqstate= -1;

         if ((c.qstate & 0x08080808)==0x08080808 && (cNqstate & 0x00020002)==0x00020002
               &&(cWqstate & 0x00000404)==0x00000404 && (cNWqstate& 0x00000001)==0x00000001)
         {
            if ((c.qstate & 0x80808080)==0x80808080 && (cNqstate & 0x00200020)==0x00200020
                  &&(cWqstate & 0x00004040)==0x00004040 && (cNWqstate& 0x00000010)==0x00000010)
            {
               c.flags |= 0x3000;
               if ((c.flags & 0xf800)==0xf000)
               {
                  killCage(c);
               }
               c.pstate = 0xffffffff;
            }
            else
            {
               c.flags |= 0x1000;
               if ((c.flags & 0x5800)==0x5000)
               {
                  tranquilizeCage(c);
               }
               c.pstate |= 0x0f0f0f0f;
            }
            c.flags &= 0xf7ff;  // Reset the Rattling bit
         }
         else
         {
            c.flags &= 0x07ff;
            x = c.x<<4;  y = c.y<<4;
            xm = (x-16);  //pin
            ym = (y-16);  //pin

            if ((c.qstate & 0x08000000) != 0x08000000
                  ||(cNqstate & 0x00020000) != 0x00020000
                  ||(cWqstate & 0x00000400) != 0x00000400
                  ||(cNWqstate& 0x00000001) != 0x00000001)  // first 8x8 not hibernating/morgue
            {
               ix00= (int)(c.q[0]) & 0xffff;
               ix10= (int)(c.q[1]) & 0xffff;
               ix20= (int)(c.q[2]) & 0xffff;
               ix30= (int)(c.q[3]) & 0xffff;

               ix12= (ix00 & 0x00ff) | (ix10 & 0xff00);
               ix32= (ix20 & 0x00ff) | (ix30 & 0xff00);

               ix21= (ix00 & 0x0f0f) | (ix20 & 0xf0f0);
               ix31= (ix10 & 0x0f0f) | (ix30 & 0xf0f0);

               if (cN != null) {
                  ix01= (ix00 & 0xf0f0) | (cN.q[6] & 0x0f0f);
                  ix11= (ix10 & 0xf0f0) | (cN.q[7] & 0x0f0f);
               }
               else {
                  ix01= (ix00 & 0xf0f0);
                  ix11= (ix10 & 0xf0f0);
               }

               if (cW != null) {
                  ix02= (ix00 & 0xff00) | (cW.q[9] & 0x00ff);
                  ix22= (ix20 & 0xff00) | (cW.q[11] & 0x00ff);
               }
               else {
                  ix02= (ix00 & 0xff00);
                  ix22= (ix20 & 0xff00);
               }

               ix13= (ix01 & 0x00ff) | (ix11 & 0xff00);
               ix23= (ix02 & 0x0f0f) | (ix22 & 0xf0f0);
               ix33= (ix21 & 0x00ff) | (ix31 & 0xff00);

               if (cNW != null) {
                  ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00) | (cNW.q[15] & 0x000f);
               }
               else {
                  ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00);
               }

               n0 = (short)(
                     (0x000f & munch[ix00])
                     | (0x00f0 & munch[ix01])
                     | (0x0f00 & munch[ix02])
                     | (0xf000 & munch[ix03]));

               n1 = (short)(
                     (0x000f & munch[ix10])
                     | (0x00f0 & munch[ix11])
                     | (0x0f00 & munch[ix12])
                     | (0xf000 & munch[ix13]));

               n2 = (short)(
                     (0x000f & munch[ix20])
                     | (0x00f0 & munch[ix21])
                     | (0x0f00 & munch[ix22])
                     | (0xf000 & munch[ix23]));

               n3 = (short)(
                     (0x000f & munch[ix30])
                     | (0x00f0 & munch[ix31])
                     | (0x0f00 & munch[ix32])
                     | (0xf000 & munch[ix33]));


               xor0 = c.p[0] ^ n0;
               xor1 = c.p[1] ^ n1;
               xor2 = c.p[2] ^ n2;

               if ((xor0 & 0xf000) == 0)
               {
                  if ((n0 & 0xf000) == 0)         // NW 2x2 corner
                     c.pstate |= 0x11000000;    // morgue & hiber
                  else
                     c.pstate |= 0x01000000;    // just hibernation

                  if (((xor1 | xor0) & 0xf0f0) == 0)   // N 8x2 Horizontal border
                  {
                     if (((n1 | n0) & 0xf0f0) == 0)
                        c.pstate |= 0x22000000;
                     else
                        c.pstate |= 0x02000000;
                  }
                  else
                  {
                     c.pstate &= 0x55ffffff;

                     if (cN != null) {
                        rattleCage(cN);
                     }
                     else {
                        allocateCage(x,ym);
                        cN=c.N;
                     }
                  }

                  if (((xor2 | xor0) & 0xff00) == 0)   // W 2x8 Vertical border
                  {
                     if (((n2 | n0) & 0xff00) == 0)
                        c.pstate |= 0x44000000;
                     else
                        c.pstate |= 0x04000000;

                     if ((xor2 | xor1 | xor0 | (c.p[3] ^ n3)) == 0)   // whole 8x8 block
                     {
                        if (n0==0 && n1==0 && n2==0 && n3==0)
                           c.pstate |= 0x88000000;
                        else
                           c.pstate |= 0x08000000;

                     }
                     else
                     {
                        c.pstate &= 0x77ffffff;
                     }
                  }
                  else
                  {
                     c.pstate &= 0x33ffffff;

                     if (cW != null) {
                        rattleCage(cW);
                     }
                     else {
                        allocateCage(xm,y);
                        cW=c.W;
                     }
                  }
               }
               else
               {
                  c.pstate &= 0x00ffffff;

                  if (cN != null) {
                     rattleCage(cN);
                  }
                  else {
                     allocateCage(x,ym);
                     cN=c.N;
                  }

                  if (cW != null) {
                     rattleCage(cW);
                  }
                  else {
                     allocateCage(xm,y);
                     cW=c.W;
                  }

                  if (cNW != null) {
                     rattleCage(cNW);
                  }
                  else {
                     allocateCage(xm,ym);
                     cNW=c.NW;
                  }
               }

               c.p[0]=n0; c.p[2]=n2;
               c.p[1]=n1; c.p[3]=n3;
            }
            else
            {
               c.pstate |= 0x0f000000;

               if ((c.p[0] & 0xf000) == 0)
                  c.pstate |= 0x11000000;

               if (((c.p[1] | c.p[0]) & 0xf0f0) == 0)
                  c.pstate |= 0x22000000;

               if (((c.p[2] | c.p[0]) & 0xff00) == 0)
                  c.pstate |= 0x44000000;

               if (c.p[3]==0 && c.p[2]==0 && c.p[1]==0 && c.p[0]==0)
                  c.pstate |= 0x88000000;
            }

            if ((c.qstate & 0x02080000) != 0x02080000
                  ||(cWqstate & 0x00000104) != 0x00000104)  // second 8x8 (lower left):
                  {
                     ix00= (int)(c.q[4]) & 0xffff;
                     ix10= (int)(c.q[5]) & 0xffff;
                     ix20= (int)(c.q[6]) & 0xffff;
                     ix30= (int)(c.q[7]) & 0xffff;

                     ix12= (ix00 & 0x00ff) | (ix10 & 0xff00);
                     ix32= (ix20 & 0x00ff) | (ix30 & 0xff00);

                     ix01= (ix00 & 0xf0f0) | (c.q[2] & 0x0f0f);
                     ix11= (ix10 & 0xf0f0) | (c.q[3] & 0x0f0f);
                     ix21= (ix20 & 0xf0f0) | (ix00 & 0x0f0f);
                     ix31= (ix30 & 0xf0f0) | (ix10 & 0x0f0f);

                     if (cW != null)
                     {
                        ix02= (ix00 & 0xff00) | (cW.q[13] & 0x00ff);
                        ix22= (ix20 & 0xff00) | (cW.q[15] & 0x00ff);
                        ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00) | (cW.q[11] & 0x000f);
                     }
                     else
                     {
                        ix02= (ix00 & 0xff00);
                        ix22= (ix20 & 0xff00);
                        ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00);
                     }

                     ix13= (ix01 & 0x00ff) | (ix11 & 0xff00);
                     ix23= (ix02 & 0x0f0f) | (ix22 & 0xf0f0);
                     ix33= (ix21 & 0x00ff) | (ix31 & 0xff00);

                     n0 = (short)(
                           (0x000f & munch[ix00])
                           | (0x00f0 & munch[ix01])
                           | (0x0f00 & munch[ix02])
                           | (0xf000 & munch[ix03]));

                     n1 = (short)(
                           (0x000f & munch[ix10])
                           | (0x00f0 & munch[ix11])
                           | (0x0f00 & munch[ix12])
                           | (0xf000 & munch[ix13]));

                     n2 = (short)(
                           (0x000f & munch[ix20])
                           | (0x00f0 & munch[ix21])
                           | (0x0f00 & munch[ix22])
                           | (0xf000 & munch[ix23]));

                     n3 = (short)(
                           (0x000f & munch[ix30])
                           | (0x00f0 & munch[ix31])
                           | (0x0f00 & munch[ix32])
                           | (0xf000 & munch[ix33]));


                     xor0 = c.p[4] ^ n0;
                     xor1 = c.p[5] ^ n1;
                     xor2 = c.p[6] ^ n2;

                     if ((xor0 & 0xf000) == 0)
                     {
                        if ((n0 & 0xf000) == 0)         // NW 2x2 corner
                           c.pstate |= 0x00110000;    // morgue & hiber
                        else
                           c.pstate |= 0x00010000;    // just hibernation

                        if (((xor1 | xor0) & 0xf0f0) == 0)   // N 8x2 Horizontal border
                        {
                           if (((n1 | n0) & 0xf0f0) == 0)
                              c.pstate |= 0x00220000;
                           else
                              c.pstate |= 0x00020000;

                        }
                        else
                        {
                           c.pstate &= 0xff55ffff;
                        }

                        if (((xor2 | xor0) & 0xff00) == 0)   // W 2x8 Vertical border
                        {
                           if (((n2 | n0) & 0xff00) == 0)
                              c.pstate |= 0x00440000;
                           else
                              c.pstate |= 0x00040000;

                           if ((xor2 | xor1 | xor0 | (c.p[7] ^ n3)) == 0)   // whole 8x8 block
                           {
                              if (n0==0 && n1==0 && n2==0 && n3==0)
                                 c.pstate |= 0x00880000;
                              else
                                 c.pstate |= 0x00080000;

                           }
                           else
                           {
                              c.pstate &= 0xff77ffff;
                           }
                        }
                        else
                        {
                           c.pstate &= 0xff33ffff;

                           if (cW != null) {
                              rattleCage(cW);
                           }
                           else {
                              allocateCage(xm,y);
                              cW=c.W;
                           }
                        }
                     }
                     else
                     {
                        c.pstate &= 0xff00ffff;

                        if (cW != null) {
                           rattleCage(cW);
                        }
                        else {
                           allocateCage(xm,y);
                           cW=c.W;
                        }
                     }

                     c.p[4]=n0; c.p[6]=n2;
                     c.p[5]=n1; c.p[7]=n3;
                  }
            else
            {
               c.pstate |= 0x000f0000;

               if ((c.p[4] & 0xf000) == 0)
                  c.pstate |= 0x00110000;

               if (((c.p[5] | c.p[4]) & 0xf0f0) == 0)
                  c.pstate |= 0x00220000;

               if (((c.p[6] | c.p[4]) & 0xff00) == 0)
                  c.pstate |= 0x00440000;

               if (c.p[7]==0 && c.p[6]==0 && c.p[5]==0 && c.p[4]==0)
                  c.pstate |= 0x00880000;
            }

            if ((c.qstate & 0x04000800) != 0x04000800
                  ||(cNqstate & 0x00010002) != 0x00010002)   // third 8x8 (upper right):
                  {
                     ix00= (int)(c.q[8]) & 0xffff;
                     ix10= (int)(c.q[9]) & 0xffff;
                     ix20= (int)(c.q[10])& 0xffff;
                     ix30= (int)(c.q[11])& 0xffff;

                     ix02= (ix00 & 0xff00) | (c.q[1] & 0x00ff);
                     ix12= (ix00 & 0x00ff) | (ix10 & 0xff00);
                     ix22= (ix20 & 0xff00) | (c.q[3] & 0x00ff);
                     ix32= (ix20 & 0x00ff) | (ix30 & 0xff00);

                     ix21= (ix20 & 0xf0f0) | (ix00 & 0x0f0f);
                     ix31= (ix30 & 0xf0f0) | (ix10 & 0x0f0f);

                     if (cN != null)
                     {
                        ix01= (ix00 & 0xf0f0) | (cN.q[14] & 0x0f0f);
                        ix11= (ix10 & 0xf0f0) | (cN.q[15] & 0x0f0f);
                        ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00) | (cN.q[7] & 0x000f);
                     }
                     else
                     {
                        ix01= (ix00 & 0xf0f0);
                        ix11= (ix10 & 0xf0f0);
                        ix03= (ix02 & 0xf0f0) | (ix01 & 0x0f00);
                     }

                     ix13= (ix01 & 0x00ff) | (ix11 & 0xff00);
                     ix23= (ix02 & 0x0f0f) | (ix22 & 0xf0f0);
                     ix33= (ix21 & 0x00ff) | (ix31 & 0xff00);

                     n0 = (short)(
                           (0x000f & munch[ix00])
                           | (0x00f0 & munch[ix01])
                           | (0x0f00 & munch[ix02])
                           | (0xf000 & munch[ix03]));

                     n1 = (short)(
                           (0x000f & munch[ix10])
                           | (0x00f0 & munch[ix11])
                           | (0x0f00 & munch[ix12])
                           | (0xf000 & munch[ix13]));

                     n2 = (short)(
                           (0x000f & munch[ix20])
                           | (0x00f0 & munch[ix21])
                           | (0x0f00 & munch[ix22])
                           | (0xf000 & munch[ix23]));

                     n3 = (short)(
                           (0x000f & munch[ix30])
                           | (0x00f0 & munch[ix31])
                           | (0x0f00 & munch[ix32])
                           | (0xf000 & munch[ix33]));


                     xor0 = c.p[8] ^ n0;
                     xor1 = c.p[9] ^ n1;
                     xor2 = c.p[10] ^ n2;

                     if ((xor0 & 0xf000) == 0)
                     {
                        if ((n0 & 0xf000) == 0)         // NW 2x2 corner
                           c.pstate |= 0x00001100;    // morgue & hiber
                        else
                           c.pstate |= 0x00000100;    // just hibernation

                        if (((xor1 | xor0) & 0xf0f0) == 0)   // N 8x2 Horizontal border
                        {
                           if (((n1 | n0) & 0xf0f0) == 0)
                              c.pstate |= 0x00002200;
                           else
                              c.pstate |= 0x00000200;
                        }
                        else
                        {
                           c.pstate &= 0xffff55ff;

                           if (cN != null) {
                              rattleCage(cN);
                           }
                           else {
                              allocateCage(x,ym);
                              cN=c.N;
                           }
                        }

                        if (((xor2 | xor0) & 0xff00) == 0)   // W 2x8 Vertical border
                        {
                           if (((n2 | n0) & 0xff00) == 0)
                              c.pstate |= 0x00004400;
                           else
                              c.pstate |= 0x00000400;

                           if ((xor2 | xor1 | xor0 | (c.p[11] ^ n3)) == 0)   // whole 8x8 block
                           {
                              if (n0==0 && n1==0 && n2==0 && n3==0)
                                 c.pstate |= 0x00008800;
                              else
                                 c.pstate |= 0x00000800;
                           }
                           else
                           {
                              c.pstate &= 0xffff77ff;
                           }
                        }
                        else
                        {
                           c.pstate &= 0xffff33ff;
                        }
                     }
                     else
                     {
                        c.pstate &= 0xffff00ff;

                        if (cN != null) {
                           rattleCage(cN);
                        }
                        else {
                           allocateCage(x,ym);
                           cN=c.N;
                        }
                     }

                     c.p[8]=n0;  c.p[10]=n2;
                     c.p[9]=n1;  c.p[11]=n3;
                  }
            else
            {
               c.pstate |= 0x00000f00;

               if ((c.p[8] & 0xf000) == 0)
                  c.pstate |= 0x00001100;

               if (((c.p[9] | c.p[8]) & 0xf0f0) == 0)
                  c.pstate |= 0x00002200;

               if (((c.p[10] | c.p[8]) & 0xff00) == 0)
                  c.pstate |= 0x00004400;

               if (c.p[11]==0 && c.p[10]==0 && c.p[9]==0 && c.p[8]==0)
                  c.pstate |= 0x00008800;
            }

            if ((c.qstate & 0x01040208) != 0x01040208)   // fourth 8x8 (lower right):
            {
               ix00= (int)(c.q[12]) & 0xffff;
               ix10= (int)(c.q[13]) & 0xffff;
               ix20= (int)(c.q[14]) & 0xffff;
               ix30= (int)(c.q[15]) & 0xffff;

               ix02= (ix00 & 0xff00) | (c.q[5] & 0x00ff);
               ix12= (ix10 & 0xff00) | (ix00 & 0x00ff);
               ix22= (ix20 & 0xff00) | (c.q[7] & 0x00ff);
               ix32= (ix30 & 0xff00) | (ix20 & 0x00ff);

               ix01= (ix00 & 0xf0f0) | (c.q[10] & 0x0f0f);
               ix11= (ix10 & 0xf0f0) | (c.q[11] & 0x0f0f);
               ix21= (ix20 & 0xf0f0) | (ix00 & 0x0f0f);
               ix31= (ix30 & 0xf0f0) | (ix10 & 0x0f0f);

               ix03= (ix01 & 0x0f00) | (ix02 & 0xf0f0) | (c.q[3] & 0x000f);
               ix13= (ix01 & 0x00ff) | (ix11 & 0xff00);
               ix23= (ix02 & 0x0f0f) | (ix22 & 0xf0f0);
               ix33= (ix12 & 0x0f0f) | (ix32 & 0xf0f0);

               n0 = (short)(
                     (0x000f & munch[ix00])
                     | (0x00f0 & munch[ix01])
                     | (0x0f00 & munch[ix02])
                     | (0xf000 & munch[ix03]));

               n1 = (short)(
                     (0x000f & munch[ix10])
                     | (0x00f0 & munch[ix11])
                     | (0x0f00 & munch[ix12])
                     | (0xf000 & munch[ix13]));

               n2 = (short)(
                     (0x000f & munch[ix20])
                     | (0x00f0 & munch[ix21])
                     | (0x0f00 & munch[ix22])
                     | (0xf000 & munch[ix23]));

               n3 = (short)(
                     (0x000f & munch[ix30])
                     | (0x00f0 & munch[ix31])
                     | (0x0f00 & munch[ix32])
                     | (0xf000 & munch[ix33]));


               xor0 = c.p[12] ^ n0;
               xor1 = c.p[13] ^ n1;
               xor2 = c.p[14] ^ n2;

               if ((xor0 & 0xf000) == 0)
               {
                  if ((n0 & 0xf000) == 0)         // NW 2x2 corner
                     c.pstate |= 0x00000011;    // morgue & hiber
                  else
                     c.pstate |= 0x00000001;    // just hibernation

                  if (((xor1 | xor0) & 0xf0f0) == 0)   // N 8x2 Horizontal border
                  {
                     if (((n1 | n0) & 0xf0f0) == 0)
                        c.pstate |= 0x00000022;
                     else
                        c.pstate |= 0x00000002;
                  }
                  else
                  {
                     c.pstate &= 0xffffff55;
                  }

                  if (((xor2 | xor0) & 0xff00) == 0)   // W 2x8 Vertical border
                  {
                     if (((n2 | n0) & 0xff00) == 0)
                        c.pstate |= 0x00000044;
                     else
                        c.pstate |= 0x00000004;

                     if ((xor2 | xor1 | xor0 | (c.p[15] ^ n3)) == 0)   // whole 8x8 block
                     {
                        if (n0==0 && n1==0 && n2==0 && n3==0)
                           c.pstate |= 0x00000088;
                        else
                           c.pstate |= 0x00000008;
                     }
                     else
                     {
                        c.pstate &= 0xffffff77;
                     }
                  }
                  else
                  {
                     c.pstate &= 0xffffff33;
                  }
               }
               else
               {
                  c.pstate &= 0xffffff00;
               }

               c.p[12]=n0; c.p[14]=n2;
               c.p[13]=n1; c.p[15]=n3;
            }
            else
            {
               c.pstate |= 0x0000000f;

               if ((c.p[12] & 0xf000) == 0)
                  c.pstate |= 0x00000011;

               if (((c.p[13] | c.p[12]) & 0xf0f0) == 0)
                  c.pstate |= 0x00000022;

               if (((c.p[14] | c.p[12]) & 0xff00) == 0)
                  c.pstate |= 0x00000044;

               if (c.p[15]==0 && c.p[14]==0 && c.p[13]==0 && c.p[12]==0)
                  c.pstate |= 0x00000088;
            }
         }
         if (!backCorrect) c.pstate=0;
      }
      gencount++;
      backCorrect=true;
      return;
   }

   private void incinerateCages(boolean beNice)
   {
      LifeCell victim;

      if (beNice) victim=caretaker;   // only decaying bodies
      else victim=morgue;            // whole morgue

      while (victim != null)
      {
         if ((victim.flags & 1) == 0)  // not being displayed
         {
            if (victim.Prev != null) victim.Prev.Next = victim.Next;
            else morgue=null;
            if (victim.Next != null) victim.Next.Prev = victim.Prev;

            if (victim.S != null) victim.S.N = null;
            if (victim.E != null) victim.E.W = null;
            if (victim.SE!= null) victim.SE.NW = null;
            if (victim.N != null) victim.N.S = null;
            if (victim.W != null) victim.W.E = null;
            if (victim.NW!= null) victim.NW.SE = null;

            hashTable.delete(victim);
         }
         victim = victim.Next;

         // no more references to the victim: it should become a victim to garbage collection.
      }

      caretaker=morgue;              // start at top again
   }

   private LifeCell allocateCage(int x, int y)
   {
      LifeCell c;
      int xp=(x+16), xm=(x-16);  //pin
      int yp=(y+16), ym=(y-16);  //pin

      //System.out.println("* Allocating " + (x>>4) +","+ (y>>4));

      try {c = new LifeCell();}
      catch (OutOfMemoryError e)
      {
         System.out.println("Out of memory error");
         incinerateCages(true);

         try {c = new LifeCell();}
         catch (OutOfMemoryError e2)
         {
            incinerateCages(false);

            c = new LifeCell();
         }
      }

      c.x=(short)(x>>4); c.y=(short)(y>>4);
      c.S = hashTable.retrieve(x, yp);
      if (c.S != null) c.S.N = c;
      c.E = hashTable.retrieve(xp,y);
      if (c.E != null) c.E.W = c;
      c.SE= hashTable.retrieve(xp,yp);
      if (c.SE != null) c.SE.NW = c;
      c.N = hashTable.retrieve(x, ym);
      if (c.N != null) c.N.S = c;
      c.W = hashTable.retrieve(xm,y);
      if (c.W != null) c.W.E = c;
      c.NW= hashTable.retrieve(xm,ym);
      if (c.NW != null) c.NW.SE = c;

      c.Next=living;
      if (living != null) living.Prev=c;
      living=c;

      addToDisplay(c);

      hashTable.store(c);

      //dumpState();

      return c;
   }

   // Put Cage in the morgue state.  There are only 2 possible fates from here:
   // rattleCage() and incinerateCages(): return to the living list, or be deallocated.
   private boolean killCage(LifeCell c)
   {
      if (c==null) return false;
      if ((c.flags & 0x02) != 0) return false;  // already in morgue
      c.flags ^= 0x02;

      //System.out.println("**** Killing " + c.x+","+c.y);

      // Remove from living list
      if (c.Prev == null) living = c.Next;
      else c.Prev.Next = c.Next;
      if (c.Next != null) c.Next.Prev = c.Prev;

      // Put in morgue
      c.Next = morgue;
      c.Prev = null;
      if (morgue != null) morgue.Prev = c;
      morgue = c;

      //dumpState();

      return true;
   }

   // Put Cage in the hibernation state.  There is only 1 possible fate from here:
   // rattleCage() to return to the living list.  But you can hibernate forever.
   private boolean tranquilizeCage(LifeCell c)
   {
      if (c==null) return false;
      if ((c.flags & 0x04) != 0) return false;  // already in hibernation
      c.flags ^= 0x04;

      // Remove from living list
      if (c.Prev == null) living = c.Next;
      else c.Prev.Next = c.Next;
      if (c.Next != null) c.Next.Prev = c.Prev;

      // Put in hibernation
      c.Next = hibernating;
      c.Prev = null;
      if (hibernating != null) hibernating.Prev = c;
      hibernating = c;

      return true;
   }

   // This cage will have to be calculated next generation.  Take out of either
   // morgue or hibernation state, whichever the case may be:
   private boolean rattleCage(LifeCell c)
   {
      if (c==null) return false;
      c.flags |= 0x0800;  // set the Rattling bit
      if ((c.flags & 0x06)==0) return false;

      //System.out.print("** ");

      if ((c.flags & 0x02) != 0)  // resurrect from morgue
      {
         if ((c.flags & 1) == 0) addToDisplay(c);

         c.flags &= 0x0ff1;
         //            c.flags &= 0x0ffd;
         c.pstate=c.qstate=0;

         //System.out.println("Resurrect " + c.x+","+c.y);
         // Remove from morgue
         if (caretaker==c) caretaker=c.Next;

         if (c.Prev == null) morgue = c.Next;
         else c.Prev.Next = c.Next;
         if (c.Next != null) c.Next.Prev = c.Prev;

      }
      else                      // wake up from hibernation
      {
         c.flags &= 0x0ff1;
         //            c.flags &= 0x0ffb;
         c.pstate=c.qstate=0;

         // Remove from hibernation
         if (c.Prev == null) hibernating = c.Next;
         else c.Prev.Next = c.Next;
         if (c.Next != null) c.Next.Prev = c.Prev;
      }

      // Put in living list
      c.Next = living;
      c.Prev = null;
      if (living != null) living.Prev = c;
      living = c;

      //dumpState();

      return true;
   }


   private LifeCell getBlockRef(LifeCoordinate cor, boolean force)
   {
      int x=cor.x, y=cor.y, x2, y2;
      LifeCell c;
      boolean hEdge=false, vEdge=false;

      if (qCycle) { x--; y--;
         //pin
      }

      c = hashTable.retrieve(x,y);

      if (force)
      {
         if (c==null) c = allocateCage(x,y);
         else rattleCage(c);

         if (qCycle)
         {
            if ((x & 0x0e) == 0x0e) vEdge=true;
            if ((y & 0x0e) == 0x0e) hEdge=true;

            x2=(x+16);  //pin
            y2=(y+16);  //pin

            if (vEdge)
            {
               if (c.E == null) allocateCage(x2,y);
               else rattleCage(c.E);

               if (hEdge)
               {
                  if (c.SE == null) allocateCage(x2,y2);
                  else rattleCage(c.SE);
               }
            }
            if (hEdge)
            {
               if (c.S == null) allocateCage(x,y2);
               else rattleCage(c.S);
            }
         }
         else
         {
            if ((x & 0x0e) == 0) vEdge=true;
            if ((y & 0x0e) == 0) hEdge=true;

            x2=(x-16);  //pin
            y2=(y-16);  //pin

            if (vEdge)
            {
               if (c.W == null) allocateCage(x2,y);
               else rattleCage(c.W);

               if (hEdge)
               {
                  if (c.NW == null) allocateCage(x2,y2);
                  else rattleCage(c.NW);
               }
            }
            if (hEdge)
            {
               if (c.N == null) allocateCage(x,y2);
               else rattleCage(c.N);
            }
         }
      }

      return c;
   }

   private int getBlockIndex(LifeCoordinate cor)
   {
      int x=cor.x, y=cor.y;

      if (qCycle) { x--; y--; }

      return (x & 0x8)|((y & 0xc)>>1)|((x & 0x4)>>2);
   }

   private int getBlockBitmask(LifeCoordinate cor)
   {
      int x=cor.x, y=cor.y;

      if (qCycle) { x--; y--; }

      return 0x8000>>((x & 2)==0?0:8)
         >>((y & 2)==0?0:4)
         >>((y & 1)==0?0:2)
         >>((x & 1)==0?0:1);
   }

   // implementation of the public interface:

   public void changeCell(LifeCoordinate cor, boolean state)
   {
      int ix, bitset;
      LifeCell c;

      backCorrect=false;

      c=getBlockRef(cor,true);
      ix=getBlockIndex(cor);
      bitset=getBlockBitmask(cor);

      if (qCycle)
      {
         if (((c.q[ix] & bitset)!=0) ^ state)
         {
            c.q[ix] ^= bitset;

            c.qstate = 0;
            if (c.N != null) c.N.qstate &= 0xff00ff00;
            if (c.W != null) c.W.qstate &= 0xffff0000;
            if (c.NW != null) c.NW.qstate &= 0xffffff00;
         }
      }
      else
      {
         if (((c.p[ix] & bitset)!=0) ^ state)
         {
            c.p[ix] ^= bitset;

            c.pstate = 0;
            if (c.S != null) c.S.pstate &= 0x00ff00ff;
            if (c.E != null) c.E.pstate &= 0x0000ffff;
            if (c.SE != null) c.SE.pstate &= 0x00ffffff;
         }
      }
   }

   public void changeCell(int x, int y, boolean state)
   {
      LifeCoordinate cor = new LifeCoordinate();
      cor.x=(short)x; cor.y=(short)y;

      changeCell(cor, state);
   }

   public boolean testCell(LifeCoordinate cor)
   {
      int ix, bitset;
      LifeCell c;

      c=getBlockRef(cor,false);
      if (c==null) return false;  // no block, no cell
      ix=getBlockIndex(cor);
      bitset=getBlockBitmask(cor);

      if (qCycle)
      {
         if ((c.q[ix] & bitset)==0) return false;
         else return true;
      }
      else
      {
         if ((c.p[ix] & bitset)==0) return false;
         else return true;
      }
   }

   public boolean testCell(int x, int y)
   {
      LifeCoordinate cor = new LifeCoordinate();
      cor.x=(short)x; cor.y=(short)y;

      return testCell(cor);
   }

   /*
    * setRules()
    *
    * Expand crunch[], munch[] tables from Rule[] table.
    *
    * This is done so that we can work from a 4x4 -> 2x2
    * conversion instead of the old 3x3 -> 1x1.  This makes the
    * algorithm at least 4x faster, at the price of a lookup table
    * 128 times larger, and some extra initialization time.
    */
   private void setRules(boolean[] Rule)
   {
      int ic000, i3000, i0c00, i0300, i00c0, i0030,
      i8000, i4000, i2000, i1000, i0800, i0400, i0200, i0100,
      i0080, i0040, i0020, i0010;
      int r8000, r4000, r2000, r1000, r0800, r0400,
      r0080, r0020, r0008, r;
      int r8000a, r4000a, r2000a, r2000b;
      int r0800a, r0800b, r0200a, r0200b,
      r0100a, r0100b, r0100c, r0100d;
      int r0080a, r0040a, r0040b,
      r0010a, r0010b, r0010c, r0010d;
      int r0004a, r0004b, r0004c, r0004d,
      r0002a, r0002b, r0002c, r0002d,
      r0001a, r0001b;
      int m1, m2;


      for (int i=0; i<0x10000; i+=0x10)
      {
         ic000=i&0xc000; i3000=i&0x3000;
         i0c00=i&0x0c00; i0300=i&0x0300;
         i00c0=i&0x00c0; i0030=i&0x0030;
         i8000=i&0x8000; i4000=i&0x4000; i2000=i&0x2000; i1000=i&0x1000;
         i0800=i&0x0800; i0400=i&0x0400; i0200=i&0x0200; i0100=i&0x0100;
         i0080=i&0x0080; i0040=i&0x0040; i0020=i&0x0020; i0010=i&0x0010;

         r8000 = (ic000 >>> 7) | (i3000 >>> 8) | (i0c00 >>> 9) | (i0080 >>> 1) | (i0020 >>> 2);
         r4000 =  i00c0        | (i0030 >>> 1) | (i4000 >>> 6) | (i1000 >>> 7) | (i0400 >>> 8);
         r2000 = (i3000 >>> 5) | (i0c00 >>> 6) | (i0300 >>> 7) | (i0020 <<  1);
         r1000 = (i0030 <<  2) | (i1000 >>> 4) | (i0400 >>> 5) | (i0100 >>> 6);

         r0800 = (i0c00 >>> 3) | (i0300 >>> 4) | (ic000 >>>13) | (i0080 >>> 7);
         r0400 = (i00c0 >>> 6) | (i0400 >>> 2) | (i0100 >>> 3) | (i4000 >>>12);
         r     = (i0300 >>> 1) | (ic000 >>>10) | (i3000 >>>11) | (i0080 >>> 4) | (i0020 >>> 5);
         r0200a = (Rule[r] ? 0x0220:0);
         r0200b = (Rule[r | 64] ? 0x0220:0);
         r     = (i00c0 >>> 3) | (i0030 >>> 4) |  i0100        | (i4000 >>> 9) | (i1000 >>>10);
         r0100a = (Rule[r] ? 0x0110:0);
         r0100b = (Rule[r | 64] ? 0x0110:0);
         r0100c = (Rule[r | 128] ? 0x0110:0);
         r0100d = (Rule[r | 192] ? 0x0110:0);

         r0080 = (i00c0 <<  1) |  i0030        | (i8000 >>> 9) | (i2000 >>>10) | (i0800 >>>11);
         r     = (ic000 >>> 8) | (i3000 >>> 9) | (i0c00 >>>10) | (i0040 <<  2) | (i0010 <<  1);
         r0040a = (Rule[r] ? 0x0440:0);
         r0040b = (Rule[r | 4] ? 0x0440:0);
         r0020 = (i0030 <<  3) | (i2000 >>> 7) | (i0800 >>> 8) | (i0200 >>> 9);
         r     = (i3000 >>> 6) | (i0c00 >>> 7) | (i0300 >>> 8) | (i0010 <<  4);
         r0010a = (Rule[r] ? 0x0110:0);
         r0010b = (Rule[r | 4] ? 0x0110:0);
         r0010c = (Rule[r | 32] ? 0x0110:0);
         r0010d = (Rule[r | 36] ? 0x0110:0);

         r0008 = (i00c0 >>> 5) | (i0800 >>> 5) | (i0200 >>> 6) | (i8000 >>>15);
         r     = (i0c00 >>> 4) | (i0300 >>> 5) | (ic000 >>>14) | (i0040 >>> 4);
         r0004a = (Rule[r] ? 0x4004:0);
         r0004b = (Rule[r | 32] ? 0x4004:0);
         r0004c = (Rule[r | 256] ? 0x4004:0);
         r0004d = (Rule[r | 288] ? 0x4004:0);
         r     = (i00c0 >>> 2) | (i0030 >>> 3) | (i0200 >>> 3) | (i8000 >>>12) | (i2000 >>>13);
         r0002a = (Rule[r] ? 0x2002:0);
         r0002b = (Rule[r | 128] ? 0x2002:0);
         r0002c = (Rule[r | 256] ? 0x2002:0);
         r0002d = (Rule[r | 384] ? 0x2002:0);
         r     = (i0300 >>> 2) | (ic000 >>>11) | (i3000 >>>12) | (i0040 >>> 1) | (i0010 >>> 2);
         r0001a = (Rule[r] ? 0x1001:0);
         r0001b = (Rule[r | 256] ? 0x1001:0);

         r8000a = (Rule[r8000] ? 0x8008:0);
         r4000a = (Rule[r4000] ? 0x4004:0);
         r2000a = (Rule[r2000] ? 0x2002:0);
         r2000b = (Rule[r2000 | 1] ? 0x2002:0);
         r0800a = (Rule[r0800] ? 0x0880:0);
         r0800b = (Rule[r0800 | 8] ? 0x0880:0);
         r0080a = (Rule[r0080] ? 0x0880:0);

         // 0
         m1 = r8000a | r4000a | r2000a | (Rule[r1000] ? 0x1001:0)
            | r0800a | (Rule[r0400] ? 0x0440:0) | r0200a | r0100a;
         m2 = r0080a | r0040a | (Rule[r0020] ? 0x0220:0) | r0010a
            | (Rule[r0008] ? 0x8008:0) | r0004a | r0002a | r0001a;
         crunch[i] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 1:  i0001 = 1,  i0003 = 1
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 1] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 8] ? 0x0440:0) | r0200a | r0100b;
         m2 = r0080a | r0040a | (Rule[r0020 | 2] ? 0x0220:0) | r0010b
            | (Rule[r0008 | 16] ? 0x8008:0) | r0004b | r0002b | r0001b;
         crunch[i+1] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+1]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 2:  i0002 = 2,  i0003 = 2
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 2] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 16] ? 0x0440:0) | r0200b | r0100c;
         m2 = r0080a | r0040a | (Rule[r0020 | 4] ? 0x0220:0) | r0010a
            | (Rule[r0008 | 32] ? 0x8008:0) | r0004a | r0002c | r0001a;
         crunch[i+2] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+2]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 3:  i0001 = 1,  i0002 = 2,  i0003 = 3
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 3] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 24] ? 0x0440:0) | r0200b | r0100d;
         m2 = r0080a | r0040a | (Rule[r0020 | 6] ? 0x0220:0) | r0010b
            | (Rule[r0008 | 48] ? 0x8008:0) | r0004b | r0002d | r0001b;
         crunch[i+3] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+3]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         r4000a = (Rule[r4000 | 1] ? 0x4004:0);
         r0080a = (Rule[r0080 | 2] ? 0x0880:0);

         // 4:  i0004 = 4,  i000c = 4
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 8] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 64] ? 0x0440:0) | r0200a | r0100a;
         m2 = r0080a | r0040b | (Rule[r0020 | 16] ? 0x0220:0) | r0010c
            | (Rule[r0008 | 128] ? 0x8008:0) | r0004c | r0002a | r0001a;
         crunch[i+4] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+4]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 5:  i0001 = 1,  i0003 = 1,  i0004 = 4, i000c = 4
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 9] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 72] ? 0x0440:0) | r0200a | r0100b;
         m2 = r0080a | r0040b | (Rule[r0020 | 18] ? 0x0220:0) | r0010d
            | (Rule[r0008 | 144] ? 0x8008:0) | r0004d | r0002b | r0001b;
         crunch[i+5] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+5]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 6:  i0002 = 2,  i0003 = 2,  i0004 = 4,  i000c = 4
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 10] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 80] ? 0x0440:0) | r0200b | r0100c;
         m2 = r0080a | r0040b | (Rule[r0020 | 20] ? 0x0220:0) | r0010c
            | (Rule[r0008 | 160] ? 0x8008:0) | r0004c | r0002c | r0001a;
         crunch[i+6] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+6]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 7:  i0001 = 1,  i0002 = 2, i0003 = 3,  i0004 = 4. i000c = 4
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 11] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 88] ? 0x0440:0) | r0200b | r0100d;
         m2 = r0080a | r0040b | (Rule[r0020 | 22] ? 0x0220:0) | r0010d
            | (Rule[r0008 | 176] ? 0x8008:0) | r0004d | r0002d | r0001b;
         crunch[i+7] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+7]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         r8000a = (Rule[r8000 | 1] ? 0x8008:0);
         r4000a = (Rule[r4000 | 2] ? 0x4004:0);
         r2000a = (Rule[r2000 | 8] ? 0x2002:0);
         r2000b = (Rule[r2000 | 9] ? 0x2002:0);
         r0800a = (Rule[r0800 | 64] ? 0x0880:0);
         r0800b = (Rule[r0800 | 72] ? 0x0880:0);
         r0080a = (Rule[r0080 | 4] ? 0x0880:0);

         // 8:  i0008 = 8,  i000c = 8
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 16] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 128] ? 0x0440:0) | r0200a | r0100a;
         m2 = r0080a | r0040a | (Rule[r0020 | 32] ? 0x0220:0) | r0010a
            | (Rule[r0008 | 256] ? 0x8008:0) | r0004a | r0002a | r0001a;
         crunch[i+8] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+8]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 9:  i0001 = 1,  i0003 = 1,  i0008 = 8,  i000c = 8
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 17] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 136] ? 0x0440:0) | r0200a | r0100b;
         m2 = r0080a | r0040a | (Rule[r0020 | 34] ? 0x0220:0) | r0010b
            | (Rule[r0008 | 272] ? 0x8008:0) | r0004b | r0002b | r0001b;
         crunch[i+9] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+9]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 10:  i0002 = 2,  i0003 = 2,  i0008 = 8,  i000c = 8
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 18] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 144] ? 0x0440:0) | r0200b | r0100c;
         m2 = r0080a | r0040a | (Rule[r0020 | 36] ? 0x0220:0) | r0010a
            | (Rule[r0008 | 288] ? 0x8008:0) | r0004a | r0002c | r0001a;
         crunch[i+10] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+10]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 11:  i0001 = 1,  i0003 = 1,  i0008 = 8,  i000c = 8
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 19] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 152] ? 0x0440:0) | r0200b | r0100d;
         m2 = r0080a | r0040a | (Rule[r0020 | 38] ? 0x0220:0) | r0010b
            | (Rule[r0008 | 304] ? 0x8008:0) | r0004b | r0002d | r0001b;
         crunch[i+11] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+11]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         r4000a = (Rule[r4000 | 3] ? 0x4004:0);
         r0080a = (Rule[r0080 | 6] ? 0x0880:0);

         // 12:  i0004 = 4,  i0008 = 8,  i000c = 12
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 24] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 192] ? 0x0440:0) | r0200a | r0100a;
         m2 = r0080a | r0040b | (Rule[r0020 | 48] ? 0x0220:0) | r0010c
            | (Rule[r0008 | 384] ? 0x8008:0) | r0004c | r0002a | r0001a;
         crunch[i+12] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+12]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 13:  i0001 = 1,  i0003 = 1,  i0004 = 4,  i0008 = 8,  i000c = 12
         m1 = r8000a | r4000a | r2000a | (Rule[r1000 | 25] ? 0x1001:0)
            | r0800a | (Rule[r0400 | 200] ? 0x0440:0) | r0200a | r0100b;
         m2 = r0080a | r0040b | (Rule[r0020 | 50] ? 0x0220:0) | r0010d
            | (Rule[r0008 | 400] ? 0x8008:0) | r0004d | r0002b | r0001b;
         crunch[i+13] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+13]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 14:  i0002 = 2,  i0003 = 2,  i0004 = 4,  i0008 = 8,  i000c = 12
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 26] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 208] ? 0x0440:0) | r0200b | r0100c;
         m2 = r0080a | r0040b | (Rule[r0020 | 52] ? 0x0220:0) | r0010c
            | (Rule[r0008 | 416] ? 0x8008:0) | r0004c | r0002c | r0001a;
         crunch[i+14] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+14]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

         // 15:  i0001 = 1,  i0002 = 2,  i0003 = 3,
         //      i0004 = 4,  i0008 = 8,  i000c = 12
         m1 = r8000a | r4000a | r2000b | (Rule[r1000 | 27] ? 0x1001:0)
            | r0800b | (Rule[r0400 | 216] ? 0x0440:0) | r0200b | r0100d;
         m2 = r0080a | r0040b | (Rule[r0020 | 54] ? 0x0220:0) | r0010d
            | (Rule[r0008 | 432] ? 0x8008:0) | r0004d | r0002d | r0001b;
         crunch[i+15] = (short)((m1 & 0xff00) | (m2 & 0x00ff));
         munch[i+15]  = (short)((m1 & 0x00ff) | (m2 & 0xff00));

      }
      rattleAllCages();
   }

   public void removeFromDisplay(LifeCell c)
   {
      if ((c.flags & 1)==0) return;

      if (c.DisplayPrev != null) c.DisplayPrev.DisplayNext = c.DisplayNext;
      else display=c.DisplayNext;
      if (c.DisplayNext != null) c.DisplayNext.DisplayPrev = c.DisplayPrev;
      c.DisplayNext=null;
      c.DisplayPrev=null;

      c.flags &= 0xfffe;
   }

   private void addToDisplay(LifeCell c)
   {
      if ((c.flags & 1) != 0) return;

      if (display!=null) display.DisplayPrev=c;
      c.DisplayNext=display;
      display=c;

      c.flags |= 1;
   }


   public void freshenView()
   {
      for (LifeCell c=living; c!=null; c=c.Next) addToDisplay(c);
      for (LifeCell c=hibernating; c!=null; c=c.Next) addToDisplay(c);
   }

   /*
    * Clear()
    *
    * Clear the universe
    */
   public void clear()
   {
      living=display=morgue=hibernating=caretaker=null;
      hashTable = new LifeHash();
      qCycle=false;
      last_gencount=gencount=countdown_gen=0;
      predicted_blap_interval = 0;
      last_blap_interval = interval/2;
      last_timestamp=0;
   }

   public boolean isEmpty()
   {
      if (living==null && hibernating==null) return true;
      return false;
   }

   public void setRefresh(int millis)
   {
      interval=millis;
   }

   public void setSpeed(int gensPerBlap)
   {
      speed=gensPerBlap;
   }

   private boolean isItTimeToDisplay()
   {
      timenow=System.currentTimeMillis();

      if (gencount==last_gencount) return false;

      // Execute rule that when running full speed, the number
      // of skipped generations from one frame refresh to the
      // next should not differ by more than 10%.
      if (speed == 0 && last_gen_interval > 0)
      {
         long leeway = last_gen_interval/10;   // 10%
         if (leeway == 0) leeway=1;  // always allow a difference of 1
         if (gencount-last_gencount <
             last_gen_interval - leeway) return false;
         if (gencount-last_gencount >
             last_gen_interval + leeway)
         {
            last_gen_interval = gencount-last_gencount;
            last_gencount=gencount;   // blap!
            return true;
         }
      }

      // Check the time!
      // Can we do another generation before displaying?
      // Yes: return false;

      long time_past = timenow-last_timestamp;
      long predicted_blap_interval1000=predicted_blap_interval/1000;

      if (speed>0)
      {
         // skip at least the requested number of generations
         if (gencount-last_gencount < speed) return false;

         long time2sleep = interval - predicted_blap_interval1000
            - ((time_past/(gencount-last_gencount))/2);

         if (time2sleep > 0)
         {
            try
            {
               Thread.currentThread().sleep(time2sleep);
            }catch (InterruptedException ie)
            {}
         }
         timenow=System.currentTimeMillis();
         time_past = timenow-last_timestamp;

         last_gen_interval = gencount-last_gencount;
         last_gencount=gencount;   // blap!
         return true;
         /* old method, which made fps inaccurate on Wintel machines
            while (time_past+predicted_blap_interval1000
            +((time_past/(gencount-last_gencount))/2)
            < interval)
            {
            try
            {   Thread.currentThread().sleep(5);
            }catch (InterruptedException ie)
            {}
            timenow=System.currentTimeMillis();
            time_past = timenow-last_timestamp;
            }
          */
      }
      else
      {
         // enforce the rule that you can't spend more than 80%
         // of the time in the display code:
         if (time_past*5 < predicted_blap_interval1000) return false;
      }

      if (time_past+predicted_blap_interval1000
            +((time_past/(gencount-last_gencount))/2)
            >= interval)
      {
         last_gen_interval = gencount-last_gencount;
         last_gencount=gencount;   // blap!
         return true;
      }

      return false;  // no blap
   }

   private void markTime(boolean break4blap)
   {
      // record the most recent experimental length of time
      // that it takes to display the universe in the applet window:
      last_timestamp=System.currentTimeMillis();

      if (break4blap)
      {
         if (timenow!=0) last_blap_interval=last_timestamp-timenow;

         predicted_blap_interval =
            (predicted_blap_interval*7 + last_blap_interval*1000)/8;
         // weighted average to smooth out the bumps
         // because Windows-95 platform reports milliseconds
         // in bunches of 50 and 60 (by the 18.2 ms clock)
      }
   }


   private void rattleAllCages()
   {
      for (LifeCell c=living; c!=null; c=c.Next) c.pstate=c.qstate=0;
      while (morgue!=null) rattleCage(morgue);
      while (hibernating!=null) rattleCage(hibernating);
   }

   private void stepBack()
   {
      if (backCorrect)
      {
         qCycle=!qCycle;
         gencount--;

         rattleAllCages();
      }
      backCorrect=false;
   }

   /*
    * Generate does numgens generations on the current life universe,
    * and returns the results.  If numgens==0, it goes forever; a value
    * of -1 is also valid if the previous generation has been kept.
    *
    * The changes to the universe are stored in the LifeField object.
    *
    * The return values are as follows:
    * true = just a pit-stop to display
    * false = really stop
    */
   public boolean generate(int numgens, boolean break4blap)
   {
      if (numgens== -1)
      {
         stepBack();
         goFlag=false;
         return false;
      }

      if (!break4blap)
      {
         countdown_gen = numgens;
         last_gen_interval = 0;
      }
      markTime(break4blap);

      while (goFlag)
      {
         if ((gencount & 0x7f) == 0) incinerateCages(true);  // delayed deallocation

         if (qCycle)
         {
            try {generate_q();}
            catch (OutOfMemoryError e_q)  // if out of memory
            {
               goFlag=false;
               stepBack();
               throw new OutOfMemoryError("Generation may not be correct");
            }
            qCycle = false;              // going into p cycle
         }
         else
         {
            try {generate_p();}
            catch (OutOfMemoryError e_p)
            {
               goFlag=false;
               stepBack();
               throw new OutOfMemoryError("Generation may not be correct");
            }
            qCycle = true;
         }

         if (numgens!=0)
         {
            countdown_gen--;
            if (countdown_gen==0)
            {
               goFlag=false;
               return false;
            }
         }
         if (isItTimeToDisplay()) return true;
      }
      return false;
   }
}
