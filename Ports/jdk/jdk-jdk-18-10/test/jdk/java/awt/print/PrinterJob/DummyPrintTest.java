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
 * @bug      6921664
 * @key printer
 * @summary  Verifies number of copies and the job name are passed to a
 *           3rd party PrintService.
 * @run      main DummyPrintTest
 */
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.HashSet;
import java.util.Set;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterState;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;
import javax.print.event.PrintServiceAttributeListener;


public class DummyPrintTest {

    public static void main(String[] args) throws Exception {
        // register custom print service implementation
        String printerName = "myDummyPrintService";
        PrintServiceLookup.registerService(new DummyPrintService(printerName));
        // calling third party print logic
        thirdPartyPrintLogic(printerName);
    }

    static void thirdPartyPrintLogic(String printerName) throws Exception {
        PrinterJob printerjob = PrinterJob.getPrinterJob();
        printerjob.setCopies(2);
        printerjob.setJobName("myJobName");
        printerjob.setPrintable(new DummyPrintable());
        for (PrintService printService : PrinterJob.lookupPrintServices()) {
            System.out.println("check printer name of service " + printService);
            if (printerName.equals(printService.getName())) {
                System.out.println("correct printer service do print...");
                printerjob.setPrintService(printService);
                printerjob.print();
                break;
            }
        }
    }
}

class DummyPrintService implements PrintService {
    private final String _name;
    private final Set<DocFlavor> _supportedFlavors;
    private final PrintServiceAttributeSet _printServiceAttributeSet;

    public DummyPrintService(String name) {
        _name = name;
        _supportedFlavors = new HashSet<DocFlavor>();
        _supportedFlavors.add(DocFlavor.SERVICE_FORMATTED.PAGEABLE);
        _supportedFlavors.add(DocFlavor.SERVICE_FORMATTED.PRINTABLE);
        _printServiceAttributeSet = new HashPrintServiceAttributeSet();
        _printServiceAttributeSet.add(new PrinterName(name, null));
        _printServiceAttributeSet.add(PrinterState.IDLE);
    }

    @Override
    public String toString() {
        return "Dummy Printer : " + getName();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public DocPrintJob createPrintJob() {
        return new DummyDocPrintJob(this);
    }

    @Override
    public boolean isDocFlavorSupported(DocFlavor flavor) {
        return _supportedFlavors.contains(flavor);
    }

    @Override
    public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
        return category.cast(_printServiceAttributeSet.get(category));
    }

    @Override
    public PrintServiceAttributeSet getAttributes() {
        return _printServiceAttributeSet;
    }

    @Override
    public DocFlavor[] getSupportedDocFlavors() {
        return _supportedFlavors.toArray(new DocFlavor[_supportedFlavors.size()]);
    }

    @Override
    public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
        return null;
    }

    @Override
    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }

    @Override
    public Class<?>[] getSupportedAttributeCategories() {
        return null;
    }

    @Override
    public Object getSupportedAttributeValues(Class<? extends Attribute> category,
                                    DocFlavor flavor, AttributeSet attributes) {
        return null;
    }

    @Override
    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
                                                 AttributeSet attributes) {
        return null;
    }

    @Override
    public boolean isAttributeCategorySupported(Class<? extends Attribute> category) {
        return false;
    }

    @Override
    public boolean isAttributeValueSupported(Attribute attrval,
                                             DocFlavor flavor,
                                             AttributeSet attributes) {
        return false;
    }

    @Override
    public void addPrintServiceAttributeListener(PrintServiceAttributeListener listener) {
    }

    @Override
    public void removePrintServiceAttributeListener(PrintServiceAttributeListener listener) {
    }
}

class DummyDocPrintJob implements DocPrintJob {
    private static int _counter;
    private final PrintService _printService;
    private final PrintJobAttributeSet _printJobAttributeSet;

    public DummyDocPrintJob(PrintService printService) {
        _counter++;
        _printService = printService;
        _printJobAttributeSet = new HashPrintJobAttributeSet();
    }

    @Override
    public PrintService getPrintService() {
        return _printService;
    }

    @Override
    public PrintJobAttributeSet getAttributes() {
        return _printJobAttributeSet;
    }

    @Override
    public void addPrintJobAttributeListener(PrintJobAttributeListener listener,
                                    PrintJobAttributeSet printJobAttributeSet) {
    }

    @Override
    public void removePrintJobAttributeListener(PrintJobAttributeListener listener) {
    }

    @Override
    public void addPrintJobListener(PrintJobListener listener) {
    }

    @Override
    public void removePrintJobListener(PrintJobListener listener) {
    }

    @Override
    public void print(Doc doc,
                      PrintRequestAttributeSet printRequestAttributeSet)
          throws PrintException {
        System.out.println("job name: " + printRequestAttributeSet.get(JobName.class));
        System.out.println("copies: " + printRequestAttributeSet.get(Copies.class));
        if(printRequestAttributeSet.get(JobName.class) == null ||
            printRequestAttributeSet.get(Copies.class) == null) {
            throw new RuntimeException("Copies and JobName is not passed correctly");
        }
    }
}

class DummyPrintable implements Printable {
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex == 0) {
            return Printable.PAGE_EXISTS;
        } else {
            return Printable.NO_SUCH_PAGE;
        }
    }
}
