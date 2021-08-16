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
 * @bug 8165212
 * @summary This manual test case displays scale values of Graphics and the
 *          underlying device configuration. Any change to host display's DPI
 *          value should reflect corresponding changes in the scale values
 *          of both Frame and Backbuffer (VolatileImage).
 * @run main/othervm/manual -Dsun.java2d.d3d=false -Dsun.java2d.opengl=false VolatileImageConfigurationTest
 */
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import java.awt.HeadlessException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class VolatileImageConfigurationTest
        extends JFrame
        implements ActionListener {
    /* Test frame and completion status */
    private static JFrame testFrame;
    private static volatile boolean testComplete = false;
    private static volatile boolean testResult = false;

    /* Main frame's dimensions */
    private static final int TEST_WIDTH = 600;
    private static final int TEST_HEIGHT = 600;
    private static final int TEST_MIN_DURATION = 3000;
    private static final int TEST_TOTAL_DURATION = 45000;

    /*
     * Frame will display information text explaining how to run the manual
     * test, and two buttons- Pass and Fail to determine the end-result.
     */
    private JTextArea infoTextArea;
    private JPanel buttonPanel;
    private JPanel testPanel;
    private JButton passButton;
    private JButton failButton;

    public VolatileImageConfigurationTest() {
        /* Default constructor. Initialize the UI components */
        super("Volatile Image Configuration Update Test");
        initComponents();
    }

    private void initComponents() {
        /* Create the text area with steps to execute the test */
        String description
                = "\n Volatile Image Configuration Update Test.\n"
                + " 1. The test displays scale values of component and the"
                + " underlying graphics device configuration.\n"
                + " 2. Kindly change the display's DPI settings from OS"
                + " control panel and observe the application.\n"
                + " 3. Select Pass if the scale values for both component & "
                + "underlying device configuration are updated as per the "
                + "\ndisplay's DPI value.\n";
        infoTextArea = new JTextArea(description);

        /* Create the test panel where user will observe the drawing */
        testPanel = new DisplayPanel();

        /* Create the buttons with event listeners */
        passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.setEnabled(true);
        passButton.addActionListener(this);

        failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.setEnabled(true);
        failButton.addActionListener(this);

        /* Add the buttons to a separate panel with flowlayout */
        buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);

        /* Add all the created components to the master frame */
        setLayout(new BorderLayout(10, 10));
        add(infoTextArea, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(testPanel, BorderLayout.CENTER);

        /* Set the dimensions */
        setSize(TEST_WIDTH, TEST_HEIGHT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        /* Button event listener */
        String command = e.getActionCommand();

        if (command.equals("Pass")) {
            /* Test has passed. Dispose the frame with success message */
            testComplete = true;
            testResult = true;
            System.out.println("Test Passed.");
        } else if (command.equals("Fail")) {
            /* Test has failed. Dispose the frame and throw exception */
            testComplete = true;
            testResult = false;
        }
    }

    private static void constructTestUI() {
        /* Construct the test's user interface */
        testFrame = new VolatileImageConfigurationTest();
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setLocationRelativeTo(null);
        testFrame.setVisible(true);
    }

    private static void destructTestUI() {
        /* Destroy the test's user interface */
        testFrame.dispose();
    }

    static class DisplayPanel extends JPanel {
        /* Display panel settings */
        private static final int PANEL_WIDTH = 600;
        private static final int PANEL_HEIGHT = 500;
        private static final int PANEL_X = 20;
        private static final int PANEL_Y = 80;
        private static final String MSG = "%s scale: [%2.2f, %2.2f]";
        private VolatileImage vImg;

        public DisplayPanel() throws HeadlessException {
            setSize(PANEL_WIDTH, PANEL_HEIGHT);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            /* Display graphics configuration values of the component */
            drawInfo(g, PANEL_X, PANEL_Y, "Frame", Color.BLUE);
            int attempts = 0;
            do {
                /* Display graphics configuration values of volatile image */
                drawBackingStoreImage(g);
            } while (vImg.contentsLost() && ++attempts < 3);
        }

        private void drawInfo(Graphics g, int x, int y,
                String msg, Color color) {
            g.setColor(color);
            g.setFont(g.getFont().deriveFont(24f));
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform tx = g2d.getTransform();

            g.drawString(msg, x, y);
            String text = String.format(MSG,
                                        "Graphics",
                                        tx.getScaleX(),
                                        tx.getScaleY());
            int dy = 20;
            g.drawString(text, x, y + dy);

            tx = g2d.getDeviceConfiguration().getDefaultTransform();
            text = String.format(MSG,
                                 "Device Config",
                                 tx.getScaleX(),
                                 tx.getScaleY());
            g.drawString(text, x, y + 2 * dy);
        }

        private void drawBackingStoreImage(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            GraphicsConfiguration gc = g2d.getDeviceConfiguration();
            if (vImg == null ||
                vImg.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                /* Create a new volatile image */
                vImg = createVolatileImage(PANEL_WIDTH, PANEL_HEIGHT / 3);
            }

            Graphics vImgGraphics = vImg.createGraphics();
            vImgGraphics.setColor(Color.WHITE);
            vImgGraphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT / 3);
            drawInfo(vImgGraphics,
                     PANEL_X,
                     PANEL_Y,
                     "Backbuffer",
                     Color.MAGENTA);
            g.drawImage(vImg, 0, PANEL_Y * 2, this);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    /* Construct the test interface */
                    constructTestUI();
                } catch (Exception ex) {
                    /* Throw an exception indicating error while creating UI */
                    throw new RuntimeException("Test Failed. Error while "
                            + "creating the test interface.");
                }
            }
        });

        try {
            /* Provide sufficient time for user to act upon the manual test */
            long totalWaitDuration = 0;
            do {
                Thread.sleep(TEST_MIN_DURATION);
                totalWaitDuration += TEST_MIN_DURATION;
            } while (!testComplete && totalWaitDuration < TEST_TOTAL_DURATION);
        } catch(InterruptedException ite) {
            /* No-op. The thread continues execution further */
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    /* Destroy the test interface */
                    destructTestUI();
                } catch (Exception ex) {
                    /* No-op */
                }
            }
        });

        /* Check for the test result and throw exception if required */
        if (testResult == false) {
            throw new RuntimeException("Test Failed. Incorrect scale values "
                + "were seen during the test execution.");
        }
    }
}