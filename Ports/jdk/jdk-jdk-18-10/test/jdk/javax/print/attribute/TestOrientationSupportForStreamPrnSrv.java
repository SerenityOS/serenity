/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4882305
 * @summary  Verifies if StreamPrintServ.getSupportedAttributeValues returns
 *           valid Orientation attribute and not null for image/jpeg DocFlavor.
 * @run main TestOrientationSupportForStreamPrnSrv
 */
import java.io.File;
import java.io.FileOutputStream;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.standard.OrientationRequested;

public class TestOrientationSupportForStreamPrnSrv {

    public static void main(java.lang.String[] args) throws Exception {
        PrintService service = null;
        PrintService defService = PrintServiceLookup.lookupDefaultPrintService();
        File f = null;
        FileOutputStream fos = null;
        String mType = "application/postscript";
        DocFlavor flavors[] = null;

        f = new File("streamexample1.ps");
        fos = new FileOutputStream(f);
        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.
                lookupStreamPrintServiceFactories(DocFlavor.INPUT_STREAM.JPEG,
                        mType);
        if (factories.length > 0) {
            System.out.println("output format "+factories[0].getOutputFormat());
            service = factories[0].getPrintService(fos);
            flavors = factories[0].getSupportedDocFlavors();
        }
        System.out.println("Stream Print Service "+service);

        if (service == null) {
            throw new RuntimeException("No Stream Print Service found");
        }
        System.out.println("is OrientationRequested supported? "+
                service.isAttributeCategorySupported(OrientationRequested.class));

        for (int k = 0; k < flavors.length; k ++) {
            Object obj = service.getSupportedAttributeValues(OrientationRequested.class,
                    flavors[k], null);
            if (flavors[k].equals(DocFlavor.INPUT_STREAM.JPEG)) {
                if (obj == null) {
                    throw new RuntimeException(""
                            + "StreamPrintServ.getSupportedAttributeValues "
                            + "returns null for image/jpeg DocFlavor for "
                            + "supported Orientation category");
                }
            }
        }
    }
}

