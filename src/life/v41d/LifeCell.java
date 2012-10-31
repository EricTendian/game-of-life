package life.v41d;

/**************************************************************
 LifeCell.java

 (c) 1996-2001 by Alan Hensel. All Rights Reserved.
**************************************************************/
    /*
        group of cells (lowest level):

         1 2
         3 4   = 2x2 cells

        group of 2x2's:

         1 3
         2 4   = 4x4 cells  (stored in 16-bit short)

        group of 4x4's:

         1 2
         3 4   = 8x8 cells

        group of 8x8's:

         1 3
         2 4   = 16x16 cells  (stored in LifeCell class)


        So the bit-to-cell mapping goes like this:

                                 |
         0  1  8  9  10 11 18 19 | 80 81 88 89  90 91 98 99
         2  3  a  b  12 13 1a 1b | 82 83 8a 8b  92 93 9a 9b
         4  5  c  d  14 15 1c 1d | 84 85 8c 8d  94 95 9c 9d
         6  7  e  f  16 17 1e 1f | 86 87 8e 8f  96 97 9e 9f
                                 |
        20 21 28 29  30 31 38 39 | a0 a1 a8 a9  b0 b1 b8 b9
        22 23 2a 2b  32 33 3a 3b | a2 a3 aa ab  b2 b3 ba bb
        24 25 2c 2d  34 35 3c 3d | a4 a5 ac ad  b4 b5 bc bd
        26 27 2e 2f  36 37 3e 3f | a6 a7 ae af  b6 b7 be bf
       __________________________|__________________________
                                 |
        40 41 48 49  50 51 58 59 | c0 c1 c8 c9  d0 d1 d8 d9
        42 43 4a 4b  52 53 5a 5b | c2 c3 ca cb  d2 d3 da db
        44 45 4c 4d  54 55 5c 5d | c4 c5 cc cd  d4 d5 dc dd
        46 47 4e 4f  56 57 5e 5f | c6 c7 ce cf  d6 d7 de df
                                 |
        60 61 68 69  70 71 78 79 | e0 e1 e8 e9  f0 f1 f8 f9
        62 63 6a 6b  72 73 7a 7b | e2 e3 ea eb  f2 f3 fa fb
        64 65 6c 6d  74 75 7c 7d | e4 e5 ec ed  f4 f5 fc fd
        66 67 6e 6f  76 77 7e 7f | e6 e7 ee ef  f6 f7 fe ff
                                 |

        Each of these 16x16 blocks has an associated x,y
        coordinate in the universe, which are also stored as
        16-bit shorts.  Thus the total life universe size is
        2^20 x 2^20, or just over a million by a million.

        In the p cycle, real coordinates are 16x, 16y.
        In the q cycle, real coordinates are 16x+1, 16y+1.

        This staggering occurs because the algorithm calls for
        calculating 4 cells at a time by table lookup on the 4x4
        block surrounding it.  It is simply easier to stagger the
        cycles than it is to bit-shift all the time.

        p -> q:  Use p's S, E, SE blocks;
                 Affect q's S, E, SE blocks.

        q -> p:  Use q's N, W, NW blocks;
                 Affect p's N, W, NW blocks.
    */

class LifeCell
{
   short p[] = new short[16];
   LifeCell S, E, SE;

   short q[] = new short[16];
   LifeCell N, W, NW;

   short x,y;

   LifeCell Next, Prev;         /* Doubly linked */
   LifeCell DisplayNext, DisplayPrev;
   LifeCell Down;

   int pstate, qstate;
   //  bitmap:
   // |31 30 29 28.27 26 25 24|23 22 21 20.19 18 17 16|15 14 13 12.11 10  9  8| 7  6  5  4. 3  2  1  0|
   // | Northwest quadrant    | Southwest quadrant    | Northeast quadrant    | Southeast quadrant    |
   // | morgue    | hiber     | morgue    | hiber     | morgue    | hiber     | morgue    | hiber     |
   //
   // each group of 4 bits:
   // Whole 8x8 | Vertical 2x8 | Horizontal 8x2 | Corner 2x2

   int flags;
   //  bitmap:
   //  15 14 13 12.11 10  9  8. 7  6  5  4. 3  2  1  0
   //   |  |  |  |  |     |  |              |  |  |  Display (1=in)
   //   |  |  |  |  |     |  |              |  |  Morgue (1=in)
   //   |  |  |  |  |     |  |              |  Hibernation (1=in)
   //   |  |  |  |  |                       Morgue & displayed (incineratable)
   //   |  |  |  |  the Rattling bit
   //   |  |  |  q in hibernation
   //   |  |  q in morgue
   //   |  p in hibernation
   //   p in morgue
}
