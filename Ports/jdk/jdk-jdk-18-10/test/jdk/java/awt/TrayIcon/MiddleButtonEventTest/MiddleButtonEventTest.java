/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6272324 8204161
 * @summary Tests that clicking TrayIcon with middle button generates events.
 * @run main/manual MiddleButtonEventTest
 */

import java.awt.Button;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Panel;
import java.awt.SystemTray;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MiddleButtonEventTest {

    private static final Frame instructionFrame = new Frame();
    private static TrayIcon trayIcon = null;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n" +
            "Tests that clicking TrayIcon with middle button generates events.\n"+
            "When the test is started you will see three-color icon in the " +
            "system tray.\n Click on it with the middle mouse button:\n" +
            "- MOUSE_PRESSED, MOUSE_RELEASED, MOUSE_CLICKED events should be\n"+
            "  generated for the middle button.\n" +
            "  If so, the test passed, otherwise failed.";
    private static final TextArea eventOutputArea = new TextArea("", 5, 50,
                                                    TextArea.SCROLLBARS_BOTH);
    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;

    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
            System.out.println("The System Tray is not supported, " +
                    "so this test can not be run in this scenario.");
            return;
        }
        countDownLatch = new CountDownLatch(1);

        createInstructionUI();
        createTestUI();
        countDownLatch.await(15, TimeUnit.MINUTES);
        disposeUI();
        if (!testResult) {
            throw new RuntimeException("Test failed!");
        }
    }

    private static void createInstructionUI() {
        GridBagLayout layout = new GridBagLayout();
        Panel mainControlPanel = new Panel(layout);
        Panel resultButtonPanel = new Panel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        TextArea instructionTextArea = new TextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);
        mainControlPanel.add(instructionTextArea, gbc);

        Button passButton = new Button("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            countDownLatch.countDown();
        });

        Button failButton = new Button("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(e -> {
            countDownLatch.countDown();
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

        gbc.gridx = 0;
        gbc.gridy = 3;
        mainControlPanel.add(eventOutputArea, gbc);

        instructionFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                countDownLatch.countDown();
            }
        });
        instructionFrame.pack();
        instructionFrame.add(mainControlPanel);
        instructionFrame.pack();
        instructionFrame.setVisible(true);
    }

    private static void createTestUI() throws Exception {
        BufferedImage im = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);
        Graphics gr = im.createGraphics();
        gr.setColor(Color.white);
        gr.fillRect(0, 0, 16, 5);
        gr.setColor(Color.blue);
        gr.fillRect(0, 5, 16, 10);
        gr.setColor(Color.red);
        gr.fillRect(0, 10, 16, 16);

        trayIcon = new TrayIcon(im);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                printEventStr(e.toString());
            }

            public void mouseReleased(MouseEvent e) {
                printEventStr(e.toString());
            }

            public void mouseClicked(MouseEvent e) {
                printEventStr(e.toString());
            }
        });
        SystemTray.getSystemTray().add(trayIcon);
    }

    private static void disposeUI() {
        SystemTray.getSystemTray().remove(trayIcon);
        instructionFrame.dispose();
    }

    private static void printEventStr(String msg) {
        eventOutputArea.append(msg + "\n");
        System.out.println(msg);
    }
}
