/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test %I% %E%
  @key headful
  @bug 6315717
  @summary verifies that robot could accept extra buttons
  @author Andrei Dmitriev : area=awt.mouse
  @library ../../regtesthelpers
  @build Util
  @run main RobotExtraButton
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class RobotExtraButton extends Frame {
    static Robot robot;
    public static void main(String []s){
        RobotExtraButton frame = new RobotExtraButton();
        frame.setSize(300, 300);
        frame.setVisible(true);
        frame.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    System.out.println("PRESSED "+e);
                }
                public void mouseReleased(MouseEvent e) {
                    System.out.println("RELEASED "+e);
                }
                public void mouseClicked(MouseEvent e) {
                    System.out.println("CLICKED "+e);
                }
            });
        Util.waitForIdle(robot);
        int [] buttonMask = new int[MouseInfo.getNumberOfButtons()]; // = InputEvent.getButtonDownMasks();
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonMask[i] = InputEvent.getMaskForButton(i+1);
            System.out.println("TEST: "+buttonMask[i]);
        }

        try {
            robot = new Robot();
            robot.mouseMove(frame.getLocationOnScreen().x + frame.getWidth()/2, frame.getLocationOnScreen().y + frame.getHeight()/2);
            /*
            if (MouseInfo.getNumberOfButtons() <= 3) {
                System.out.println("Number Of Buttons = "+ MouseInfo.getNumberOfButtons() +". Finish!");
                return;
                }*/

            System.out.println("TEST: press 1");
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            System.out.println("TEST: press 2");

            robot.mousePress(InputEvent.BUTTON2_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON2_MASK);
            Util.waitForIdle(robot);
            System.out.println("TEST: press 3");

            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
            Util.waitForIdle(robot);
            System.out.println("--------------------------------------------------");
            for (int i = 0; i < buttonMask.length; i++){
                System.out.println("button would = " +i + " : value = " +buttonMask[i]);
                robot.mousePress(buttonMask[i]);
                robot.delay(50);
                robot.mouseRelease(buttonMask[i]);
                Util.waitForIdle(robot);
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Test failed.", e);
        }
    }
}
