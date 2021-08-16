/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6683775 6794764 8186617
 * @summary Painting artifacts is seen when panel is made setOpaque(false) for a
 *          translucent window
 */

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class bug6683775 {
    static final int LOC = 100,
            SIZE = 200;

    public static void main(String[] args) throws Exception {
        GraphicsConfiguration gc = getGC();
        if (gc == null || !gc.getDevice().isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            return;
        }
        Robot robot = new Robot();
        final JFrame testFrame = new JFrame(gc);

        SwingUtilities.invokeAndWait(() -> {
            JFrame backgroundFrame = new JFrame("Background frame");
            backgroundFrame.setUndecorated(true);
            JPanel panel = new JPanel();
            panel.setBackground(Color.RED);
            backgroundFrame.add(panel);
            backgroundFrame.setBounds(LOC, LOC, SIZE, SIZE);
            backgroundFrame.setVisible(true);

            testFrame.setUndecorated(true);
            JPanel p = new JPanel();
            p.setOpaque(false);
            testFrame.add(p);
            setOpaque(testFrame, false);
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testFrame.setBounds(LOC, LOC, SIZE, SIZE);
            testFrame.setVisible(true);
        });

        robot.waitForIdle();
        Thread.sleep(1500);

        //robot.getPixelColor() didn't work right for some reason
        BufferedImage capture =
                robot.createScreenCapture(new Rectangle(LOC, LOC, SIZE, SIZE));

        SwingUtilities.invokeAndWait(testFrame::dispose);

        int redRGB = Color.RED.getRGB();
        if (redRGB != capture.getRGB(SIZE/2, SIZE/2)) {
            throw new RuntimeException("Transparent frame is not transparent!");
        }
    }

    public static void setOpaque(Window window, boolean opaque) {
        Color bg = window.getBackground();
        if (bg == null) {
            bg = new Color(0, 0, 0, 0);
        }
        window.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(),
                                       opaque ? 255 : 0));
    }

    private static GraphicsConfiguration getGC() {
        GraphicsConfiguration transparencyCapableGC =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
        if (!transparencyCapableGC.isTranslucencyCapable()) {
            transparencyCapableGC = null;

            GraphicsEnvironment env =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();

            for (int i = 0; i < devices.length && transparencyCapableGC == null; i++) {
                GraphicsConfiguration[] configs = devices[i].getConfigurations();
                for (int j = 0; j < configs.length && transparencyCapableGC == null; j++) {
                    if (configs[j].isTranslucencyCapable()) {
                        transparencyCapableGC = configs[j];
                    }
                }
            }
        }
        return transparencyCapableGC;
    }
}
