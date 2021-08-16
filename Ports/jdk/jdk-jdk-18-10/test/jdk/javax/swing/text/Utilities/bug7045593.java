/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7045593
 * @summary Possible Regression : JTextfield cursor placement behavior algorithm has changed
 * @author Pavel Porvatov
 */

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;

public class bug7045593 {
    private static volatile JTextField jtf;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                jtf = new JTextField("WW");

                JFrame frame = new JFrame();
                frame.getContentPane().add(jtf);
                frame.pack();
                frame.setVisible(true);
            }
        });

        Robot robot = new Robot();
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    Rectangle r = jtf.modelToView(1);

                    int delta = 2;

                    for (int x = r.x - delta; x < r.x + delta; x++) {
                        assertEquals(jtf.viewToModel(new Point(x, r.y)), 1);
                    }

                    System.out.println("Passed.");
                } catch (BadLocationException e) {
                    throw new RuntimeException("Test failed", e);
                }
            }
        });
    }

    private static void assertEquals(int i1, int i2) {
        if (i1 != i2) {
            throw new RuntimeException("Test failed, " + i1 + " != " + i2);
        }
    }
}
