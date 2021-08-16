/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, BELLSOFT. All rights reserved.
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
 * @bug 8265761
 * @requires (os.family == "windows")
 * @summary Font with missed font family name is not properly printed on Windows
 * @run main/othervm/manual PrintFontWithMissedFontFamilyTest
 */

import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PrintFontWithMissedFontFamilyTest {

    private static final String DESCRIPTION =
            " 1. Setup 'Microsoft Print to PDF' printer on Windows.\n" +
                    " 2. Press Print button to print a text with a custom font to PDF.\n" +
                    " 3. Choose 'Microsoft Print to PDF' on the print dialog and press OK.\n" +
                    " 4. Open the PDF file.\n" +
                    "    Three lines with text ABCDEF and the same custom font should be printed.\n" +
                    " 5. Compare the text from the pdf with three strings drawn on the right pane.\n" +
                    "    The pdf and the pane text should have the same font.\n" +
                    " 6. If so, press PASS button, otherwise press FAIL button.\n";

    private static final CountDownLatch testEndedSignal = new CountDownLatch(1);
    private static final int testTimeout = 300000;
    private static volatile String testFailureMsg;
    private static volatile boolean testPassed;
    private static volatile boolean testFinished;
    private static final float FONT_SIZE = 32.0f;
    private static final String TEXT = "ABCDEF";
    private static final String FONT_FILE = "SampleFontMissedFontFamily.ttf";
    private static Font customFont;

    public static void main(String[] args) throws Exception {

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

        final JDialog dialog = new JDialog();
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

        JTextArea textArea = new JTextArea(DESCRIPTION);
        textArea.setEditable(false);

        final JButton testButton = new JButton("Print");
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
            fail("TitledBorder label is cut off");
        });

        try {
            File dir = new File(System.getProperty("test.src", "."));
            File customFontFile = new File(dir, FONT_FILE);
            customFont = Font.createFont(Font.TRUETYPE_FONT, customFontFile);
            customFont = customFont.deriveFont(FONT_SIZE);
        } catch(Exception e) {
            fail("Unable to load the custom font: " + e);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel textPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                drawText((Graphics2D) g, customFont, TEXT);
            }
        };

        textPanel.setPreferredSize(new Dimension(300, 300));

        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(testTimeoutLabel);
        mainPanel.add(labelPanel, BorderLayout.NORTH);
        mainPanel.add(textPanel, BorderLayout.EAST);
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

    private static void doTest() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                new TestPrintable();
            } catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void drawText(Graphics2D g, Font font, String text) {

        Font prevFont = g.getFont();
        g.setFont(font);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, true);

        Rectangle clip = g.getClipBounds();
        int cx = (int) clip.getCenterX();
        int cy = (int) (0.5 * clip.getCenterY());

        FontMetrics metrics = g.getFontMetrics();
        int w = metrics.stringWidth(text);
        int h = metrics.getHeight();

        int x = cx - w / 2;
        int y = cy - h / 4;

        g.drawString(text, x, y);

        GlyphVector gv = font.createGlyphVector(frc, text);
        g.drawGlyphVector(gv, x, y + h);

        gv = font.deriveFont(1.0f).createGlyphVector(frc, text);
        int fontSize = font.getSize();

        AffineTransform scale = AffineTransform.getScaleInstance(fontSize, fontSize);
        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            gv.setGlyphTransform(i, scale);
        }

        g.drawGlyphVector(gv, x, y + 2 * h);

        g.setFont(prevFont);
    }

    private static class TestPrintable implements Printable {

        TestPrintable() throws PrinterException {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(PrintServiceLookup.lookupDefaultPrintService());
            job.setPrintable(this);

            if (job.printDialog()) {
                job.print();
            } else {
                throw new RuntimeException("Printing was canceled!");
            }
        }

        void paint(Graphics2D g) {
            drawText(g, customFont, TEXT);
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int index) {
            if (index == 0) {
                paint((Graphics2D) graphics);
                return PAGE_EXISTS;
            } else {
                return NO_SUCH_PAGE;
            }
        }
    }
}
