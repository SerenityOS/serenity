/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4485755 6361370 6448717 5080051 6939417 8016343
 * @summary dialog doesn't have way to specify margins
 *          for 6361370, verify exception for offline printer in Windows
 *          for 6448717, faster display of print dialog
 *          for 6500903, verify status of printer if accepting jobs or not
 *          for 8016343, verify printing to non-default printer
 * @author prr
 * @run main/manual DialogMargins
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class DialogMargins extends Frame {

  public DialogMargins() {
     super("Dialog Margins Test");

    Button printButton = new Button ("Print ...");
    add("Center", printButton);
    printButton.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent e) {
                     new MarginsPrinter();
                }
    });

    addWindowListener (new WindowAdapter() {
         public void windowClosing (WindowEvent e) {
            dispose();
         }

     });

     pack();
     setVisible (true);
  }

class MarginsPrinter implements Printable {

  PrinterJob myPrinterJob;
  PageFormat myPageFormat;

  public MarginsPrinter() {
      PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
      //aset.add(MediaSizeName.ISO_A4);
      //aset.add(new MediaPrintableArea(0f,0f,210f,297f,MediaPrintableArea.MM));
      myPrinterJob = PrinterJob.getPrinterJob();
      myPageFormat = myPrinterJob.pageDialog(aset);
      myPrinterJob.setPrintable(this, myPageFormat);
      //myPrinterJob.setPrintable(this);
      if (myPrinterJob.printDialog(aset)) {
          try {
             //PrintRequestAttributeSet newaset =
                   //new HashPrintRequestAttributeSet();
              myPrinterJob.print(aset);

          } catch (PrinterException pe ) {
              System.out.println("DialogMargins Exception caught:" + pe);
          }
      }
   }

  public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {

     if (pageIndex > 0) {
        return Printable.NO_SUCH_PAGE;
     }

     Graphics2D g2d = (Graphics2D)graphics;
     g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
     g2d.drawString("ORIGIN("+pageFormat.getImageableX()+","+
                             pageFormat.getImageableY()+")", 20, 20);
     g2d.drawString("X THIS WAY", 200, 50);
     g2d.drawString("Y THIS WAY", 60 , 200);
     g2d.drawString("Graphics is " + g2d.getClass().getName(), 100, 100);
     g2d.drawRect(0,0,(int)pageFormat.getImageableWidth(),
                      (int)pageFormat.getImageableHeight());
     g2d.setColor(Color.black);
     g2d.drawRect(1,1,(int)pageFormat.getImageableWidth()-2,
                      (int)pageFormat.getImageableHeight()-2);

     return  Printable.PAGE_EXISTS;
  }

}
  public static void main( String[] args) {

  String[] instructions =
        {
         "You must have a printer available to perform this test",
         "Specify various pageformats and compare the printed results with the",
         "request."
       };
      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

     new DialogMargins();
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
