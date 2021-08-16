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
 @bug 8156099
 @summary Drag and drop of link from web browser, DataFlavor types
    application/x-java-url and text/uri-list, getTransferData returns null
 @run main/manual DragLinkFromBrowser
 */

import java.awt.Frame;
import java.awt.Button;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class DragLinkFromBrowser implements ActionListener {

    private static GridBagLayout layout;
    private static Panel mainControlPanel;
    private static Panel resultButtonPanel;
    private static TextArea instructionTextArea;
    private static Button passButton;
    private static Button failButton;
    private static Frame mainFrame;
    private static Thread mainThread = null;
    private static volatile boolean testPassed = false;
    private static volatile boolean isInterrupted = false;
    private static volatile String failMessage;
    private static final int testTimeOut = 300000;
    private static JFrame urlFrame;
    private static JPanel urlPanel;

    public static void dragLinkFromWebBrowser() {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                urlFrame = new JFrame();
                urlPanel = new JPanel();
                failMessage = "Dragging link from web browser Failed. "
                            + "getTransferData returns null";
                urlFrame.getContentPane().add(urlPanel);
                urlPanel.setTransferHandler(new TransferHandler() {
                    @Override
                    public boolean canImport(final TransferSupport support) {
                        return true;
                    }

                    @Override
                    public boolean importData(final TransferSupport support) {
                        final Transferable transferable =
                            support.getTransferable();
                        final DataFlavor[] flavors
                                = transferable.getTransferDataFlavors();

                        for (final DataFlavor flavor : flavors) {
                            try {
                                final Object transferData
                                        = transferable.getTransferData(flavor);

                                if (transferData == null) {
                                    JOptionPane.showMessageDialog(urlPanel,
                                                                failMessage);
                                    break;
                                } else {
                                    String flavorMessage = flavor.toString();
                                    String transferDataMessage =
                                        transferData.toString();
                                    if (flavorMessage.contains("error")
                                        || transferDataMessage.contains("null")) {
                                        JOptionPane.showMessageDialog(urlPanel,
                                                                    failMessage);
                                        break;
                                    }
                                }
                            } catch (UnsupportedFlavorException e) {
                                testFailed("UnsupportedFlavorException - "
                                    + "test Failed");
                            } catch (IOException e) {
                                testFailed("IOException - test Failed");
                            }
                        }

                        return true;
                    }
                });

                urlFrame.setBounds(500, 10, 200, 200);
                urlFrame.setVisible(true);
            }
        });
    }

    private void createInstructionUI() {
        mainFrame = new Frame("Drag and drop link from web browser");
        layout = new GridBagLayout();
        mainControlPanel = new Panel(layout);
        resultButtonPanel = new Panel(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        String instructions
                = "INSTRUCTIONS:"
                + "\n   1. Open any browser."
                + "\n   2. Select and drag URL from the browser page and "
                + "drop it on the panel"
                + "\n   3. If test fails, then a popup message will be displayed,"
                + " click Ok and \n       click Fail button."
                + "\n   5. Otherwise test passed. Click Pass button.";

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
                    testFailed("Dragging link from web browser Failed");
                    break;
            }
        }
    }

    public static void cleanUp() {
        urlFrame.dispose();
        mainFrame.dispose();
    }

    public static void testFailed(String message) {
        testPassed = false;
        isInterrupted = true;
        failMessage = message;
        mainThread.interrupt();
    }

    public static void main(final String[] args) throws Exception {

        DragLinkFromBrowser linkFromBrowser = new DragLinkFromBrowser();
        linkFromBrowser.createInstructionUI();
        linkFromBrowser.dragLinkFromWebBrowser();

        mainThread = Thread.currentThread();
        try {
            mainThread.sleep(testTimeOut);
        } catch (InterruptedException ex) {
            if (!testPassed) {
                throw new RuntimeException(failMessage);
            }
        } finally {
            cleanUp();
        }

        if (!isInterrupted) {
            throw new RuntimeException("Test Timed out after "
                    + testTimeOut / 1000 + " seconds");
        }
    }
}

