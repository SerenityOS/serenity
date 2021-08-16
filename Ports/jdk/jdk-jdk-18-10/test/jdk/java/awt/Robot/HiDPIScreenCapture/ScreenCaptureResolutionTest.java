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

 /*
 * @test
 * @bug 8162959
 * @summary Visually validate multiresolution screencapture.
 * @run main/othervm/manual -Dsun.java2d.uiScale=2 ScreenCaptureResolutionTest
 */
import java.awt.AWTException;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CountDownLatch;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.List;
import java.awt.Image;
import java.awt.image.MultiResolutionImage;

public class ScreenCaptureResolutionTest {

    public static void main(String args[]) throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        TestUI test = new TestUI(latch);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    test.createUI();
                } catch (Exception ex) {
                    throw new RuntimeException("Exception while creating UI");
                }
            }
        });
        latch.await(3, TimeUnit.SECONDS);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    test.validateScreenCapture();
                } catch (Exception ex) {
                    throw new RuntimeException("Exception while"
                        + " validating ScreenCapture");
                }
            }
        });
        boolean status = latch.await(5, TimeUnit.MINUTES);
        if (!status) {
            System.out.println("Test timed out.");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    test.disposeUI();
                } catch (Exception ex) {
                    throw new RuntimeException("Exception while disposing UI");
                }
            }
        });

        if (test.testResult == false) {
            throw new RuntimeException("Test Failed.");
        }
    }
}

class TestUI {

    private static JFrame mainFrame;
    private static JFrame outputImageFrame;
    private static JFrame inputImageFrame;
    private static JPanel outputControlPanel;
    private static JPanel mainControlPanel;

    private static JTextArea instructionTextArea;

    private static JPanel inputImagePanel;
    private static JPanel resultButtonPanel;
    private static JButton passButton;
    private static JButton failButton;

    private static GridBagLayout layout;
    private final CountDownLatch latch;
    public boolean testResult = false;

    public TestUI(CountDownLatch latch) throws Exception {
        this.latch = latch;
    }

    public void validateScreenCapture() throws AWTException {
        Robot robot = new Robot();
        outputControlPanel = new JPanel(layout);
        GridBagConstraints ogbc = new GridBagConstraints();

        MultiResolutionImage image
                = robot.createMultiResolutionScreenCapture(inputImageFrame.getBounds());
        List<Image> imageList = image.getResolutionVariants();
        int size = imageList.size();
        BufferedImage lowResImage = (BufferedImage) imageList.get(0);
        BufferedImage highResImage = (BufferedImage) imageList.get(1);

        outputImageFrame = new JFrame("Output");
        outputImageFrame.getContentPane().setLayout(new GridBagLayout());
        ogbc.gridx = 0;
        ogbc.gridy = 0;
        ogbc.fill = GridBagConstraints.HORIZONTAL;
        outputControlPanel.add(new JLabel(new ImageIcon(lowResImage)), ogbc);
        int width = lowResImage.getWidth();
        int height = lowResImage.getHeight();
        JLabel labelImg1 = new JLabel("LEFT:Width: " + width
                                      + " Height: " + height);
        ogbc.gridx = 0;
        ogbc.gridy = 1;
        outputControlPanel.add(labelImg1, ogbc);
        ogbc.gridx = 1;
        ogbc.gridy = 0;
        outputControlPanel.add(new JLabel(new ImageIcon(highResImage)), ogbc);
        width = highResImage.getWidth();
        height = highResImage.getHeight();
        JLabel labelImg2 = new JLabel("RIGHT:Width: " + width
                                      + " Height: " + height);
        ogbc.gridx = 1;
        ogbc.gridy = 1;
        outputControlPanel.add(labelImg2, ogbc);
        outputControlPanel.setBackground(Color.GRAY);
        outputImageFrame.add(outputControlPanel);

        outputImageFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        outputImageFrame.setBounds(600, 0, 400, 300);
        outputImageFrame.setLocationRelativeTo(null);
        outputImageFrame.setVisible(true);
    }

    public final void createUI() throws Exception {

        mainFrame = new JFrame("ScreenCaptureResolutionTest");

        layout = new GridBagLayout();
        mainControlPanel = new JPanel(layout);
        resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        // Create Test instructions
        String instructions
                = "INSTRUCTIONS:"
                + "\n Test to Visually validate MultiResolutionn Image. "
                + "\n 1. Check if output window contains two screenshot "
                + "\n    of input window. "
                + "\n 2. Right image should be twice the size of Left. "
                + "\n 3. Quality of Right image should be better than Left. "
                + "\n If all the three conditons are satisfied, then "
                + "\n click pass else fail.";
        instructionTextArea = new JTextArea();
        instructionTextArea.setText(instructions);
        instructionTextArea.setEnabled(false);
        instructionTextArea.setDisabledTextColor(Color.black);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;

        inputImagePanel = new JPanel(layout);
        JLabel label = new JLabel("Resolution");
        inputImagePanel.add(label);
        inputImageFrame = new JFrame("Input");
        inputImageFrame.add(inputImagePanel);

        inputImageFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        inputImageFrame.setBounds(100, 200, 100, 100);
        inputImageFrame.setVisible(true);

        passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            outputImageFrame.dispose();
            inputImageFrame.dispose();
            testResult = true;
            mainFrame.dispose();
            latch.countDown();

        });
        failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputImageFrame.dispose();
                inputImageFrame.dispose();
                testResult = false;
                mainFrame.dispose();
                latch.countDown();
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
        mainFrame.setVisible(true);
    }

    public void disposeUI() {
        outputImageFrame.dispose();
        inputImageFrame.dispose();
        mainFrame.setVisible(false);
        mainFrame.dispose();
    }

}
