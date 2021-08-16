/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4254954
 * @summary PageFormat would fail on solaris when setting orientation
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

public class ReverseLandscapeTest extends Frame  {

 private TextCanvas c;

 public static void main(String args[]) {
    ReverseLandscapeTest f = new ReverseLandscapeTest();
    f.show();
 }

 public ReverseLandscapeTest() {
    super("JDK 1.2 Text Printing");

    c = new TextCanvas();
    add("Center", c);

    PrinterJob pj = PrinterJob.getPrinterJob();

    PageFormat pf = pj.defaultPage();
    pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);

    // This code can be added if one wishes to test printing
//     pf = pj.pageDialog(pf);

//     if (pj != null && pj.printDialog()) {

//         pj.setPrintable(c, pf);
//         try {
//             pj.print();
//         } catch (PrinterException pe) {
//         } finally {
//             System.err.println("PRINT RETURNED");
//         }
//     }

    addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
             System.exit(0);
            }
    });

    pack();
 }

 class TextCanvas extends Panel implements Printable {

    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {
      int iw = getWidth();
      int ih = getHeight();
      Graphics2D g2d = (Graphics2D)g;

      if (pgIndex > 0)
         return Printable.NO_SUCH_PAGE;

      g2d.translate(pgFmt.getImageableX(), pgFmt.getImageableY());
      g2d.translate(iw/2, ih/2);
      g2d.setFont(new Font("Times",Font.PLAIN, 12));
      g2d.setPaint(new Color(0,0,0));
      g2d.setStroke(new BasicStroke(1f));
      g2d.drawString("Print REVERSE_LANDSCAPE", 30, 40);

      return Printable.PAGE_EXISTS;
    }

    public void paint(Graphics g) {
      g.drawString("Print REVERSE_LANDSCAPE", 30, 40);
    }

     public Dimension getPreferredSize() {
        return new Dimension(250, 100);
    }
 }

}
