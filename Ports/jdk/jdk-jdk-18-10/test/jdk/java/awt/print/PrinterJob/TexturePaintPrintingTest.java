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
 * @test
 * @bug 8040635
 * @summary  Verifies if TexturePaint is printed in osx
 * @run main/manual TexturePaintPrintingTest
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class TexturePaintPrintingTest extends Component implements Printable {
    private static void printTexture() {
        f = new JFrame("Texture Printing Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final TexturePaintPrintingTest gpt = new TexturePaintPrintingTest();
        Container c = f.getContentPane();
        c.add(BorderLayout.CENTER, gpt);

        final JButton print = new JButton("Print");
        print.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable(gpt);
                final boolean doPrint = job.printDialog();
                if (doPrint) {
                    try {
                        job.print();
                    } catch (PrinterException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        c.add(print, BorderLayout.SOUTH);

        f.pack();
        f.setVisible(true);
    }

    public Dimension getPreferredSize() {
        return new Dimension(500,500);
    }

    public void paint(Graphics g) {
        doPaint((Graphics2D)g);
    }

    public int print( Graphics graphics, PageFormat format, int index ) {
        Graphics2D g2d = (Graphics2D)graphics;
        g2d.translate(format.getImageableX(), format.getImageableY());
        doPaint(g2d);
        return index == 0 ? PAGE_EXISTS : NO_SUCH_PAGE;
    }

    static final float DIM = 100;
    public void doPaint(Graphics2D g2d) {
        BufferedImage patternImage = new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        Graphics gImage = patternImage.getGraphics();
        gImage.setColor(Color.WHITE);
        gImage.drawLine(0,1,1,0);
        gImage.setColor(Color.BLACK);
        gImage.drawLine(0,0,1,1);
        gImage.dispose();

        Rectangle2D.Double shape = new Rectangle2D.Double(0,0,DIM*6/5, DIM*8/5);
        g2d.setPaint(new TexturePaint(patternImage, new Rectangle2D.Double(0,0,
                     DIM*6/50, DIM*8/50)));
        g2d.fill(shape);
        g2d.setPaint(Color.BLACK);
        g2d.draw(shape);
    }

    public static synchronized void pass() {
        testPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail() {
        testPassed = false;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    private static Thread mainThread;
    private static boolean testPassed;
    private static boolean testGeneratedInterrupt;
    private static JFrame f = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //createUI();
                doTest(TexturePaintPrintingTest::printTexture);
            }
        });
        mainThread = Thread.currentThread();
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException("TexturePaint did not print");
            }
        }
        if (!testGeneratedInterrupt) {
            throw new RuntimeException("user has not executed the test");
        }
    }

    private static void doTest(Runnable action) {
        String description
                = " A TexturePaint graphics will be shown on console.\n"
                + " The same graphics is sent to printer.\n"
                + " Please verify if TexturePaint shading is printed.\n"
                + " If none is printed, press FAIL else press PASS";

        final JDialog dialog = new JDialog();
        dialog.setTitle("printSelectionTest");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton testButton = new JButton("Start Test");
        final JButton passButton = new JButton("PASS");
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            f.dispose();
            dialog.dispose();
            pass();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            f.dispose();
            dialog.dispose();
            fail();
        });
        testButton.addActionListener((e) -> {
            testButton.setEnabled(false);
            action.run();
            passButton.setEnabled(true);
            failButton.setEnabled(true);
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(testButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);

        dialog.pack();
        dialog.setVisible(true);
        dialog.addWindowListener(new WindowAdapter() {
           @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("main dialog closing");
                testGeneratedInterrupt = false;
                mainThread.interrupt();
            }
        });
    }
}
