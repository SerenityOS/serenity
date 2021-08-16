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
  @test
  @key headful
  @bug 6317481 8012325
  @summary REG:Pressing the mouse, dragging and releasing it outside the button triggers ActionEvent, XAWT
  @author Dmitry.Cherepanov@SUN.COM area=awt.event
  @run main EnterAsGrabbedEvent
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EnterAsGrabbedEvent
{
    //Declare things used in the test, like buttons and labels here
    private static Frame frame;
    private static Button button;
    private static volatile boolean enterTriggered = false;
    private static volatile boolean actionTriggered = false;

    private static void init()
    {
        frame = new Frame();
        frame.setLayout(new FlowLayout());
        button = new Button("button");
        button.addActionListener(actionEvent -> {
            actionTriggered = true;
        });
        frame.add(button);
        frame.setBounds(100, 100, 200, 200);
        frame.setVisible(true);
        frame.validate();
      }

    public static void main(String[] args) throws Exception {
        try {
            Robot r = new Robot();
            r.setAutoDelay(200);
            r.setAutoWaitForIdle(true);
            SwingUtilities.invokeAndWait(EnterAsGrabbedEvent::init);
            r.waitForIdle();

            Point loc = button.getLocationOnScreen();
            r.mouseMove(loc.x+button.getWidth()/2, loc.y+button.getHeight()/2);
            r.mousePress(InputEvent.BUTTON1_MASK);

            // in this case (drag mouse outside the button):
            // NotifyEnter (->MouseEnter) should be dispatched to the top-level
            // event if the grabbed window is the component (button)
            frame.addMouseListener(
                    new MouseAdapter() {
                        public void mouseEntered(MouseEvent me) {
                            System.out.println(me);
                            enterTriggered = true;
                        }

                        // Just for tracing
                        public void mouseExited(MouseEvent me) {
                            System.out.println(me);
                        }
                    });

            // Just for tracing
            button.addMouseListener(
                    new MouseAdapter(){
                        public void mouseEntered(MouseEvent me){
                            System.out.println(me);
                        }
                        public void mouseExited(MouseEvent me){
                            System.out.println(me);
                        }
                    });

            r.mouseMove(loc.x+button.getWidth() + 1, loc.y+button.getHeight()/2);

            r.mouseRelease(InputEvent.BUTTON1_MASK);

            if (!enterTriggered) {
                throw new RuntimeException("Test failed. MouseEntered was not triggered");
            }

            if (actionTriggered) {
                throw new RuntimeException("Test failed. ActionEvent triggered");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }
}
