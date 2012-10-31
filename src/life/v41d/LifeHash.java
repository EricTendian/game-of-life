package life.v41d;

/**************************************************************
 LifeHash.java

 This class keeps a fast-lookup table of LifeCell blocks.

  (c) Alan Hensel, Apr 1996. All Rights Reserved.
**************************************************************/

class LifeHash
{
   private LifeCell[][] hashTable;
   private short keyX, keyY;
   private static int HASHSIZE=6;

   LifeHash()
   {
      hashTable = new LifeCell[1<<HASHSIZE][1<<HASHSIZE];
   }

   private void makeKeys(short x, short y)
   {
      keyX = (short)(x & ((1<<HASHSIZE)-1));
      keyY = (short)(y & ((1<<HASHSIZE)-1));
   }

   public void store(LifeCell c)
   {
      makeKeys(c.x, c.y);
      c.Down = hashTable[keyX][keyY];
      hashTable[keyX][keyY] = c;
   }

   public LifeCell retrieve(int x, int y)
   {
      short sx=(short)(x>>4), sy=(short)(y>>4);
      LifeCell oldc;

      makeKeys(sx,sy);
      oldc = hashTable[keyX][keyY];
      while (oldc != null && (oldc.x != sx || oldc.y != sy))
         oldc = oldc.Down;

      return oldc;
   }

   public LifeCell retrieve(LifeCoordinate cor)
   {
      return retrieve(cor.x, cor.y);
   }

   public void delete(LifeCell c)
   {
      LifeCell cur, prev=null;

      makeKeys(c.x, c.y);
      cur=hashTable[keyX][keyY];

      while (cur!=null && (cur.x!=c.x || cur.y!=c.y))
      {
         prev=cur;
         cur=cur.Down;
      }

      if (cur!=null)
      {
         if (prev!=null) prev.Down=cur.Down;
         else hashTable[keyX][keyY]=cur.Down;
         cur.Down=null;
      }
   }
}
