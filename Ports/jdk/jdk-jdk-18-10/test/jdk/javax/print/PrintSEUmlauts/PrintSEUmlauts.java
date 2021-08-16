/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;

/*
 * @test
 * @bug 8067364
 * @summary Printing to Postscript doesn't support dieresis
 * @build PrintSEUmlauts
 * @run main/othervm PrintSEUmlauts
 */
public class PrintSEUmlauts implements Printable {

    public static void main(String[] args) throws Exception {

        GraphicsEnvironment.getLocalGraphicsEnvironment();

        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
        String mime = DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType();

        StreamPrintServiceFactory[] factories =
                StreamPrintServiceFactory.
                        lookupStreamPrintServiceFactories(flavor, mime);
        if (factories.length == 0) {
            System.out.println("No print service found.");
            return;
        }

        FileOutputStream output = new FileOutputStream("out.ps");
        StreamPrintService service = factories[0].getPrintService(output);

        SimpleDoc doc =
             new SimpleDoc(new PrintSEUmlauts(),
                           DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                           new HashDocAttributeSet());
        DocPrintJob job = service.createPrintJob();
        job.addPrintJobListener(new PrintJobAdapter() {
            @Override
            public void printJobCompleted(PrintJobEvent pje) {
                testPrintAndExit();
            }
        });

        job.print(doc, new HashPrintRequestAttributeSet());
    }

    private static final boolean DEBUG = false;
    private static void testPrintAndExit() {
        String expected = "<e4>";
        String content = "";

        File file = new File("out.ps");
        if (!DEBUG) {
            file.deleteOnExit();
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            stream.read(data);
            content = new String(data, StandardCharsets.ISO_8859_1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (!content.contains(expected)) {
            System.err.println("FAIL");
            if (DEBUG) {
                System.err.println("printing content");
                System.err.println(content);
            }
            throw new RuntimeException("Expected <e4> to represent '\u00e4' but not found!");
        }
        System.err.println("SUCCESS");
    }

    public int print(Graphics g, PageFormat pf, int pg) {
       if (pg > 0) return NO_SUCH_PAGE;
       g.drawString("\u00e4", 100, 100);
       return PAGE_EXISTS;
   }
}
