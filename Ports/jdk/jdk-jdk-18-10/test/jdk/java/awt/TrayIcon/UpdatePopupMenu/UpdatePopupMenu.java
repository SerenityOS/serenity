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
 @test
 @bug 8147841
 @key headful
 @summary Updating Tray Icon popup menu does not update menu items on Mac OS X
 @run main/manual UpdatePopupMenu
 */

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.AWTException;
import java.awt.Button;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UpdatePopupMenu implements ActionListener {

    private static final int imageSize = 32;
    private static final int imageInset = 4;
    private static GridBagLayout layout;
    private static Panel mainControlPanel;
    private static Panel resultButtonPanel;
    private static TextArea instructionTextArea;
    private static Button passButton;
    private static Button failButton;
    private static Frame mainFrame;
    private static Thread mainThread = null;
    private static boolean testPassed = false;
    private static boolean isInterrupted = false;
    private static final int testTimeOut = 300000;

    private Image createSystemTrayIconImage() {
        final BufferedImage trayImage = new BufferedImage(
                imageSize,
                imageSize,
                BufferedImage.TYPE_INT_ARGB);

        final Graphics2D imageGraphics = (Graphics2D) trayImage.getGraphics();

        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        imageGraphics.setColor(new Color(255, 255, 255, 0));
        imageGraphics.fillRect(0, 0, trayImage.getWidth(),
                trayImage.getHeight());

        imageGraphics.setColor(Color.green);

        int imageWidth = trayImage.getWidth() - 2 * imageInset;
        int imageHeight = trayImage.getHeight() - 2 * imageInset;

        imageGraphics.fillOval(imageInset, imageInset, imageWidth, imageHeight);
        imageGraphics.setColor(Color.darkGray);
        imageGraphics.drawOval(imageInset, imageInset, imageWidth, imageHeight);

        return trayImage;
    }

    private PopupMenu createPopupMenu(final TrayIcon trayIcon,
            final int menuCount) {

        final PopupMenu trayIconPopupMenu = new PopupMenu();

        for (int i = 1; i <= menuCount; ++i) {
            final MenuItem popupMenuItem = new MenuItem("MenuItem_" + i);

            popupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    trayIcon.setPopupMenu(createPopupMenu(trayIcon,
                            menuCount + 1));
                }
            });

            trayIconPopupMenu.add(popupMenuItem);
        }

        return trayIconPopupMenu;
    }

    private void createSystemTrayIcons() {

        final TrayIcon trayIcon = new TrayIcon(createSystemTrayIconImage());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Update Popup Menu items");

        try {
            trayIcon.setPopupMenu(createPopupMenu(trayIcon, 2));
            SystemTray.getSystemTray().add(trayIcon);

        } catch (AWTException ex) {
            throw new RuntimeException("System Tray cration failed");
        }
    }

    private void createInstructionUI() {
        mainFrame = new Frame("Updating TrayIcon Popup Menu Item Test");
        layout = new GridBagLayout();
        mainControlPanel = new Panel(layout);
        resultButtonPanel = new Panel(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        String instructions
                = "INSTRUCTIONS:"
                + "\n   1. Click on the System Tray Icon"
                + "\n   2. Click on any of the displayed Menu items"
                + "\n   3. Repeat step 1 and count the number of items in the "
                + "Menu"
                + "\n   4. The number of items in the Menu should increase by 1"
                + "\n   5. Repeating steps 1, 2 and 3 should not break 4th step"
                + "\n   6. Click Fail if the 4th step is broken, Otherwise "
                + "click Pass ";

        instructionTextArea = new TextArea();
        instructionTextArea.setText(instructions);
        instructionTextArea.setEnabled(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        passButton = new Button("Pass");
        passButton.setName("Pass");
        passButton.addActionListener(this);

        failButton = new Button("Fail");
        failButton.setName("Fail");
        failButton.addActionListener(this);

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
        mainFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() instanceof Button) {
            Button btn = (Button) ae.getSource();
            switch (btn.getName()) {
                case "Pass":
                    testPassed = true;
                    isInterrupted = true;
                    mainThread.interrupt();
                    break;

                case "Fail":
                    testPassed = false;
                    isInterrupted = true;
                    mainThread.interrupt();
                    break;
            }
        }
    }

    private static void cleanUp() {
        mainFrame.dispose();
    }

    public static void main(final String[] args) throws Exception {
        if (SystemTray.isSupported()) {

            UpdatePopupMenu updatePopupMenu = new UpdatePopupMenu();
            updatePopupMenu.createInstructionUI();
            updatePopupMenu.createSystemTrayIcons();

            mainThread = Thread.currentThread();
            try {
                mainThread.sleep(testTimeOut);
            } catch (InterruptedException ex) {
                if (!testPassed) {
                    throw new RuntimeException("Updating TrayIcon popup menu"
                            + " items FAILED");
                }
            } finally {
                cleanUp();
            }

            if (!isInterrupted) {
                throw new RuntimeException("Test Timed out after "
                        + testTimeOut / 1000 + " seconds");
            }

        } else {
            System.out.println("System Tray is not supported on this platform");
        }
    }
}
