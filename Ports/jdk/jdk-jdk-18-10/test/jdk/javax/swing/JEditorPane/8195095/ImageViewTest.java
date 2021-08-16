/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8195095 8206238 8208638
 * @summary Tests if Images are scaled correctly in JEditorPane.
 * @run main ImageViewTest
 */
import java.awt.Robot;
import java.awt.Point;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class ImageViewTest {

    private static JFrame f;

    private static void test(Robot r, JEditorPane editorPane,
                            final int WIDTH, final int HEIGHT )  throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            f = new JFrame();
            editorPane.setEditable(false);
            f.add(editorPane);
            f.setSize(WIDTH + 20, HEIGHT + 40);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            //This line will trigger the imageupdate, and consequently, the view
            //will be populated with the appropriate color when the pixel color
            //is queried by robot.
            editorPane.getUI().getPreferredSize(editorPane);
            f.setVisible(true);
        });

        r.waitForIdle();
        r.delay(500);

        SwingUtilities.invokeAndWait(() -> {
            Insets insets = editorPane.getInsets();
            Point loc = editorPane.getLocationOnScreen();

            final Color blue = Color.BLUE;
            final int offset = 10;

            Color center = r.getPixelColor(loc.x + insets.left + WIDTH / 2,
                                            loc.y + insets.top + HEIGHT / 2);
            Color left = r.getPixelColor(loc.x + insets.left + offset,
                                            loc.y + insets.top + HEIGHT / 2);
            Color right = r.getPixelColor(loc.x + insets.left + WIDTH - offset,
                                            loc.y + insets.top + HEIGHT / 2);
            Color bottom = r.getPixelColor(loc.x + insets.left + WIDTH / 2,
                                            loc.y + insets.top + HEIGHT - offset);
            Color top = r.getPixelColor(loc.x + insets.left + WIDTH / 2,
                                            loc.y + insets.top + offset);

            f.dispose();

            System.out.println("center color: " + center);
            System.out.println("left color: " + left);
            System.out.println("right color: " + right);
            System.out.println("bottom color: " + bottom);
            System.out.println("top color: " + top);
            System.out.println();

            if (!(blue.equals(center) && blue.equals(left) && blue.equals(right) &&
                    blue.equals(top) && blue.equals(bottom))) {
                throw new RuntimeException("Test failed: Image not scaled correctly");
            }
        });

        r.waitForIdle();
    }

    public static void main(String[] args) throws Exception {

        final String ABSOLUTE_FILE_PATH = ImageViewTest.class.getResource("circle.png").getPath();

        System.out.println(ABSOLUTE_FILE_PATH);

        Robot r = new Robot();

        final JEditorPane[] editorPanes = new JEditorPane[11];

        SwingUtilities.invokeAndWait(() -> {
            editorPanes[0] = new JEditorPane("text/html",
                    "<img height=\"200\" src=\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[1] = new JEditorPane("text/html",
                    "<img width=\"200\" src=\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[2] = new JEditorPane("text/html",
                    "<img width=\"200\" height=\"200\" src=\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[3] = new JEditorPane("text/html",
                    "<img src=\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[4] = new JEditorPane("text/html",
                    "<img width=\"100\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[5] = new JEditorPane("text/html",
                    "<img height=\"100\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[6] = new JEditorPane("text/html",
                    "<img width=\"100\" height=\"100\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[7] = new JEditorPane("text/html",
                    "<img width=\"50\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[8] = new JEditorPane("text/html",
                    "<img height=\"50\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[9] = new JEditorPane("text/html",
                    "<img width=\"300\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

            editorPanes[10] = new JEditorPane("text/html",
                    "<img height=\"300\" src =\"file:///" + ABSOLUTE_FILE_PATH + "\"");

        });

        r.waitForIdle();

        System.out.println("Test with only height set to 200");
        test(r, editorPanes[0], 200, 200);

        System.out.println("Test with only width set to 200");
        test(r, editorPanes[1], 200, 200);

        System.out.println("Test with both of them set");
        test(r, editorPanes[2], 200, 200);

        System.out.println("Test with none of them set to 200");
        test(r, editorPanes[3], 200, 200);

        System.out.println("Test with only width set to 100");
        test(r, editorPanes[4], 100, 100);

        System.out.println("Test with only height set to 100");
        test(r, editorPanes[5], 100, 100);

        System.out.println("Test with both width and height set to 100");
        test(r, editorPanes[6], 100, 100);

        System.out.println("Test with only width set to 50");
        test(r, editorPanes[7], 50, 50);

        System.out.println("Test with only height set to 50");
        test(r, editorPanes[8], 50, 50);

        System.out.println("Test with only width set to 300");
        test(r, editorPanes[9], 300, 300);

        System.out.println("Test with only height set to 300");
        test(r, editorPanes[10], 300, 300);

        System.out.println("Test Passed.");
    }
}
