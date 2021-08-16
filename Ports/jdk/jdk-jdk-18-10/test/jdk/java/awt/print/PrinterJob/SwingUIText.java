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
 * @bug 6488219 6560738 7158350 8017469
 * @summary Test that text printed in Swing UI measures and looks OK.
 * @run main/manual=yesno PrintTextTest
 */

import java.awt.*;
import javax.swing.*;
import java.awt.print.*;

public class SwingUIText implements Printable {

    static String[] instructions = {
        "This tests that when a Swing UI is printed, that the text",
        "in each component properly matches the length of the component",
        "as seen on-screen, and that the spacing of the text is of",
        "reasonable even-ness. This latter part is very subjective and",
        "the comparison has to be with JDK1.5 GA, or JDK 1.6 GA",
    };

    static JFrame frame;

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              createUI();
          }
      });
    }

    public static void createUI() {

        Sysout.createDialogWithInstructions(instructions);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4,1));

        String text = "marvelous suspicious solving";
        displayText(panel, text);

        String itext = "\u0641\u0642\u0643 \u0644\u0627\u064b";
        itext = itext+itext+itext+itext+itext+itext+itext;
        displayText(panel, itext);

        String itext2 = "\u0641"+text;
        displayText(panel, itext2);

        JEditorPane editor = new JEditorPane();
        editor.setContentType("text/html");
        String CELL = "<TD align=\"center\"><font style=\"font-size: 18;\">Text</font></TD>";
        String TABLE_BEGIN = "<TABLE BORDER=1 cellpadding=1 cellspacing=0 width=100%>";
        String TABLE_END = "</TABLE>";
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html><body>").append(TABLE_BEGIN);
        for (int j = 0; j < 15; j++) {
            buffer.append(CELL);
        }
        buffer.append("</tr>");
        buffer.append(TABLE_END).append("</body></html>");
        editor.setText(buffer.toString());

        panel.add(editor);

        frame = new JFrame("Swing UI Text Printing Test");
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);

        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        job.setPrintable(new SwingUIText(), pf);
        if (job.printDialog()) {
            try { job.print(); }
            catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
        }
    }


    static void displayText(JPanel p, String text) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2,1));
        JPanel row = new JPanel();
        Font font = new Font("Dialog", Font.PLAIN, 12);

        JLabel label = new JLabel(text);
        label.setFont(font);
        row.add(label);

        JButton button = new JButton("Print "+text);
        button.setMnemonic('P');
        button.setFont(font);
        row.add(button);

        panel.add(row);

        row = new JPanel();
        JTextField textField = new JTextField(text);
        row.add(textField);

        JTextArea textArea = new JTextArea();
        textArea.setText(text);
        row.add(textArea);

        panel.add(row);
        p.add(panel);
    }

    public int print(Graphics g, PageFormat pf, int pageIndex)
        throws PrinterException {

        if (pageIndex >= 1) {
            return Printable.NO_SUCH_PAGE;
        }
        g.translate((int)pf.getImageableX(), (int)pf.getImageableY());
        frame.printAll(g);

        return Printable.PAGE_EXISTS;
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
      instructionsText = new TextArea( "", 10, maxStringLength, scrollBoth );
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
