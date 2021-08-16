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
 * @bug 4242639
 * @summary Printing quality problem on Canon and NEC
 * @author prr
 * @run main/manual RasterTest
 */
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;


public class RasterTest extends Frame implements ActionListener {

 private RasterCanvas c;

 public static void main(String args[]) {
  String[] instructions =
        {
         "You must have a printer available to perform this test",
         "This test uses rendering operations which force the implementation",
         "to print the page as a raster",
         "You should see two square images, the 1st containing overlapping",
         "composited squares, the lower image shows a gradient paint.",
         "The printed output should match the on-screen display, although",
         "only colour printers will be able to accurately reproduce the",
         "subtle color changes."
       };
      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

    RasterTest f = new RasterTest();
    f.show();
 }

 public RasterTest() {
        super("Java 2D Raster Printing");

    c = new RasterCanvas();
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

        setBackground(Color.white);

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


 class RasterCanvas extends Canvas implements Printable {


    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {
      if (pgIndex > 0)
         return Printable.NO_SUCH_PAGE;

         Graphics2D g2d= (Graphics2D)g;
         g2d.translate(pgFmt.getImageableX(), pgFmt.getImageableY());
         doPaint(g2d);
      return Printable.PAGE_EXISTS;
    }

    public void paint(Graphics g) {
       doPaint(g);
    }

    public void paintComponent(Graphics g) {
       doPaint(g);
    }

    public void doPaint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;

        g2.setColor(Color.black);

        BufferedImage bimg = new BufferedImage(200, 200,
                                                 BufferedImage.TYPE_INT_ARGB);
        Graphics ig = bimg.getGraphics();
        Color alphared = new Color(255, 0, 0, 128);
        Color alphagreen = new Color(0, 255, 0, 128);
        Color alphablue = new Color(0, 0, 255, 128);
        ig.setColor(alphared);
        ig.fillRect(0,0,200,200);
        ig.setColor(alphagreen);
        ig.fillRect(25,25,150,150);
        ig.setColor(alphablue);
        ig.fillRect(75,75,125,125);
        g.drawImage(bimg, 10, 25, this);

        GradientPaint gp =
         new GradientPaint(10.0f, 10.0f, alphablue, 210.0f, 210.0f, alphared, true);
        g2.setPaint(gp);
        g2.fillRect(10, 240, 200, 200);

     }

    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

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
