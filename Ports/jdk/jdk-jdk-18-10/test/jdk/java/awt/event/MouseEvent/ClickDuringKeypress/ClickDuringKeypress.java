/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test 1.2 98/08/05
  @key headful
  @bug 4515763
  @summary Tests that clicking mouse and pressing keys generates correct amount of click-counts
  @author andrei.dmitriev: area=awt.mouse
  @run main ClickDuringKeypress
*/

/**
 * ClickDuringKeypress.java
 *
 * summary:
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

public class ClickDuringKeypress implements MouseListener
 {
   //Declare things used in the test, like buttons and labels here
   final static int CLICKCOUNT = 10;
   final static int DOUBLE_CLICK_AUTO_DELAY = 10;
   volatile int lastClickCount = 0;
   volatile boolean clicked = false;
   volatile boolean ready = false;

   Frame frame;
   Robot robot;

   public void init()
    {
      //Create instructions for the user here, as well as set up
      // the environment -- set the layout manager, add buttons,
      // etc.

      frame = new Frame("ClickDuringKeypress");
      frame.addMouseListener(this);
      frame.addWindowListener(new WindowAdapter() {
          public void windowActivated(WindowEvent e) {
              synchronized(ClickDuringKeypress.this) {
                  ready = true;
                  ClickDuringKeypress.this.notifyAll();
              }
          }
      });
      frame.setBounds(0, 0, 400, 400);

      start();

    }//End  init()

   public void start ()
    {
      try {
        robot = new Robot();
      } catch (AWTException e) {
        System.out.println("Could not create Robot.");
        throw new RuntimeException("Couldn't create Robot.  Test fails");
      }

      robot.mouseMove(200, 200);
      frame.show();

      synchronized(this) {
          try {
              if (!ready) {
                  wait(10000);
              }
          } catch (InterruptedException ex) {
          }
          if (!ready) {
              System.out.println("Not Activated. Test fails");
              throw new RuntimeException("Not Activated. Test fails");
          }
      }

      doTest();

      //What would normally go into main() will probably go here.
      //Use System.out.println for diagnostic messages that you want
      //to read after the test is done.
      //Use Sysout.println for messages you want the tester to read.

    }// start()

    // Mouse should be over the Frame by this point
    private void doTest() {
      robot.setAutoDelay(2000);
      robot.waitForIdle();
      robot.keyPress(KeyEvent.VK_B);
      robot.mousePress(InputEvent.BUTTON1_MASK);
      robot.delay(10);
      robot.mouseRelease(InputEvent.BUTTON1_MASK);
      // Should trigger mouseClicked
      robot.keyRelease(KeyEvent.VK_B);
      robot.delay(1000);

      robot.setAutoDelay(DOUBLE_CLICK_AUTO_DELAY);
      for (int i = 0; i < CLICKCOUNT / 2; i++) {
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(10);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.keyPress(KeyEvent.VK_B);
        robot.delay(10);
        robot.keyRelease(KeyEvent.VK_B);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(10);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
      }
      robot.waitForIdle();
      // check results
      robot.delay(200);
      if (!clicked) {
          System.out.println("No MOUSE_CLICKED events received.  Test fails.");
          throw new RuntimeException("No MOUSE_CLICKED events received.  Test fails.");
      }
      if (lastClickCount != CLICKCOUNT) {
          System.out.println("Actual click count: " + lastClickCount + " does not match expected click count: " + CLICKCOUNT + ".  Test fails");
          throw new RuntimeException("Actual click count: " + lastClickCount + " does not match expected click count: " + CLICKCOUNT + ".  Test fails");

      }
      // else test passes.
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {
        System.out.println(e.toString());
        clicked = true;
        lastClickCount = e.getClickCount();
    }

     public static void main(String[] args) {
         new ClickDuringKeypress().init();
     }

 }// class ClickDuringKeypress
