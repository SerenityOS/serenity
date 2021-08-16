/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
/*
 * test
 * @bug 4666224
*/

public class bug4666224 extends JApplet {
    final static int placements[] = { JTabbedPane.LEFT, JTabbedPane.RIGHT, JTabbedPane.TOP, JTabbedPane.BOTTOM };
    private JTabbedPane tabPane;
    private JPanel mainPanel;

    public bug4666224() throws Exception {
       java.awt.EventQueue.invokeAndWait( () -> {
           tabPane = new JTabbedPane();
           tabPane.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Point pt = e.getPoint();
                    System.out.println("Index at location: "
                        + tabPane.indexAtLocation(pt.x, pt.y));
                }
            });
            InputMap inputMap = createInputMap();
            SwingUtilities.replaceUIInputMap(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
            ActionMap actionMap = createActionMap();
            SwingUtilities.replaceUIActionMap(getRootPane(), actionMap);

            //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            tabPane.setTabPlacement(JTabbedPane.TOP);
            tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Zero", panel);
            panel = new JPanel();
            //panel.requestFocus();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number One", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Two", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Three", new JColorChooser());
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Four", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Five", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Six", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Seven", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Eight", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Nine", panel);
            panel = new JPanel();
            panel.setPreferredSize(new Dimension(200, 300));
            tabPane.addTab("Number Ten", panel);
            mainPanel = new JPanel();
            mainPanel.add(tabPane);

            getContentPane().add(mainPanel);
                    tabPane.requestFocus();


            //pack();
            //setVisible(true);
        });
    }

    public void init() {
                String[][] instructionsSet =
                {
                        {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                "1. Click on any of the tabs, focus indicator is visible",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },
                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                    "1. type 'C' to change the tab layout to WRAP_TAB_LAYOUT ",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },

                        {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                    "1. type 'R' to align the tabs to the right side ",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },

                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                "1. type 'B' to align the tabs to the bottom side ",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },

                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                    "1. type 'L' to align the tabs to the left side ",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },


                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                        " ",
                    " ON ALL PLATFORMS",
                        "1. type 'T' to align the tabs to the top side ",
                        "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },

                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                " ",
                    " ON ALL PLATFORMS",
                        "1. type 'B' to align the tabs to the bottom side ",
                        "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                },

                {
                                " Note : Incase of Assertion failure,user can enter",
                                " remarks by pressing 'Assertion Fail Remarks ' button",
                                " ",
                                " You would see an applet with JTabbedPane. Keep the size of applet variable.",
                        " ",
                    " ON ALL PLATFORMS",
                    "1. type 'R' to align the tabs to the right side ",
                    "2. Lose focus on the window by clicking on some other window ",
                    "3. Focus indicator should disappear",
                    "4. Regain focus on the window the focus indicator should reappear." ,
                    " If focus doesn't behave as above, ",
                    " press 'Assertion Fail' else press 'Assertion Pass'",
                }
                };

                String[] exceptionsSet =
                {
                        "Focus painted incorrectly in tabbed pane(SCROLL_TAB_LAYOUT) when tabs aligned to the TOP of the window",
                        "Focus painted incorrectly in tabbed pane(SCROLL_TAB_LAYOUT) when tabs aligned to the RIGHT of the window",
                        "Focus painted incorrectly in tabbed pane(SCROLL_TAB_LAYOUT) when tabs aligned to the BOTTOM of the window",
                        "Focus painted incorrectly in tabbed pane(SCROLL_TAB_LAYOUT) when tabs aligned to the LEFT of the window",
                        "Focus painted incorrectly in tabbed pane(WRAP_TAB_LAYOUT) when tabs aligned to the LEFT of the window",
                        "Focus painted incorrectly in tabbed pane(WRAP_TAB_LAYOUT) when tabs aligned to the TOP of the window",
                        "Focus painted incorrectly in tabbed pane(WRAP_TAB_LAYOUT) when tabs aligned to the BOTTOM of the window",
                        "Focus painted incorrectly in tabbed pane(WRAP_TAB_LAYOUT) when tabs aligned to the RIGHT of the window"
                };

                Sysout.setInstructionsWithExceptions(instructionsSet,exceptionsSet);

        }

        public void start (){}

        public void destroy(){
            if(Sysout.failStatus())    {
                String failMsg = Sysout.getFailureMessages();
                failMsg = failMsg.replace('\n',' ');
                throw new RuntimeException(failMsg);
            }// End destroy
    }

    protected InputMap createInputMap() {
        return LookAndFeel.makeComponentInputMap(getRootPane(), new Object[] {
            "R", "right",
            "L", "left",
            "T", "top",
            "B", "bottom",
            "C", "changeLayout",
            "D", "dump"
        });
    }

    protected ActionMap createActionMap() {
        ActionMap map = new ActionMap();
        map.put("right", new RotateAction(JTabbedPane.RIGHT));
        map.put("left", new RotateAction(JTabbedPane.LEFT));
        map.put("top", new RotateAction(JTabbedPane.TOP));
        map.put("bottom", new RotateAction(JTabbedPane.BOTTOM));
        map.put("changeLayout", new ChangeLayoutAction());
        map.put("dump", new DumpAction());
        return map;
    }

    private class RotateAction extends AbstractAction {
        private int placement;
        public RotateAction(int placement) {
            this.placement = placement;
        }

        public void actionPerformed(ActionEvent e) {
            tabPane.setTabPlacement(placement);
        }
    }

    private class ChangeLayoutAction extends AbstractAction {
        private boolean a = true;
        public void actionPerformed(ActionEvent e) {
            if (a) {
                tabPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
                a = false;
            } else {
                tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                a = true;
            }
        }
    }

    private class DumpAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                System.out.println("Tab: " + i + " "
                    + tabPane.getUI().getTabBounds(tabPane, i));
            }
        }
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

   public static void setInstructionsWithExceptions(String instructionsSet[][],
                                                    String exceptionsSet[]) {
       createDialogWithInstructions(instructionsSet[0]);
       dialog.setInstructions(instructionsSet);
       dialog.setExceptionMessages(exceptionsSet);
   }

   public static String getFailureMessages()   {
       return dialog.failureMessages;
   }

   public static boolean failStatus()  {
       return dialog.failStatus;
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
   int maxStringLength = 70;

   Panel assertPanel;
   Button assertPass,assertFail,remarks;
   HandleAssert handleAssert;
   boolean failStatus=false;
   int instructionCounter=0;
   String instructions[][];
   int exceptionCounter=0;
   String exceptionMessages[];
   String failureMessages="<br>";
   String remarksMessage=null;
   RemarksDialog remarksDialog;

   //DO NOT call this directly, go through Sysout
   public TestDialog( Frame frame, String name )
    {
      super( frame, name );
      int scrollBoth = TextArea.SCROLLBARS_BOTH;
      instructionsText = new TextArea( "", 14, maxStringLength, scrollBoth );
      add( "North", instructionsText );

      messageText = new TextArea( "", 3, maxStringLength, scrollBoth );
      add("Center", messageText);

      assertPanel = new Panel(new FlowLayout());
      assertPass=new Button("Assertion Pass");
      assertPass.setName("Assertion Pass");
      assertFail=new Button("Assertion Fail");
      assertFail.setName("Assertion Fail");
      remarks = new Button("Assertion Fail Remarks");
      remarks.setEnabled(false);
      remarks.setName("Assertion Remarks");
      assertPanel.add(assertPass);
      assertPanel.add(assertFail);
      assertPanel.add(remarks);
      handleAssert = new HandleAssert();
      assertPass.addActionListener(handleAssert);
      assertFail.addActionListener(handleAssert);
      remarks.addActionListener(handleAssert);
      add("South",assertPanel);
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

   public void emptyMessage()   {
       messageText.setText("");
   }

   public void setInstructions(String insStr[][])    {
       instructions=insStr;
   }

   public void setExceptionMessages(String exceptionMessages[])   {
       this.exceptionMessages=exceptionMessages;
   }

   class HandleAssert implements ActionListener   {
        public void actionPerformed(ActionEvent ae)    {
           if(ae.getSource()==remarks)  {
               remarksDialog = new RemarksDialog(TestDialog.this,
                                        "Assertion Remarks Dialog",true);
               remarks.setEnabled(false);
               if(remarksMessage!=null)
                    failureMessages+=". User Remarks : "+remarksMessage;
           }
           else {
               if(instructionCounter<instructions.length-1) {
                   emptyMessage();
                   instructionCounter++;
                   printInstructions(instructions[instructionCounter]);
               }
               else {
                   emptyMessage();
                   displayMessage("Testcase Completed");
                   displayMessage("Press 'Done' button in the "+
                                                    "BaseApplet to close");
                   assertPass.setEnabled(false);
                   assertFail.setEnabled(false);
               }

               if(ae.getSource()==assertPass)    {
                   // anything to be done in future
               }
               else if(ae.getSource()==assertFail)   {
                   remarks.setEnabled(true);
                   if(!failStatus)
                       failStatus=true;
                   if(exceptionCounter<exceptionMessages.length)   {
                        failureMessages = failureMessages + "<br>"+
                                    exceptionMessages[exceptionCounter];
                   }
               }
               exceptionCounter++;
           }
        }
    }

    class RemarksDialog extends Dialog  implements ActionListener{
        Panel rootPanel,remarksPanel;
        TextArea textarea;
        Button addRemarks,cancelRemarks;
        public RemarksDialog(Dialog owner,String title,boolean modal)  {
            super(owner,title,modal);
            rootPanel = new Panel(new BorderLayout());
            remarksPanel = new Panel(new FlowLayout());
            textarea = new TextArea(5,30);
            addRemarks=new Button("Add Remarks");
            addRemarks.addActionListener(this);
            cancelRemarks = new Button("Cancel Remarks");
            cancelRemarks.addActionListener(this);
            remarksPanel.add(addRemarks);
            remarksPanel.add(cancelRemarks);
            rootPanel.add(textarea,"Center");
            rootPanel.add(remarksPanel,"South");
            add(rootPanel);
            setBounds(150,150,400,200);
            setVisible(true);
        }

        public void actionPerformed(ActionEvent ae) {
            remarksMessage=null;
            if(ae.getSource()==addRemarks)  {
                String msg = textarea.getText().trim();
                if (msg.length()>0)
                    remarksMessage=msg;
            }
            dispose();
        }

    }

}// TestDialog  class
