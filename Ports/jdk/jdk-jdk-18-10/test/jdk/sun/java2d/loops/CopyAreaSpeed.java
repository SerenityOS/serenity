/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4189070
 * @summary This test prints out the time it takes for a certain amount of
 * copyArea calls to be completed. Because the performance measurement is
 * relative, this code only provides a benchmark to run with different releases
 * to compare the outcomes.
 * @run applet/manual=done CopyAreaSpeed.html
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CopyAreaSpeed extends Applet implements Runnable {
    int top = 0;

    public void init() {
    }

    public CopyAreaSpeed()
    {
        super();
        String[] instructions =
        {
            "This test prints out the time it takes for a certain amount ",
            "of copyArea calls to be completed. Because the performance ",
            "measurement is relative, this code only provides a benchmark ",
            "to run with different releases to compare the outcomes."
        };
        Sysout.createDialogWithInstructions( instructions );
        (new Thread(this)).start();
        Button bt = new Button("Hello");
        bt.setBounds(50, 10, 50, 22);
        bt.setVisible(false);
        add(bt);
    }

    public void update(Graphics g)
    {
        paint(g);
    }

    public void paint(Graphics g)
    {
        synchronized(this) {
            Rectangle rct = g.getClipBounds();
            g.setColor(Color.white);
            g.fillRect(rct.x, rct.y, rct.width, rct.height);
            g.setFont(getFont());
            g.setColor(Color.black);

            Dimension dm = getSize();
            for (int y = 0; y <= (dm.height + 10); y += 20) {
                if (y > rct.y) {
                    int z = y / 20 + top;
                    g.drawString("" + z, 10, y);
                }               /* endif */
            }                   // endfor
        }
    }

    static long millsec(Date s, Date e) {
        long ts = s.getTime();
        long te = e.getTime();
        return te-ts;
    }

    public void run()
    {
        int count = 1000;
        int loops = count;
        Date start;
        Date end;

        start = new Date();
        while (count-- > 0) {
            Dimension dm = getSize();
            if (dm != null && dm.width != 0 && dm.height != 0) {
                synchronized(this) {
                    top++;
                    Graphics g = getGraphics();
                    g.copyArea(0, 20, dm.width, dm.height - 20, 0, -20);
                    g.setClip(0, dm.height - 20, dm.width, 20);
                    paint(g);
                    g.dispose();
                }
            }
            try {
                Thread.sleep(1);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        end = new Date();
        Sysout.println("copyArea X "+loops+" = "+ millsec(start, end) + " msec");
    }

    public static void main(String args[]) {
        Frame frm = new Frame("CopyAreaSpeed");
        frm.add(new CopyAreaSpeed());
        frm.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                System.exit(0);
            }
        });
        frm.setSize(500, 500);
        frm.show();
    }
}
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

                   if( posOfSpace <= 0 ) {
                       posOfSpace = maxStringLength - 1;
                   }

                   printStr = remainingStr.substring( 0, posOfSpace + 1 );
                   remainingStr = remainingStr.substring( posOfSpace + 1 );
                }
                else //else just print
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
