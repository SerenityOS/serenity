/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug 4828019
  @summary Frame/Window deadlock
  @run main/timeout=9999 NonEDT_GUI_Deadlock
*/

import java.awt.*;

public class NonEDT_GUI_Deadlock {
    boolean bOK = false;
    Thread badThread = null;

    public void start ()
    {
        final Frame theFrame = new Frame("Window test");
        theFrame.setSize(240, 200);

        Thread thKiller = new Thread() {
           public void run() {
              try {
                 Thread.sleep( 9000 );
              }catch( Exception ex ) {
              }
              if( !bOK ) {
                 // oops,
                 //System.out.println("Deadlock!");
                 Runtime.getRuntime().halt(0);
              }else{
                 //System.out.println("Passed ok.");
              }
           }
        };
        thKiller.setName("Killer thread");
        thKiller.start();
        Window w = new TestWindow(theFrame);
        theFrame.toBack();
        theFrame.setVisible(true);

        theFrame.setLayout(new FlowLayout(FlowLayout.CENTER));
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               bOK = true;
           }
        });



    }// start()
    class TestWindow extends Window implements Runnable {

        TestWindow(Frame f) {
            super(f);

            //setSize(240, 75);
            setLocation(0, 75);

            show();
            toFront();

            badThread = new Thread(this);
            badThread.setName("Bad Thread");
            badThread.start();

        }

        public void paint(Graphics g) {
            g.drawString("Deadlock or no deadlock?",20,80);
        }

        public void run() {

            long ts = System.currentTimeMillis();

            while (true) {
                if ((System.currentTimeMillis()-ts)>3000) {
                    this.setVisible( false );
                    dispose();
                    break;
                }

                toFront();
                try {
                    Thread.sleep(80);
                } catch (Exception e) {
                }
            }
        }
    }



    public static void main(String args[]) {
       NonEDT_GUI_Deadlock imt = new NonEDT_GUI_Deadlock();
       imt.start();
    }


}// class NonEDT_GUI_Deadlock
