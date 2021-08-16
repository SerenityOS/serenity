/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/* @test
 * @bug 8173972
 * @summary createScreenCapture not working as expected on multimonitor setup
 *          with different DPI scales.
 * @run main/manual/othervm RobotMultiDPIScreenTest
 */
public class RobotMultiDPIScreenTest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static JFrame mainFrame;
    private static Rectangle maxBounds;
    private static Rectangle[] screenBounds;
    private static double[][] scales;

    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n"
            + "Verify that screenshots are properly taken from monitors"
            + " with different DPI.\n"
            + "\n"
            + "The test is applicable for a multi-monitor system where displays"
            + " are configured to have different DPI\n"
            + "\n"
            + "1. Press Take Screenshots button\n"
            + "Check that screenshots shown on the panel are properly taken.\n";

    public static void main(String args[]) throws Exception {

        countDownLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(RobotMultiDPIScreenTest::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createUI() {

        initScreenBounds();

        mainFrame = new JFrame("DPI change test");
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        JPanel testPanel = new JPanel(new BorderLayout());

        final BufferedImage screensImage = getScreenImages();
        final JPanel screensPanel = new JPanel() {

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(screensImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        screensPanel.setPreferredSize(new Dimension(400, 200));

        JButton frameButton = new JButton("Take Screenshots");
        frameButton.addActionListener((e) -> {

            try {
                Robot robot = new Robot();
                Graphics2D g = screensImage.createGraphics();
                g.translate(-maxBounds.x, -maxBounds.y);

                for (Rectangle rect : screenBounds) {
                    MultiResolutionImage mrImage = robot.createMultiResolutionScreenCapture(rect);

                    List<Image> resolutionVariants = mrImage.getResolutionVariants();
                    Image rvImage = resolutionVariants.get(resolutionVariants.size() - 1);
                    g.drawImage(rvImage, rect.x, rect.y, rect.width, rect.height, null);
                }

                g.dispose();
                screensPanel.repaint();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        testPanel.add(screensPanel, BorderLayout.CENTER);
        testPanel.add(frameButton, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(testPanel, gbc);

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        JButton passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            disposeFrames();
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disposeFrames();
                countDownLatch.countDown();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        resultButtonPanel.add(passButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        mainControlPanel.add(resultButtonPanel, gbc);

        mainFrame.add(mainControlPanel);
        mainFrame.pack();

        mainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                disposeFrames();
                countDownLatch.countDown();
            }
        });
        mainFrame.setVisible(true);
    }

    private static void disposeFrames() {
        if (mainFrame != null && mainFrame.isVisible()) {
            mainFrame.dispose();
        }
    }

    static void initScreenBounds() {

        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();

        screenBounds = new Rectangle[devices.length];
        scales = new double[devices.length][2];
        for (int i = 0; i < devices.length; i++) {
            GraphicsConfiguration gc = devices[i].getDefaultConfiguration();
            screenBounds[i] = gc.getBounds();
            AffineTransform tx = gc.getDefaultTransform();
            scales[i][0] = tx.getScaleX();
            scales[i][1] = tx.getScaleY();
        }

        maxBounds = screenBounds[0];
        for (int i = 0; i < screenBounds.length; i++) {
            maxBounds = maxBounds.union(screenBounds[i]);
        }
    }

    private static Rectangle getCenterRect(Rectangle rect) {
        int w = rect.width / 2;
        int h = rect.height / 2;
        int x = rect.x + w / 2;
        int y = rect.y + h / 2;

        return new Rectangle(x, y, w, h);
    }

    static BufferedImage getScreenImages() {

        final BufferedImage img = new BufferedImage(maxBounds.width, maxBounds.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, maxBounds.width, maxBounds.height);
        g.translate(-maxBounds.x, -maxBounds.y);

        g.setStroke(new BasicStroke(8f));
        for (int i = 0; i < screenBounds.length; i++) {
            Rectangle r = screenBounds[i];
            g.setColor(Color.BLACK);
            g.drawRect(r.x, r.y, r.width, r.height);

            g.setColor(Color.ORANGE);
            Rectangle cr = getCenterRect(r);
            g.fillRect(cr.x, cr.y, cr.width, cr.height);

            double scaleX = scales[i][0];
            double scaleY = scales[i][1];
            float fontSize = maxBounds.height / 7;
            g.setFont(g.getFont().deriveFont(fontSize));
            g.setColor(Color.BLUE);
            g.drawString(String.format("Scale: [%2.1f, %2.1f]", scaleX, scaleY),
                    r.x + r.width / 8, r.y + r.height / 2);

        }

        g.dispose();

        return img;
    }
}
