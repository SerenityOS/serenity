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

/*
  @test
  @bug 5024549
  @summary   Pass if dialogs are modal.
  @run applet/manual PrintApplet.html
*/
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.awt.print.*;
import javax.swing.*;

public class PrintApplet extends JApplet implements Printable {
    private JButton jButton1 = new JButton();


    public PrintApplet() {
    }

    public void init() {
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        jButton1.setText("PRINT");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        jButton1.setBounds(new Rectangle(165, 248, 80, 30));
        this.setSize(new Dimension(400,300));
        this.getContentPane().setLayout(null);
        this.getContentPane().setBackground(Color.pink);
        this.getContentPane().add(jButton1, BorderLayout.SOUTH);
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }

    public String getAppletInfo() {
        return "Applet inf";
    }

    public String[][] getParameterInfo() {
        return null;
    }


   public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
       System.out.println("Calling print");
       if (page == 0) {
           Graphics2D g2 = (Graphics2D)g;
           g2.translate(pf.getImageableX(), pf.getImageableY());
           g2.setColor(Color.black);
           g2.drawString("Hello World", 20, 100);

           return Printable.PAGE_EXISTS;
       }
       return Printable.NO_SUCH_PAGE;
   }



    void jButton1_actionPerformed(ActionEvent e) {
      PrinterJob printJob = null;
      PageFormat pageFormat = null;
      Paper prtPaper = null;
      boolean bPrintFlg = true;


      try{
         printJob = PrinterJob.getPrinterJob();

      }
      catch(SecurityException se){

         bPrintFlg = false;
      }

      if (bPrintFlg) {

         pageFormat = printJob.pageDialog(printJob.defaultPage());
         System.out.println("PrintApplet: pageFormat = "+pageFormat.getWidth()/72.0+" x "+pageFormat.getHeight()/72.0);
         if (pageFormat != null) {

            prtPaper = pageFormat.getPaper();
            pageFormat.setPaper(prtPaper);


            printJob.setPrintable(this, pageFormat);
         }

         if (printJob.printDialog()) {

             try {
                 printJob.print();
             }
             catch (java.awt.print.PrinterException ex) {
                 ex.printStackTrace();
             }

         }

      }
    }
}
