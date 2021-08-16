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
 * @bug 4922036
 * @summary Confirm that no Exception is thrown and 2 identical output is produced.
 * @run main/manual MultiThreadTest
 */
import java.io.*;
import javax.print.*;


public class MultiThreadTest extends Thread {

    private PrintService service = PrintServiceLookup.lookupDefaultPrintService();
    private Doc doc = null;

    public MultiThreadTest(Doc docObject) {
        this.doc = docObject;
    }

    public void print() {
        try {
            DocPrintJob job = null;

            job = this.service.createPrintJob();
            if (job == null) {
                System.out.println("Fail: DocPrintJob is null...");
                return;
            }
            System.out.println("About to print image...");

            job.print(this.doc, null);
            System.out.println("Image printed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.print();
    }

    public static void main(String args[]) {
        if (args.length <= 0) {
            System.out.println("Usage: java MultiThreadTest <img file>");
            return;
        }
        Object printData = null;

        try {
            File file = new File(args[0]);

            printData = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);

            in.read((byte[]) printData);
            in.close();
        } catch (FileNotFoundException fe) {
            System.out.println("ByteDoc: FileNotFoundException: "
                    + fe.toString());

        } catch (IOException ie) {
            System.out.println("ByteDoc: IOException: " + ie.toString());
        }
        Doc doc1 = new ByteDoc(printData, DocFlavor.BYTE_ARRAY.GIF);
        Doc doc2 = new ByteDoc(printData, DocFlavor.BYTE_ARRAY.GIF);

        Thread thread1 = new MultiThreadTest(doc1);
        Thread thread2 = new MultiThreadTest(doc2);

        thread1.start();
        thread2.start();
    }
}


class ByteDoc implements Doc {

    protected DocFlavor flavor = null;
    protected Object printData = null;
    protected InputStream instream = null;
    protected FileReader reader = null;

    // constructor takes the resource file and the document flavor.
    public ByteDoc(Object printdata, DocFlavor docFlavor) {
        this.printData = printdata;
        this.flavor = docFlavor;
    }

    public javax.print.attribute.DocAttributeSet getAttributes() {
        return null;
    }

    public DocFlavor getDocFlavor() {
        return this.flavor;
    }

    public Object getPrintData() {
        return this.printData;
    }

    public Reader getReaderForText() {
        // Document says that if MIME type is non-text and representation class is input stream
        // then return null;
        return null;
    }

    public InputStream getStreamForBytes() {
        synchronized (this) {
            if ((this.instream == null) && (this.printData instanceof byte[])) {
                // its a byte array so create a ByteArrayInputStream.
                System.out.println("creating ByteArrayInputStream...");
                this.instream = new ByteArrayInputStream((byte[]) printData);
            }
        }
        return this.instream;
    }
}
