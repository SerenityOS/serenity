/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4257262 6708509
 * @summary Image should be sent to printer.
* @run main/manual PrintAWTImage
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;


public class PrintAWTImage extends Frame
                           implements ActionListener, Printable {

 public Image imgJava;


 public static void main(String args[]) {
    PrintAWTImage f = new PrintAWTImage();
    f.show();
 }

 public PrintAWTImage() {

    Button printButton = new Button("Print");
    setLayout(new FlowLayout());
    add(printButton);
    printButton.addActionListener(this);

    addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
             System.exit(0);
            }
    });

    pack();
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
      Image imgJava = Toolkit.getDefaultToolkit().getImage("duke.gif");
      g2d.drawImage(imgJava, 0, 0, this);

      return Printable.PAGE_EXISTS;
    }

}
