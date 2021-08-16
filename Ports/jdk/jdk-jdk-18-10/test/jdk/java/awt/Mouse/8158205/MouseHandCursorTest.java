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
 * @bug 8158205
 * @summary HiDPI hand cursor broken on Windows
 * @run main/manual/othervm -Dsun.java2d.uiScale=2 MouseHandCursorTest
 */
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MouseHandCursorTest {

    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JLabel instructionText;
    private static JButton passButton;
    private static JButton failButton;
    private static JFrame mainFrame;
    private static CountDownLatch latch;

    public static void main(String[] args) throws Exception {
        latch = new CountDownLatch(1);
        createUI();
        latch.await();
    }

    public static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                mainFrame = new JFrame("Hand Cursor Test");
                layout = new GridBagLayout();
                mainControlPanel = new JPanel(layout);
                resultButtonPanel = new JPanel(layout);

                GridBagConstraints gbc = new GridBagConstraints();
                String instructions
                        = "<html><center>INSTRUCTIONS:</center><br>"
                        + "Check the mouse cursor type on frame.<br>"
                        + "If mouse cursor is hand cursor test passed else failed"
                        + "<br><br></html>";

                instructionText = new JLabel();
                instructionText.setText(instructions);

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                mainControlPanel.add(instructionText, gbc);

                passButton = new JButton("Pass");
                passButton.setActionCommand("Pass");
                passButton.addActionListener((ActionEvent e) -> {
                    latch.countDown();
                    mainFrame.dispose();
                });

                failButton = new JButton("Fail");
                failButton.setActionCommand("Fail");
                failButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        latch.countDown();
                        mainFrame.dispose();
                        throw new RuntimeException("Test Failed");
                    }
                });
                gbc.gridx = 2;
                gbc.gridy = 0;
                resultButtonPanel.add(passButton, gbc);
                gbc.gridx = 3;
                gbc.gridy = 0;
                resultButtonPanel.add(failButton, gbc);

                gbc.gridx = 0;
                gbc.gridy = 2;
                mainControlPanel.add(resultButtonPanel, gbc);

                mainFrame.add(mainControlPanel);
                mainFrame.setSize(400, 200);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);

                mainFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        latch.countDown();
                        mainFrame.dispose();
                    }
                });
                mainFrame.getContentPane().setCursor(Cursor.
                        getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
    }
}
