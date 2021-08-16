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
 * @test %I %W
 * @bug 4298489
 * @summary Confirm that output is same as screen.
 * @author jgodinez
 * @run main/manual PrintImage
 */
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;

public class PrintImage extends Frame implements ActionListener {

        private PrintImageCanvas                printImageCanvas;

        private MenuItem        print1Menu = new MenuItem("PrintTest1");
        private MenuItem        print2Menu = new MenuItem("PrintTest2");
        private MenuItem        exitMenu = new MenuItem("Exit");

        public static void main(String[] argv) {
        String[] instructions =
           { "You must have a printer available to perform this test,",
             "prefererably Canon LaserShot A309GII.",
             "Printing must be done in Win 98 Japanese 2nd Edition.",
             "",
             "Passing test : Output of text image for PrintTest1 and PrintTest2 should be same as that on the screen.",
           };

        Sysout.createDialog( );
         Sysout.printInstructions( instructions );

                new PrintImage();
        }

        public PrintImage() {
                super("PrintImage");
                initPrintImage();
        }

        public void initPrintImage() {

                printImageCanvas = new PrintImageCanvas(this);

                initMenu();

                addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent ev) {
                                dispose();
                        }
                        public void windowClosed(WindowEvent ev) {
                                System.exit(0);
                        }
                });

                setLayout(new BorderLayout());
                add(printImageCanvas, BorderLayout.CENTER);
                pack();

                setSize(500,500);
                setVisible(true);
        }

        private void initMenu() {
                MenuBar         mb = new MenuBar();
                Menu            me = new Menu("File");
                me.add(print1Menu);
                me.add(print2Menu);
                me.add("-");
                me.add(exitMenu);
                mb.add(me);
                this.setMenuBar(mb);

                print1Menu.addActionListener(this);
                print2Menu.addActionListener(this);
                exitMenu.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
                Object target = e.getSource();
                if( target.equals(print1Menu) ) {
                        printMain1();
                }
                else if( target.equals(print2Menu) ) {
                        printMain2();
                }
                else if( target.equals(exitMenu) ) {
                        dispose();
                }
        }

        private void printMain1(){

                PrinterJob printerJob = PrinterJob.getPrinterJob();
                PageFormat pageFormat = printerJob.defaultPage();

                printerJob.setPrintable((Printable)printImageCanvas, pageFormat);

                if(printerJob.printDialog()){
                        try {
                                printerJob.print();
                        }
                        catch(PrinterException p){
                        }
                }
                else
                        printerJob.cancel();
        }

        private void printMain2(){

                PrinterJob printerJob = PrinterJob.getPrinterJob();
                PageFormat pageFormat = printerJob.pageDialog(printerJob.defaultPage());

                printerJob.setPrintable((Printable)printImageCanvas, pageFormat);

                if(printerJob.printDialog()){
                        try {
                                printerJob.print();
                        }
                        catch(PrinterException p){
                        }
                }
                else
                        printerJob.cancel();
        }

}

class PrintImageCanvas extends Canvas implements Printable {

        private PrintImage pdsFrame;

        public PrintImageCanvas(PrintImage pds) {
                pdsFrame = pds;
        }

        public void paint(Graphics g) {
                Font drawFont = new Font("MS Mincho",Font.ITALIC,50);
                g.setFont(drawFont);
                g.drawString("PrintSample!",100,150);
        }

        public int print(Graphics g, PageFormat pf, int pi)
                throws PrinterException {

                if(pi>=1)
                        return NO_SUCH_PAGE;
                else{
                        Graphics2D g2 = (Graphics2D)g;
                        g.setColor(new Color(0,0,0,200));

                        Font drawFont = new Font("MS Mincho",Font.ITALIC,50);
                        g.setFont(drawFont);
                        g.drawString("PrintSample!",100,150);
                        return PAGE_EXISTS;
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
