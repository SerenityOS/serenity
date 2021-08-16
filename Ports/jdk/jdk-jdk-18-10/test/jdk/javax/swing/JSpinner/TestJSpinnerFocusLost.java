/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4840869
 * @summary JSpinner keeps spinning while JOptionPane is shown on ChangeListener
 * @run main TestJSpinnerFocusLost
 */

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class TestJSpinnerFocusLost extends JFrame implements ChangeListener, FocusListener {

    JSpinner spinner;

    boolean spinnerGainedFocus = false;
    boolean spinnerLostFocus = false;

    static TestJSpinnerFocusLost b;
    Point p;
    Rectangle rect;
    static Robot robot;

    public static void blockTillDisplayed(Component comp) {
        Point p = null;
        while (p == null) {
            try {
                p = comp.getLocationOnScreen();
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public TestJSpinnerFocusLost() {
        spinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        spinner.addChangeListener(this);
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().addFocusListener(this);
        getContentPane().add(spinner);
    }

    public void doTest() throws Exception {
        blockTillDisplayed(spinner);
        SwingUtilities.invokeAndWait(() -> {
            ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().requestFocus();
        });

        try {
            synchronized (TestJSpinnerFocusLost.this) {
                if (!spinnerGainedFocus) {
                    TestJSpinnerFocusLost.this.wait(2000);
                }
            }


            SwingUtilities.invokeAndWait(() -> {
                p = spinner.getLocationOnScreen();
                rect = spinner.getBounds();
            });
            robot.delay(1000);
            robot.mouseMove(p.x+rect.width-5, p.y+3);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            synchronized (TestJSpinnerFocusLost.this) {
                while (!spinnerLostFocus) {
                    TestJSpinnerFocusLost.this.wait(2000);
                }
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if ( ((Integer) spinner.getValue()).intValue() != 11 ) {
            System.out.println("spinner value " + ((Integer) spinner.getValue()).intValue());
            throw new RuntimeException("Spinner value shouldn't be other than 11");
        }
    }


    private boolean changing = false;

    public void stateChanged(ChangeEvent e) {
        if (changing) {
            return;
        }
        JSpinner spinner = (JSpinner)e.getSource();
        int value = ((Integer) spinner.getValue()).intValue();
        if (value > 10) {
            changing = true;
            JOptionPane.showMessageDialog(spinner, "10 exceeded");
        }
    }

    public void focusGained(FocusEvent e) {
        synchronized (TestJSpinnerFocusLost.this) {
            spinnerGainedFocus = true;
            TestJSpinnerFocusLost.this.notifyAll();
        }
    }

    public void focusLost(FocusEvent e) {
        synchronized (TestJSpinnerFocusLost.this) {
            spinnerLostFocus = true;
            TestJSpinnerFocusLost.this.notifyAll();
        }
    }

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored) {
            System.out.println("Unsupported L&F: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] argv) throws Exception {
        robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(250);
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            System.out.println("Testing L&F: " + laf.getClassName());
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            try {
                SwingUtilities.invokeAndWait(() -> {
                    b = new TestJSpinnerFocusLost();
                    b.pack();
                    b.setLocationRelativeTo(null);
                    b.setVisible(true);
                });
                robot.waitForIdle();
                b.doTest();
                robot.delay(500);
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    if (b != null) b.dispose();
                });
            }
            robot.delay(1000);
        }
    }
}
