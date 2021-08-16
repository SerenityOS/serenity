/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 4185019
 * @summary Confirm that all of the drawString methods on Graphics2D
 *          work for printer graphics objects.
 * @run main/manual DrawStringMethods
 */

import java.awt.*;
import java.text.*;
import java.awt.font.*;
import java.awt.print.*;

public class DrawStringMethods implements Printable {

    public static void main(String args[]) {
        String[] instructions =
        {
            "Confirm that the methods are printed.",
            " For Graphics: drawString, drawString, drawChars, drawBytes",
            " For Graphics2D: drawString, drawString, drawGlyphVector"
        };
        Sysout.createDialogWithInstructions( instructions );


        PrinterJob pjob = PrinterJob.getPrinterJob();
        PageFormat pf = pjob.defaultPage();
        Book book = new Book();

        book.append(new DrawStringMethods(), pf);
        pjob.setPageable(book);

        try {
            pjob.print();
        } catch (PrinterException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static AttributedCharacterIterator getIterator(String s) {
        return new AttributedString(s).getIterator();
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        int ix = (int) pf.getImageableX();
        int iy = (int) pf.getImageableY();
        String s;

        g.setColor(Color.black);

        iy += 50;
        s = "--- Graphics methods: ---";
        g.drawString(s, ix, iy);

        iy += 30;
        s = "drawString(String str, int x, int y)";
        g.drawLine(ix, iy, ix+10, iy);
        g.drawString(s, ix+20, iy);

        iy += 30;
        s = "drawString(AttributedCharacterIterator iterator, int x, int y)";
        g.drawLine(ix, iy, ix+10, iy);
        g.drawString(getIterator(s), ix+20, iy);

        iy += 30;
        s = "drawChars(char data[], int offset, int length, int x, int y)";
        g.drawLine(ix, iy, ix+10, iy);
        g.drawChars(s.toCharArray(), 0, s.length(), ix+20, iy);

        iy += 30;
        s = "drawBytes(byte data[], int offset, int length, int x, int y)";
        byte data[] = new byte[s.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) s.charAt(i);
        }
        g.drawLine(ix, iy, ix+10, iy);
        g.drawBytes(data, 0, data.length, ix+20, iy);

        iy += 50;
        s = "--- Graphics2D methods: ---";
        g.drawString(s, ix, iy);

        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            Font f = g2d.getFont();
            FontRenderContext frc = g2d.getFontRenderContext();

            iy += 30;
            s = "drawString(String s, float x, float y)";
            g.drawLine(ix, iy, ix+10, iy);
            g2d.drawString(s, (float) ix+20, (float) iy);

            iy += 30;
            s = "drawString(AttributedCharacterIterator iterator, "+
                           "float x, float y)";
            g.drawLine(ix, iy, ix+10, iy);
            g2d.drawString(getIterator(s), (float) ix+20, (float) iy);

            iy += 30;
            s = "drawGlyphVector(GlyphVector g, float x, float y)";
            g.drawLine(ix, iy, ix+10, iy);
            g2d.drawGlyphVector(f.createGlyphVector(frc, s), ix+20, iy);
        } else {
            iy += 30;
            s = "Graphics object does not support Graphics2D methods";
            g.drawString(s, ix+20, iy);
        }

        return PAGE_EXISTS;
    }
}

class Sysout
 {
   private static TestDialog dialog;

   public static void createDialogWithInstructions( String[] instructions )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      dialog.printInstructions( instructions );
      dialog.show();
      println( "Any messages for the tester will display here." );
    }

   public static void createDialog( )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      String[] defInstr = { "Instructions will appear here. ", "" } ;
      dialog.printInstructions( defInstr );
      dialog.show();
      println( "Any messages for the tester will display here." );
    }


   public static void printInstructions( String[] instructions )
    {
      dialog.printInstructions( instructions );
    }


   public static void println( String messageIn )
    {
      dialog.displayMessage( messageIn );
    }

 }// Sysout  class

/**
  This is part of the standard test machinery.  It provides a place for the
   test instructions to be displayed, and a place for interactive messages
   to the user to be displayed.
  To have the test instructions displayed, see Sysout.
  To have a message to the user be displayed, see Sysout.
  Do not call anything in this dialog directly.
  */
class TestDialog extends Dialog
 {

   TextArea instructionsText;
   TextArea messageText;
   int maxStringLength = 80;

   //DO NOT call this directly, go through Sysout
   public TestDialog( Frame frame, String name )
    {
      super( frame, name );
      int scrollBoth = TextArea.SCROLLBARS_BOTH;
      instructionsText = new TextArea( "", 15, maxStringLength, scrollBoth );
      add( "North", instructionsText );

      messageText = new TextArea( "", 5, maxStringLength, scrollBoth );
      add("South", messageText);

      pack();

      show();
    }// TestDialog()

   //DO NOT call this directly, go through Sysout
   public void printInstructions( String[] instructions )
    {
      //Clear out any current instructions
      instructionsText.setText( "" );

      //Go down array of instruction strings

      String printStr, remainingStr;
      for( int i=0; i < instructions.length; i++ )
       {
     //chop up each into pieces maxSringLength long
     remainingStr = instructions[ i ];
     while( remainingStr.length() > 0 )
      {
        //if longer than max then chop off first max chars to print
        if( remainingStr.length() >= maxStringLength )
         {
           //Try to chop on a word boundary
           int posOfSpace = remainingStr.
          lastIndexOf( ' ', maxStringLength - 1 );

           if( posOfSpace <= 0 ) posOfSpace = maxStringLength - 1;

           printStr = remainingStr.substring( 0, posOfSpace + 1 );
           remainingStr = remainingStr.substring( posOfSpace + 1 );
         }
        //else just print
        else
         {
           printStr = remainingStr;
           remainingStr = "";
         }

            instructionsText.append( printStr + "\n" );

      }// while

       }// for

    }//printInstructions()

   //DO NOT call this directly, go through Sysout
   public void displayMessage( String messageIn )
    {
      messageText.append( messageIn + "\n" );
    }

 }// TestDialog  class
