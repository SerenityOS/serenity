/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @bug 4116029 4300383
  @summary verify that child components can draw only inside their
           visible bounds
  @author das@sparc.spb.su area=awt.print
  @run main/manual=yesno ConstrainedPrintingTest
*/

// Note there is no @ in front of test above.  This is so that the
//  harness will not mistake this file as a test file.  It should
//  only see the html file as a test file. (the harness runs all
//  valid test files, so it would run this test twice if this file
//  were valid as well as the html file.)
// Also, note the area= after Your Name in the author tag.  Here, you
//  should put which functional area the test falls in.  See the
//  AWT-core home page -> test areas and/or -> AWT team  for a list of
//  areas.
// There are several places where ManualYesNoTest appear.  It is
//  recommended that these be changed by a global search and replace,
//  such as  ESC-%  in xemacs.



/**
 * ConstrainedPrintingTest.java
 *
 * summary: verify that child components can draw only inside their
 *          visible bounds
 *
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


//Manual tests should run as applet tests if possible because they
// get their environments cleaned up, including AWT threads, any
// test created threads, and any system resources used by the test
// such as file descriptors.  (This is normally not a problem as
// main tests usually run in a separate VM, however on some platforms
// such as the Mac, separate VMs are not possible and non-applet
// tests will cause problems).  Also, you don't have to worry about
// synchronisation stuff in Applet tests the way you do in main
// tests...


public class ConstrainedPrintingTest implements ActionListener
 {
   //Declare things used in the test, like buttons and labels here
    final Frame frame = new Frame("PrintTest");
    final Button button = new Button("Print");
    final Panel panel = new Panel();
    final Component testComponent = new Component() {
        public void paint(Graphics g) {
            ConstrainedPrintingTest.paintOutsideBounds(this, g, Color.green);
        }
        public Dimension getPreferredSize() {
            return new Dimension(100, 100);
        }
    };
    final Canvas testCanvas = new Canvas() {
        public void paint(Graphics g) {
            ConstrainedPrintingTest.paintOutsideBounds(this, g, Color.red);
            // The frame is sized so that only the upper part of
            // the canvas is visible. We draw on the lower part,
            // so that we can verify that the output is clipped
            // by the parent container bounds.
            Dimension panelSize = panel.getSize();
            Rectangle b = getBounds();
            g.setColor(Color.red);
            g.setClip(null);
            for (int i = panelSize.height - b.y; i < b.height; i+= 10) {
                g.drawLine(0, i, b.width, i);
            }
        }
        public Dimension getPreferredSize() {
            return new Dimension(100, 100);
        }
    };

   public void init()
    {
      //Create instructions for the user here, as well as set up
      // the environment -- set the layout manager, add buttons,
      // etc.
        button.addActionListener(this);

        panel.setBackground(Color.white);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panel.add(testComponent);
        panel.add(testCanvas);

        frame.setLayout(new BorderLayout());
        frame.add(button, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(200, 250);
        frame.validate();
        frame.setResizable(false);

      String[] instructions =
       {
         "1.Look at the frame titled \"PrintTest\". If you see green or",
         "  red lines on the white area below the \"Print\" button, the",
         "  test fails. Otherwise go to step 2.",
         "2.Press \"Print\" button. The print dialog will appear. Select",
         "  a printer and proceed. Look at the output. If you see multiple",
         "  lines outside of the frame bounds or in the white area below",
         "  the image of the \"Print\" button, the test fails. Otherwise",
         "  the test passes."
       };
      Sysout.createDialogWithInstructions( instructions );

    }//End  init()

   public void start ()
    {
      //Get things going.  Request focus, set size, et cetera

        frame.setVisible(true);

      //What would normally go into main() will probably go here.
      //Use System.out.println for diagnostic messages that you want
      // to read after the test is done.
      //Use Sysout.println for messages you want the tester to read.

    }// start()

   //The rest of this class is the actions which perform the test...

   //Use Sysout.println to communicate with the user NOT System.out!!
   //Sysout.println ("Something Happened!");

    public void stop() {
        frame.setVisible(false);
    }

    public void destroy() {
        frame.dispose();
    }

    public void actionPerformed(ActionEvent e) {
        PageAttributes pa = new PageAttributes();
        pa.setPrinterResolution(36);
        PrintJob pjob = frame.getToolkit().getPrintJob(frame, "NewTest",
                                                       new JobAttributes(),
                                                       pa);
        if (pjob != null) {
            Graphics pg = pjob.getGraphics();
            if (pg != null) {
                pg.translate(20, 20);
                frame.printAll(pg);
                pg.dispose();
            }
            pjob.end();
        }
    }

    public static void paintOutsideBounds(Component comp,
                                          Graphics g,
                                          Color color) {
        Dimension dim = comp.getSize();
        g.setColor(color);

        g.setClip(0, 0, dim.width * 2, dim.height * 2);
        for (int i = 0; i < dim.height * 2; i += 10) {
            g.drawLine(dim.width, i, dim.width * 2, i);
        }

        g.setClip(null);
        for (int i = 0; i < dim.width * 2; i += 10) {
            g.drawLine(i, dim.height, i, dim.height * 2);
        }

        g.setClip(new Rectangle(0, 0, dim.width * 2, dim.height * 2));
        for (int i = 0; i < dim.width; i += 10) {
            g.drawLine(dim.width * 2 - i, 0, dim.width * 2, i);
        }
    }

    public static void main(String[] args) {
        ConstrainedPrintingTest c = new ConstrainedPrintingTest();

        c.init();
        c.start();
    }

 }// class ConstrainedPrintingTest

/* Place other classes related to the test after this line */





/****************************************************
 Standard Test Machinery
 DO NOT modify anything below -- it's a standard
  chunk of code whose purpose is to make user
  interaction uniform, and thereby make it simpler
  to read and understand someone else's test.
 ****************************************************/

/**
 This is part of the standard test machinery.
 It creates a dialog (with the instructions), and is the interface
  for sending text messages to the user.
 To print the instructions, send an array of strings to Sysout.createDialog
  WithInstructions method.  Put one line of instructions per array entry.
 To display a message for the tester to see, simply call Sysout.println
  with the string to be displayed.
 This mimics System.out.println but works within the test harness as well
  as standalone.
 */

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
