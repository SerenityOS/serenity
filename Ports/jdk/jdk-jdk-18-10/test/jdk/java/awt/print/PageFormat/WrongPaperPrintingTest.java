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

/* @test
   @bug 8167102 8181659
   @summary PrintRequestAttributeSet breaks page size set using PageFormat
   @run main/manual WrongPaperPrintingTest
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class WrongPaperPrintingTest implements Printable {
    private static final CountDownLatch testEndedSignal = new CountDownLatch(1);
    private static final int testTimeout = 300000;
    private static volatile String testFailureMsg;
    private static volatile boolean testPassed;
    private static volatile boolean testFinished;

    public static void main(String[] args) {
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
        }
    }

    private static void doTest() {
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(Chromaticity.MONOCHROME);

        MediaSize isoA5Size = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A5);
        float[] size = isoA5Size.getSize(Size2DSyntax.INCH);
        Paper paper = new Paper();
        paper.setSize(size[0] * 72.0, size[1] * 72.0);
        paper.setImageableArea(0.0, 0.0, size[0] * 72.0, size[1] * 72.0);
        PageFormat pf = new PageFormat();
        pf.setPaper(paper);

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new WrongPaperPrintingTest(), job.validatePage(pf));
        if (job.printDialog()) {
            try {
                job.print(aset);
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
            " To run this test it is required to have a virtual PDF\r\n" +
            " printer or any other printer supporting A5 paper size.\r\n" +
            "\r\n" +
            " 1. Verify that NOT A5 paper size is set as default for the\r\n" +
            " printer to be used.\r\n" +
            " 2. Click on \"Start Test\" button.\r\n" +
            " 3. In the shown print dialog select the printer and click\r\n" +
            " on \"Print\" button.\r\n" +
            " 4. Verify that a page with a drawn rectangle is printed on\r\n" +
            " a paper of A5 size which is (5.8 x 8.3 in) or\r\n" +
            " (148 x 210 mm).\r\n" +
            "\r\n" +
            " If the printed page size is correct, click on \"PASS\"\r\n" +
            " button, otherwise click on \"FAIL\" button.";

        final JDialog dialog = new JDialog();
        dialog.setTitle("WrongPaperPrintingTest");
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
            int leftTime = testTimeout - (int)(System.currentTimeMillis() - startTime);
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
            fail("Size of a printed page is wrong.");
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
    public int print(Graphics g, PageFormat pf, int pageIndex)
        throws PrinterException {
        if (pageIndex == 0) {
            g.setColor(Color.RED);
            g.drawRect((int)pf.getImageableX(), (int)pf.getImageableY(),
                (int)pf.getImageableWidth(), (int)pf.getImageableHeight());
            return Printable.PAGE_EXISTS;
        } else {
            return Printable.NO_SUCH_PAGE;
        }
    }
}
