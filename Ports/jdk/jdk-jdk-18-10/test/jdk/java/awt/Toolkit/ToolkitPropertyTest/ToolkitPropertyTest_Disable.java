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
  @summary verifies that sun.awt.enableExtraMouseButtons = false consumes extra events
  @author Andrei Dmitriev : area=awt.mouse
  @run main/othervm -Dsun.awt.enableExtraMouseButtons=false ToolkitPropertyTest_Disable
 */

import java.awt.*;
import java.awt.event.*;

// Testcase 1: set to FALSE and check
// Testcase 2: set to FALSE and check that extra events are not coming
//                              check that standard events are coming

public class ToolkitPropertyTest_Disable extends Frame {
    static boolean propValue;
    static Robot robot;
    static int [] buttonsPressed;
    static int [] buttonsReleased;
    static int [] buttonsClicked;

    static boolean lessThenFourButtons;

    public static void main(String []s){
        propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        buttonsPressed = new int [MouseInfo.getNumberOfButtons()];
        buttonsReleased = new int [MouseInfo.getNumberOfButtons()];
        buttonsClicked = new int [MouseInfo.getNumberOfButtons()];

        ToolkitPropertyTest_Disable frame = new ToolkitPropertyTest_Disable();
        frame.setSize(300, 300);
        frame.setVisible(true);

        MouseAdapter ma1 = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    buttonsPressed[e.getButton() - 1] += 1;
                    System.out.println("PRESSED "+e);
                }
                public void mouseReleased(MouseEvent e) {
                    buttonsReleased[e.getButton() - 1] += 1;
                    System.out.println("RELEASED "+e);
                }
                public void mouseClicked(MouseEvent e) {
                    buttonsClicked[e.getButton() - 1] += 1;
                    System.out.println("CLICKED "+e);
                }
            };

        try {
            robot = new Robot();
            robot.delay(1000);
            robot.mouseMove(frame.getLocationOnScreen().x + frame.getWidth()/2, frame.getLocationOnScreen().y + frame.getHeight()/2);

            System.out.println("Property = " + propValue);
            testCase0();

            testCase1();
            System.out.println("Number Of Buttons = "+ MouseInfo.getNumberOfButtons());

            lessThenFourButtons = (MouseInfo.getNumberOfButtons() <= 3);
            if ( !lessThenFourButtons ) {
                frame.addMouseListener(ma1);
                testCase2();
            }
        } catch (Exception e){
            e.printStackTrace();
//            throw new RuntimeException(e);
        } finally {
//            frame.removeMouseListener(ma1);
        }
    }

    public static void testCase0(){
        if (propValue){
            throw new RuntimeException("TEST FAILED (0): System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
    }

    public static void testCase1(){
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() == true){
            throw new RuntimeException("TEST FAILED (1): setting to FALSE. Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() = " + Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled());
        }
    }

    public static void testCase2(){
        emptyArrays();
        int [] buttonMasks = new int[MouseInfo.getNumberOfButtons()]; // = InputEvent.getButtonDownMasks();
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonMasks[i] = InputEvent.getMaskForButton(i+1);
            System.out.println("TEST: "+buttonMasks[i]);
        }

        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            System.out.println("button to press = " +(i+1) + " : value passed to robot = " +buttonMasks[i]);
            try {
                robot.mousePress(buttonMasks[i]);
                robot.delay(70);
                robot.mouseRelease(buttonMasks[i]);
                robot.delay(200);
                //no exception is thrown
                if (i >= 3) {
                    throw new RuntimeException("TESTCASE 2 FAILED : robot accepted the extra button " + (i+1) + " instead of throwing an exception.");
                }
            } catch (IllegalArgumentException e){
                if (i >= 3) {
                    System.out.println("Passed: an exception caught for extra button.");
                } else {
                    throw new RuntimeException("TESTCASE 2 FAILED : exception happen on standard button.", e);
                }
            }
        }
        robot.delay(2000);
        if (MouseInfo.getNumberOfButtons() < 3) {
            for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
                if (buttonsPressed[i] != 1 || buttonsReleased[i] != 1 || buttonsClicked[i] !=1 ) {
                    throw new RuntimeException("TESTCASE 2 FAILED : button " + (i+1) + " wasn't single pressed.");
                }
            }
        } else {
            for (int i = 0; i < 3; i++){
                if (buttonsPressed[i] != 1 || buttonsReleased[i] != 1 || buttonsClicked[i] !=1 ) {
                    throw new RuntimeException("TESTCASE 2 FAILED : button " + (i+1) + " wasn't single pressed.");
                }
            }

            for (int i = 3; i < MouseInfo.getNumberOfButtons(); i++){
                if (buttonsPressed[i] != 0 || buttonsReleased[i] != 0 || buttonsClicked[i] != 0 ) {
                    throw new RuntimeException("TESTCASE 2 FAILED : button " + (i+1) + " was pressed.");
                }
            }
        }
    }

    public static void emptyArrays(){
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonsPressed[i] = 0;
            buttonsReleased[i] = 0;
            buttonsClicked[i] = 0;
        }
    }

}
