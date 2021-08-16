/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.image.MultiResolutionToolkitImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

/**
 * @test
 * @key headful
 * @bug 8076106
 * @author Hendrik Schreiber
 * @summary [macosx] Drag image of TransferHandler does not honor
 * MultiResolutionImage
 * @modules java.desktop/sun.awt.image
 * @run main MultiResolutionDragImageTest TEST_DRAG
 */
public class MultiResolutionDragImageTest {

    private static final Color COLOR_1X = Color.BLUE;
    private static final Color COLOR_2X = Color.RED;
    private static JFrame frame;
    private static JTextField field;
    private static Point p;

    public static void main(String[] args) throws Exception {

        final String test = args[0];

        switch (test) {
            case "TEST_DRAG":
                testDrag();
                break;
            default:
                throw new RuntimeException("Unknown test: " + test);
        }
    }

    private static void testDrag() throws Exception {


        SwingUtilities.invokeAndWait(() -> {

            frame = new JFrame();
            field = new JTextField("Drag Me");
            setupFrame(frame, field);
            frame.setVisible(true);
        });

        final Robot robot = new Robot();
        robot.setAutoDelay(500);
        robot.setAutoWaitForIdle(true);
        robot.waitForIdle();

        // get mouse into position
        SwingUtilities.invokeAndWait(() -> {

            p = new Point(field.getWidth() / 2, field.getHeight() / 2);
            SwingUtilities.convertPointToScreen(p, field);
        });

        robot.mouseMove(p.x, p.y);
        // simulate dragging
        robot.mousePress(BUTTON1_DOWN_MASK);
        p.translate(10, 10);
        robot.mouseMove(p.x, p.y);

        p.translate(5, 5);
        final Color color = robot.getPixelColor(p.x, p.y);
        robot.mouseRelease(BUTTON1_DOWN_MASK);

        SwingUtilities.invokeAndWait(frame::dispose);

        final float scaleFactor = getScaleFactor();
        final Color testColor = (1 < scaleFactor) ? COLOR_2X : COLOR_1X;

        if (!similar(testColor, color)) {
            throw new RuntimeException(
                    "TEST FAILED: Image with wrong resolution is used for drag image!");
        }
        System.out.println("TEST PASSED!");
    }

    private static void setupFrame(final JFrame frame, final JTextField field) {

        frame.setBounds(0, 0, 50, 50);
        frame.setLayout(new BorderLayout());
        field.setDragEnabled(true);
        final TransferHandler transferHandler = field.getTransferHandler();
        transferHandler.setDragImage(createMultiResolutionImage());
        frame.getContentPane().add(field, BorderLayout.CENTER);
    }

    private static boolean similar(Color c1, Color c2){
        return similar(c1.getRed(), c2.getRed())
                && similar(c1.getGreen(), c2.getGreen())
                && similar(c1.getBlue(), c2.getBlue());
    }

    private static boolean similar(int n, int m){
        return Math.abs(n - m) <= 50;
    }

    static float getScaleFactor() {
        return (float) GraphicsEnvironment.
                getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().
                getDefaultTransform().getScaleX();
    }

    private static Image createMultiResolutionImage() {

        return new MultiResolutionToolkitImage(
                createImage(50, COLOR_1X),
                createImage(100, COLOR_2X)
        );

    }

    private static Image createImage(final int length, final Color color) {

        final BufferedImage image = new BufferedImage(length, length,
                BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics graphics = image.getGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, length, length);
        graphics.dispose();
        return image;
    }
}
