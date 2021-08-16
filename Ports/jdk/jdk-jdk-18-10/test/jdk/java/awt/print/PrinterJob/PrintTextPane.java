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
 * @test PrintTextPane.java
 * @key headful printer
 * @bug 6452415 6570471
 * @summary Test that swing text prints using GDI printer fonts.
 * @author prr: area=PrinterJob
 * @run main PrintTextPane
 */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.print.*;

public class PrintTextPane extends JTextPane implements Printable {

   static String text = "Twinkle twinkle little star, \n" +
                        "How I wonder what you are. \n" +
                        "Up above the world so high, \n" +
                        "Like a diamond in the sky. \n" +
                        "Twinkle, twinkle, little star, \n" +
                        "How I wonder what you are!\n";

    public int print(Graphics g, PageFormat pf, int page)
                                 throws PrinterException {
        if (page > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        printAll(g);
        return PAGE_EXISTS;
    }

    public void printPane(PrintRequestAttributeSet aset) {
        try {
             print(null, null, false, null, aset, false);
         } catch (PrinterException ex) {
               throw new RuntimeException(ex);
         }
    }

    public void printPaneJob(PrintRequestAttributeSet aset) {
         PrinterJob job = PrinterJob.getPrinterJob();
         job.setPrintable(this);
         try {
             job.print(aset);
         } catch (PrinterException ex) {
             throw new RuntimeException(ex);
         }
    }

    public PrintTextPane(String fontFamily) {
        super();
        SimpleAttributeSet aset = new SimpleAttributeSet();
        StyleConstants.setFontFamily(aset, fontFamily);
        setCharacterAttributes(aset, false);
        setText(text+text+text+text+text+text+text+text);
    }

    public static void main(String args[]) throws Exception {

        String os = System.getProperty("os.name");

        if (!os.startsWith("Windows")) {
             return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
            System.err.println("Warning: no printers, skipping test");
            return;
        }
        JFrame f = new JFrame("Print Text Pane1");
        f.addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        PrintTextPane monoPane = new PrintTextPane("Monospaced");
        f.add("East", monoPane);
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        PrintTextPane courPane = new PrintTextPane("Courier New");
        f.add("West", courPane);
        f.pack();
        f.setVisible(true);

        File spoolFile = File.createTempFile("CourText", ".prn");
        System.out.println(spoolFile);
        Destination dest = new Destination(spoolFile.toURI());
        aset.add(dest);
        courPane.printPane(aset);
        long courLen = spoolFile.length();
        System.out.println("CourText="+spoolFile.length());
        spoolFile.delete();

        spoolFile = File.createTempFile("MonoText", ".prn");
        System.out.println(spoolFile);
        dest = new Destination(spoolFile.toURI());
        aset.add(dest);
        monoPane.printPane(aset);
        long monoLen = spoolFile.length();
        System.out.println("MonoText="+spoolFile.length());
        spoolFile.delete();

        if (courLen > 2 * monoLen) {
            throw new RuntimeException("Shapes being printed?");
        }

        spoolFile = File.createTempFile("CourJob", ".prn");
        System.out.println(spoolFile);
        dest = new Destination(spoolFile.toURI());
        aset.add(dest);
        courPane.printPaneJob(aset);
        courLen = spoolFile.length();
        System.out.println("CourJob="+spoolFile.length());
        spoolFile.delete();

        spoolFile = File.createTempFile("MonoJob", ".prn");
        System.out.println(spoolFile);
        dest = new Destination(spoolFile.toURI());
        aset.add(dest);
        monoPane.printPaneJob(aset);
        monoLen = spoolFile.length();
        System.out.println("MonoJob="+spoolFile.length());
        spoolFile.delete();

        if (courLen > 2 * monoLen) {
            throw new RuntimeException("Shapes being printed?");
        }

    }
}
