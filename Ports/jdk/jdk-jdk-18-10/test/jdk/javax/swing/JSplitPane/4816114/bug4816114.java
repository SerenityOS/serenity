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
 * @test
 * @key headful
 * @bug 4816114 8203904
 * @summary REGRESSION: Regression in divider location behavior when JSplitPane is resized
 * @author Andrey Pikalev
 * @run main bug4816114
 */

import java.awt.Robot;
import java.awt.Dimension;
import java.awt.AWTException;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JSplitPane;
import javax.swing.BorderFactory;
import java.lang.reflect.InvocationTargetException;

public class bug4816114 {

    static JFrame fr;
    JSplitPane splitPane;

    boolean[] resized = new boolean[] { false, false, false,
                                        false, false, false };
    static int step = 0;
    boolean h_passed = false;
    boolean v_passed = false;

    static bug4816114 test = new bug4816114();

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, AWTException {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.createAndShowGUI();
                }
            });
            Robot robot = new Robot();
            robot.waitForIdle();
            Thread.sleep(1000);
            Thread.sleep(2000);

            step++;
            test.doTest(150, 300);

            step++;
            test.doTest(650, 300);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                }
            });

            step++;
            test.doTest(300, 650);

            step++;
            test.doTest(300, 150);

            step++;
            test.doTest(300, 650);

            if ( !test.isPassed() ) {
                throw new Error("The divider location is wrong.");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> fr.dispose());
        }
    }

    public void createAndShowGUI() {
        fr = new JFrame("Test");
        fr.setUndecorated(true);

        splitPane = new TestSplitPane();
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0);
        splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JButton leftButton = new JButton("LEFT");
        leftButton.setPreferredSize(new Dimension(300, 300));
        leftButton.setMinimumSize(new Dimension(150, 150));
        splitPane.setLeftComponent(leftButton);

        JButton rightButton = new JButton("RIGHT");
        rightButton.setPreferredSize(new Dimension(300, 300));
        rightButton.setMinimumSize(new Dimension(150, 150));
        splitPane.setRightComponent(rightButton);

        fr.getContentPane().add(splitPane, BorderLayout.CENTER);

        fr.pack();
        fr.setVisible(true);
    }

    void doTest(final int width, final int height)  throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                splitPane.setPreferredSize(new Dimension(width, height));
                fr.pack();
            }
        });

        synchronized (bug4816114.this) {
            while (!resized[step]) {
                bug4816114.this.wait();
            }
        }
    }

   synchronized void setPassed(int orientation, boolean passed) {
       if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
           this.h_passed = passed;
       }
       else {
           this.v_passed = passed;
       }
   }

    synchronized boolean isPassed() {
        return h_passed && v_passed;
    }


    class TestSplitPane extends JSplitPane {
        public void setDividerLocation(int location) {
            super.setDividerLocation(location);

            if ( splitPane.getDividerLocation() == 151 ) {
                setPassed(getOrientation(), true);
            }

            synchronized (bug4816114.this) {
                resized[step] = true;
                bug4816114.this.notifyAll();
            }
        }
    }
}
