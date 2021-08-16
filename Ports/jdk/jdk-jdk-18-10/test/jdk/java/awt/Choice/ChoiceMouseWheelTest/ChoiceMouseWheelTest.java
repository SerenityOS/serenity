/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 7050935
  @summary closed/java/awt/Choice/WheelEventsConsumed/WheelEventsConsumed.html fails on win32
  @library ../../regtesthelpers
  @author Oleg Pekhovskiy: area=awt-choice
  @build Util
  @run main ChoiceMouseWheelTest
*/

import test.java.awt.regtesthelpers.Util;

import java.awt.*;
import java.awt.event.*;

public class ChoiceMouseWheelTest extends Frame {

    private volatile boolean itemChanged = false;
    private volatile boolean wheelMoved = false;
    private volatile boolean frameExited = false;

    public static void main(String[] args) {
        new ChoiceMouseWheelTest();
    }

    ChoiceMouseWheelTest() {
        super("ChoiceMouseWheelTest");
        setLayout(new FlowLayout());

        Choice choice = new Choice();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        for(Integer i = 0; i < 50; i++) {
            choice.add(i.toString());
        }

        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                itemChanged = true;
            }
        });
        choice.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                wheelMoved = true;
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                frameExited = true;
            }
        });

        add(choice);
        setSize(200, 300);
        setVisible(true);
        toFront();

        try {
            Robot robot = new Robot();
            robot.setAutoDelay(20);
            Util.waitForIdle(robot);

            Point pt = choice.getLocationOnScreen();
            Dimension size = choice.getSize();
            int x = pt.x + size.width / 3;
            robot.mouseMove(x, pt.y + size.height / 2);

            // Test mouse wheel over the choice
            String name = Toolkit.getDefaultToolkit().getClass().getName();

            // mouse wheel doesn't work for the choice on X11 and Mac, so skip it
            if(!name.equals("sun.awt.X11.XToolkit")
               && !name.equals("sun.lwawt.macosx.LWCToolkit")) {
                robot.mouseWheel(1);
                Util.waitForIdle(robot);

                if(!wheelMoved || !itemChanged) {
                    throw new RuntimeException("Mouse Wheel over the choice failed!");
                }
            }

            // Test mouse wheel over the drop-down list
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Util.waitForIdle(robot);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Util.waitForIdle(robot);

            int y = getLocationOnScreen().y + getSize().height;
            while(!frameExited && y >= 0) { // move to the bottom of drop-down list
                robot.mouseMove(x, --y);
                Util.waitForIdle(robot);
            }

            if(x < 0) {
                throw new RuntimeException("Could not enter drop-down list!");
            }

            y -= choice.getHeight() / 2;
            robot.mouseMove(x, y); // move to the last visible item in the drop-down list
            Util.waitForIdle(robot);

            robot.mouseWheel(choice.getItemCount()); // wheel to the last item
            Util.waitForIdle(robot);

            // click the last item
            itemChanged = false;
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Util.waitForIdle(robot);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Util.waitForIdle(robot);

            if(!itemChanged || choice.getSelectedIndex() != choice.getItemCount() - 1) {
                throw new RuntimeException("Mouse Wheel scroll position error!");
            }

            dispose();
        } catch (AWTException e) {
            throw new RuntimeException("AWTException occurred - problem creating robot!");
        }
    }
}

