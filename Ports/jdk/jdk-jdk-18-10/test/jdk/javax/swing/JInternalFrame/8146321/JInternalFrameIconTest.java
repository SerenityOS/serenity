/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146321 8151282
 * @summary verifies JInternalFrame Icon and ImageIcon
 * @library ../../regtesthelpers
 * @build Util
 * @run main JInternalFrameIconTest
 */
import java.io.File;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class JInternalFrameIconTest {

    private static JFrame frame;
    private static JDesktopPane desktopPane;
    private static JInternalFrame internalFrame;
    private static ImageIcon titleImageIcon;
    private static Icon titleIcon;
    private static BufferedImage imageIconImage;
    private static BufferedImage iconImage;
    private static Robot robot;
    private static volatile String errorString = "";


    public static void main(String[] args) throws Exception {
        robot = new Robot();
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            executeCase(lookAndFeelItem.getClassName());
        }
        if (!"".equals(errorString)) {
            throw new RuntimeException("Error Log:\n" + errorString);
        }

    }

    private static void executeCase(String lookAndFeelString) throws Exception {
        if (tryLookAndFeel(lookAndFeelString)) {
            createImageIconUI(lookAndFeelString);
            robot.waitForIdle();
            robot.delay(1000);
            getImageIconBufferedImage();
            robot.waitForIdle();
            robot.delay(1000);
            cleanUp();
            robot.waitForIdle();
            robot.delay(1000);

            createIconUI(lookAndFeelString);
            robot.waitForIdle();
            robot.delay(1000);
            getIconBufferedImage();
            robot.waitForIdle();
            robot.delay(1000);
            cleanUp();
            robot.waitForIdle();
            robot.delay(1000);

            testIfSame(lookAndFeelString);
            robot.waitForIdle();
            robot.delay(1000);
        }

    }

    private static void createImageIconUI(final String lookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                desktopPane = new JDesktopPane();
                internalFrame = new JInternalFrame();
                frame = new JFrame();
                internalFrame.setTitle(lookAndFeelString);
                titleImageIcon = new ImageIcon() {
                    @Override
                    public int getIconWidth() {
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        return 16;
                    }

                    @Override
                    public void paintIcon(
                            Component c, Graphics g, int x, int y) {
                        g.setColor(java.awt.Color.black);
                        g.fillRect(x, y, 16, 16);
                    }
                };
                internalFrame.setFrameIcon(titleImageIcon);
                internalFrame.setSize(500, 200);
                internalFrame.setVisible(true);
                desktopPane.add(internalFrame);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(desktopPane, "Center");
                frame.setSize(500, 500);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void createIconUI(final String lookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                desktopPane = new JDesktopPane();
                internalFrame = new JInternalFrame();
                frame = new JFrame();
                internalFrame.setTitle(lookAndFeelString);
                titleIcon = new Icon() {
                    @Override
                    public int getIconWidth() {
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        return 16;
                    }

                    @Override
                    public void paintIcon(
                            Component c, Graphics g, int x, int y) {
                        g.setColor(java.awt.Color.black);
                        g.fillRect(x, y, 16, 16);
                    }
                };
                internalFrame.setFrameIcon(titleIcon);
                internalFrame.setSize(500, 200);
                internalFrame.setVisible(true);
                desktopPane.add(internalFrame);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(desktopPane, "Center");
                frame.setSize(500, 500);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void getImageIconBufferedImage() throws Exception {
        Point point = internalFrame.getLocationOnScreen();
        Rectangle rect = internalFrame.getBounds();
        Rectangle captureRect = new Rectangle(
                point.x + internalFrame.getInsets().left,
                point.y + internalFrame.getInsets().top,
                titleImageIcon.getIconWidth(),
                titleImageIcon.getIconHeight());

        System.out.println("imageicon captureRect " + captureRect);
        imageIconImage
                = robot.createScreenCapture(captureRect);
    }

    private static void getIconBufferedImage() throws Exception {
        Point point = internalFrame.getLocationOnScreen();
        Rectangle rect = internalFrame.getBounds();
        Rectangle captureRect = new Rectangle(
                point.x + internalFrame.getInsets().left,
                point.y + internalFrame.getInsets().top,
                titleIcon.getIconWidth(),
                titleIcon.getIconHeight());

        System.out.println("icon captureRect " + captureRect);
        iconImage
                = robot.createScreenCapture(captureRect);
    }

    private static void testIfSame(final String lookAndFeelString)
            throws Exception {
        if (!bufferedImagesEqual(imageIconImage, iconImage)) {
            ImageIO.write(imageIconImage, "png", new File("imageicon-fail.png"));
            ImageIO.write(iconImage, "png", new File("iconImage-fail.png"));
            String error ="[" + lookAndFeelString
                    + "] : ERROR: icon and imageIcon not same.";
            errorString += error;
            System.err.println(error);
        } else {
            System.out.println("[" + lookAndFeelString
                    + "] : SUCCESS: icon and imageIcon same.");
        }
    }

    private static boolean bufferedImagesEqual(
            BufferedImage bufferedImage1, BufferedImage bufferedImage2) {
        boolean flag = true;

        if (bufferedImage1.getWidth() == bufferedImage2.getWidth()
                && bufferedImage1.getHeight() == bufferedImage2.getHeight()) {
            final int colorTolerance = 25;
            final int mismatchTolerance = (int) (0.1
                    * bufferedImage1.getWidth() * bufferedImage1.getHeight());
            int mismatchCounter = 0;
            for (int x = 0; x < bufferedImage1.getWidth(); x++) {
                for (int y = 0; y < bufferedImage1.getHeight(); y++) {

                    int color1 = bufferedImage1.getRGB(x, y);
                    int red1 = (color1 >> 16) & 0x000000FF;
                    int green1 = (color1 >> 8) & 0x000000FF;
                    int blue1 = (color1) & 0x000000FF;

                    int color2 = bufferedImage2.getRGB(x, y);
                    int red2 = (color2 >> 16) & 0x000000FF;
                    int green2 = (color2 >> 8) & 0x000000FF;
                    int blue2 = (color2) & 0x000000FF;
                    if (red1 != red2 || green1 != green2 || blue1 != blue2) {
                        ++mismatchCounter;
                        if ((Math.abs(red1 - red2) > colorTolerance)
                                || (Math.abs(green1 - green2) > colorTolerance)
                                || (Math.abs(blue1 - blue2) > colorTolerance)) {

                            flag = false;
                        }
                    }
                }
            }
            if (mismatchCounter > mismatchTolerance) {
                flag = false;
            }
        } else {
            System.err.println("ERROR: size is different");
            flag = false;
        }
        return flag;
    }

    private static void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        //This test case is not applicable for Motif and gtk LAFs
        if(lookAndFeelString.contains("motif")
                || lookAndFeelString.contains("gtk")) {
            return false;
        }
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);

        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            return false;
        }
        return true;
    }
}
