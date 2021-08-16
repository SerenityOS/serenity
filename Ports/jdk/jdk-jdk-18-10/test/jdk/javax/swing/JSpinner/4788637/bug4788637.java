/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @bug 4788637 7124307
 * @key headful
 * @summary JSpinner buttons don't conform to most platform conventions
 */
public final class bug4788637 {

    private static JSpinner spinner;
    private static JFrame fr;

    private static Robot robot;
    private int step;
    private boolean spinnerValueChanged[] = {false, false, false};

    private static Point p;
    private static Rectangle rect;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(50);
        robot.setAutoWaitForIdle(true);
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            bug4788637 app = new bug4788637();
            try {
                SwingUtilities.invokeAndWait(app::createAndShowGUI);
                robot.waitForIdle();
                SwingUtilities.invokeAndWait(()-> {
                    spinner.requestFocus();
                    p = spinner.getLocationOnScreen();
                    rect = spinner.getBounds();
                });
                app.start();
            } finally {
                SwingUtilities.invokeAndWait(app::destroy);
            }
        }
    }

    public void createAndShowGUI() {
        fr = new JFrame("Test");
        fr.setLayout( new GridBagLayout() );

        SpinnerModel model = new SpinnerNumberModel(50, 1, 100, 1);
        spinner = new JSpinner(model);
        fr.add(spinner,new GridBagConstraints());

        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                synchronized (bug4788637.this) {
                    spinnerValueChanged[step] = true;
                    bug4788637.this.notifyAll();
                }
            }
        });

        fr.setSize(200, 200);
        fr.setLocationRelativeTo(null);
        fr.setVisible(true);
        fr.toFront();
    }

    public void start() {
        try {
            Thread.sleep(1000);
            // Move mouse to the up arrow button
            robot.mouseMove(p.x+rect.width-3, p.y+3);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            synchronized (bug4788637.this) {
                if (!spinnerValueChanged[step]) {
                    bug4788637.this.wait(3000);
                }
            }

            // Move mouse out of JSpinner
            robot.mouseMove(p.x+rect.width-3, p.y-3);
            synchronized (bug4788637.this) {
                step++;
                if (!spinnerValueChanged[step]) {
                    bug4788637.this.wait(3000);
                }
            }

            // Move mouse to the up arrow button
            robot.mouseMove(p.x+rect.width-3, p.y+3);
            synchronized (bug4788637.this) {
                step++;
                if (!spinnerValueChanged[step]) {
                    bug4788637.this.wait(3000);
                }
            }

            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void destroy() {
        fr.dispose();
        synchronized (bug4788637.this) {
            if (!spinnerValueChanged[0] ||
                    spinnerValueChanged[1] ||
                    !spinnerValueChanged[2]) {
                throw new Error("JSpinner buttons don't conform to most platform conventions");
            }
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
