/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511023
 * @summary Image should be sent to printer, no exceptions thrown
 * @author prr
 * @run main/manual PrintVolatileImage
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;

public class PrintVolatileImage extends Component
                            implements ActionListener, Printable {

    VolatileImage vimg = null;

    public static void main(String args[]) {
       Frame f = new Frame();
       PrintVolatileImage pvi = new PrintVolatileImage();
       f.add("Center", pvi);
       Button b = new Button("Print");
       b.addActionListener(pvi);
       f.add("South", b);
       f.pack();
       f.show();
    }

    public PrintVolatileImage() {
    }

    public Dimension getPreferredSize() {
       return new Dimension(100,100);
    }

    public void paint(Graphics g) {
       if (vimg == null) {
           vimg = createVolatileImage(100,100);
           Graphics ig = vimg.getGraphics();
           ig.setColor(Color.white);
           ig.fillRect(0,0,100,100);
           ig.setColor(Color.black);
           ig.drawLine(0,0,100,100);
           ig.drawLine(100,0,0,100);
        }
        g.drawImage(vimg, 0,0, null);
    }

   public void actionPerformed(ActionEvent e) {

       PrinterJob pj = PrinterJob.getPrinterJob();

       if (pj != null && pj.printDialog()) {
           pj.setPrintable(this);
           try {
               pj.print();
           } catch (PrinterException pe) {
           } finally {
               System.err.println("PRINT RETURNED");
           }
       }
   }


    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {
      if (pgIndex > 0)
         return Printable.NO_SUCH_PAGE;

      Graphics2D g2d = (Graphics2D)g;
      g2d.translate(pgFmt.getImageableX(), pgFmt.getImageableY());
      paint(g);
      return Printable.PAGE_EXISTS;
    }

}
