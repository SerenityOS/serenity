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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/* @test
 * @bug 8175293 8176097
 * @summary Window set location to a display with different DPI does not properly work
 * @run main/manual/othervm WindowResizingOnSetLocationTest
 */
public class WindowResizingOnSetLocationTest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static TestFrame frame;
    private static JFrame mainFrame;
    private static Rectangle[] screenBounds;
    private static double[][] scales;
    private static int screen1 = -1;
    private static int screen2 = -1;

    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n"
            + "Verify that a window is properly resized after setting the location"
            + " to a display with different DPI.\n"
            + "\n"
            + "The test is applicable for a multi-monitor system where displays"
            + " are configured to have different DPI\n"
            + "\n"
            + "1. Press Show Frame button\n"
            + "  (note that the button is disabled in case there are no two monitors"
            + " with different DPI)\n"
            + "The frame should appear in the center of the display (either"
            + " on the first or on the second).\n"
            + "2. Check that the string \"scales [ScaleX, ScaleY]\" is painted on the window"
            + " where ScaleX and ScaleY are the scales for current display.\n"
            + "The scales are calculated as DPI / 96 and are 1 for the DPI value 96"
            + " and 2 for the DPI value 192.\n"
            + "3. Press 'Move to another display' button.\n"
            + "4. Check that the frame appears on the another display.\n"
            + "5. Check that the string \"scales [ScaleX, ScaleY]\" is updated"
            + " to show the right display scales.\n"
            + "6. Check that the window is properly resized.\n"
            + "7. Check that the window is properly repainted and does not contain drawing artifacts\n"
            + "If all tests are passed, press PASS, else press FAIL.\n";

    public static void main(String args[]) throws Exception {

        countDownLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(WindowResizingOnSetLocationTest::createUI);
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
        JButton frameButton = new JButton("Show Frame");
        frameButton.addActionListener((e) -> {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getScreenDevices()[screen1]
                    .getDefaultConfiguration();

            Rectangle rect = getCenterRect(screenBounds[screen2]);
            frame = new TestFrame(gc, rect);
            frame.setVisible(true);

        });
        frameButton.setEnabled(screen1 != -1 && screen2 != -1);
        testPanel.add(getDisplaysComponent(), BorderLayout.CENTER);
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
        if (frame != null && frame.isVisible()) {
            frame.dispose();
        }

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

        for (int i = 0; i < devices.length; i++) {
            for (int j = i + 1; j < devices.length; j++) {
                if (scales[i][0] != scales[j][0] || scales[i][1] != scales[j][1]) {
                    screen1 = i;
                    screen2 = j;
                }
            }
        }
    }

    private static Rectangle getCenterRect(Rectangle rect) {
        int w = rect.width / 2;
        int h = rect.height / 2;
        int x = rect.x + w / 2;
        int y = rect.y + h / 2;

        return new Rectangle(x, y, w, h);
    }

    static JComponent getDisplaysComponent() {

        Rectangle rect = screenBounds[0];
        for (int i = 0; i < screenBounds.length; i++) {
            rect = rect.union(screenBounds[i]);
        }

        final BufferedImage img = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rect.width, rect.height);
        g.translate(-rect.x, -rect.y);

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
            float fontSize = rect.height / 7;
            g.setFont(g.getFont().deriveFont(fontSize));
            g.setColor(Color.BLUE);
            g.drawString(String.format("Scale: [%2.1f, %2.1f]", scaleX, scaleY),
                    r.x + r.width / 8, r.y + r.height / 2);

        }

        g.dispose();

        JPanel panel = new JPanel() {

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);

            }
        };

        panel.setPreferredSize(new Dimension(400, 200));

        return panel;
    }

    static class TestFrame extends JFrame {

        private final TestMultiResolutionImage mrImage;

        public TestFrame(GraphicsConfiguration gc, Rectangle rect) throws HeadlessException {
            super(gc);
            setBounds(rect);
            mrImage = new TestMultiResolutionImage(rect.width, rect.height);

            JPanel panel = new JPanel(new FlowLayout()) {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    AffineTransform tx = ((Graphics2D) g).getTransform();
                    mrImage.scaleX = tx.getScaleX();
                    mrImage.scaleY = tx.getScaleY();
                    Insets insets = getInsets();
                    g.drawImage(mrImage, insets.left, insets.bottom, null);
                }
            };

            JButton button = new JButton("Move to another display");
            button.addActionListener((e) -> {
                GraphicsConfiguration config = getGraphicsConfiguration();

                GraphicsDevice[] devices = GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getScreenDevices();


                int index = devices[screen1].getDefaultConfiguration().equals(config)
                        ? screen2 : screen1;

                Rectangle r = getCenterRect(screenBounds[index]);
                frame.setBounds(r);
            });

            panel.add(button);
            add(panel);
        }
    }

    static class TestMultiResolutionImage extends AbstractMultiResolutionImage {

        final int width;
        final int height;
        double scaleX;
        double scaleY;

        public TestMultiResolutionImage(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth(ImageObserver observer) {
            return width;
        }

        @Override
        public int getHeight(ImageObserver observer) {
            return height;
        }

        @Override
        protected Image getBaseImage() {
            return getResolutionVariant(width, height);
        }

        @Override
        public Image getResolutionVariant(double destImageWidth, double destImageHeight) {

            int w = (int) destImageWidth;
            int h = (int) destImageHeight;

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.scale(scaleX, scaleY);
            int red = (int) (255 / scaleX);
            int green = (int) (250 / scaleX);
            int blue = (int) (20 / scaleX);
            g.setColor(new Color(red, green, blue));
            g.fillRect(0, 0, width, height);

            g.setColor(Color.decode("#87CEFA"));
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, 24));
            g.drawString(String.format("scales: [%1.2fx, %1.2fx]", scaleX, scaleY),
                    width / 6, height / 2);

            g.dispose();
            return img;
        }

        @Override
        public List<Image> getResolutionVariants() {
            return Collections.unmodifiableList(Arrays.asList(getBaseImage()));
        }
    }
}
