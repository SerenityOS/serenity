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
 * @bug 8162796
 * @summary  Verifies if RadialGradientPaint is printed in osx
 * @run main/manual RadialGradientPrintingTest
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;
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

public class RadialGradientPrintingTest extends Component implements Printable {
    private static Thread mainThread;
    private static boolean testPassed;
    private static boolean testGeneratedInterrupt;
    private static JFrame f = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //createUI();
                doTest(RadialGradientPrintingTest::createUI);
            }
        });
        mainThread = Thread.currentThread();
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException("LinearGradientPaint did not print");
            }
        }
        if (!testGeneratedInterrupt) {
            throw new RuntimeException("user has not executed the test");
        }
    }

    public static void createUI() {
        f = new JFrame("RadialGradient Printing Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final RadialGradientPrintingTest gpt = new RadialGradientPrintingTest();
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

        g2d.translate(DIM*0.2, DIM*0.2);
        Shape s = new Rectangle2D.Float(0, 0, DIM*2, DIM*2);

        // RadialGradientPaint
        Point2D centre = new Point2D.Float(DIM/2.0f, DIM/2.0f);
        float radius = DIM/2.0f;
        Point2D focus = new Point2D.Float(DIM/3.0f, DIM/3.0f);
        float stops[] = {0.0f, 1.0f};
        Color colors[] =  { Color.red, Color.white} ;

        RadialGradientPaint rgp =
           new RadialGradientPaint(centre, radius, focus, stops, colors,
              RadialGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(rgp);
        g2d.fill(s);

        g2d.translate(DIM*2.2, 0);
        Color colors1[] =  { Color.red, Color.blue, Color.green} ;
        float stops1[] = {0.0f, 0.5f, 1.0f};
        RadialGradientPaint rgp1 =
           new RadialGradientPaint(centre, radius, focus, stops1, colors1,
              RadialGradientPaint.CycleMethod.REFLECT);
        g2d.setPaint(rgp1);
        g2d.fill(s);

        g2d.translate(-DIM*2.2, DIM*2.2);
        Color colors2[] =  { Color.red, Color.blue, Color.green, Color.white} ;
        float stops2[] = {0.0f, 0.3f, 0.6f, 1.0f};
        RadialGradientPaint rgp2 =
           new RadialGradientPaint(centre, radius, focus, stops2, colors2,
              RadialGradientPaint.CycleMethod.REPEAT);
        g2d.setPaint(rgp2);
        g2d.fill(s);

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

    private static void doTest(Runnable action) {
        String description
                = " A RadialGradientPaint graphics will be shown on console.\n"
                + " The same graphics is sent to printer.\n"
                + " Please verify if RadialGradientPaint shading is printed.\n"
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
