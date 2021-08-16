/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
  test
  @bug 6243382 8006070
  @summary Dragging of mouse outside of a List and Choice area don't work properly on XAWT
  @author Dmitry.Cherepanov@SUN.COM area=awt.list
  @run applet/manual=yesno MouseDraggedOutCauseScrollingTest.html
*/

import java.applet.Applet;
import java.awt.*;

public class MouseDraggedOutCauseScrollingTest extends Applet
{
    Choice choice;
    List singleList;
    List multipleList;

    public void init()
    {
        this.setLayout (new GridLayout (1, 3));

        choice = new Choice();
        singleList = new List(3, false);
        multipleList = new List(3, true);

        choice.add("Choice");
        for (int i = 1; i < 100; i++){
            choice.add(""+i);
        }

        singleList.add("Single list");
        for (int i = 1; i < 100; i++)
            singleList.add(""+i);

        multipleList.add("Multiple list");
        for (int i = 1; i < 100; i++)
            multipleList.add(""+i);

        this.add(choice);
        this.add(singleList);
        this.add(multipleList);

        String toolkitName = Toolkit.getDefaultToolkit().getClass().getName();
        if (!toolkitName.equals("sun.awt.X11.XToolkit")) {
            String[] instructions =
            {
                "This test is not applicable to the current platform. Press PASS"
            };
            Sysout.createDialogWithInstructions( instructions );
        } else {
            String[] instructions =
            {
                "0) Please note, that this is only Motif/XAWT test. At first, make the applet active",
                "1.1) Click on the choice",
                "1.2) Press the left button of the mouse and keep on any item of the choice, for example 5",
                "1.3) Drag mouse out of the area of the unfurled list, at the same time hold the X coordinate of the mouse position about the same",
                "1.4) To make sure, that when the Y coordinate of the mouse position higher of the upper bound of the list then scrolling UP of the list and selected item changes on the upper. If not, the test failed",
                "1.5) To make sure, that when the Y coordinate of the mouse position under of the lower bound of the list then scrolling DOWN of the list and selected item changes on the lower. If not, the test failed",
                "-----------------------------------",
                "2.1) Click on the single list",
                "2.2) Press the left button of the mouse and keep on any item of the list, for example 5",
                "2.3) Drag mouse out of the area of the unfurled list, at the same time hold the X coordinate of the mouse position about the same",
                "2.4) To make sure, that when the Y coordinate of the mouse position higher of the upper bound of the list then scrolling UP of the list and selected item changes on the upper. If not, the test failed",
                "2.5) To make sure, that when the Y coordinate of the mouse position under of the lower bound of the list then scrolling DOWN of the list and selected item changes on the lower. If not, the test failed",
                "-----------------------------------",
                "3.1) Click on the multiple list",
                "3.2) Press the left button of the mouse and keep on any item of the list, for example 5",
                "3.3) Drag mouse out of the area of the unfurled list, at the same time hold the X coordinate of the mouse position about the same",
                "3.4) To make sure, that when the Y coordinate of the mouse position higher of the upper bound of the list then scrolling of the list NO OCCURED and selected item NO CHANGES on the upper. If not, the test failed",
                "3.5) To make sure, that when the Y coordinate of the mouse position under of the lower bound of the list then scrolling of the list NO OCCURED and selected item NO CHANGES on the lower. If not, the test failed",
                "4) Test passed."
            };
            Sysout.createDialogWithInstructions( instructions );
        }

    }//End  init()

    public void start ()
    {
        setSize (400,100);
        setVisible(true);
        validate();

    }// start()

}// class ManualYesNoTest

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
        dialog.setVisible(true);
        println( "Any messages for the tester will display here." );
    }

    public static void createDialog( )
    {
        dialog = new TestDialog( new Frame(), "Instructions" );
        String[] defInstr = { "Instructions will appear here. ", "" } ;
        dialog.printInstructions( defInstr );
        dialog.setVisible(true);
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
        add("Center", messageText);

        pack();

        setVisible(true);
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
        System.out.println(messageIn);
    }

}// TestDialog  class
