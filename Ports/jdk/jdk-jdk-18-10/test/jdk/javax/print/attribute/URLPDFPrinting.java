/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4899773
 * @summary Test for DocFlavor.URL.PDF support.  No exception should be thrown.
 * @run main URLPDFPrinting
*/

import java.awt.*;
import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;
import java.io.*;
import java.util.Locale;
import java.net.URL;

public class URLPDFPrinting {
        /**
         * Constructor
         */
         public URLPDFPrinting() {
                super();
        }
        /**
         * Starts the application.
         */
        public static void main(java.lang.String[] args) {
                URLPDFPrinting pd = new URLPDFPrinting();
                PrintService service[] = null, defService = null;

                service = PrintServiceLookup.lookupPrintServices(DocFlavor.URL.PDF, null);
                if (service.length == 0) {
                        System.out.println("No PrintService support DocFlavor.URL.PDF");
                        return;
                } else {
                        defService = service[0];
                        System.out.println("Print Service which supports DocFlavor.URL.PDF: "+defService);
                }

                System.out.println("is DocFlavor.URL.PDF supported? "+defService.isDocFlavorSupported(DocFlavor.URL.PDF));
                HashPrintRequestAttributeSet prSet = new HashPrintRequestAttributeSet();
                prSet.add(new Destination(new File("./dest.prn").toURI()));

                DocPrintJob pj = defService.createPrintJob();
                PrintDocument prDoc = new PrintDocument();
                try {
                        pj.print(prDoc, prSet);
                } catch (Exception e) {
                        e.printStackTrace();
                }

        }
}

class PrintDocument implements Doc {
        InputStream fStream = null;
        DocFlavor flavor = null;
        HashDocAttributeSet docSet = new HashDocAttributeSet();
        URL url =  null;

        public PrintDocument() {
                try {
                        url =  PrintDocument.class.getResource("hello.pdf");
                        try{ Thread.sleep(6000); }catch(Exception e){ e.printStackTrace();}
                        fStream = url.openStream();
                        System.out.println("URL input stream "+fStream);
                } catch(Exception e) {
                        e.printStackTrace();
                }
                docSet.add(OrientationRequested.LANDSCAPE);
        }

        public DocAttributeSet getAttributes() {
                System.out.println("getAttributes called");
                return docSet;
        }

        public DocFlavor getDocFlavor() {
                System.out.println("getDocFlavor called");
                return DocFlavor.URL.PDF;
        }

        public Object getPrintData(){
                System.out.println("getPrintData called");
                return url;
        }

        public Reader getReaderForText() {
                return null;
        }

        public InputStream getStreamForBytes() {
                System.out.println("getStreamForBytes called");
                return fStream;
        }
}
