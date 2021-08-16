/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240654
 * @summary Test printing alpha colors - banded printing works with ZGC.
 * @key headful printer
 * @requires (os.family == "windows")
 * @requires vm.gc.Z
 * @run main/manual/othervm -XX:+UseZGC -Dsun.java2d.d3d=false AlphaPrintTest
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class AlphaPrintTest extends JPanel implements Printable {

    static final int W=400, H=600;

    static volatile JFrame frame = null;
    static volatile AlphaPrintTest comp = null;
    static Color color = Color.red;
    static volatile boolean passed = false;
    static volatile boolean printInvoked = false;
    static volatile boolean done = false;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("Alpha Color Print Test");
            frame.setLayout(new GridLayout(1, 2));
            frame.add(comp = new AlphaPrintTest());
            frame.add(new InstructionPanel());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });

        while (!done || !printInvoked) {
            Thread.sleep(1000);
        }

        SwingUtilities.invokeAndWait(() -> frame.dispose());

        if (!passed) {
            throw new RuntimeException("Test failed.");
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(W, H);
    }


    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintContent(g);
    };

    private void paintContent(Graphics g) {
        Color c = new Color(255, 0, 0, 240); // not a solid color.
        g.setColor(c);
        g.drawLine(0, 0, W, H);
        g.drawLine(W, 0, 0, H);

        for (int i=10; i < 150; i+=10) {
            g.drawRect(i, i, W-(i*2), H-(i*2));
        }
        g.drawString("Alpha Paint Test", W/2-30, H/2);
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        if (pageIndex == 0) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            paintContent(g);
            return Printable.PAGE_EXISTS;
        }
        return Printable.NO_SUCH_PAGE;
    }

    public void doPrint() {
        printInvoked = true;
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(this);
        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (PrinterException e) {
                e.printStackTrace();
                done = true;
            }
        }
    }

    public void doClose(boolean pass) {
        if (printInvoked) {
            passed = pass;
            done = true;
        }
    }
}

class  InstructionPanel extends JPanel implements ActionListener {

    static final String INSTRUCTIONS =
            "You must have a printer to peform this test.\n" +
                    "Press the print button which will bring up a print dialog." +
                    "Select a suitable printer, and confirm to print. " +
                    "Examine the printed output. It should closely resemble the rendering in" +
                    " the panel to the left. If yes, press PASS, else press FAIL";

    InstructionPanel() {
        GridLayout gl1 = new GridLayout(2, 1);
        setLayout(gl1);
        JTextArea ta = new JTextArea(INSTRUCTIONS);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        add(ta);
        JPanel p = new JPanel();
        JButton print = new JButton("Print");
        JButton pass = new JButton("PASS");
        JButton fail = new JButton("FAIL");
        print.addActionListener(this);
        pass.addActionListener(this);
        fail.addActionListener(this);
        p.add(print);
        p.add(pass);
        p.add(fail);
        add(p);
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 600);
    }


    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
            case "Print" -> AlphaPrintTest.comp.doPrint();
            case "PASS" -> AlphaPrintTest.comp.doClose(true);
            case "FAIL" -> AlphaPrintTest.comp.doClose(false);
        }

    }

}
