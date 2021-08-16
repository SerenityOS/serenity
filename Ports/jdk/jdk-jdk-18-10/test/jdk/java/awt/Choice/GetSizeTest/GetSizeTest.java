/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4255631
  @summary Solaris: Size returned by Choice.getSize() does not match actual size
  @author Andrei Dmitriev : area=Choice
  run main GetSizeTest.html
*/

import java.awt.Choice;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GetSizeTest {

    static String []s = {"Choice 1",
                         "Choice 2",
                         "unselected choices",
                         "what choices do I have?",
                         "Will I pick the same thing in the future?",
                };
    static volatile boolean passed = false;

    public static void main(String args[]) throws Exception {
        Frame f = null;
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(150);

            f = new Frame("choice test");

            Panel p = new Panel();
            p.setLayout(null);

            Choice c = new Choice();
            for (int i = 0; i < s.length; i++)
                    c.addItem(s[i]);

            c.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    System.err.println("Test passed");
                    passed = true;
                }
            });

            p.add(c);

            f.add(p);

            f.setSize(300, 300);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            c.setSize(200, 200);
            f.validate();

            robot.waitForIdle();

            Point pt = c.getLocationOnScreen();
            robot.mouseMove(pt.x + c.getWidth() - 10, pt.y + c.getHeight() / 2);
            robot.waitForIdle();
            robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
            robot.waitForIdle();
        } finally {
            if (f != null) {
                f.dispose();
            }
        }
        if (!passed) {
            throw new RuntimeException( "Timeout. Choice component size is not actual size." );
        }
        System.err.println("Test passed.");
    }
}
