/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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
 * @summary Verifies the client property xawt.mwm_decor_title for Linux.
 *          Note: the test requires GNOME Shell window manager and will automatically
 *          pass with any other WM.
 * @requires (os.family == "linux")
 * @run main WindowTitleVisibleTestLinuxGnome
 */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.io.File;
import javax.imageio.*;

public class WindowTitleVisibleTestLinuxGnome
{
    private static WindowTitleVisibleTestLinuxGnome theTest;

    private Robot robot;

    private JFrame frame;
    private JRootPane rootPane;

    private Rectangle titleBarBounds;
    private BufferedImage titleBarImageVisible;
    private BufferedImage titleBarImageNotVisible;

    private int DELAY = 1000;

    public WindowTitleVisibleTestLinuxGnome() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void performTest() {
        constructAndShowFrame();

        robot.delay(DELAY);

        Insets insets = frame.getInsets();
        Rectangle bounds = frame.getBounds();
        titleBarBounds = new Rectangle(bounds.x, bounds.y, bounds.width, insets.top);
        captureTitleBarVisible();

        robot.delay(DELAY);

        hideTitleBar();

        robot.delay(DELAY);

        captureTitleBarNotVisible();

        if (imagesEqual(titleBarImageVisible, titleBarImageNotVisible)) {
            throw new RuntimeException("Test failed: title bars shown and hidden are the same.");
        }

        runSwing(() -> frame.dispose());

        frame = null;
        rootPane = null;
    }

    private static boolean imagesEqual(BufferedImage img1, BufferedImage img2) {
        for (int px = 0; px < img1.getWidth(); px++) {
            for (int py = 0; py < img1.getHeight(); py++) {
                int rgb1 = img1.getRGB(px, py);
                int rgb2 = img2.getRGB(px, py);
                if (rgb1 != rgb2) {
                    return false;
                }
            }
        }

        return true;
    }

    private void captureTitleBarNotVisible() {
        titleBarImageNotVisible = robot.createScreenCapture(titleBarBounds);
    }

    private void hideTitleBar() {
        runSwing( () -> {
            rootPane.putClientProperty("xawt.mwm_decor_title", false);
            frame.setVisible(false);
            frame.setVisible(true);
        });
    }

    private void captureTitleBarVisible() {
        titleBarImageVisible = robot.createScreenCapture(titleBarBounds);
    }

    private void constructAndShowFrame() {
        runSwing(() -> {
            frame = new JFrame("IIIIIIIIIIIIIIII");
            frame.setBounds(100, 100, 300, 150);
            rootPane = frame.getRootPane();
            rootPane.putClientProperty("xawt.mwm_decor_title", true);
            JComponent contentPane = (JComponent) frame.getContentPane();
            JPanel comp = new JPanel();
            contentPane.add(comp);
            comp.setBackground(Color.RED);
            frame.setVisible(true);
        });
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private static void runSwing(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getWindowManagerID() {
        String WMID = null;
        try {
            Process p = new ProcessBuilder("xprop", "-root", "_NET_SUPPORTING_WM_CHECK").start();
            System.out.println( new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) );
            String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final String windowID = stdout.substring(stdout.lastIndexOf(" ")).strip();

            p = new ProcessBuilder("xprop", "-id", windowID, "_NET_WM_NAME").start();
            System.out.println( new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) );
            stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            WMID = stdout.substring(stdout.lastIndexOf("=")).strip();
            System.out.println("WM name: " + WMID);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return WMID;
    }

    public static void main(String[] args) {
        final String WMID = getWindowManagerID();
        if (WMID == null) {
            System.out.println("Failed to determine Window Manager. The test will not run and is considered passed.");
            return;
        } else if (!WMID.toLowerCase().contains("gnome")) {
            System.out.println("Window Manager " + WMID + " is not supported, only GNOME is. The test will not run and is considered passed.");
            return;
        }

        try {
            runSwing(() -> theTest = new WindowTitleVisibleTestLinuxGnome());
            theTest.performTest();
        } finally {
            if (theTest != null) {
                runSwing(() -> theTest.dispose());
            }
        }
    }
}

