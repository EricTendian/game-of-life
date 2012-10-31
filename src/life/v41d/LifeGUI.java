package life.v41d;

/**************************************************************
 LifeGUI.java

 (c) 1996-2001 by Alan Hensel. All Rights Reserved.
**************************************************************/

import java.awt.*;

class LifeGUI extends Panel
{
   private int fieldWidth, fieldHeight;
   private int fieldSizeX, fieldSizeY;
   private int cellMagnitude, pixelsPerCell, ppc, ppc2;
   private boolean specialCase;  // for awful bug, Netscape 4.05 and lower
   private Color fgColor = new Color(0, 0, 160);
   private Color bgColor = Color.lightGray;
   private Color gridColor = Color.gray;
   private boolean grids;
   private int toolbarHeight;
   public int oX=1, oY=1;   // applet coordinates at upper left of the viewing window
   public int xOrig, yOrig;   // universe location at the upper left of the viewing window
   private Graphics g;
   private boolean viewChanged=true;
   private int[] xTranslate, yTranslate;
   private boolean boundsCheck=true;

   // -----------------------------------------------------------------------
   // All the stuff between these lines used to belong to the class StateTable,
   // but it was inlined for performance reasons. (8%)
   private boolean[][] field;
   private boolean[] column;

   public boolean getState(int x, int y)
   {
      if (x>=0 && y>=0 && x<fieldSizeX && y<fieldSizeY) return field[x][y];
      return false;
   }

   public void toggleState(int x, int y)
   {
      if (x>=0 && y>=0 && x<fieldSizeX && y<fieldSizeY) field[x][y]^=true;
   }

   public void setState(int x, int y, boolean state)
   {
      if (x>=0 && y>=0 && x<fieldSizeX && y<fieldSizeY) field[x][y]=state;
   }
   // -----------------------------------------------------------------------

   LifeGUI(int fieldX, int fieldY, int cellMag, int tbh, boolean g)
   {
      fieldWidth = fieldX;
      fieldHeight = fieldY;
      cellMagnitude = cellMag;
      grids = g;
      toolbarHeight = tbh;

      moveTo(0,0);
   }

   public void moveTo(int centerX, int centerY)
   {
      buildNewField();

      xOrig = centerX - (fieldSizeX/2);
      yOrig = centerY - (fieldSizeY/2);
   }

   public int getCenterX()
   {
      return xOrig+(fieldSizeX/2);
   }

   public int getCenterY()
   {
      return yOrig+(fieldSizeY/2);
   }

   private void buildNewField()
   {
      pixelsPerCell=1<<cellMagnitude;

      int newSizeX = fieldWidth/pixelsPerCell;
      int newSizeY = fieldHeight/pixelsPerCell;

      boolean[][] newField = new boolean[newSizeX][newSizeY];

      if (field != null)
      {
         xOrig += (fieldSizeX - newSizeX)/2;
         yOrig += (fieldSizeY - newSizeY)/2;
      }

      field = newField;
      fieldSizeX = newSizeX;
      fieldSizeY = newSizeY;

      ppc = pixelsPerCell - ((cellMagnitude > 1 && grids)? 1:0);
      if (!LifeButton.clearRectBroken)
      {
         ppc2 = ppc;  // normal
         specialCase=false;
      }
      else
      {
         ppc2 = ppc-1;  // for Netscapes 4.05 and below
         if (ppc2==0) specialCase=true;
         else specialCase=false;
      }
      createTranslationTables();
   }

   private void createTranslationTables()
   {
      xTranslate=new int[fieldSizeX];
      for (int i=0; i<fieldSizeX; i++)
         xTranslate[i] = i*pixelsPerCell+oX;

      yTranslate=new int[fieldSizeY];
      for (int j=0; j<fieldSizeY; j++)
         yTranslate[j] = j*pixelsPerCell+oY;
   }

   public Color newColor(String sc)
   {
      Color c=null;

      if (sc!=null)
      {
         try {
            c=new Color(Integer.parseInt(sc, 16));
         }catch(NumberFormatException e) {c=null;}
      }
      return c;
   }

   public void setColors(String fg, String bg, String grid)
   {
      Color c;

      c = newColor(fg);
      if (c!=null) fgColor = c;

      c = newColor(bg);
      if (c!=null) bgColor = c;

      c = newColor(grid);
      if (c!=null) gridColor = c;
   }

   public boolean setZoom(int level)
   {
      if (cellMagnitude==level) return false;
      cellMagnitude = level;

      buildNewField();

      viewChanged = true;
      return true;
   }

   public int getZoom() {
      return cellMagnitude;
   }

   public int getCellsAcross() {
      return fieldSizeX;
   }

   public int getCellsDown() {
      return fieldSizeY;
   }

   public int getPixelsPerCell() {
      return pixelsPerCell;
   }

   public int convertAppletToFieldX(int x) {
      return (x-oX)>>cellMagnitude;
   }

   public int convertAppletToFieldY(int y) {
      return (y-oY-toolbarHeight)>>cellMagnitude;
   }

   public int convertFieldToUniverseX(int x) {
      return x + xOrig;
   }

   public int convertFieldToUniverseY(int y) {
      return y + yOrig;
   }

   public void setGraphicsContext(Graphics gr) {
      g=gr;
      g.setColor(fgColor);
      g.setXORMode(bgColor);
   }

   public void drawCell(int x, int y) {
      drawCell(x,y, getState(x,y));
   }

   public void drawCell(int x, int y, boolean state)
   {

      /* This is the less efficient solution, which is not broken
         in Netscape 3.0.
         if (state) g.setColor(fgColor);
         else      g.setColor(bgColor);

         g.fillRect(xTranslate[x], yTranslate[y], ppc, ppc);
       */

      // This is the more efficient solution, which is unfortunately
      // broken in Netscape 3.0.
      if (state) g.fillRect(xTranslate[x], yTranslate[y], ppc, ppc);
      else
      {
         if (specialCase) {
            g.setColor(bgColor);
            g.fillRect(xTranslate[x], yTranslate[y], 1, 1);
            g.setColor(fgColor);
         }
         else {
            g.clearRect(xTranslate[x], yTranslate[y], ppc2, ppc2);
         }
      }
   }

   public void drawGrid()
   {
      int origX = oX-1, origY = oY-1;

      // no xor mode for the grid
      g.setPaintMode();

      /* This is the less efficient solution, which is not broken
         in Netscape 3.0.
         g.setColor(bgColor);
         g.fillRect(oX, oY, fieldWidth+oX, fieldHeight+oY);
       */

      // This is the solution which is guaranteed to agree with
      // the more efficient solution in drawCell().
      g.clearRect(oX, oY, fieldWidth+oX, fieldHeight+oY);

      g.setColor(gridColor);

      if (cellMagnitude>1 && grids)
      {
         for (int i=0; i<=fieldSizeY; i++) {
            g.drawLine(origX, i*pixelsPerCell+origY,
                  fieldWidth+origX, i*pixelsPerCell+origY);
         }
         for (int i=0; i<=fieldSizeX; i++) {
            g.drawLine(i*pixelsPerCell+origX, origY,
                  i*pixelsPerCell+origX, fieldHeight+origY);
         }
      }
      else   // cells are too small for a full grid:
      {
         int rightX = fieldWidth+oX;
         int bottomY = fieldHeight+oY;

         g.drawLine(origX, origY, rightX, origY);
         g.drawLine(origX, bottomY, rightX, bottomY);
         g.drawLine(origX, origY, origX, bottomY);
         g.drawLine(rightX, origY, rightX, bottomY);
      }
      g.setColor(fgColor);
      g.setXORMode(bgColor);
   }

   public boolean cellState(int x, int y) {
      return getState(x,y);
   }

   public void changeCell(int x, int y) {
      toggleState(x,y);
   }

   public void changeCell(int x, int y, boolean state) {
      setState(x,y,state);
   }

   public void clear() {
      field = new boolean[fieldSizeX][fieldSizeY];
      createTranslationTables();
   }

   private void updateCell(int x, int y, boolean state)
   {
      if (boundsCheck) {
         if (x<0 || x>=fieldSizeX) return;
         column = field[x];
         if (y<0 || y>=fieldSizeY) return;
      }
      else {
         column = field[x];
      }

      if (column[y]!=state) {
         column[y]=state;
         g.fillRect(xTranslate[x], yTranslate[y], ppc, ppc);
      }
   }

   private void updateCel(int x, int y, boolean state)
   {
      if (boundsCheck) {
         if (x<0 || x>=fieldSizeX || y<0 || y>=fieldSizeY) return;
      }

      if (column[y]!=state) {
         column[y]=state;
         g.fillRect(xTranslate[x], yTranslate[y], ppc, ppc);
      }
   }

   private boolean display_p(LifeGen u)
   {
      // check only those with "display" bit set; skip the rest.
      // display only those quadrants with hibernation and morgue
      // bits = 0.
      // traverse both the living & hibernating lists.
      // return false if couldn't complete the generation.

      short c,d,e,f;
      LifeCell nextc;
      int i;
      int j0,j1,j2,j3,j4,j5,j6,j7,j8,j9,j10,j11,j12,j13,j14,j15;


      for (LifeCell cell=u.display; cell != null; cell=nextc)
      {
         nextc=cell.DisplayNext;

         if ((cell.flags & 0x02)!=0)
         {
            u.removeFromDisplay(cell);
         }

         i  = cell.x*16 - xOrig;
         j0 = cell.y*16 - yOrig;

         if (i +16 < 0 || i +1 > fieldSizeX 
               || j0+16 < 0 || j0+1 > fieldSizeY)
         {
            u.removeFromDisplay(cell);
         }
         else
         {
            j1=j0+1;   j2=j1+1;   j3=j2+1;
            j4=j3+1;   j5=j4+1;   j6=j5+1;   j7=j6+1;
            j8=j7+1;   j9=j8+1;   j10=j9+1;  j11=j10+1;
            j12=j11+1; j13=j12+1; j14=j13+1; j15=j14+1;

            if (i >=0 && i+15<fieldSizeX
             && j0>=0 && j15<fieldSizeY)
               boundsCheck=false;
            else boundsCheck=true;

            c=cell.p[0]; d=cell.p[2]; e=cell.p[4]; f=cell.p[6];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell( i, j0,((c&0x0080)!=0));
            updateCel( i, j1,((c&0x0020)!=0));
            updateCel( i, j2,((c&0x0008)!=0));
            updateCel( i, j3,((c&0x0002)!=0));
            updateCel( i, j4,((d&0x0080)!=0));
            updateCel( i, j5,((d&0x0020)!=0));
            updateCel( i, j6,((d&0x0008)!=0));
            updateCel( i, j7,((d&0x0002)!=0));
            updateCel( i, j8,((e&0x0080)!=0));
            updateCel( i, j9,((e&0x0020)!=0));
            updateCel( i,j10,((e&0x0008)!=0));
            updateCel( i,j11,((e&0x0002)!=0));
            updateCel( i,j12,((f&0x0080)!=0));
            updateCel( i,j13,((f&0x0020)!=0));
            updateCel( i,j14,((f&0x0008)!=0));
            updateCel( i,j15,((f&0x0002)!=0));

            i++;
            updateCell( i, j0,((c&0x0040)!=0));
            updateCel( i, j1,((c&0x0010)!=0));
            updateCel( i, j2,((c&0x0004)!=0));
            updateCel( i, j3,((c&0x0001)!=0));
            updateCel( i, j4,((d&0x0040)!=0));
            updateCel( i, j5,((d&0x0010)!=0));
            updateCel( i, j6,((d&0x0004)!=0));
            updateCel( i, j7,((d&0x0001)!=0));
            updateCel( i, j8,((e&0x0040)!=0));
            updateCel( i, j9,((e&0x0010)!=0));
            updateCel( i,j10,((e&0x0004)!=0));
            updateCel( i,j11,((e&0x0001)!=0));
            updateCel( i,j12,((f&0x0040)!=0));
            updateCel( i,j13,((f&0x0010)!=0));
            updateCel( i,j14,((f&0x0004)!=0));
            updateCel( i,j15,((f&0x0001)!=0));

            i++;
            c=cell.p[1]; d=cell.p[3]; e=cell.p[5]; f=cell.p[7];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell( i, j0,((c&0x0080)!=0));
            updateCel( i, j1,((c&0x0020)!=0));
            updateCel( i, j2,((c&0x0008)!=0));
            updateCel( i, j3,((c&0x0002)!=0));
            updateCel( i, j4,((d&0x0080)!=0));
            updateCel( i, j5,((d&0x0020)!=0));
            updateCel( i, j6,((d&0x0008)!=0));
            updateCel( i, j7,((d&0x0002)!=0));
            updateCel( i, j8,((e&0x0080)!=0));
            updateCel( i, j9,((e&0x0020)!=0));
            updateCel( i,j10,((e&0x0008)!=0));
            updateCel( i,j11,((e&0x0002)!=0));
            updateCel( i,j12,((f&0x0080)!=0));
            updateCel( i,j13,((f&0x0020)!=0));
            updateCel( i,j14,((f&0x0008)!=0));
            updateCel( i,j15,((f&0x0002)!=0));

            i++;
            updateCell( i, j0,((c&0x0040)!=0));
            updateCel( i, j1,((c&0x0010)!=0));
            updateCel( i, j2,((c&0x0004)!=0));
            updateCel( i, j3,((c&0x0001)!=0));
            updateCel( i, j4,((d&0x0040)!=0));
            updateCel( i, j5,((d&0x0010)!=0));
            updateCel( i, j6,((d&0x0004)!=0));
            updateCel( i, j7,((d&0x0001)!=0));
            updateCel( i, j8,((e&0x0040)!=0));
            updateCel( i, j9,((e&0x0010)!=0));
            updateCel( i,j10,((e&0x0004)!=0));
            updateCel( i,j11,((e&0x0001)!=0));
            updateCel( i,j12,((f&0x0040)!=0));
            updateCel( i,j13,((f&0x0010)!=0));
            updateCel( i,j14,((f&0x0004)!=0));
            updateCel( i,j15,((f&0x0001)!=0));

            i++;
            c=cell.p[8]; d=cell.p[10]; e=cell.p[12]; f=cell.p[14];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell(i, j0,((c&0x0080)!=0));
            updateCel(i, j1,((c&0x0020)!=0));
            updateCel(i, j2,((c&0x0008)!=0));
            updateCel(i, j3,((c&0x0002)!=0));
            updateCel(i, j4,((d&0x0080)!=0));
            updateCel(i, j5,((d&0x0020)!=0));
            updateCel(i, j6,((d&0x0008)!=0));
            updateCel(i, j7,((d&0x0002)!=0));
            updateCel(i, j8,((e&0x0080)!=0));
            updateCel(i, j9,((e&0x0020)!=0));
            updateCel(i,j10,((e&0x0008)!=0));
            updateCel(i,j11,((e&0x0002)!=0));
            updateCel(i,j12,((f&0x0080)!=0));
            updateCel(i,j13,((f&0x0020)!=0));
            updateCel(i,j14,((f&0x0008)!=0));
            updateCel(i,j15,((f&0x0002)!=0));

            i++;
            updateCell(i, j0,((c&0x0040)!=0));
            updateCel(i, j1,((c&0x0010)!=0));
            updateCel(i, j2,((c&0x0004)!=0));
            updateCel(i, j3,((c&0x0001)!=0));
            updateCel(i, j4,((d&0x0040)!=0));
            updateCel(i, j5,((d&0x0010)!=0));
            updateCel(i, j6,((d&0x0004)!=0));
            updateCel(i, j7,((d&0x0001)!=0));
            updateCel(i, j8,((e&0x0040)!=0));
            updateCel(i, j9,((e&0x0010)!=0));
            updateCel(i,j10,((e&0x0004)!=0));
            updateCel(i,j11,((e&0x0001)!=0));
            updateCel(i,j12,((f&0x0040)!=0));
            updateCel(i,j13,((f&0x0010)!=0));
            updateCel(i,j14,((f&0x0004)!=0));
            updateCel(i,j15,((f&0x0001)!=0));

            i++;
            c=cell.p[9]; d=cell.p[11]; e=cell.p[13]; f=cell.p[15];
            updateCell(i, j0,((c&0x8000)!=0));
            updateCel(i, j1,((c&0x2000)!=0));
            updateCel(i, j2,((c&0x0800)!=0));
            updateCel(i, j3,((c&0x0200)!=0));
            updateCel(i, j4,((d&0x8000)!=0));
            updateCel(i, j5,((d&0x2000)!=0));
            updateCel(i, j6,((d&0x0800)!=0));
            updateCel(i, j7,((d&0x0200)!=0));
            updateCel(i, j8,((e&0x8000)!=0));
            updateCel(i, j9,((e&0x2000)!=0));
            updateCel(i,j10,((e&0x0800)!=0));
            updateCel(i,j11,((e&0x0200)!=0));
            updateCel(i,j12,((f&0x8000)!=0));
            updateCel(i,j13,((f&0x2000)!=0));
            updateCel(i,j14,((f&0x0800)!=0));
            updateCel(i,j15,((f&0x0200)!=0));

            i++;
            updateCell(i, j0,((c&0x4000)!=0));
            updateCel(i, j1,((c&0x1000)!=0));
            updateCel(i, j2,((c&0x0400)!=0));
            updateCel(i, j3,((c&0x0100)!=0));
            updateCel(i, j4,((d&0x4000)!=0));
            updateCel(i, j5,((d&0x1000)!=0));
            updateCel(i, j6,((d&0x0400)!=0));
            updateCel(i, j7,((d&0x0100)!=0));
            updateCel(i, j8,((e&0x4000)!=0));
            updateCel(i, j9,((e&0x1000)!=0));
            updateCel(i,j10,((e&0x0400)!=0));
            updateCel(i,j11,((e&0x0100)!=0));
            updateCel(i,j12,((f&0x4000)!=0));
            updateCel(i,j13,((f&0x1000)!=0));
            updateCel(i,j14,((f&0x0400)!=0));
            updateCel(i,j15,((f&0x0100)!=0));

            i++;
            updateCell(i, j0,((c&0x0080)!=0));
            updateCel(i, j1,((c&0x0020)!=0));
            updateCel(i, j2,((c&0x0008)!=0));
            updateCel(i, j3,((c&0x0002)!=0));
            updateCel(i, j4,((d&0x0080)!=0));
            updateCel(i, j5,((d&0x0020)!=0));
            updateCel(i, j6,((d&0x0008)!=0));
            updateCel(i, j7,((d&0x0002)!=0));
            updateCel(i, j8,((e&0x0080)!=0));
            updateCel(i, j9,((e&0x0020)!=0));
            updateCel(i,j10,((e&0x0008)!=0));
            updateCel(i,j11,((e&0x0002)!=0));
            updateCel(i,j12,((f&0x0080)!=0));
            updateCel(i,j13,((f&0x0020)!=0));
            updateCel(i,j14,((f&0x0008)!=0));
            updateCel(i,j15,((f&0x0002)!=0));

            i++;
            updateCell(i, j0,((c&0x0040)!=0));
            updateCel(i, j1,((c&0x0010)!=0));
            updateCel(i, j2,((c&0x0004)!=0));
            updateCel(i, j3,((c&0x0001)!=0));
            updateCel(i, j4,((d&0x0040)!=0));
            updateCel(i, j5,((d&0x0010)!=0));
            updateCel(i, j6,((d&0x0004)!=0));
            updateCel(i, j7,((d&0x0001)!=0));
            updateCel(i, j8,((e&0x0040)!=0));
            updateCel(i, j9,((e&0x0010)!=0));
            updateCel(i,j10,((e&0x0004)!=0));
            updateCel(i,j11,((e&0x0001)!=0));
            updateCel(i,j12,((f&0x0040)!=0));
            updateCel(i,j13,((f&0x0010)!=0));
            updateCel(i,j14,((f&0x0004)!=0));
            updateCel(i,j15,((f&0x0001)!=0));
         }
      }

      return true;
   }

   private boolean display_q(LifeGen u)
   {
      /* check only those with "display" bit set; skip the rest.
         display only those quadrants with hibernation and morgue
         bits = 0. */
      /* traverse both the living & hibernating lists. */
      /* return false if couldn't complete the generation. */

      short c,d,e,f;
      LifeCell nextc;
      int i;
      int j0,j1,j2,j3,j4,j5,j6,j7,j8,j9,j10,j11,j12,j13,j14,j15;


      for (LifeCell cell=u.display; cell != null; cell=nextc)
      {
         nextc=cell.DisplayNext;

         if ((cell.flags & 0x02)!=0)
         {
            u.removeFromDisplay(cell);
         }

         i  = cell.x*16 - xOrig + 1;
         j0 = cell.y*16 - yOrig + 1;

         if (i +15 < 0 || i  > fieldSizeX 
          || j0+15 < 0 || j0 > fieldSizeY)
         {
            u.removeFromDisplay(cell);
         }
         else
         {
            j1=j0+1;   j2=j1+1;   j3=j2+1;
            j4=j3+1;   j5=j4+1;   j6=j5+1;   j7=j6+1;
            j8=j7+1;   j9=j8+1;   j10=j9+1;  j11=j10+1;
            j12=j11+1; j13=j12+1; j14=j13+1; j15=j14+1;

            if (i >=0 && i+15<fieldSizeX
                  && j0>=0 && j15<fieldSizeY)
               boundsCheck=false;
            else boundsCheck=true;

            c=cell.q[0]; d=cell.q[2]; e=cell.q[4]; f=cell.q[6];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell( i, j0,((c&0x0080)!=0));
            updateCel( i, j1,((c&0x0020)!=0));
            updateCel( i, j2,((c&0x0008)!=0));
            updateCel( i, j3,((c&0x0002)!=0));
            updateCel( i, j4,((d&0x0080)!=0));
            updateCel( i, j5,((d&0x0020)!=0));
            updateCel( i, j6,((d&0x0008)!=0));
            updateCel( i, j7,((d&0x0002)!=0));
            updateCel( i, j8,((e&0x0080)!=0));
            updateCel( i, j9,((e&0x0020)!=0));
            updateCel( i,j10,((e&0x0008)!=0));
            updateCel( i,j11,((e&0x0002)!=0));
            updateCel( i,j12,((f&0x0080)!=0));
            updateCel( i,j13,((f&0x0020)!=0));
            updateCel( i,j14,((f&0x0008)!=0));
            updateCel( i,j15,((f&0x0002)!=0));

            i++;
            updateCell( i, j0,((c&0x0040)!=0));
            updateCel( i, j1,((c&0x0010)!=0));
            updateCel( i, j2,((c&0x0004)!=0));
            updateCel( i, j3,((c&0x0001)!=0));
            updateCel( i, j4,((d&0x0040)!=0));
            updateCel( i, j5,((d&0x0010)!=0));
            updateCel( i, j6,((d&0x0004)!=0));
            updateCel( i, j7,((d&0x0001)!=0));
            updateCel( i, j8,((e&0x0040)!=0));
            updateCel( i, j9,((e&0x0010)!=0));
            updateCel( i,j10,((e&0x0004)!=0));
            updateCel( i,j11,((e&0x0001)!=0));
            updateCel( i,j12,((f&0x0040)!=0));
            updateCel( i,j13,((f&0x0010)!=0));
            updateCel( i,j14,((f&0x0004)!=0));
            updateCel( i,j15,((f&0x0001)!=0));

            i++;
            c=cell.q[1]; d=cell.q[3]; e=cell.q[5]; f=cell.q[7];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell( i, j0,((c&0x0080)!=0));
            updateCel( i, j1,((c&0x0020)!=0));
            updateCel( i, j2,((c&0x0008)!=0));
            updateCel( i, j3,((c&0x0002)!=0));
            updateCel( i, j4,((d&0x0080)!=0));
            updateCel( i, j5,((d&0x0020)!=0));
            updateCel( i, j6,((d&0x0008)!=0));
            updateCel( i, j7,((d&0x0002)!=0));
            updateCel( i, j8,((e&0x0080)!=0));
            updateCel( i, j9,((e&0x0020)!=0));
            updateCel( i,j10,((e&0x0008)!=0));
            updateCel( i,j11,((e&0x0002)!=0));
            updateCel( i,j12,((f&0x0080)!=0));
            updateCel( i,j13,((f&0x0020)!=0));
            updateCel( i,j14,((f&0x0008)!=0));
            updateCel( i,j15,((f&0x0002)!=0));

            i++;
            updateCell( i, j0,((c&0x0040)!=0));
            updateCel( i, j1,((c&0x0010)!=0));
            updateCel( i, j2,((c&0x0004)!=0));
            updateCel( i, j3,((c&0x0001)!=0));
            updateCel( i, j4,((d&0x0040)!=0));
            updateCel( i, j5,((d&0x0010)!=0));
            updateCel( i, j6,((d&0x0004)!=0));
            updateCel( i, j7,((d&0x0001)!=0));
            updateCel( i, j8,((e&0x0040)!=0));
            updateCel( i, j9,((e&0x0010)!=0));
            updateCel( i,j10,((e&0x0004)!=0));
            updateCel( i,j11,((e&0x0001)!=0));
            updateCel( i,j12,((f&0x0040)!=0));
            updateCel( i,j13,((f&0x0010)!=0));
            updateCel( i,j14,((f&0x0004)!=0));
            updateCel( i,j15,((f&0x0001)!=0));

            i++;
            c=cell.q[8]; d=cell.q[10]; e=cell.q[12]; f=cell.q[14];
            updateCell( i, j0,((c&0x8000)!=0));
            updateCel( i, j1,((c&0x2000)!=0));
            updateCel( i, j2,((c&0x0800)!=0));
            updateCel( i, j3,((c&0x0200)!=0));
            updateCel( i, j4,((d&0x8000)!=0));
            updateCel( i, j5,((d&0x2000)!=0));
            updateCel( i, j6,((d&0x0800)!=0));
            updateCel( i, j7,((d&0x0200)!=0));
            updateCel( i, j8,((e&0x8000)!=0));
            updateCel( i, j9,((e&0x2000)!=0));
            updateCel( i,j10,((e&0x0800)!=0));
            updateCel( i,j11,((e&0x0200)!=0));
            updateCel( i,j12,((f&0x8000)!=0));
            updateCel( i,j13,((f&0x2000)!=0));
            updateCel( i,j14,((f&0x0800)!=0));
            updateCel( i,j15,((f&0x0200)!=0));

            i++;
            updateCell( i, j0,((c&0x4000)!=0));
            updateCel( i, j1,((c&0x1000)!=0));
            updateCel( i, j2,((c&0x0400)!=0));
            updateCel( i, j3,((c&0x0100)!=0));
            updateCel( i, j4,((d&0x4000)!=0));
            updateCel( i, j5,((d&0x1000)!=0));
            updateCel( i, j6,((d&0x0400)!=0));
            updateCel( i, j7,((d&0x0100)!=0));
            updateCel( i, j8,((e&0x4000)!=0));
            updateCel( i, j9,((e&0x1000)!=0));
            updateCel( i,j10,((e&0x0400)!=0));
            updateCel( i,j11,((e&0x0100)!=0));
            updateCel( i,j12,((f&0x4000)!=0));
            updateCel( i,j13,((f&0x1000)!=0));
            updateCel( i,j14,((f&0x0400)!=0));
            updateCel( i,j15,((f&0x0100)!=0));

            i++;
            updateCell(i, j0,((c&0x0080)!=0));
            updateCel(i, j1,((c&0x0020)!=0));
            updateCel(i, j2,((c&0x0008)!=0));
            updateCel(i, j3,((c&0x0002)!=0));
            updateCel(i, j4,((d&0x0080)!=0));
            updateCel(i, j5,((d&0x0020)!=0));
            updateCel(i, j6,((d&0x0008)!=0));
            updateCel(i, j7,((d&0x0002)!=0));
            updateCel(i, j8,((e&0x0080)!=0));
            updateCel(i, j9,((e&0x0020)!=0));
            updateCel(i,j10,((e&0x0008)!=0));
            updateCel(i,j11,((e&0x0002)!=0));
            updateCel(i,j12,((f&0x0080)!=0));
            updateCel(i,j13,((f&0x0020)!=0));
            updateCel(i,j14,((f&0x0008)!=0));
            updateCel(i,j15,((f&0x0002)!=0));

            i++;
            updateCell(i, j0,((c&0x0040)!=0));
            updateCel(i, j1,((c&0x0010)!=0));
            updateCel(i, j2,((c&0x0004)!=0));
            updateCel(i, j3,((c&0x0001)!=0));
            updateCel(i, j4,((d&0x0040)!=0));
            updateCel(i, j5,((d&0x0010)!=0));
            updateCel(i, j6,((d&0x0004)!=0));
            updateCel(i, j7,((d&0x0001)!=0));
            updateCel(i, j8,((e&0x0040)!=0));
            updateCel(i, j9,((e&0x0010)!=0));
            updateCel(i,j10,((e&0x0004)!=0));
            updateCel(i,j11,((e&0x0001)!=0));
            updateCel(i,j12,((f&0x0040)!=0));
            updateCel(i,j13,((f&0x0010)!=0));
            updateCel(i,j14,((f&0x0004)!=0));
            updateCel(i,j15,((f&0x0001)!=0));

            i++;
            c=cell.q[9]; d=cell.q[11]; e=cell.q[13]; f=cell.q[15];
            updateCell(i, j0,((c&0x8000)!=0));
            updateCel(i, j1,((c&0x2000)!=0));
            updateCel(i, j2,((c&0x0800)!=0));
            updateCel(i, j3,((c&0x0200)!=0));
            updateCel(i, j4,((d&0x8000)!=0));
            updateCel(i, j5,((d&0x2000)!=0));
            updateCel(i, j6,((d&0x0800)!=0));
            updateCel(i, j7,((d&0x0200)!=0));
            updateCel(i, j8,((e&0x8000)!=0));
            updateCel(i, j9,((e&0x2000)!=0));
            updateCel(i,j10,((e&0x0800)!=0));
            updateCel(i,j11,((e&0x0200)!=0));
            updateCel(i,j12,((f&0x8000)!=0));
            updateCel(i,j13,((f&0x2000)!=0));
            updateCel(i,j14,((f&0x0800)!=0));
            updateCel(i,j15,((f&0x0200)!=0));

            i++;
            updateCell(i, j0,((c&0x4000)!=0));
            updateCel(i, j1,((c&0x1000)!=0));
            updateCel(i, j2,((c&0x0400)!=0));
            updateCel(i, j3,((c&0x0100)!=0));
            updateCel(i, j4,((d&0x4000)!=0));
            updateCel(i, j5,((d&0x1000)!=0));
            updateCel(i, j6,((d&0x0400)!=0));
            updateCel(i, j7,((d&0x0100)!=0));
            updateCel(i, j8,((e&0x4000)!=0));
            updateCel(i, j9,((e&0x1000)!=0));
            updateCel(i,j10,((e&0x0400)!=0));
            updateCel(i,j11,((e&0x0100)!=0));
            updateCel(i,j12,((f&0x4000)!=0));
            updateCel(i,j13,((f&0x1000)!=0));
            updateCel(i,j14,((f&0x0400)!=0));
            updateCel(i,j15,((f&0x0100)!=0));

            i++;
            updateCell(i, j0,((c&0x0080)!=0));
            updateCel(i, j1,((c&0x0020)!=0));
            updateCel(i, j2,((c&0x0008)!=0));
            updateCel(i, j3,((c&0x0002)!=0));
            updateCel(i, j4,((d&0x0080)!=0));
            updateCel(i, j5,((d&0x0020)!=0));
            updateCel(i, j6,((d&0x0008)!=0));
            updateCel(i, j7,((d&0x0002)!=0));
            updateCel(i, j8,((e&0x0080)!=0));
            updateCel(i, j9,((e&0x0020)!=0));
            updateCel(i,j10,((e&0x0008)!=0));
            updateCel(i,j11,((e&0x0002)!=0));
            updateCel(i,j12,((f&0x0080)!=0));
            updateCel(i,j13,((f&0x0020)!=0));
            updateCel(i,j14,((f&0x0008)!=0));
            updateCel(i,j15,((f&0x0002)!=0));

            i++;
            updateCell(i, j0,((c&0x0040)!=0));
            updateCel(i, j1,((c&0x0010)!=0));
            updateCel(i, j2,((c&0x0004)!=0));
            updateCel(i, j3,((c&0x0001)!=0));
            updateCel(i, j4,((d&0x0040)!=0));
            updateCel(i, j5,((d&0x0010)!=0));
            updateCel(i, j6,((d&0x0004)!=0));
            updateCel(i, j7,((d&0x0001)!=0));
            updateCel(i, j8,((e&0x0040)!=0));
            updateCel(i, j9,((e&0x0010)!=0));
            updateCel(i,j10,((e&0x0004)!=0));
            updateCel(i,j11,((e&0x0001)!=0));
            updateCel(i,j12,((f&0x0040)!=0));
            updateCel(i,j13,((f&0x0010)!=0));
            updateCel(i,j14,((f&0x0004)!=0));
            updateCel(i,j15,((f&0x0001)!=0));
         }
      }
      return true;
   }

   public void updateAll(LifeGen u)
   {
      if (viewChanged) {
         u.freshenView();
         viewChanged = false;
      }

      if (u.qCycle) display_q(u);
      else          display_p(u);
   }

}