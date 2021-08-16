/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 * @test
 * @key headful
 * @bug 8143054
 */
public final class MouseModifiersInKeyEvent {

    private static int modifiersEX = 0;
    private static int modifiers = 0;
    private static JFrame f;
    private static Rectangle bounds;

    public static void main(final String[] args) throws Exception {
        for (int i = 1; i <= MouseInfo.getNumberOfButtons(); ++i) {
            test(InputEvent.getMaskForButton(i));
        }
    }

    private static void test(final int mask) throws Exception {
        final Robot r = new Robot();
        r.setAutoDelay(100);
        r.setAutoWaitForIdle(true);

        EventQueue.invokeAndWait(MouseModifiersInKeyEvent::createAndShowGUI);
        r.waitForIdle();
        EventQueue.invokeAndWait(() -> bounds = f.getBounds());

        r.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        r.mousePress(mask);
        r.keyPress(KeyEvent.VK_A);
        r.keyRelease(KeyEvent.VK_A);

        EventQueue.invokeAndWait(() -> f.dispose());

        r.mouseRelease(mask);

        // all extended modifiers should work
        if (modifiersEX != mask) {
            System.err.println("Expected modifiersEX: " + mask);
            System.err.println("Actual modifiersEX: " + modifiersEX);
            throw new RuntimeException();
        }
        // old modifiers work only for button1
        if (modifiersEX == InputEvent.BUTTON1_DOWN_MASK) {
            if (modifiers != InputEvent.BUTTON1_MASK) {
                System.err.println("Expected modifiers: " + InputEvent.BUTTON1_MASK);
                System.err.println("Actual modifiers: " + modifiers);
                throw new RuntimeException();
            }
        }
        modifiersEX = 0;
        modifiers = 0;
    }

    private static void createAndShowGUI() {
        f = new JFrame();

        final Component component = new JTextField();
        component.addKeyListener(new MyKeyListener());

        f.add(component);
        f.setSize(300, 300);
        f.setLocationRelativeTo(null);
        f.setAlwaysOnTop(true);
        f.setVisible(true);
    }

    static final class MyKeyListener extends KeyAdapter {

        public void keyPressed(final KeyEvent e) {
            modifiersEX = e.getModifiersEx();
            modifiers = e.getModifiers();
        }
    }
}

