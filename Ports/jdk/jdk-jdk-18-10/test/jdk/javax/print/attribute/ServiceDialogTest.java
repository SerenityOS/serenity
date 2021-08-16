/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4910388 4871089 4998624
 * @summary Confirm that
 *      1. After choosing Reverse Landscape in the system default print
 *       Print Service (2nd in the list), it
 *          will reset to portrait in "Test Printer"
 *      2. Print To File button is not cleared when switching between the
 *         2nd service (system default printer) and Test Printer.
 *      3. Make sure "Postscript" printer is the default and make sure the
 *         "print to file" button is disabled.  File Dialog should not be
 *         shown after pressing print button.
 *
 * @run main/manual ServiceDialogTest
 */
import java.awt.*;
import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;
import javax.print.event.*;
import java.io.*;
import java.util.Locale;

public class ServiceDialogTest {
        /**
         * Constructor
         */
         public ServiceDialogTest() {
                super();
        }
        /**
         * Starts the application.
         */
        public static void main(java.lang.String[] args) {
                ServiceDialogTest pd = new ServiceDialogTest();
                PrintService services[] = new PrintService[3];
                services[1] = PrintServiceLookup.lookupDefaultPrintService();

                FileOutputStream fos = null;
                File f = null;
                String mType = "application/postscript";
                DocFlavor flavor = DocFlavor.INPUT_STREAM.JPEG;
                try {
                        f = new File("streamexample.ps");
                        fos = new FileOutputStream(f);
                        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor, mType);
                        if (factories.length > 0) {
                                services[0] = factories[0].getPrintService(fos);
                        } else {
                                throw new RuntimeException("No StreamPrintService available which would support "+flavor);
                        }

                        services[2] = new TestPrintService("Test Printer");

            //System.out.println("is "+flavor+" supported? "+services[0].isDocFlavorSupported(flavor));
            //System.out.println("is Orientation supported? "+services[0].isAttributeCategorySupported(OrientationRequested.class));
            //System.out.println("is REVERSE PORTRAIT supported ? "+services[0].isAttributeValueSupported(OrientationRequested.REVERSE_PORTRAIT, flavor, null));

            HashPrintRequestAttributeSet prSet = new HashPrintRequestAttributeSet();
            prSet.add(new Destination(new File("./dest.prn").toURI()));
            PrintService selService = ServiceUI.printDialog(null, 200, 200, services, services[0], flavor, prSet);
                        Attribute attr[] = prSet.toArray();
                        for (int x = 0; x < attr.length; x ++) {
                                System.out.println(attr[x]);
                        }

                        //DocPrintJob pj = service.createPrintJob();
                        //PrintDocument prDoc = new PrintDocument();
                        //pj.print(prDoc, null);

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}


class TestPrintService implements PrintService
{

    private static DocFlavor textByteFlavor = null;
    private static final DocFlavor supportedDocFlavors[] = (new DocFlavor[] {
             javax.print.DocFlavor.INPUT_STREAM.JPEG
    });

    private static final Class serviceAttrCats[] = (new Class[] {
             javax.print.attribute.standard.PrinterName.class
    });

    private static final Class otherAttrCats[] = (new Class [] {
             javax.print.attribute.standard.Copies.class,
             javax.print.attribute.standard.OrientationRequested.class,
             javax.print.attribute.standard.Destination.class,
    });

    private String printer = null;

    public TestPrintService() {
    }

    public TestPrintService(String printerName) {
        if (printerName == null) {
            throw new IllegalArgumentException("null printer name");
        } else {
            printer = printerName;
        }
    }

    public String getName()
    {
        return printer;
    }


    public DocPrintJob createPrintJob()
    {
        return  null;
    }

    public PrintServiceAttributeSet getUpdatedAttributes()
    {
        return null;
    }


    public void addPrintServiceAttributeListener(PrintServiceAttributeListener printserviceattributelistener)
    {
    }

    public void removePrintServiceAttributeListener(PrintServiceAttributeListener printserviceattributelistener)
    {
    }

    public PrintServiceAttribute getAttribute(Class category)
    {
        return null;
    }

    public PrintServiceAttributeSet getAttributes()
    {
        HashPrintServiceAttributeSet aSet = new HashPrintServiceAttributeSet();
            return aSet;
    }

    public DocFlavor[] getSupportedDocFlavors()
    {
        int i = supportedDocFlavors.length;
        DocFlavor adocflavor[] = new DocFlavor[i];
        System.arraycopy(supportedDocFlavors, 0, adocflavor, 0, i);
        return adocflavor;
    }

    public boolean isDocFlavorSupported(DocFlavor docflavor)
    {
        for (int i = 0; i < supportedDocFlavors.length; i++) {
            if (docflavor.equals(supportedDocFlavors[i])) {
                return true;
            }
        }
        return false;
    }

    public Class[] getSupportedAttributeCategories()
    {
        int i = otherAttrCats.length;
        Class aclass[] = new Class[i];
        System.arraycopy(otherAttrCats, 0, aclass, 0, otherAttrCats.length);
        return aclass;
    }

    public boolean isAttributeCategorySupported(Class category)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }

        for (int i = 0; i < otherAttrCats.length; i++) {
            if (category == otherAttrCats[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes) {

        if (attrval == OrientationRequested.PORTRAIT)
                return true;
        else if (attrval == OrientationRequested.LANDSCAPE)
                return true;
                else
                        return false;
    }

    public Object getDefaultAttributeValue(Class category)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (category == javax.print.attribute.standard.Copies.class)
                return new Copies(1);

        if (category == javax.print.attribute.standard.OrientationRequested.class)
                return OrientationRequested.PORTRAIT;

        return null;
    }

    public Object getSupportedAttributeValues(Class category, DocFlavor docflavor, AttributeSet attributeset)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }

        if (docflavor != null) {
            if (!isDocFlavorSupported(docflavor)) {
                throw new IllegalArgumentException(docflavor + " is an unsupported flavor");
            }
        }
        if (!isAttributeCategorySupported(category)) {
            return null;
        }
        if (category == javax.print.attribute.standard.Copies.class ) {
               return new CopiesSupported(1, 5);
        }
        if (category == javax.print.attribute.standard.OrientationRequested.class ) {
               OrientationRequested req[] = { OrientationRequested.PORTRAIT, OrientationRequested.LANDSCAPE };
               return req;
        }

        return null;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor docflavor, AttributeSet attributeset) {

        if (docflavor != null && !isDocFlavorSupported(docflavor)) {
            throw new IllegalArgumentException("flavor " + docflavor + "is not supported");
        }
        if (attributeset == null) {
            return null;
        }

        HashAttributeSet hashattributeset = new HashAttributeSet();
        Attribute attributearray[] = attributeset.toArray();
        for (int i = 0; i < attributearray.length; i++) {
            try {
                Attribute attribute = attributearray[i];
                if (!isAttributeCategorySupported(attribute.getCategory())) {
                     hashattributeset.add(attribute);
                } else {
                  if (!isAttributeValueSupported(attribute, docflavor, attributeset)) {
                     hashattributeset.add(attribute);
                  }
                }
            }
            catch (ClassCastException classcastexception) {

            }
        }

        if (hashattributeset.isEmpty()) {
            return null;
        }
        return hashattributeset;
    }

    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }

    public String toString() {
        return "Printer : " + getName();
    }

    public boolean equals(Object obj) {
        return obj == this || (obj instanceof TestPrintService) && ((TestPrintService)obj).getName().equals(getName());
    }

    public int hashCode() {
        return getClass().hashCode() + getName().hashCode();
    }

}
