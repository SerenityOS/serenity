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
  @bug 6397684
  @summary PASS if no VM crash.
  @run main NullGetName
*/


import javax.print.*;
import javax.print.attribute.*;
import javax.print.event.*;
import java.awt.print.*;


public class NullGetName {

    public static void main(String[] args) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
                try {
                printerJob.setPrintService(new ImagePrintService());
                } catch (PrinterException e) {
                }
    }
}


class ImagePrintService implements PrintService {


    public Class[] getSupportedAttributeCategories() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAttributeCategorySupported(Class category) {
        // TODO Auto-generated method stub
        return false;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocFlavor[] getSupportedDocFlavors() {
        // TODO Auto-generated method stub
        return null;
    }


    public boolean isDocFlavorSupported(DocFlavor flavor) {
        if(DocFlavor.SERVICE_FORMATTED.PAGEABLE.equals(flavor))
            return true;
        if(DocFlavor.SERVICE_FORMATTED.PRINTABLE.equals(flavor))
            return true;
        return false;
    }

    public DocPrintJob createPrintJob() {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceUIFactory getServiceUIFactory() {
        // TODO Auto-generated method stub
        return null;
    }


    public PrintServiceAttributeSet getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addPrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {
        // TODO Auto-generated method stub

    }

    public void removePrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {
        // TODO Auto-generated method stub

    }

    public Object getDefaultAttributeValue(Class category) {
        // TODO Auto-generated method stub
        return null;
    }

        public <T extends PrintServiceAttribute> T
        getAttribute(Class<T> category) {
            // TODO Auto-generated method stub
        return null;
    }

    public boolean isAttributeValueSupported(Attribute attrval,
            DocFlavor flavor, AttributeSet attributes) {
        // TODO Auto-generated method stub
        return false;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
            AttributeSet attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getSupportedAttributeValues(Class category, DocFlavor flavor,
            AttributeSet attributes) {
        // TODO Auto-generated method stub
        return null;
    }

}
