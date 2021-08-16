/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 6274267
 * @summary Checks that ScrollPaneLayout properly calculates preferred
 * layout size.
 * @author Mikhail Lapshin
 * @run main bug6274267
 */

import javax.swing.*;
import java.awt.*;

public class bug6274267 {
    private JFrame frame;
    private Component left;

    public static void main(String[] args) throws Exception {
        final bug6274267 test = new bug6274267();
        Robot robot = new Robot();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.setupUI1();
                }
            });
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.setupUI2();
                }
            });
            test.test();
        } finally {
            if (test.frame != null) {
                test.frame.dispose();
            }
        }
    }

    private void setupUI1() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        left = new JPanel();
        left.setPreferredSize(new Dimension(100, 100));

        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(100, 50));
        Component right = new JScrollPane(rightPanel);

        JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);

        frame = new JFrame();
        frame.add(split);
        frame.pack();
    }

    // It is important to separate frame.pack() from frame.setVisible(true).
    // Otherwise the repaint manager will combine validate() calls and
    // the bug won't appear.
    private void setupUI2() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void test() throws Exception {
        if (left.getSize().width == 100) {
            System.out.println("Test passed");
        } else {
            throw new RuntimeException("ScrollPaneLayout sometimes improperly " +
                    "calculates the preferred layout size. ");
        }
    }
}
