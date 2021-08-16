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
 * @bug 4223328
 * @summary Printer graphics must behave the same as screen graphics
 * @author prr
 * @run main/manual PrintNullString
 */


import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.text.*;

public class PrintNullString extends Frame implements ActionListener {

 private TextCanvas c;

 public static void main(String args[]) {

  String[] instructions =
        {
         "You must have a printer available to perform this test",
         "This test should print a page which contains the same",
         "text messages as in the test window on the screen",
         "The messages should contain only 'OK' and 'expected' messages",
         "There should be no FAILURE messages.",
         "You should also monitor the command line to see if any exceptions",
         "were thrown",
         "If the page fails to print, but there were no exceptions",
         "then the problem is likely elsewhere (ie your printer)"
       };
      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

    PrintNullString f = new PrintNullString();
    f.show();
 }

 public PrintNullString() {
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

    String nullStr = null;
    String emptyStr = new String();
    AttributedString nullAttStr = null;
    AttributedString emptyAttStr = new AttributedString(emptyStr);
    AttributedCharacterIterator nullIterator = null;
    AttributedCharacterIterator emptyIterator = emptyAttStr.getIterator();

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

        // API 1: null & empty drawString(String, int, int);
        try {
             g.drawString(nullStr, 20, 40);
             g.drawString("FAILURE: No NPE for null String, int", 20, 40);
        } catch (NullPointerException e) {
          g.drawString("caught expected NPE for null String, int", 20, 40);
        }/* catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for null String, int",
                        20, 40);
        }*/

        //try {
             g.drawString(emptyStr, 20, 60);
             g.drawString("OK for empty String, int", 20, 60);
        /*} catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for empty String, int",
                        20, 60);
        }*/


        // API 2: null & empty drawString(String, float, float);
        try {
             g.drawString(nullStr, 20.0f, 80.0f);
             g.drawString("FAILURE: No NPE for null String, float", 20, 80);
        } catch (NullPointerException e) {
          g.drawString("caught expected NPE for null String, float", 20, 80);
        } /*catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for null String, float",
                        20, 80);
        }*/
        //try {
             g.drawString(emptyStr, 20.0f, 100.0f);
             g.drawString("OK for empty String, float", 20.0f, 100.f);
        /* } catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for empty String, float",
                        20, 100);
        }*/

        // API 3: null & empty drawString(Iterator, int, int);
        try {
             g.drawString(nullIterator, 20, 120);
             g.drawString("FAILURE: No NPE for null iterator, float", 20, 120);
        } catch (NullPointerException e) {
          g.drawString("caught expected NPE for null iterator, int", 20, 120);
        } /*catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for null iterator, int",
                       20, 120);
        } */
        try {
             g.drawString(emptyIterator, 20, 140);
             g.drawString("FAILURE: No IAE for empty iterator, int",
                           20, 140);
        } catch (IllegalArgumentException e) {
          g.drawString("caught expected IAE for empty iterator, int",
                        20, 140);
        } /*catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for empty iterator, int",
                       20, 140);
        } */


        // API 4: null & empty drawString(Iterator, float, int);
        try {
             g.drawString(nullIterator, 20.0f, 160.0f);
             g.drawString("FAILURE: No NPE for null iterator, float", 20, 160);
        } catch (NullPointerException e) {
          g.drawString("caught expected NPE for null iterator, float", 20, 160);
        } /*catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for null iterator, float",
                        20, 160);
        } */

        try {
             g.drawString(emptyIterator, 20, 180);
             g.drawString("FAILURE: No IAE for empty iterator, float",
                           20, 180);
        } catch (IllegalArgumentException e) {
          g.drawString("caught expected IAE for empty iterator, float",
                        20, 180);
        } /*catch (Exception e) {
          g.drawString("FAILURE: unexpected exception for empty iterator, float",
                       20, 180);
        } */
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
