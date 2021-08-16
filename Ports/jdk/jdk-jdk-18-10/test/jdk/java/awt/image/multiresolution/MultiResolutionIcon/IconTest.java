/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8147648 8163160
 * @summary [hidpi] multiresolution image: wrong resolution variant is used as
 * icon in the Unity panel
 * @requires os.family == "linux"
 * @run main/manual/othervm -Dsun.java2d.uiScale=2 IconTest
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class IconTest {

    private final static int SZ = 8;
    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JLabel instructionText;
    private static JButton passButton;
    private static JButton failButton;
    private static JButton testButton;
    private static JFrame f;
    private static CountDownLatch latch;

    private static volatile boolean failed;

    private static BufferedImage generateImage(int scale, Color c) {
        int x = SZ * scale;
        BufferedImage img = new BufferedImage(x, x, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        try {
            if (g != null) {
                g.setColor(c);
                g.fillRect(0, 0, x, x);
                g.setColor(Color.GREEN);
                g.drawRect(0, 0, x-1, x-1);
            }
        } finally {
            g.dispose();
        }
        return img;
    }


    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f = new JFrame("TrayIcon Test");

                final BaseMultiResolutionImage IMG = new BaseMultiResolutionImage(
                        new BufferedImage[]{generateImage(1, Color.RED), generateImage(2, Color.BLUE)});
                layout = new GridBagLayout();
                mainControlPanel = new JPanel(layout);
                resultButtonPanel = new JPanel(layout);
                f.setIconImage(IMG);
                GridBagConstraints gbc = new GridBagConstraints();
                String instructions
                        = "<html>INSTRUCTIONS:<br>"
                        + "Check if test button icon and unity icon<br>"
                        + "(icon in a side dock, if present)<br>"
                        + "are both blue with green border.<br><br>"
                        + "NB: Icon in the top bar may be presented in grayscale.<br><br>"
                        + "If Icon color is blue press pass"
                        + " else press fail.<br><br></html>";

                instructionText = new JLabel();
                instructionText.setText(instructions);

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                mainControlPanel.add(instructionText, gbc);
                testButton = new JButton("Test");
                testButton.setActionCommand("Test");
                mainControlPanel.add(testButton, gbc);

                testButton.setIcon(new ImageIcon(IMG));
                gbc.gridx = 0;
                gbc.gridy = 0;
                resultButtonPanel.add(testButton, gbc);

                passButton = new JButton("Pass");
                passButton.setActionCommand("Pass");
                passButton.addActionListener((ActionEvent e) -> {
                    latch.countDown();
                    f.dispose();
                });
                failButton = new JButton("Fail");
                failButton.setActionCommand("Fail");
                failButton.addActionListener(e -> {
                    failed = true;
                    latch.countDown();
                    f.dispose();
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
                        latch.countDown();
                        f.dispose();
                    }
                });
            }
        });
    }

    public static void main(String[] args) throws Exception {
        latch = new CountDownLatch(1);
        createUI();
        latch.await();

        if (failed) {
            throw new RuntimeException("Test Failed");
        }
    }
}

