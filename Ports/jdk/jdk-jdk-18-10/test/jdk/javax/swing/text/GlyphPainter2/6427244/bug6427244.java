/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @key headful
 * @bug 6427244 8144240 8166003 8169879
 * @summary Test that pressing HOME correctly moves caret in I18N document.
 * @author Sergey Groznyh
 * @library ../../../regtesthelpers
 * @build JRobot
 * @run main bug6427244
 */

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Position;

public class bug6427244 {
    private static final JRobot ROBOT = JRobot.getRobot();

    final static int TP_SIZE = 200;
    final static String[] SPACES = new String[] {
        "\u0020", // ASCII space
        "\u2002", // EN space
        "\u2003", // EM space
        "\u2004", // THREE-PER-EM space
        "\u2005", // ... etc.
        "\u2006",
        //"\u2007",
        "\u2008",
        "\u2009",
        "\u200a",
        "\u200b",
        "\u205f",
        "\u3000",
    };
    final static String[] WORDS = new String[] {
        "It", "is", "a", "long", "paragraph", "for", "testing", "GlyphPainter2\n\n",
    };

    public static void main(String[] args) {
        bug6427244 t = new bug6427244();
        for (String space: SPACES) {
            t.init(space);
            t.testCaretPosition();
        }

        System.out.println("OK");
        // Dispose the test interface upon completion
        t.destroyTestInterface();
    }

    void init(final String space) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    String text = null;
                    for (String word: WORDS) {
                        if (text == null) {
                            text = "";
                        } else {
                            text += space;
                        }
                        text += word;
                    }
                    tp = new JTextPane();
                    tp.setText(text +
                            "Some arabic: \u062a\u0641\u0627\u062d and some not.");
                    if (jf == null) {
                        jf = new JFrame();
                        jf.setTitle("bug6427244");
                        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        jf.setSize(TP_SIZE, TP_SIZE);
                        jf.setVisible(true);
                    }
                    Container c = jf.getContentPane();
                    c.removeAll();
                    c.add(tp);
                    c.invalidate();
                    c.validate();
                    dim = c.getSize();
                }
            });
            blockTillDisplayed(tp);
            ROBOT.waitForIdle();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void destroyTestInterface() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // Dispose the frame
                    jf.dispose();
                 }
            });
        } catch (Exception ex) {
            // No-op
        }
    }

    void blockTillDisplayed(JComponent comp) throws Exception {
        while (comp != null && isCompVisible == false) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        isCompVisible = comp.isVisible();
                     }
                });

                if (isCompVisible == false) {
                    // A short wait for component to be visible
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                // No-op. Thread resumed from sleep
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void testCaretPosition() {
        final Point p[] = new Point[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    p[0] = tp.getLocationOnScreen();

                    // the right-top corner position
                    p[0].x += (dim.width - 5);
                    p[0].y += 5;
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        ROBOT.mouseMove(p[0].x, p[0].y);
        ROBOT.clickMouse();
        ROBOT.hitKey(KeyEvent.VK_HOME);
        ROBOT.waitForIdle();
        // this will fail if caret moves out of the 1st line.
        if (getCaretOrdinate() != 0) {
            // Dispose the test interface upon completion
            destroyTestInterface();
            throw new RuntimeException("Test Failed.");
        }
    }

    int getCaretOrdinate() {
        final int[] y = new int[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Shape s;
                    try {
                        s = tp.getUI().getRootView(tp).modelToView(
                                        tp.getCaretPosition(), tp.getBounds(),
                                        Position.Bias.Forward);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    y[0] = s.getBounds().y;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return y[0];
    }

    private JFrame jf;
    private JTextPane tp;
    private Dimension dim;
    private volatile boolean isCompVisible = false;
}
