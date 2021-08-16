/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6735293
 * @summary javax.swing.text.NavigationFilter.getNextVisualPositionFrom() not always throws BadLocationException
 * @author Pavel Porvatov
 */

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;

public class bug6735293 {
    private static volatile JFormattedTextField jtf;

    private static volatile NavigationFilter nf;

    private static volatile JFrame jFrame;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                jtf = new JFormattedTextField();
                nf = new NavigationFilter();
                jtf.setText("A text message");

                jFrame = new JFrame();
                jFrame.getContentPane().add(jtf);
                jFrame.pack();
                jFrame.setVisible(true);
            }
        });

        Thread.sleep(1000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Position.Bias[] biasRet = {Position.Bias.Forward};

                for (int direction : new int[]{
                        SwingConstants.EAST,
                        SwingConstants.WEST,
                        //  the following constants still will lead to "BadLocationException: Length must be positive"
                        SwingConstants.SOUTH,
                        SwingConstants.NORTH,
                }) {
                    for (int position : new int[]{-100, Integer.MIN_VALUE}) {
                        for (Position.Bias bias : new Position.Bias[]{Position.Bias.Backward, Position.Bias.Forward}) {
                            try {
                                nf.getNextVisualPositionFrom(jtf, position, bias, direction, biasRet);

                                throw new RuntimeException("BadLocationException was not thrown: position = " +
                                        position + ", bias = " + bias + ", direction = " + direction);
                            } catch (BadLocationException e) {
                                // Ok
                            }
                        }
                    }
                }

                jFrame.dispose();
            }
        });
    }
}
