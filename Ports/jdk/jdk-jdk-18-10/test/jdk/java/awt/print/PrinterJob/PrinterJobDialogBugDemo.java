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
 * @bug 4775862
 * @run main/manual PrinterJobDialogBugDemo
 */
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class PrinterJobDialogBugDemo extends JFrame implements Printable {

  public static void main(String[] args) {
    new PrinterJobDialogBugDemo();
  }

  private PrinterJobDialogBugDemo() {
    super("Printer Job Dialog Bug Demo");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(700,700);

    JButton btnPrint = new JButton("Print...");
    btnPrint.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ae) {
          showPrintDialog();
        }
      });

    Container contentPane = getContentPane();
    contentPane.add(
      new JLabel("<html>This is the main Application Window. " +
                 "To demonstrate the problem:" +
                 "<ol>" +
                 "<li>Click the Print button at the bottom of this window. " +
                 "The Print dialog will appear." +
                 "<li>Select another application window." +
                 "<li>On the Windows taskbar, click the coffee-cup icon for " +
                 "this demo application.  This brings this window to the " +
                 "front but the Print dialog remains hidden. " +
                 "Since this window " +
                 "is no longer selectable, it can't be moved aside to expose "
+
                 "the Print dialog that is now behind it." +
                 "</ol>",
                 SwingConstants.CENTER),
      BorderLayout.NORTH);
    contentPane.add(btnPrint, BorderLayout.SOUTH);
    setVisible(true);
  }

  private void showPrintDialog() {
    PrinterJob printJob = PrinterJob.getPrinterJob();
    printJob.setPrintable(this);
    PrintRequestAttributeSet printRequestAttrSet =
      new HashPrintRequestAttributeSet();
    printJob.printDialog(printRequestAttrSet);
  }

  public int print(java.awt.Graphics g, java.awt.print.PageFormat pageFormat,
int pageIndex) {
    if (pageIndex == 0) {
      return(PAGE_EXISTS);
    } else {
      return(NO_SUCH_PAGE);
    }
  }
}
