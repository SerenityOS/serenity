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
 * @bug 4937672 5100706 6252456
 * @run main/othervm/manual -Djava.security.manager=allow SecurityDialogTest
 */

import java.awt.* ;
import java.awt.print.* ;
import java.io.*;
import java.security.*;
import javax.print.*;
import javax.print.attribute.*;

public class SecurityDialogTest {


    public static void main ( String args[] ) {

        String[] instructions =
           {
            "You must have a printer available to perform this test.",
            "This test brings up a native and cross-platform page and",
            "print dialogs.",
            "The dialogs should be displayed even when ",
            "there is no queuePrintJob permission.",
            "If the dialog has an option to save to file, the option ought",
            "to be disabled if there is no read/write file permission.",
            "You should test this by trying different policy files."
          };

         Sysout.createDialog( );
         Sysout.printInstructions( instructions );

        SecurityDialogTest pjc = new SecurityDialogTest() ;
    }


  public SecurityDialogTest() {

      PrinterJob pj = PrinterJob.getPrinterJob() ;

      // Install a security manager which does not allow reading and
      // writing of files.
      //PrintTestSecurityManager ptsm = new PrintTestSecurityManager();
      SecurityManager ptsm = new SecurityManager();

      try {
          System.setSecurityManager(ptsm);
      } catch (SecurityException e) {
          System.out.println("Could not run test - security exception");
      }

      try {
          PrintJob pjob = Toolkit.getDefaultToolkit().getPrintJob(new Frame(), "Printing", null, null);
          Sysout.println("If the value of pjob is null, the test fails.\n");
          Sysout.println("        pjob = "+pjob);
      } catch (SecurityException e) {
      }

      PrintService[] services = PrinterJob.lookupPrintServices();
      for (int i=0; i<services.length; i++) {
          System.out.println("SecurityDialogTest service "+i+" : "+services[i]);
      }

      PrintService defservice = pj.getPrintService();
      System.out.println("SecurityDialogTest default service : "+defservice);

      System.out.println("SecurityDialogTest native PageDialog ");
      PageFormat pf1 = pj.pageDialog(new PageFormat());

      System.out.println("SecurityDialogTest swing PageDialog ");
      PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
      PageFormat pf2 = pj.pageDialog(attributes);

      // With the security manager installed, save to file should now
      // be denied.
      System.out.println("SecurityDialogTest native printDialog ");
      pj.printDialog();

      System.out.println("SecurityDialogTest swing printDialog ");
      pj.printDialog(attributes);
  }


    class PrintTestSecurityManager extends SecurityManager {
        public void checkPackageAccess(String pkg) {
        }
        public void checkPropertyAccess(String key) {
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
