/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;


import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;

import javax.print.*;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.Attribute;

import sun.java2d.*;
import sun.print.*;

public final class CPrinterJob extends RasterPrinterJob {
    // NOTE: This uses RasterPrinterJob as a base, but it doesn't use
    // all of the RasterPrinterJob functions. RasterPrinterJob will
    // break down printing to pieces that aren't necessary under MacOSX
    // printing, such as controlling the # of copies and collating. These
    // are handled by the native printing. RasterPrinterJob is kept for
    // future compatibility and the state keeping that it handles.

    private static String sShouldNotReachHere = "Should not reach here.";

    private volatile SecondaryLoop printingLoop;
    private AtomicReference<Throwable> printErrorRef = new AtomicReference<>();

    private boolean noDefaultPrinter = false;

    private static Font defaultFont;

    private String tray = null;

    // This is the NSPrintInfo for this PrinterJob. Protect multi thread
    //  access to it. It is used by the pageDialog, jobDialog, and printLoop.
    //  This way the state of these items is shared across these calls.
    //  PageFormat data is passed in and set on the fNSPrintInfo on a per call
    //  basis.
    private long fNSPrintInfo = -1;
    private Object fNSPrintInfoLock = new Object();

    static {
        // AWT has to be initialized for the native code to function correctly.
        Toolkit.getDefaultToolkit();
    }

    /**
     * Presents a dialog to the user for changing the properties of
     * the print job.
     * This method will display a native dialog if a native print
     * service is selected, and user choice of printers will be restricted
     * to these native print services.
     * To present the cross platform print dialog for all services,
     * including native ones instead use
     * {@code printDialog(PrintRequestAttributeSet)}.
     * <p>
     * PrinterJob implementations which can use PrintService's will update
     * the PrintService for this PrinterJob to reflect the new service
     * selected by the user.
     * @return {@code true} if the user does not cancel the dialog;
     * {@code false} otherwise.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    @Override
    public boolean printDialog() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        if (noDefaultPrinter) {
            return false;
        }

        if (attributes == null) {
            attributes = new HashPrintRequestAttributeSet();
        }

        if (getPrintService() instanceof StreamPrintService) {
            return super.printDialog(attributes);
        }

        return jobSetup(getPageable(), checkAllowedToPrintToFile());
    }

    /**
     * Displays a dialog that allows modification of a
     * {@code PageFormat} instance.
     * The {@code page} argument is used to initialize controls
     * in the page setup dialog.
     * If the user cancels the dialog then this method returns the
     * original {@code page} object unmodified.
     * If the user okays the dialog then this method returns a new
     * {@code PageFormat} object with the indicated changes.
     * In either case, the original {@code page} object is
     * not modified.
     * @param page the default {@code PageFormat} presented to the
     *            user for modification
     * @return    the original {@code page} object if the dialog
     *            is cancelled; a new {@code PageFormat} object
     *          containing the format indicated by the user if the
     *          dialog is acknowledged.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since     1.2
     */
    @Override
    public PageFormat pageDialog(PageFormat page) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        if (noDefaultPrinter) {
            return page;
        }

        if (getPrintService() instanceof StreamPrintService) {
            return super.pageDialog(page);
        }

        PageFormat pageClone = (PageFormat) page.clone();
        boolean doIt = pageSetup(pageClone, null);
        return doIt ? pageClone : page;
    }

    /**
     * Clones the {@code PageFormat} argument and alters the
     * clone to describe a default page size and orientation.
     * @param page the {@code PageFormat} to be cloned and altered
     * @return clone of {@code page}, altered to describe a default
     *                      {@code PageFormat}.
     */
    @Override
    public PageFormat defaultPage(PageFormat page) {
        PageFormat newPage = (PageFormat)page.clone();
        getDefaultPage(newPage);
        return newPage;
    }

    @Override
    protected void setAttributes(PrintRequestAttributeSet attributes) throws PrinterException {
        super.setAttributes(attributes);

        if (attributes == null) {
            return;
        }
        Attribute attr = attributes.get(Media.class);
        if (attr instanceof CustomMediaTray) {
            CustomMediaTray customTray = (CustomMediaTray) attr;
            tray = customTray.getChoiceName();
        }

        PageRanges pageRangesAttr =  (PageRanges)attributes.get(PageRanges.class);
        if (isSupportedValue(pageRangesAttr, attributes)) {
            SunPageSelection rangeSelect = (SunPageSelection)attributes.get(SunPageSelection.class);
            // If rangeSelect is not null, we are using AWT's print dialog that has
            // All, Selection, and Range radio buttons
            if (rangeSelect == null || rangeSelect == SunPageSelection.RANGE) {
                int[][] range = pageRangesAttr.getMembers();
                // setPageRange will set firstPage and lastPage as called in getFirstPage
                // and getLastPage
                setPageRange(range[0][0] - 1, range[0][1] - 1);
            } else {
                // if rangeSelect is SunPageSelection.ALL
                // then setPageRange appropriately
                setPageRange(-1, -1);
            }
        }
    }

    private void setPageRangeAttribute(int from, int to, boolean isRangeSet) {
        if (attributes != null) {
            // since native Print use zero-based page indices,
            // we need to store in 1-based format in attributes set
            // but setPageRange again uses zero-based indices so it should be
            // 1 less than pageRanges attribute
            if (isRangeSet) {
                attributes.add(new PageRanges(from+1, to+1));
                attributes.add(SunPageSelection.RANGE);
                setPageRange(from, to);
            } else {
                attributes.add(SunPageSelection.ALL);
            }
        }
    }

    private void setCopiesAttribute(int copies) {
        if (attributes != null) {
            attributes.add(new Copies(copies));
            super.setCopies(copies);
        }
    }

    volatile boolean onEventThread;

    @Override
    protected void cancelDoc() throws PrinterAbortException {
        super.cancelDoc();
        if (printingLoop != null) {
            printingLoop.exit();
        }
    }

    private void completePrintLoop() {
        Runnable r = new Runnable() { public void run() {
            synchronized(this) {
                performingPrinting = false;
            }
            if (printingLoop != null) {
                printingLoop.exit();
            }
        }};

        if (onEventThread) {
            try { EventQueue.invokeAndWait(r); } catch (Exception e) { e.printStackTrace(); }
        } else {
            r.run();
        }
    }

    boolean isPrintToFile = false;
    private void setPrintToFile(boolean printToFile) {
        isPrintToFile = printToFile;
    }

    private void setDestinationFile(String dest) {
        if (attributes != null && dest != null) {
            try {
               URI destURI = new URI(dest);
               attributes.add(new Destination(destURI));
               destinationAttr = "" + destURI.getSchemeSpecificPart();
            } catch (Exception e) {
            }
        }
    }

    private String getDestinationFile() {
        return destinationAttr;
    }

    @SuppressWarnings("removal")
    @Override
    public void print(PrintRequestAttributeSet attributes) throws PrinterException {
        // NOTE: Some of this code is copied from RasterPrinterJob.

        // this code uses javax.print APIs
        // this will make it print directly to the printer
        // this will not work if the user clicks on the "Preview" button
        // However if the printer is a StreamPrintService, its the right path.
        PrintService psvc = getPrintService();

        if (psvc == null && !isPrintToFile) {
            throw new PrinterException("No print service found.");
        }

        if (psvc instanceof StreamPrintService) {
            spoolToService(psvc, attributes);
            return;
        }


        setAttributes(attributes);
        // throw exception for invalid destination
        if (destinationAttr != null) {
            validateDestination(destinationAttr);
        }

        /* Get the range of pages we are to print. If the
         * last page to print is unknown, then we print to
         * the end of the document. Note that firstPage
         * and lastPage are 0 based page indices.
         */

        int firstPage = getFirstPage();
        int lastPage = getLastPage();
        if(lastPage == Pageable.UNKNOWN_NUMBER_OF_PAGES) {
            int totalPages = mDocument.getNumberOfPages();
            if (totalPages != Pageable.UNKNOWN_NUMBER_OF_PAGES) {
                lastPage = mDocument.getNumberOfPages() - 1;
            }
        }

        try {
            synchronized (this) {
                performingPrinting = true;
                userCancelled = false;
            }
            printErrorRef.set(null);

            //Add support for PageRange
            PageRanges pr = (attributes == null) ?  null
                                                 : (PageRanges)attributes.get(PageRanges.class);
            int[][] prMembers = (pr == null) ? new int[0][0] : pr.getMembers();
            int loopi = 0;
            do {
                if (EventQueue.isDispatchThread()) {
                    // This is an AWT EventQueue, and this print rendering loop needs to block it.

                    onEventThread = true;

                    printingLoop = AccessController.doPrivileged(new PrivilegedAction<SecondaryLoop>() {
                        @Override
                        public SecondaryLoop run() {
                            return Toolkit.getDefaultToolkit()
                                    .getSystemEventQueue()
                                    .createSecondaryLoop();
                        }
                    });

                    try {
                        // Fire off the print rendering loop on the AppKit thread, and don't have
                        //  it wait and block this thread.
                        if (printLoop(false, firstPage, lastPage)) {
                            // Start a secondary loop on EDT until printing operation is finished or cancelled
                            printingLoop.enter();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
              } else {
                    // Fire off the print rendering loop on the AppKit, and block this thread
                    //  until it is done.
                    // But don't actually block... we need to come back here!
                    onEventThread = false;

                    try {
                        printLoop(true, firstPage, lastPage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (++loopi < prMembers.length) {
                     firstPage = prMembers[loopi][0]-1;
                     lastPage = prMembers[loopi][1] -1;
                }
            }  while (loopi < prMembers.length);
        } finally {
            synchronized (this) {
                // NOTE: Native code shouldn't allow exceptions out while
                // printing. They should cancel the print loop.
                performingPrinting = false;
                notify();
            }
            if (printingLoop != null) {
                printingLoop.exit();
            }

            Throwable printError = printErrorRef.getAndSet(null);
            if (printError != null) {
                if (printError instanceof PrinterException) {
                    throw (PrinterException) printError;
                }
                throw (PrinterException)
                    new PrinterException().initCause(printError);
            }
        }

        // Normalize the collated, # copies, numPages, first/last pages. Need to
        //  make note of pageRangesAttr.

        // Set up NSPrintInfo with the java settings (PageFormat & Paper).

        // Create an NSView for printing. Have knowsPageRange return YES, and give the correct
        //  range, or MAX? if unknown. Have rectForPage do a peekGraphics check before returning
        //  the rectangle. Have drawRect do the real render of the page. Have printJobTitle do
        //  the right thing.

        // Call NSPrintOperation, it will call NSView.drawRect: for each page.

        // NSView.drawRect: will create a CPrinterGraphics with the current CGContextRef, and then
        //  pass this Graphics onto the Printable with the appropriate PageFormat and index.

        // Need to be able to cancel the NSPrintOperation (using code from RasterPrinterJob, be
        //  sure to initialize userCancelled and performingPrinting member variables).

        // Extensions available from AppKit: Print to PDF or EPS file!
    }

    /**
     * Returns the resolution in dots per inch across the width
     * of the page.
     */
    @Override
    protected double getXRes() {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Returns the resolution in dots per inch down the height
     * of the page.
     */
    @Override
    protected double getYRes() {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPrintableX(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPrintableY(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPrintableWidth(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPrintableHeight(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPageWidth(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    @Override
    protected double getPhysicalPageHeight(Paper p) {
        // NOTE: This is not used in the CPrinterJob code path.
        return 0;
    }

    /**
     * Begin a new page. This call's Window's
     * StartPage routine.
     */
    protected void startPage(PageFormat format, Printable painter, int index) throws PrinterException {
        // NOTE: This is not used in the CPrinterJob code path.
        throw new PrinterException(sShouldNotReachHere);
    }

    /**
     * End a page.
     */
    @Override
    protected void endPage(PageFormat format, Printable painter, int index) throws PrinterException {
        // NOTE: This is not used in the CPrinterJob code path.
        throw new PrinterException(sShouldNotReachHere);
    }

    /**
     * Prints the contents of the array of ints, 'data'
     * to the current page. The band is placed at the
     * location (x, y) in device coordinates on the
     * page. The width and height of the band is
     * specified by the caller.
     */
    @Override
    protected void printBand(byte[] data, int x, int y, int width, int height) throws PrinterException {
        // NOTE: This is not used in the CPrinterJob code path.
        throw new PrinterException(sShouldNotReachHere);
    }

    /**
     * Called by the print() method at the start of
     * a print job.
     */
    @Override
    protected void startDoc() throws PrinterException {
        // NOTE: This is not used in the CPrinterJob code path.
        throw new PrinterException(sShouldNotReachHere);
    }

    /**
     * Called by the print() method at the end of
     * a print job.
     */
    @Override
    protected void endDoc() throws PrinterException {
        // NOTE: This is not used in the CPrinterJob code path.
        throw new PrinterException(sShouldNotReachHere);
    }

    /* Called by cancelDoc */
    @Override
    protected native void abortDoc();

    /**
     * Displays the page setup dialog placing the user's
     * settings into 'page'.
     */
    public boolean pageSetup(PageFormat page, Printable painter) {
        CPrinterDialog printerDialog = new CPrinterPageDialog(null, this, page, painter);
        printerDialog.setVisible(true);
        boolean result = printerDialog.getRetVal();
        printerDialog.dispose();
        return result;
    }

    /**
     * Displays the print dialog and records the user's settings
     * into this object. Return false if the user cancels the
     * dialog.
     * If the dialog is to use a set of attributes, useAttributes is true.
     */
    private boolean jobSetup(Pageable doc, boolean allowPrintToFile) {
        CPrinterDialog printerDialog = new CPrinterJobDialog(null, this, doc, allowPrintToFile);
        printerDialog.setVisible(true);
        boolean result = printerDialog.getRetVal();
        printerDialog.dispose();
        return result;
    }

    /**
     * Alters the orientation and Paper to match defaults obtained
     * from a printer.
     */
    private native void getDefaultPage(PageFormat page);

    /**
     * validate the paper size against the current printer.
     */
    @Override
    protected native void validatePaper(Paper origPaper, Paper newPaper );

    // The following methods are CPrinterJob specific.

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() {
        synchronized (fNSPrintInfoLock) {
            if (fNSPrintInfo != -1) {
                dispose(fNSPrintInfo);
            }
            fNSPrintInfo = -1;
        }
    }

    private native long createNSPrintInfo();
    private native void dispose(long printInfo);

    private long getNSPrintInfo() {
        // This is called from the native side.
        synchronized (fNSPrintInfoLock) {
            if (fNSPrintInfo == -1) {
                fNSPrintInfo = createNSPrintInfo();
            }
            return fNSPrintInfo;
        }
    }

    private native boolean printLoop(boolean waitUntilDone, int firstPage, int lastPage) throws PrinterException;

    private PageFormat getPageFormat(int pageIndex) {
        // This is called from the native side.
        PageFormat page;
        try {
            page = getPageable().getPageFormat(pageIndex);
        } catch (Exception e) {
            return null;
        }
        return page;
    }

    private Printable getPrintable(int pageIndex) {
        // This is called from the native side.
        Printable painter;
        try {
            painter = getPageable().getPrintable(pageIndex);
        } catch (Exception e) {
            return null;
        }
        return painter;
    }

    private String getPrinterName(){
        // This is called from the native side.
        PrintService service = getPrintService();
        if (service == null) return null;
        return service.getName();
    }

    private String getPrinterTray() {
        return tray;
    }

    private void setPrinterServiceFromNative(String printerName) {
        // This is called from the native side.
        PrintService[] services = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);

        for (int i = 0; i < services.length; i++) {
            PrintService service = services[i];

            if (printerName.equals(service.getName())) {
                try {
                    setPrintService(service);
                } catch (PrinterException e) {
                    // ignored
                }
                return;
            }
        }
    }

    private Rectangle2D getPageFormatArea(PageFormat page) {
        Rectangle2D.Double pageFormatArea =
            new Rectangle2D.Double(page.getImageableX(),
                    page.getImageableY(),
                    page.getImageableWidth(),
                    page.getImageableHeight());
        return pageFormatArea;
    }

    private boolean cancelCheck() {
        // This is called from the native side.

        // This is used to avoid deadlock
        // We would like to just call if isCancelled(),
        // but that will block the AppKit thread against whomever is holding the synchronized lock
        boolean cancelled = (performingPrinting && userCancelled);
        if (cancelled) {
            try {
                LWCToolkit.invokeLater(new Runnable() { public void run() {
                    try {
                    cancelDoc();
                    } catch (PrinterAbortException pae) {
                        // no-op, let the native side handle it
                    }
                }}, null);
            } catch (java.lang.reflect.InvocationTargetException ite) {}
        }
        return cancelled;
    }

    private PeekGraphics createFirstPassGraphics(PrinterJob printerJob, PageFormat page) {
        // This is called from the native side.
        BufferedImage bimg = new BufferedImage((int)Math.round(page.getWidth()), (int)Math.round(page.getHeight()), BufferedImage.TYPE_INT_ARGB_PRE);
        PeekGraphics peekGraphics = createPeekGraphics(bimg.createGraphics(), printerJob);
        Rectangle2D pageFormatArea = getPageFormatArea(page);
        initPrinterGraphics(peekGraphics, pageFormatArea);
        return peekGraphics;
    }

    private void printToPathGraphics(    final PeekGraphics graphics, // Always an actual PeekGraphics
                                        final PrinterJob printerJob, // Always an actual CPrinterJob
                                        final Printable painter, // Client class
                                        final PageFormat page, // Client class
                                        final int pageIndex,
                                        final long context) throws PrinterException {
        // This is called from the native side.
        Runnable r = new Runnable() { public void run() {
            try {
                SurfaceData sd = CPrinterSurfaceData.createData(page, context); // Just stores page into an ivar
                if (defaultFont == null) {
                    defaultFont = new Font("Dialog", Font.PLAIN, 12);
                }
                Graphics2D delegate = new SunGraphics2D(sd, Color.black, Color.white, defaultFont);

                Graphics2D pathGraphics = new CPrinterGraphics(delegate, printerJob); // Just stores delegate into an ivar
                Rectangle2D pageFormatArea = getPageFormatArea(page);
                initPrinterGraphics(pathGraphics, pageFormatArea);
                painter.print(pathGraphics, page, pageIndex);
                delegate.dispose();
                delegate = null;
        } catch (PrinterException pe) { throw new java.lang.reflect.UndeclaredThrowableException(pe); }
        }};

        if (onEventThread) {
            try { EventQueue.invokeAndWait(r);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable te = ite.getTargetException();
                if (te instanceof PrinterException) throw (PrinterException)te;
                else te.printStackTrace();
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            r.run();
        }

    }

    // Returns either 1. an array of 3 object (PageFormat, Printable, PeekGraphics) or 2. null
    private Object[] getPageformatPrintablePeekgraphics(final int pageIndex) {
        final Object[] ret = new Object[3];
        final PrinterJob printerJob = this;

        Runnable r = new Runnable() { public void run() { synchronized(ret) {
            try {
                Pageable pageable = getPageable();
                PageFormat pageFormat = pageable.getPageFormat(pageIndex);
                if (pageFormat != null) {
                    Printable printable = pageable.getPrintable(pageIndex);
                    if (printable != null) {
                        BufferedImage bimg =
                              new BufferedImage(
                                  (int)Math.round(pageFormat.getWidth()),
                                  (int)Math.round(pageFormat.getHeight()),
                                  BufferedImage.TYPE_INT_ARGB_PRE);
                        PeekGraphics peekGraphics =
                         createPeekGraphics(bimg.createGraphics(), printerJob);
                        Rectangle2D pageFormatArea =
                             getPageFormatArea(pageFormat);
                        initPrinterGraphics(peekGraphics, pageFormatArea);

                        // Do the assignment here!
                        ret[0] = pageFormat;
                        ret[1] = printable;
                        ret[2] = peekGraphics;
                    }
                }
            } catch (Exception e) {} // Original code bailed on any exception
        }}};

        if (onEventThread) {
            try { EventQueue.invokeAndWait(r); } catch (Exception e) { e.printStackTrace(); }
        } else {
            r.run();
        }

        synchronized(ret) {
            if (ret[2] != null)
                return ret;
            return null;
        }
    }

    private Rectangle2D printAndGetPageFormatArea(final Printable printable, final Graphics graphics, final PageFormat pageFormat, final int pageIndex) {
        final Rectangle2D[] ret = new Rectangle2D[1];

        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (ret) {
                    try {
                        int pageResult = printable.print(
                            graphics, pageFormat, pageIndex);
                        if (pageResult != Printable.NO_SUCH_PAGE) {
                            ret[0] = getPageFormatArea(pageFormat);
                        }
                    } catch (Throwable t) {
                        printErrorRef.compareAndSet(null, t);
                    }
                }
            }
        };

        if (onEventThread) {
            try {
                EventQueue.invokeAndWait(r);
            } catch (Throwable t) {
                printErrorRef.compareAndSet(null, t);
            }
        } else {
            r.run();
        }

        synchronized (ret) {
            return ret[0];
        }
    }

    // upcall from native
    private static void detachPrintLoop(final long target, final long arg) {
        new Thread(null, () -> _safePrintLoop(target, arg),
                   "PrintLoop", 0, false).start();
    }
    private static native void _safePrintLoop(long target, long arg);

    @Override
    protected void startPage(PageFormat arg0, Printable arg1, int arg2, boolean arg3) throws PrinterException {
        // TODO Auto-generated method stub
    }

    @Override
    protected MediaSize getMediaSize(Media media, PrintService service,
            PageFormat page) {
        if (media == null || !(media instanceof MediaSizeName)) {
            return getDefaultMediaSize(page);
        }
        MediaSize size = MediaSize.getMediaSizeForName((MediaSizeName) media);
        return size != null ? size : getDefaultMediaSize(page);
    }

    private MediaSize getDefaultMediaSize(PageFormat page){
            final int inch = 72;
            Paper paper = page.getPaper();
            float width = (float) (paper.getWidth() / inch);
            float height = (float) (paper.getHeight() / inch);
            return new MediaSize(width, height, MediaSize.INCH);
    }

    @Override
    protected MediaPrintableArea getDefaultPrintableArea(PageFormat page, double w, double h) {
        final float dpi = 72.0f;
        Paper paper = page.getPaper();
        return new MediaPrintableArea(
                (float) (paper.getImageableX() / dpi),
                (float) (paper.getImageableY() / dpi),
                (float) (paper.getImageableWidth() / dpi),
                (float) (paper.getImageableHeight() / dpi),
                MediaPrintableArea.INCH);
    }
}
