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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/*
 * @test
 * @bug 8162350
 * @summary RepaintManager shifts repainted region when the floating point UI scale is used
 * @run main/manual/othervm -Dsun.java2d.uiScale=1.5 RepaintManagerFPUIScaleTest
 */
public class RepaintManagerFPUIScaleTest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n"
            + "Check JScrollPane correctly repaints the view"
            + " when UI scale has floating point value:\n"
            + "\n"
            + "1. Scroll down the JScrollPane\n"
            + "2. Select some values\n"
            + "If the scrolled selected value is painted without artifacts,"
            + "press PASS, else press FAIL.";

    public static void main(String args[]) throws Exception {

        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(RepaintManagerFPUIScaleTest::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);

        if (!testResult) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createUI() {

        final JFrame mainFrame = new JFrame("Motif L&F icons test");
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        JComponent testPanel = createComponent();
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
            mainFrame.dispose();
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.dispose();
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
                mainFrame.dispose();
                countDownLatch.countDown();
            }
        });
        mainFrame.setVisible(true);
    }

    private static JComponent createComponent() {

        int N = 100;
        String[] data = new String[N];
        for (int i = 0; i < N; i++) {
            data[i] = "Floating point test List Item: " + i;
        }
        JList list = new JList(data);
        list.setCellRenderer(new TestListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(list);
        return scrollPane;
    }

    private static Color[] COLORS = {
        Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE, Color.GRAY
    };

    private static Image createTestImage(int width, int height, int colorindex) {

        Color color = COLORS[colorindex % COLORS.length];

        AffineTransform tx = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getDefaultTransform();

        Image baseImage = createTestImage(width, height, 1, 1, color);
        Image rvImage = createTestImage(width, height, tx.getScaleX(), tx.getScaleY(), color);

        return new BaseMultiResolutionImage(baseImage, rvImage);
    }

    private static Image createTestImage(int w, int h,
            double scaleX, double scaleY, Color color) {

        int width = (int) Math.ceil(scaleX * w);
        int height = (int) Math.ceil(scaleY * h);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.scale(scaleX, scaleY);
        g.setColor(color);
        int d = 1;
        int d2 = 2 * d;
        g.drawLine(d, h / 2, w - d2, h / 2);
        g.drawLine(w / 2, d, w / 2, h - d2);
        g.drawRect(d, d, w - d2, h - d2);
        g.dispose();

        return img;
    }

    static class TestListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component retValue = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );
            setIcon(new ImageIcon(createTestImage(20, 10, index)));
            return retValue;
        }
    }
}
