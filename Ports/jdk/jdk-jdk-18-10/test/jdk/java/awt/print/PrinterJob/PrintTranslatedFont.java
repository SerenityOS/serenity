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
 * @bug 6359734
 * @summary Test that fonts with a translation print where they should.
 * @author prr
 * @run main/manual PrintTranslatedFont
 */


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.text.*;

public class PrintTranslatedFont extends Frame implements ActionListener {

 private TextCanvas c;

 public static void main(String args[]) {

  String[] instructions =
        {
         "You must have a printer available to perform this test",
         "This test should print a page which contains the same",
         "content as the test window on the screen, in particular the lines",
         "should be immediately under the text",
         "You should also monitor the command line to see if any exceptions",
         "were thrown",
         "If an exception is thrown, or the page doesn't print properly",
         "then the test fails",
       };
      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

    PrintTranslatedFont f = new PrintTranslatedFont();
    f.show();
 }

 public PrintTranslatedFont() {
    super("JDK 1.2 drawString Printing");

    c = new TextCanvas();
    add("Center", c);

    Button printButton = new Button("Print");
    printButton.addActionListener(this);
    add("South", printButton);

    addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
             System.exit(0);
            }
    });

    pack();
 }

 public void actionPerformed(ActionEvent e) {

   PrinterJob pj = PrinterJob.getPrinterJob();

   if (pj != null && pj.printDialog()) {

       pj.setPrintable(c);
       try {
            pj.print();
      } catch (PrinterException pe) {
      } finally {
         System.err.println("PRINT RETURNED");
      }
   }
 }

 class TextCanvas extends Panel implements Printable {

    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {

      if (pgIndex > 0)
         return Printable.NO_SUCH_PAGE;

      Graphics2D g2d = (Graphics2D)g;
      g2d.translate(pgFmt.getImageableX(), pgFmt.getImageableY());

      paint(g);

      return Printable.PAGE_EXISTS;
    }

    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D)g1;

          Font f = new Font("Dialog", Font.PLAIN, 20);
          int tx = 20;
          int ty = 20;
          AffineTransform at = AffineTransform.getTranslateInstance(tx, ty);
          f = f.deriveFont(at);
          g.setFont(f);

          FontMetrics fm = g.getFontMetrics();
          String str = "Basic ascii string";
          int sw = fm.stringWidth(str);
          int posx = 20, posy = 40;
          g.drawString(str, posx, posy);
          g.drawLine(posx+tx, posy+ty+2, posx+tx+sw, posy+ty+2);

          posx = 20; posy = 70;
          str = "Test string compound printing \u2203\u2200";
          sw = fm.stringWidth(str);
          g.drawString(str, posx, posy);
          g.drawLine(posx+tx, posy+ty+2, posx+tx+sw, posy+ty+2);
    }

     public Dimension getPreferredSize() {
        return new Dimension(450, 250);
    }
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
