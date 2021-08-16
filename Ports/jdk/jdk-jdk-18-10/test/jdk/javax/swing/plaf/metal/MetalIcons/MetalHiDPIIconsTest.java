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

/*
 * @test
 * @bug 8160986 8174845 8176883 8165828
 * @summary Bad rendering of Swing UI controls with Metal L&F on HiDPI display
 * @run main/manual MetalHiDPIIconsTest
 */
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class MetalHiDPIIconsTest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n"
            + "Verify that icons are painted smoothly for standard Swing UI controls.\n\n"
            + "If the display does not support HiDPI mode press PASS.\n\n"
            + "1. Run the SwingSet2 demo on HiDPI Display.\n"
            + "2. Select Java Look and Feel. It is equivalent to Metal Look And Feel\n"
            + "3. Check that the icons are painted smoothly on Swing UI controls like:\n"
            + "  - JRadioButton\n"
            + "  - JCheckBox\n"
            + "  - JComboBox\n"
            + "  - JScrollPane (vertical and horizontal scroll bars)\n"
            + "  - JRadioButtonMenuItem\n"
            + "  - JCheckBoxMenuItem\n"
            + "and others...\n\n"
            + "If so, press PASS, else press FAIL.\n";

    public static void main(String args[]) throws Exception {
        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(MetalHiDPIIconsTest::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);

        if (!testResult) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createUI() {

        final JFrame mainFrame = new JFrame("Metal L&F icons test");
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 0;
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
        gbc.gridy = 1;
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
}
