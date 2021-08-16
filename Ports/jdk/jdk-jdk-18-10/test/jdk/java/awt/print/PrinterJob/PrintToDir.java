/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful printer
 * @bug 4973278 8015586
 * @run main PrintToDir
 * @summary Must throw exception when printing to an invalid filename - a dir.
 */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.print.PrintService;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.util.PropertyPermission;

public class PrintToDir extends Frame implements Printable {

    boolean firstTime = true;
    double sx, sy;
    Shape clip, firstClip;

    TextField tf = new TextField();
    Label tfLabel = new Label ("File Name");
    Panel p = new Panel (new GridLayout(2,2));
    Button b = new Button("Print");

    PrintToDir() {
        add("South", p);
        p.add(tfLabel);
        p.add(tf);
        p.add(b);
        setSize(300, 300);
        setVisible(true);
    }

    public int print(Graphics g, PageFormat pf, int pageIndex)  {
        Graphics2D g2 = (Graphics2D)g;
        if (pageIndex>=1) {
                return Printable.NO_SUCH_PAGE;
        }
        g2.drawString("hello world", 100, 100);
        return Printable.PAGE_EXISTS;
    }

    void doPrintJob(String fileStr) {
        PageAttributes pa = new PageAttributes();
        JobAttributes ja = new JobAttributes();
        ja.setDialog(JobAttributes.DialogType.NONE);
        ja.setDestination(JobAttributes.DestinationType.FILE);
        ja.setFileName(fileStr);
        try {
            PrintJob pjob = Toolkit.getDefaultToolkit().getPrintJob(this,
                                        "PrintDialog Testing", ja, pa);
            if (pjob != null) {
                System.out.println("Printjob successfully created: " + pjob);
                Graphics g = pjob.getGraphics();
                this.printAll(g);
                g.dispose();
                pjob.end();
            }
            System.out.println("Printing completed");
        } catch (IllegalArgumentException e) {
            System.out.println("PrintJob passed.");
            return;
        }
        throw new RuntimeException("PrintJob::IllegalArgumentException expected but not thrown. \nTEST FAILED");
    }

    public static void doPrinterJob(String fileStr, OrientationRequested o) {
        PrinterJob  pj = PrinterJob.getPrinterJob();
        PrintService ps = pj.getPrintService();
        if (ps == null) {
          System.out.println("No print service found.");
          return;
        }
        pj.setPrintable(new PrintToDir());
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(o);
        File f = new File(fileStr);
        //      f.deleteOnExit();
        URI dest = f.toURI();
        Destination d = new Destination(dest);
        if (ps.isAttributeValueSupported(d, null, null)) {
            aset.add(d);
            try {
                pj.print(aset);
            } catch (PrinterException e) {
                System.out.println("PrinterJob passed.");
                return;
            }
            throw new RuntimeException("PrinterJob:PrinterException expected but not thrown. \nTEST FAILED");
        } else {
            System.out.println("Destination attribute is not a supported value.  PrinterJob passed.");
        }
    }


    public static void main(String arg[]) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            System.out.println("Security manager detected");
            try {
                security.checkPermission(new FilePermission("<<ALL FILES>>", "read,write"));
                security.checkPermission(new PropertyPermission("user.dir", "read"));
            } catch (SecurityException se) {
                System.out.println("Security requirement not obtained.  TEST PASSED");
                return;
            }
        }
        String[] testStr = {".", ""};
        for (int i=0; i<testStr.length; i++) {
            System.out.println("Testing file name = \""+testStr[i]+"\"");
            doPrinterJob(testStr[i], OrientationRequested.PORTRAIT);
            PrintToDir ptd = new PrintToDir();
            ptd.doPrintJob(testStr[i]);
            ptd.dispose();
        }
        System.out.println("TEST PASSED");
    }

}
