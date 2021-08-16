/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
  test %W% %E%  %I%, %G%
  @bug 6315717
  @summary  manual control over the Robot
  @author Andrei Dmitriev : area=awt.robot
  @run applet/manual=yesno ManualInstructions.html
*/

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

public class ManualInstructions extends Applet
{
    final static long SEND_DELAY = 1000;

    public static void main(String s[]){
        ManualInstructions mi = new ManualInstructions();
        mi.init();
        mi.start();
    }

    static Robot robot;
    Point mouseLocation; //where mouse should be pressed each time
    Panel target = new Panel();
    Button pressOn = new Button("press on ...");
    Button releaseOn = new Button("release on ...");
    Button clickOn = new Button("click on ...");
    Choice buttonNumber = new Choice();

    public void init()
    {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        this.setLayout (new BorderLayout ());

        target.setBackground(Color.green);
        target.setName("GreenBox");//for the ease of debug
        target.setPreferredSize(new Dimension(100, 100));
        String toolkit = Toolkit.getDefaultToolkit().getClass().getName();

        // on X systems two buttons are reserved for wheel though they are countable by MouseInfo.
        int buttonsNumber = toolkit.equals("sun.awt.windows.WToolkit")?MouseInfo.getNumberOfButtons():MouseInfo.getNumberOfButtons()-2;

        for (int i = 0; i < 8; i++){
            buttonNumber.add("BUTTON"+(i+1)+"_MASK");
        }

        pressOn.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    System.out.println("Now pressing : " + (buttonNumber.getSelectedIndex()+1));

                    Timer timer = new Timer();
                    TimerTask robotInteraction = new TimerTask(){
                            public void run(){
                                robot.mouseMove(updateTargetLocation().x, updateTargetLocation().y);
                                robot.mousePress(getMask(buttonNumber.getSelectedIndex()+1));
                            }
                        };
                    timer.schedule(robotInteraction, SEND_DELAY);
                }
            });

        releaseOn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                System.out.println("Now releasing : " + (buttonNumber.getSelectedIndex()+1));
                Timer timer = new Timer();
                TimerTask robotInteraction = new TimerTask(){
                        public void run(){
                            robot.mouseMove(updateTargetLocation().x, updateTargetLocation().y);
                            robot.mouseRelease(getMask(buttonNumber.getSelectedIndex()+1));
                        }
                    };
                timer.schedule(robotInteraction, SEND_DELAY);
            }
        });

        clickOn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                System.out.println("Now clicking : " + (buttonNumber.getSelectedIndex()+1));
                Timer timer = new Timer();
                TimerTask robotInteraction = new TimerTask(){
                        public void run(){
                            robot.mouseMove(updateTargetLocation().x, updateTargetLocation().y);
                            robot.mousePress(getMask(buttonNumber.getSelectedIndex()+1));
                            robot.mouseRelease(getMask(buttonNumber.getSelectedIndex()+1));
                        }
                    };
                timer.schedule(robotInteraction, SEND_DELAY);
            }

        });
        target.addMouseListener(new MouseAdapter(){
           public void mousePressed(MouseEvent e){
                Sysout.println(""+e);
           }
           public void mouseReleased(MouseEvent e){
                Sysout.println(""+e);
           }
           public void mouseClicked(MouseEvent e){
                Sysout.println(""+e);
           }
        });

        String[] instructions =
        {
            "Do provide an instruction to the robot by",
            "choosing the button number to act and ",
            "pressing appropriate java.awt.Button on the left.",
            "Inspect an output in the TextArea below.",
            "Please don't generate non-natural sequences like Release-Release, etc.",
            "If you use keyboard be sure that you released the keyboard shortly.",
            "If events are generated well press Pass, otherwise Fail."
        };
        Sysout.createDialogWithInstructions( instructions );

    }//End  init()

    private int getMask(int button){
        return InputEvent.getMaskForButton(button);

        /*
            //this only works for standard buttons and for old JDK builds
        int mask = 0;
        switch (button){
        case 1: {
            mask = InputEvent.BUTTON1_MASK;
            break;
        }
        case 2: {
            mask = InputEvent.BUTTON2_MASK;
            break;
        }
        case 3: {
            mask = InputEvent.BUTTON3_MASK;
            break;
        }
        }
        return mask;
        */
    }

    private Point updateTargetLocation() {
        return new Point(target.getLocationOnScreen().x + target.getWidth()/2, target.getLocationOnScreen().y + target.getHeight()/2);
    }

    public void start ()
    {
        //Get things going.  Request focus, set size, et cetera
        setSize (200,200);
        setVisible(true);
        validate();
        Frame f = new Frame ("Set action for Robot here.");
        f.setLayout(new FlowLayout());
        f.add(buttonNumber);
        f.add(pressOn);
        f.add(releaseOn);
        f.add(clickOn);
        f.add(target);
        f.pack();
        f.setVisible(true);
     }// start()
}// class

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
    int maxStringLength = 120;

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
