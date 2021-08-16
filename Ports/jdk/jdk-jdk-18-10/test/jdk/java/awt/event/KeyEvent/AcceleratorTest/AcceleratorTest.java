/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6680988
  @summary verify that various shortcuts and accelerators work
  @author yuri.nesterenko : area=awt.keyboard
  @run applet/manual=yesno AcceleratorTest.html
*/

/**
 * AcceleratorTest.java
 *
 * summary:
 */

//import java.applet.Applet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;


public class AcceleratorTest extends JApplet
{
    //Declare things used in the test, like buttons and labels here
    static int pressed = 0;
    Hashtable<String, Integer> cmdHash = new Hashtable<String, Integer>();
    String[] CMD = {
        "\u042E, keep me in focus",
        "Item Cyrl Be",
        "Item English Period",
        "Item English N",
        "\u0436"
    };

    JFrame jfr;

    public void init()
    {
        //Create instructions for the user here, as well as set up
        // the environment -- set the layout manager, add buttons,
        // etc.
        this.setLayout (new BorderLayout ());

        String[] instructions =
        {
            " Ensure you have Russian keyboard layout as a currently active.",
            "(1) Press Ctrl+\u0411 (a key with \",<\" on it) ",
            "(2) Find a . (period) in this layout (perhaps \"/?\" or \"7&\" key).",
            "Press Ctrl+.",
            "(3) Press Crtl+ regular English . (period) key (on \".>\" )",
            "(4) Press Ctrl+ key with English N.",
            "(5) Press Alt+\u042E (key with \".>\")",
            "(6) Press Alt+\u0436 (key with \";:\")",
            "If all expected commands will be fired, look for message",
            "\"All tests passed\""
        };
        Sysout.createDialogWithInstructions( instructions );
        for(int i = 0; i < CMD.length; i++) {
            cmdHash.put(CMD[i], 0);
        }

        jfr = new JFrame();
        JButton jbu;
        jfr.add((jbu = new JButton(CMD[0])));
        jbu.setMnemonic(java.awt.event.KeyEvent.getExtendedKeyCodeForChar('\u042E'));
        jbu.addActionListener( new ALi(CMD[0]));


        JMenuBar menuBar = new JMenuBar();
        jfr.setJMenuBar(menuBar);
        JMenu menu = new JMenu("Menu");
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem(CMD[1]);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.getExtendedKeyCodeForChar('\u0431'),
                        InputEvent.CTRL_DOWN_MASK));

        JMenuItem menuItemEnglish = new JMenuItem(CMD[2]);
        menuItemEnglish.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD,
                        InputEvent.CTRL_DOWN_MASK));
        JMenuItem menuItemE1 = new JMenuItem(CMD[3]);
        menuItemE1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                        InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener( new ALi(CMD[1]));
        menuItemEnglish.addActionListener( new ALi(CMD[2]));
        menuItemE1.addActionListener( new ALi(CMD[3]));
        menu.add(menuItem);
        menu.add(menuItemEnglish);
        menu.add(menuItemE1);

        KeyStroke ks;
        InputMap im = new InputMap();
        ks = KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar('\u0436'), java.awt.event.InputEvent.ALT_DOWN_MASK);
        im.put(ks, "pushAction");
        im.setParent(jbu.getInputMap(JComponent.WHEN_FOCUSED));
        jbu.setInputMap(JComponent.WHEN_FOCUSED, im);

        jbu.getActionMap().put("pushAction",
            new AbstractAction("pushAction") {
                  public void actionPerformed(ActionEvent evt) {
                      if( evt.getActionCommand().equals(CMD[4])) {
                          cmdHash.put(CMD[4], 1);
                      }
                      boolean notYet = false;
                      for(int i = 0; i < CMD.length; i++) {
                          if(cmdHash.get(CMD[i]) == 0 ) notYet = true;
                      }
                      Sysout.println("Fired");
                      if( !notYet ) {
                          Sysout.println("All tests passed.");
                      }
                  }
            }
        );


        jfr.setBounds(650,0,200,200);
        jfr.setVisible(true);

    }//End  init()

    public void start ()
    {
        //Get things going.  Request focus, set size, et cetera
        setSize (200,200);
        setVisible(true);
        validate();

    }// start()
    public class ALi implements ActionListener {
        String expectedCmd;
        public ALi( String eCmd ) {
            expectedCmd = eCmd;
        }
        public void actionPerformed(ActionEvent ae) {
            if( cmdHash.containsKey(ae.getActionCommand()) ) {
                cmdHash.put(expectedCmd, 1);
            }
            boolean notYet = false;
            for(int i = 0; i < CMD.length; i++) {
                if(cmdHash.get(CMD[i]) == 0 ) notYet = true;
                //Sysout.println(CMD[i]+":"+cmdHash.get(CMD[i]));
            }
            Sysout.println("FIRED");
            if( !notYet ) {
                Sysout.println("All tests passed.");
            }
        }
    }


}// class AcceleratorTest

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
    private static boolean numbering = false;
    private static int messageNumber = 0;

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

    /* Enables message counting for the tester. */
    public static void enableNumbering(boolean enable){
        numbering = enable;
    }

    public static void printInstructions( String[] instructions )
    {
        dialog.printInstructions( instructions );
    }


    public static void println( String messageIn )
    {
        if (numbering) {
            messageIn = "" + messageNumber + " " + messageIn;
            messageNumber++;
        }
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
