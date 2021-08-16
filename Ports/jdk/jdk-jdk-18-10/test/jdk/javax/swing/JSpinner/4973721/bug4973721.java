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
   @bug 4973721
   @summary Up and Down Arrow key buttons are not working for the JSpinner in
   @        Synth LAF
   @library ../../regtesthelpers
   @build Util
   @author Oleg Mokhovikov
   @run main bug4973721
 */

import java.awt.Robot;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.*;

public class bug4973721 implements ChangeListener, FocusListener {
    static volatile boolean bStateChanged = false;
    static volatile boolean bFocusGained = false;
    static JSpinner spinner;
    static final Object listener = new bug4973721();
    static JFrame frame;

    public void focusLost(FocusEvent e) {}

    public synchronized void focusGained(FocusEvent e) {
        System.out.println("focusGained");
        bFocusGained = true;
        notifyAll();
    }

    public synchronized void stateChanged(ChangeEvent e) {
        System.out.println("stateChanged");
        bStateChanged = true;
        notifyAll();
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.synth.SynthLookAndFeel");

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    spinner = new JSpinner();
                    frame.getContentPane().add(spinner);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    frame.pack();
                    frame.setVisible(true);
                    spinner.addChangeListener((ChangeListener)listener);
                    spinner.addFocusListener((FocusListener)listener);
                    spinner.requestFocus();

                }
            });

            synchronized(listener) {
                if (!bFocusGained) {
                    System.out.println("waiting focusGained...");
                    try {
                        listener.wait(5000);
                    }
                    catch (InterruptedException e) {}
                }
            }

            boolean hasFocus = Util.invokeOnEDT(
                    new java.util.concurrent.Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return spinner.hasFocus();
                }
            });

            if (!bFocusGained && !hasFocus) {
                throw new RuntimeException("Couldn't request focus for" +
                        " spinner");
            }
            Robot robot = new Robot();
            robot.setAutoDelay(50);

            Util.hitKeys(robot, KeyEvent.VK_UP);
            robot.waitForIdle();
            Thread.sleep(1000);

            if (!bStateChanged) {
                throw new RuntimeException("Up arrow key button doesn't work" +
                        " for a spinner in Synth L&F");
            }

            bStateChanged = false;

            Util.hitKeys(robot, KeyEvent.VK_DOWN);
            robot.waitForIdle();
            Thread.sleep(1000);

            if (!bStateChanged) {
                throw new RuntimeException("Down arrow key button doesn't" +
                        " work for a spinner in Synth L&F");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }
}
