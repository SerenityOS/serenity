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
 * @bug 4886069 8023045
 * @summary Confirm that printer recognizes the Legal selection either by
 *          prompting the user to put Legal paper or automatically selecting
 *          the tray containing Legal Paper.  The printout image should not
 *          be shifted up by about 3".
 * @run main/manual PrintTest
 *
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.io.*;


public class PrintTest extends JFrame {
  private JPanel contentPane;
  private JMenuBar jMenuBar1 = new JMenuBar();
  private JMenu jMenuFile = new JMenu();
  private JMenuItem jMenuItem1 = new JMenuItem();
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel jPanel1 = new JPanel();
  private BorderLayout borderLayout2 = new BorderLayout();
  private JScrollPane jScrollPane1 = new JScrollPane();
  private JTextArea jTextArea1 = new JTextArea();
  private Border border1;

  //Construct the frame
  public PrintTest() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();

    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  private void jbInit() throws Exception  {
    contentPane = (JPanel) this.getContentPane();
    border1 = BorderFactory.createLineBorder(Color.black,1);
    contentPane.setLayout(borderLayout1);
    this.setTitle("Print Test");
    jMenuFile.setText("File");
    jMenuItem1.setText("Print");
    jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80, java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuItem1_actionPerformed(e);
      }
    });
    jPanel1.setLayout(borderLayout2);
    jTextArea1.setBorder(border1);
    jTextArea1.setText("1. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "2. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "3. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "4. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "5. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "6. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "7. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "8. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "9. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "10. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "11. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "12. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "13. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "14. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "15. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "16. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "17. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "18. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "19. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "20. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "21. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "22. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "23. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "24. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "25. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "26. This is a printer test designed to illustrate a bug in the java printing API.\n\n"+
                       "27. This is a printer test designed to illustrate a bug in the java printing API.");
    jMenuFile.add(jMenuItem1);
    contentPane.add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(jScrollPane1, BorderLayout.CENTER);
    jScrollPane1.getViewport().add(jTextArea1, null);
    jScrollPane1.setPreferredSize(new Dimension(468,648));
    jTextArea1.setPreferredSize(new Dimension(468,864));
    jMenuBar1.add(jMenuFile);
    this.setJMenuBar(jMenuBar1);
  }

  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }

  void jMenuItem1_actionPerformed(ActionEvent e) {
    PrintUtils.printComponent(jTextArea1);
  }




  public static class PrintUtils implements Printable {
    private JComponent componentToBePrinted;
    protected double scale =1.0;
    PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();


    public static void printComponent(JComponent c) {
      new PrintUtils(c).print();
    }

    public PrintUtils(JComponent componentToBePrinted) {
      this.componentToBePrinted = componentToBePrinted;

    }

    void print() {
      DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
          pras.add(MediaSizeName.NA_LEGAL);

      PrintService printService[] = PrintServiceLookup.lookupPrintServices(flavor,pras);
      PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
      if ((defaultService == null) || (printService.length == 0)) {
          System.out.println("No default print service found. Test aborted.");
          return;
      }

      PrintService service = ServiceUI.printDialog(null,100,100,printService,defaultService,flavor,pras);

      if(service != null) {
        DocPrintJob job = service.createPrintJob();
        DocAttributeSet das = new HashDocAttributeSet();

        Doc doc = new SimpleDoc(this,flavor,das);

        try {
          job.print(doc,pras);

        } catch(PrintException pe) {
          pe.printStackTrace();
        }
      }

    }


    public int print(Graphics g, PageFormat pageFormat, int pageIndex)
    {

      double h=componentToBePrinted.getHeight();
      double pageHeight=pageFormat.getImageableHeight();

      if (pageIndex * pageHeight > h * scale) {
        return(NO_SUCH_PAGE);
      } else {

        Graphics2D g2d = (Graphics2D)g;

        //move past unprintable area
        double xOffset=pageFormat.getImageableX();
        double yOffset=pageFormat.getImageableY();
        g2d.translate(xOffset,yOffset);


        //move to correct page taking into account the scaling
        double newx=0;
        double newy=pageHeight*(-pageIndex);
        g2d.translate(newx / 1.0,newy / 1.0 );

        //print

        componentToBePrinted.print(g2d);
        return(PAGE_EXISTS);
        }
    }

    public static void disableDoubleBuffering(Component c) {
      RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(false);
    }

    /** Re-enables double buffering globally. */

    public static void enableDoubleBuffering(Component c) {
      RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(true);
    }
}

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    PrintTest frame = new PrintTest();
    frame.pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }

}
