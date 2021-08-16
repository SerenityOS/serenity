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
 * @test InvalidPage.java
 * @bug 4671634 6506286
 * @summary Invalid page format can crash win32 JRE
 * @author prr
 * @run main/manual InvalidPage
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

public class InvalidPage extends Frame implements Printable {

  PrinterJob pJob;
  PageFormat pf;

  public InvalidPage() {
    super ("Validate Page Test");
    pJob = PrinterJob.getPrinterJob();
    pf = pJob.defaultPage();
    Paper p = pf.getPaper();
    p.setImageableArea(0,0,p.getWidth(), p.getHeight());
    pf.setPaper(p);
    setLayout(new FlowLayout());
    Panel panel = new Panel();
    Button printButton = new Button ("Print");
    printButton.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent e) {
                    try {
                         if (pJob.printDialog()) {
                             pJob.setPrintable(InvalidPage.this, pf);
                             pJob.print();
                    }
                    } catch (PrinterException pe ) {
                    }
                }
    });
    panel.add (printButton);
    add(panel);

    addWindowListener (new WindowAdapter() {
         public void windowClosing (WindowEvent e) {
            dispose();
            System.exit (0);
         }

      });
      setSize (200, 200);
      setVisible (true);
  }

  public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {

     if (pageIndex > 1) {
        return Printable.NO_SUCH_PAGE;
     }

     Graphics2D g2d = (Graphics2D)graphics;

     g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
     g2d.drawString("ORIGIN", 30, 30);
     g2d.drawString("X THIS WAY", 200, 50);
     g2d.drawString("Y THIS WAY", 60 , 200);
     g2d.drawRect(0,0,(int)pageFormat.getImageableWidth(),
                      (int)pageFormat.getImageableHeight());
     if (pageIndex == 0) {
        g2d.setColor(Color.black);
     } else {
        g2d.setColor(new Color(0,0,0,128));
     }
     g2d.drawRect(1,1,(int)pageFormat.getImageableWidth()-2,
                      (int)pageFormat.getImageableHeight()-2);

     g2d.drawLine(0,0,
                  (int)pageFormat.getImageableWidth(),
                  (int)pageFormat.getImageableHeight());
     g2d.drawLine((int)pageFormat.getImageableWidth(),0,
                   0,(int)pageFormat.getImageableHeight());
     return  Printable.PAGE_EXISTS;
  }

  public static void main( String[] args) {
  String[] instructions =
        {
         "You must have a printer available to perform this test",
         "Press the print button, which brings up a print dialog and",
         "in the dialog select a printer and press the print button",
         "in the dialog. Repeat for as many printers as you have installed",
         "On solaris and linux just one printer is sufficient",
         "Collect the output and examine it, each print job has two pages",
         "of very similar output, except that the 2nd page of the job may",
         "appear in a different colour, and the output near the edge of",
         "the page may be clipped. This is OK. Hold up both pieces of paper",
         "to the light and confirm that the lines and text (where present)",
         "are positioned identically on both pages",
         "The test fails if the JRE crashes, or if the output from the two",
         "pages of a job is aligned differently"
       };
      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

     new InvalidPage();
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
