/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 4506788 7147408
 * @summary  Tests if cursor gets stuck after insertion a character
 * @run main bug4506788
 */

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class bug4506788 {

    private volatile boolean passed = false;
    private JEditorPane jep;

    public static void main(final String[] args) {
        bug4506788 app = new bug4506788();
        app.init();
        app.start();
    }

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new RuntimeException("FAILED: SwingUtilities.invokeAndWait method failed then creating and showing GUI");
        }
    }

    public void start() {
        Robot robot;
        try {
            robot = new Robot();
            robot.setAutoDelay(100);
        } catch (AWTException e) {
            throw new RuntimeException("Robot could not be created");
        }

        robot.waitForIdle();

        Point p;
        try {
            p = getJEPLocOnScreen();
        } catch (Exception e) {
            throw new RuntimeException("Could not get JEditorPane location on screen");
        }

        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.keyPress(KeyEvent.VK_HOME);
        robot.keyRelease(KeyEvent.VK_HOME);
        robot.keyPress(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_RIGHT);
        robot.keyPress(KeyEvent.VK_X);
        robot.keyRelease(KeyEvent.VK_X);
        robot.keyPress(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_RIGHT);

        robot.waitForIdle();

        if (!passed) {
            throw new RuntimeException("Test failed.");
        }
    }

    private Point getJEPLocOnScreen() throws Exception {

        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                result[0] = jep.getLocationOnScreen();
            }
        });

        return result[0];
    }

    private void createAndShowGUI() {
        jep = new JEditorPane();
        String text = "abc";
        JFrame f = new JFrame();
        jep.setEditorKit(new StyledEditorKit());
        jep.setText(text);
        jep.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                passed = (e.getDot() == 3);
            }
        });

        DefaultStyledDocument doc = (DefaultStyledDocument) jep.getDocument();
        MutableAttributeSet atr = new SimpleAttributeSet();
        StyleConstants.setBold(atr, true);
        doc.setCharacterAttributes(1, 1, atr, false);

        f.getContentPane().add(jep);
        f.setSize(100, 100);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
