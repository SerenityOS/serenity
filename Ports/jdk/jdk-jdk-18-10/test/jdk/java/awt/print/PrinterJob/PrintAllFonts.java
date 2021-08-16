/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4884389 7183516
 * @summary Font specified with face name loses style on printing
 * @run main/manual PrintRotatedText
 */

import java.awt.*;
import java.awt.print.*;
import java.awt.GraphicsEnvironment;

public class PrintAllFonts implements Printable {

    static Font[] allFonts;
    int fontNum = 0;
    int startNum = 0;
    int lineHeight = 18;
    boolean done = false;
    int thisPage = 0;


    public static void main(String[] args) throws Exception {

        String[] instructions =
        {
            "You must have a printer available to perform this test and should use Win 98.",
            "This bug is system dependent and is not always reproducible.",
            " ",
            "A passing test will have all text printed with correct font style.",
        };

        Sysout.createDialog( );
        Sysout.printInstructions( instructions );

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        allFonts = ge.getAllFonts();

        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(new PrintAllFonts());
        if (pj.printDialog()) {
           pj.print();
        }
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {

        if (fontNum >= allFonts.length && pageIndex > thisPage) {
            return NO_SUCH_PAGE;
        }
        if (pageIndex > thisPage) {
            startNum = fontNum;
            thisPage = pageIndex;
        } else {
            fontNum = startNum;
        }
        g.setColor(Color.black);

        int hgt = (int)pf.getImageableHeight();
        int fontsPerPage = hgt/lineHeight;
        int x = (int)pf.getImageableX()+10;
        int y = (int)pf.getImageableY()+lineHeight;

        for (int n = 0; n < fontsPerPage; n++) {
            Font f = allFonts[fontNum].deriveFont(Font.PLAIN, 16);
            g.setFont(f);
            g.drawString(f.getFontName(), x, y);
            y+= lineHeight;
            fontNum++;
            if (fontNum >= allFonts.length) {
                break;
            }
        }
        return PAGE_EXISTS;
    }
}

class Sysout {
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
class TestDialog extends Dialog {

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
      add("Center", messageText);

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
