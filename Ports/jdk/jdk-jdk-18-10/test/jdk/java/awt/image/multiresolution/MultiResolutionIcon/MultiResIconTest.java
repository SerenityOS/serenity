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

/**
 * @test
 * @key headful
 * @bug 8149371 8169043
 * @summary multi-res. image: -Dsun.java2d.uiScale does not work for Window
 * icons (some ambiguity for Window.setIconImages()?)
 * @requires (os.family == "windows")
 * @run main/othervm/manual -Dsun.java2d.uiScale=2 MultiResIconTest
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MultiResIconTest {

    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JLabel instructionText;
    private static JButton passButton;
    private static JButton failButton;
    private static JDialog f;
    private static CountDownLatch latch;
    private static TestFrame frame;
    private static boolean testPassed;

    private static BufferedImage generateImage(int x, Color c) {

        BufferedImage img = new BufferedImage(x, x, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, x, x);
        g.setColor(Color.WHITE);
        g.fillRect(x / 3, x / 3, x / 3, x / 3);
        return img;
    }

    public MultiResIconTest() throws Exception {
        latch = new CountDownLatch(1);
        createUI();
        latch.await();

        if (!testPassed) {
            throw new RuntimeException("User Pressed Failed Button");
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new TestFrame();
            f = new JDialog(frame);
            f.setTitle("Instruction Dialog");
            layout = new GridBagLayout();
            mainControlPanel = new JPanel(layout);
            resultButtonPanel = new JPanel(layout);
            GridBagConstraints gbc = new GridBagConstraints();
            String instructions
                    = "<html>    INSTRUCTIONS:<br>"
                    + "This test is for Windows OS only.<br>"
                    + "Make sure that 'Use Small Icons' setting is not set<br>"
                    + "on Windows Taskbar Properties <br>"
                    + "1) Test frame title icon and frame color should be green."
                    + "<br>"
                    + "2) Test frame task bar icon should be blue<br>"
                    + "3) If color are same as mentioned in 1 and 2 press pass<br>"
                    + "   else press fail.<br><br></html>";

            instructionText = new JLabel();
            instructionText.setText(instructions);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            mainControlPanel.add(instructionText, gbc);
            passButton = new JButton("Pass");
            passButton.setActionCommand("Pass");
            passButton.addActionListener((ActionEvent e) -> {
                testPassed = true;
                latch.countDown();
                f.dispose();
                frame.dispose();
            });
            failButton = new JButton("Fail");
            failButton.setActionCommand("Fail");
            failButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    testPassed = false;
                    latch.countDown();
                    f.dispose();
                    frame.dispose();
                    throw new RuntimeException("Test Failed");
                }
            });
            gbc.gridx = 1;
            gbc.gridy = 0;
            resultButtonPanel.add(passButton, gbc);
            gbc.gridx = 2;
            gbc.gridy = 0;
            resultButtonPanel.add(failButton, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            mainControlPanel.add(resultButtonPanel, gbc);

            f.add(mainControlPanel);
            f.setSize(400, 200);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    testPassed = false;
                    latch.countDown();
                    f.dispose();
                    frame.dispose();
                }
            });
        });
    }

    private static class TestFrame extends JFrame {

        private static final int W = 200;

        private static final BaseMultiResolutionImage IMG
                = new BaseMultiResolutionImage(
                        new BufferedImage[]{generateImage(W, Color.RED),
                            generateImage(2 * W, Color.GREEN),
                            generateImage(4 * W, Color.BLUE)});

        private static final BaseMultiResolutionImage ICON
                = new BaseMultiResolutionImage(
                        new BufferedImage[]{generateImage(16, Color.RED),
                            generateImage(32, Color.GREEN),
                            generateImage(64, Color.BLUE),
                            generateImage(128, Color.BLACK),
                            generateImage(256, Color.GRAY)});

        public TestFrame() {
            createUI();
        }

        private void createUI() {
            setTitle("Test Frame");
            setIconImage(ICON);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            setSize(W, W);
            setLocation(50, 50);
            setResizable(false);
            setVisible(true);
        }

        @Override
        public void paint(Graphics gr) {
            gr.drawImage(IMG, 0, 0, this);
        }
    }

    public static void main(String[] args) throws Exception {
        new MultiResIconTest();
    }
}

