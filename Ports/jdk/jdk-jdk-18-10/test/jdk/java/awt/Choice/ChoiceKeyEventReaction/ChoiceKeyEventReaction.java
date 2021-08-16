/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6252982
 * @key headful
 * @summary PIT: Keyboard FocusTraversal not working when choice's drop-down is visible, on XToolkit
 * @author andrei.dmitriev : area=awt.choice
 * @run main ChoiceKeyEventReaction
 */

import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ChoiceKeyEventReaction
{
    private static Robot robot;
    private static Choice choice1 = new Choice();
    private static Point pt;
    private static TextField tf = new TextField("Hi");
    private static boolean keyTypedOnTextField = false;
    private static boolean itemChanged = false;
    private static Frame frame;
    private static String toolkit;

    public static void main(String[] args) {
        createAndShowGUI();

        try {
            robot = new Robot();
            robot.setAutoDelay(100);
            robot.waitForIdle();

            moveFocusToTextField();
            testKeyOnChoice(InputEvent.BUTTON1_MASK, KeyEvent.VK_UP);
        } catch (Exception e) {
            throw new RuntimeException("Test failed. Exception thrown: "+e);
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    private static void createAndShowGUI() {
        frame = new Frame();
        toolkit = Toolkit.getDefaultToolkit().getClass().getName();
        System.out.println("Current toolkit is :" +toolkit);
        for (int i = 1; i<20; i++){
            choice1.add("item-0"+i);
        }

        tf.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent ke) {
                keyTypedOnTextField = true;
                System.out.println(ke);
            }
        });

        choice1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                itemChanged = true;
                System.out.println(e);
            }
        });
        choice1.setFocusable(false);

        frame.add(tf);
        frame.add(choice1);
        frame.setLayout (new FlowLayout());
        frame.setSize (200,200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void testKeyOnChoice(int button, int key) {
        pt = choice1.getLocationOnScreen();
        robot.mouseMove(pt.x + choice1.getWidth()/2, pt.y + choice1.getHeight()/2);

        robot.mousePress(button);
        robot.mouseRelease(button);
        robot.waitForIdle();

        robot.keyPress(key);
        robot.keyRelease(key);
        robot.waitForIdle();

        System.out.println("keyTypedOnTextField = "+keyTypedOnTextField +": itemChanged = " + itemChanged);
        if (itemChanged) {
            throw new RuntimeException("Test failed. ItemChanged event occur on Choice.");
        }

        // We may just write
        // if (toolkit.equals("sun.awt.windows.WToolkit") == keyTypedOnTextField) {fail;}
        // but  must report differently in these cases so put two separate if statements for simplicity.
        if (!toolkit.equals("sun.awt.X11.XToolkit") &&
               !keyTypedOnTextField) {
           throw new RuntimeException("Test failed. (Win32/MacOS) KeyEvent wasn't addressed to TextField. ");
        }

        if (toolkit.equals("sun.awt.X11.XToolkit") &&
                keyTypedOnTextField) {
            throw new RuntimeException("Test failed. (XToolkit/MToolkit). KeyEvent was addressed to TextField.");
        }

        System.out.println("Test passed. Unfocusable Choice doesn't react on keys.");
    }

    public static void moveFocusToTextField() {
        pt = tf.getLocationOnScreen();
        robot.mouseMove(pt.x + tf.getWidth()/2, pt.y + tf.getHeight()/2);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }
}
