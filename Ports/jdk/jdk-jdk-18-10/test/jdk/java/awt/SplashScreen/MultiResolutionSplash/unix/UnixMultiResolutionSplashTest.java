/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SplashScreen;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * @test
 * @key headful
 * @bug 8145174 8151787 8168657
 * @summary HiDPI splash screen support on Linux
 * @modules java.desktop/sun.java2d
 * @requires (os.family == "linux")
 * @run main UnixMultiResolutionSplashTest
 */

public class UnixMultiResolutionSplashTest {

    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 200;
    private static int inx = 0;
    private static final ImageInfo[] tests = {
        new ImageInfo("splash1.png", "splash1@200pct.png", Color.BLUE, Color.GREEN),
        new ImageInfo("splash2", "splash2@2x", Color.WHITE, Color.BLACK),
        new ImageInfo("splash3.", "splash3@200pct.", Color.YELLOW, Color.RED)
    };

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            generateImages();
            for (ImageInfo test : tests) {
                createChildProcess(test);
            }
        } else {
            int index = Integer.parseInt(args[0]);
            testSplash(tests[index]);
        }
    }

    static void createChildProcess(ImageInfo test) {
        String javaPath = System.getProperty("java.home");
        File file = new File(test.name1x);
        String classPathDir = System.getProperty("java.class.path");
        Map<String, String> env = new HashMap<String, String>();
        env.put("GDK_SCALE", "2");
        int exitValue = doExec(env, javaPath + File.separator + "bin" + File.separator
                + "java", "-splash:" + file.getAbsolutePath(), "-cp",
                classPathDir, "UnixMultiResolutionSplashTest", String.valueOf(inx++));
        if (exitValue != 0) {
            throw new RuntimeException("Test Failed");
        }
    }

    static void testSplash(ImageInfo test) throws Exception {
        SplashScreen splashScreen = SplashScreen.getSplashScreen();
        if (splashScreen == null) {
            throw new RuntimeException("Splash screen is not shown!");
        }
        Graphics2D g = splashScreen.createGraphics();
        Rectangle splashBounds = splashScreen.getBounds();
        int screenX = (int) splashBounds.getCenterX();
        int screenY = (int) splashBounds.getCenterY();
        Robot robot = new Robot();
        Color splashScreenColor = robot.getPixelColor(screenX, screenY);

        float scaleFactor = getScaleFactor();
        Color testColor = (1 < scaleFactor) ? test.color2x : test.color1x;
        if (!compare(testColor, splashScreenColor)) {
            throw new RuntimeException(
                    "Image with wrong resolution is used for splash screen!");
        }
    }

    static int doExec(Map<String, String> envToSet, String... cmds) {
        Process p = null;
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Map<String, String> env = pb.environment();
        for (String cmd : cmds) {
            System.out.print(cmd + " ");
        }
        System.out.println();
        if (envToSet != null) {
            env.putAll(envToSet);
        }
        BufferedReader rdr = null;
        try {
            List<String> outputList = new ArrayList<>();
            pb.redirectErrorStream(true);
            p = pb.start();
            rdr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String in = rdr.readLine();
            while (in != null) {
                outputList.add(in);
                in = rdr.readLine();
                System.out.println(in);
            }
            p.waitFor();
            p.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return p.exitValue();
    }

    static void testFocus() throws Exception {

        System.out.println("Focus Test!");
        Robot robot = new Robot();
        robot.setAutoDelay(50);
        Frame frame = new Frame();
        frame.setSize(100, 100);
        String test = "123";
        TextField textField = new TextField(test);
        textField.selectAll();
        frame.add(textField);
        frame.setVisible(true);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyPress(KeyEvent.VK_B);
        robot.keyRelease(KeyEvent.VK_B);
        robot.waitForIdle();

        frame.dispose();
        if (!textField.getText().equals("ab")) {
            throw new RuntimeException("Focus is lost! " +
                "Expected 'ab' got " + "'" + textField.getText() + "'.");
        }
    }

    static boolean compare(Color c1, Color c2) {
        return compare(c1.getRed(), c2.getRed())
                && compare(c1.getGreen(), c2.getGreen())
                && compare(c1.getBlue(), c2.getBlue());
    }

    static boolean compare(int n, int m) {
        return Math.abs(n - m) <= 50;
    }

    static float getScaleFactor() {

        final Dialog dialog = new Dialog((Window) null);
        dialog.setSize(100, 100);
        dialog.setModal(true);
        float[] scaleFactors = new float[1];
        Panel panel = new Panel() {

            @Override
            public void paint(Graphics g) {
                String scaleStr = System.getenv("GDK_SCALE");
                if (scaleStr != null && !scaleStr.equals("")) {
                    try {
                        scaleFactors[0] = Float.valueOf(scaleStr);
                    } catch (NumberFormatException ex) {
                        scaleFactors[0] = 1.0f;
                    }
                }
                dialog.setVisible(false);
            }
        };
        dialog.add(panel);
        dialog.setVisible(true);
        dialog.dispose();
        return scaleFactors[0];
    }

    static void generateImages() throws Exception {
        for (ImageInfo test : tests) {
            generateImage(test.name1x, test.color1x, 1);
            generateImage(test.name2x, test.color2x, 2);
        }
    }

    static void generateImage(String name, Color color, int scale) throws Exception {
        File file = new File(name);
        if (file.exists()) {
            return;
        }
        BufferedImage image = new BufferedImage(scale * IMAGE_WIDTH, scale * IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.fillRect(0, 0, scale * IMAGE_WIDTH, scale * IMAGE_HEIGHT);
        ImageIO.write(image, "png", file);
    }

    static class ImageInfo {

        final String name1x;
        final String name2x;
        final Color color1x;
        final Color color2x;

        public ImageInfo(String name1x, String name2x, Color color1x, Color color2x) {
            this.name1x = name1x;
            this.name2x = name2x;
            this.color1x = color1x;
            this.color2x = color2x;
        }
    }
}

