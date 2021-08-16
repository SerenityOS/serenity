/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4530474
 * @summary  Tests if background-color CSS attribute in HTML font tag in class attribute
 * @author Denis Sharypov
 * @run main bug4530474
 */

import java.awt.*;
import javax.swing.*;

import java.io.*;

public class bug4530474 {

    private static final Color TEST_COLOR = Color.BLUE;
    private static JEditorPane jep;

    public static void main(String args[]) throws Exception {

        final Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();
        robot.delay(500);

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {

                boolean passed = false;

                Point p = jep.getLocationOnScreen();
                Dimension d = jep.getSize();
                int x0 = p.x;
                int y = p.y + d.height / 3;

                StringBuilder builder = new StringBuilder("Test color: ");
                builder.append(TEST_COLOR.toString());
                builder.append(" resut colors: ");

                for (int x = x0; x < x0 + d.width; x++) {
                    Color color = robot.getPixelColor(x, y);
                    builder.append(color);

                    if (TEST_COLOR.equals(color)) {
                        passed = true;
                        break;
                    }
                }

                if (!passed) {
                    throw new RuntimeException("Test Fail. " + builder.toString());
                }
            }
        });

    }

    private static void createAndShowGUI() {

        JFrame mainFrame = new JFrame("bug4530474");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jep = new JEditorPane();

        try {
            File file = new File(System.getProperty("test.src", "."), "test.html");
            jep.setPage(file.toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mainFrame.getContentPane().add(jep);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }
}
