/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4885629
 * @summary With JSplitPane in VERTICAL_SPLIT, SplitPaneBorder draws bottom edge of divider
 * @author Andrey Pikalev
 */

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;


public class bug4885629 {

    private static final Color darkShadow = new Color(100,120,200);
    private static final Color darkHighlight = new Color(200,120,50);
    private static final Color lightHighlight = darkHighlight.brighter();
    private static final Color BGCOLOR = Color.blue;

    private static JSplitPane sp;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new BasicLookAndFeel() {
                public boolean isSupportedLookAndFeel(){ return true; }
                public boolean isNativeLookAndFeel(){ return false; }
                public String getDescription() { return "Foo"; }
                public String getID() { return "FooID"; }
                public String getName() { return "FooName"; }
        });

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();

                    JComponent a = new JPanel();
                    a.setBackground(Color.white);
                    a.setMinimumSize(new Dimension(10, 10));

                    JComponent b = new JPanel();
                    b.setBackground(Color.white);
                    b.setMinimumSize(new Dimension(10, 10));

                    sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, a, b);
                    sp.setPreferredSize(new Dimension(20, 20));
                    sp.setBackground(BGCOLOR);

                    Border bo = new BasicBorders.SplitPaneBorder(lightHighlight,
                            Color.red);
                    Border ibo = new EmptyBorder(0, 0, 0, 0);
                    sp.setBorder(bo);
                    sp.setMinimumSize(new Dimension(200, 200));

                    ((BasicSplitPaneUI) sp.getUI()).getDivider().setBorder(ibo);

                    frame.getContentPane().setLayout(new FlowLayout());
                    frame.getContentPane().setBackground(darkShadow);
                    frame.getContentPane().add(sp);

                    frame.setSize(200, 200);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setVisible(true);
                }
            });

            final Robot robot = new Robot();
            robot.waitForIdle();
            robot.delay(1000);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Rectangle rect = ((BasicSplitPaneUI) sp.getUI()).getDivider().getBounds();

                    Point p = rect.getLocation();

                    SwingUtilities.convertPointToScreen(p, sp);

                    for (int i = 0; i < rect.width; i++) {
                        if (!BGCOLOR.equals(robot.getPixelColor(p.x + i, p.y + rect.height - 1))) {
                            throw new Error("The divider's area has incorrect color.");
                        }
                    }
                }
            });
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
