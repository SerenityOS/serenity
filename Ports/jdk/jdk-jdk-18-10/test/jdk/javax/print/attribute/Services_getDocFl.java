/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;

/*
 * @test
 * @bug 4901243 8040139 8167291
 * @summary JPG, GIF, and PNG DocFlavors (URL) should be supported if Postscript is supported.
 * @run main Services_getDocFl
*/


public class Services_getDocFl {
    public static void main (String [] args) {

        HashPrintRequestAttributeSet prSet = null;
        boolean psSupported = false,
            pngImagesSupported = false,
            gifImagesSupported = false,
            jpgImagesSupported = false;
        String mimeType;

        PrintService[] serv = PrintServiceLookup.lookupPrintServices(null, null);
        if (serv.length==0) {
            System.out.println("no PrintService  found");
        } else {
            System.out.println("number of Services "+serv.length);
        }


        for (int i = 0; i<serv.length ;i++) {
            System.out.println("           PRINT SERVICE: "+ i+" "+serv[i]);
            DocFlavor[] flavors = serv[i].getSupportedDocFlavors();
            pngImagesSupported = false;
            gifImagesSupported = false;
            jpgImagesSupported = false;
            psSupported = false;
            for (int j=0; j<flavors.length; j++) {
                System.out.println(flavors[j]);

                if (flavors[j].equals(DocFlavor.URL.PNG)) {
                    pngImagesSupported = true;
                } else if (flavors[j].equals(DocFlavor.URL.GIF)) {
                    gifImagesSupported = true;
                } else if (flavors[j].equals(DocFlavor.URL.JPEG)) {
                    jpgImagesSupported = true;
                } else if (flavors[j].getMimeType().indexOf("postscript") != -1) {
                    psSupported = true;
                }
            }

            if (psSupported && !(pngImagesSupported && gifImagesSupported &&
                jpgImagesSupported)) {

                throw new RuntimeException("Error: URL image DocFlavors are not reported as supported");
            }
        }

    }
}

