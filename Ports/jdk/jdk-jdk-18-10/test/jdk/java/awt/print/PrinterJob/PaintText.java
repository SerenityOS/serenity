/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful printer
 * @bug 6498340
 * @summary No exception when printing text with a paint.
 * @run main PaintText
 */

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.swing.*;

public class PaintText extends Component implements Printable {

    static int preferredSize;
    static int NUMTABS = 6;
    int tabNumber;

    public static void main(String args[]) {

        PrinterJob pjob = PrinterJob.getPrinterJob();
        if (pjob.getPrintService() == null) {
            System.out.println("No printers: cannot continue");
            return;
        }

        PageFormat pf = pjob.defaultPage();
        preferredSize = (int)pf.getImageableWidth();

        Book book = new Book();

        JTabbedPane p = new JTabbedPane();

        for (int id=1; id <= NUMTABS; id++) {
            String name = "Tab " + new Integer(id);
            PaintText ptt = new PaintText(id);
            p.add(name, ptt);
            book.append(ptt, pf);
        }
        pjob.setPageable(book);

        JFrame f = new JFrame();
        f.add(BorderLayout.CENTER, p);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.pack();
        f.show();

        /* Non-jtreg execution will display the dialog */
        if (System.getProperty("test.jdk") == null) {
            if (!pjob.printDialog()) {
                return;
            }
        }
        try {
            pjob.print();
        } catch (PrinterException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            f.dispose();
        }
    }

    public PaintText(int id) {
        tabNumber = id;
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        System.out.println(""+pageIndex);
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(pf.getImageableX(),  pf.getImageableY());
        g.drawString("ID="+tabNumber,100,20);
        g.translate(0, 25);
        paint(g);
        return PAGE_EXISTS;
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        return new Dimension(preferredSize, preferredSize);
    }

    public void paint(Graphics g) {

        /* fill with white before any transformation is applied */
        g.setColor(Color.white);
        g.fillRect(0, 0, getSize().width, getSize().height);

        Graphics2D g2d = (Graphics2D)g;

        Font f = new Font(Font.DIALOG, Font.PLAIN, 40);
        Color c = new Color(0,0,255,96);
        Paint p = new GradientPaint(0f, 0f, Color.green,
                                    10f, 10f, Color.red,
                                    true);
        String s = "Sample Text To Paint";
        float x = 20, y= 50;

        switch (tabNumber) {
        case 1:
            g2d.setFont(f);
            g2d.setColor(c);
            g2d.drawString(s, x, y);
            break;

        case 2:
            g2d.setFont(f);
            g2d.setPaint(p);
            g2d.drawString(s, x, y);
            break;

        case 3:
            AttributedString as = new AttributedString(s);
            as.addAttribute(TextAttribute.FONT, f);
            as.addAttribute(TextAttribute.FOREGROUND, c);
            g2d.drawString(as.getIterator(), x, y);
            break;

        case 4:
            as = new AttributedString(s);
            as.addAttribute(TextAttribute.FONT, f);
            as.addAttribute(TextAttribute.FOREGROUND, p);
            g2d.drawString(as.getIterator(), x, y);
            break;

        case 5:
            as = new AttributedString(s);
            as.addAttribute(TextAttribute.FONT, f);
            as.addAttribute(TextAttribute.FOREGROUND, c);
            FontRenderContext frc = g2d.getFontRenderContext();
            TextLayout tl = new TextLayout(as.getIterator(), frc);
            tl.draw(g2d, x, y);
            break;

        case 6:
            as = new AttributedString(s);
            as.addAttribute(TextAttribute.FONT, f);
            as.addAttribute(TextAttribute.FOREGROUND, p);
            frc = g2d.getFontRenderContext();
            tl = new TextLayout(as.getIterator(), frc);
            tl.draw(g2d, x, y);
            break;

        default:
        }
    }

}
