/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 8214702
   @summary Verifies text position for whitespaced string in printing Swing text
   @run main/manual TestTextPosInPrint
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class TestTextPosInPrint implements Printable {
    private static final CountDownLatch testEndedSignal = new CountDownLatch(1);
    private static final int testTimeout = 300000;
    private static volatile String testFailureMsg;
    private static volatile boolean testPassed;
    private static volatile boolean testFinished;
    private static PrinterJob job;
    private static JPanel panel;
    private static JFrame f;

    public static void main(String[] args) throws Exception {
        job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
            System.out.println("This test requires printers to be installed. Exiting.");
            return;
        }
        SwingUtilities.invokeLater(() -> createAndShowTestDialog());

        try {
            if (!testEndedSignal.await(testTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException(String.format(
                    "Test timeout '%d ms' elapsed.", testTimeout));
            }
            if (!testPassed) {
                String failureMsg = testFailureMsg;
                if ((failureMsg != null) && (!failureMsg.trim().isEmpty())) {
                    throw new RuntimeException(failureMsg);
                } else {
                    throw new RuntimeException("Test failed.");
                }
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        } finally {
            testFinished = true;
            SwingUtilities.invokeAndWait(() -> f.dispose());
        }
    }

    private static void doTest() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        f = new JFrame();
        f.setLocationRelativeTo(null);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        Font font = new Font("Serif", Font.PLAIN, 12);
        JLabel l1 = new JLabel("      1. ABCDE");
        l1.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel l2 = new JLabel("      2. ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
        l2.setHorizontalAlignment(SwingConstants.LEFT);
        //JLabel l3 = new JLabel("      3. ABCDE          ");
        JLabel l3 = new JLabel("      3. ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
        l3.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(BorderLayout.NORTH, l1);
        panel.add(BorderLayout.CENTER, l2);
        panel.add(BorderLayout.SOUTH, l3);
        f.getContentPane().add(BorderLayout.NORTH, panel);
        f.setSize(400, 300);
        f.setVisible(true);

        job.setPrintable(new TestTextPosInPrint());
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException pe) {
                throw new RuntimeException(pe);
            }
        }
    }

    private static void pass() {
        testPassed = true;
        testEndedSignal.countDown();
    }

    private static void fail(String failureMsg) {
        testFailureMsg = failureMsg;
        testPassed = false;
        testEndedSignal.countDown();
    }

    private static String convertMillisToTimeStr(int millis) {
        if (millis < 0) {
            return "00:00:00";
        }
        int hours = millis / 3600000;
        int minutes = (millis - hours * 3600000) / 60000;
        int seconds = (millis - hours * 3600000 - minutes * 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static void createAndShowTestDialog() {
        String description =
            " 1. Click on \"Start Test\" button.\r\n" +
            " 2. Multiple strings will be displayed on console.\r\n" +
            " 3. A print dialog will be shown. Select any printer to print. " +
            "\r\n" +
            " If the printed output of the strings are same without any alignment issue, click on \"PASS\"\r\n" +
            " button, otherwise click on \"FAIL\" button.";

        final JDialog dialog = new JDialog();
        dialog.setTitle("SaveFileWithoutPrinter");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                fail("Main dialog was closed.");
            }
        });

        final JLabel testTimeoutLabel = new JLabel(String.format(
            "Test timeout: %s", convertMillisToTimeStr(testTimeout)));
        final long startTime = System.currentTimeMillis();
        final Timer timer = new Timer(0, null);
        timer.setDelay(1000);
        timer.addActionListener((e) -> {
            int leftTime = testTimeout - (int) (System.currentTimeMillis() - startTime);
            if ((leftTime < 0) || testFinished) {
                timer.stop();
                dialog.dispose();
            }
            testTimeoutLabel.setText(String.format(
                "Test timeout: %s", convertMillisToTimeStr(leftTime)));
        });
        timer.start();

        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);

        final JButton testButton = new JButton("Start Test");
        final JButton passButton = new JButton("PASS");
        final JButton failButton = new JButton("FAIL");
        testButton.addActionListener((e) -> {
            testButton.setEnabled(false);
            new Thread(() -> {
                try {
                    doTest();

                    SwingUtilities.invokeLater(() -> {
                        passButton.setEnabled(true);
                        failButton.setEnabled(true);
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    dialog.dispose();
                    fail("Exception occurred in a thread executing the test.");
                }
            }).start();
        });
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
            pass();
        });
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            dialog.dispose();
            fail("Printed texts are not aligned as shown in console");
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(testTimeoutLabel);
        mainPanel.add(labelPanel, BorderLayout.NORTH);
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(testButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);

        dialog.pack();
        dialog.setVisible(true);
    }

    @Override
    public int print(Graphics pg, PageFormat pf, int pageNum)
        throws PrinterException {
        if (pageNum > 0){
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D) pg;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        panel.paint(g2);
        return Printable.PAGE_EXISTS;
    }
}
