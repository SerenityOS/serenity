/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.BasicStroke;
import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FileDialog;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import java.awt.print.Pageable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import javax.print.PrintService;

import java.io.File;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.print.PeekGraphics;
import sun.print.PeekMetrics;

import java.net.URI;
import java.net.URISyntaxException;

import javax.print.PrintServiceLookup;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.Attribute;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.PageRanges;

import sun.awt.Win32FontManager;

import sun.print.RasterPrinterJob;
import sun.print.SunAlternateMedia;
import sun.print.SunPageSelection;
import sun.print.Win32MediaTray;
import sun.print.Win32PrintService;
import sun.print.PrintServiceLookupProvider;
import sun.print.ServiceDialog;

import java.awt.Frame;
import java.io.FilePermission;

import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;
import sun.java2d.DisposerTarget;

/**
 * A class which initiates and executes a Win32 printer job.
 *
 * @author Richard Blanchard
 */
public final class WPrinterJob extends RasterPrinterJob
        implements DisposerTarget {

 /* Class Constants */


/* Instance Variables */

    /**
     * These are Windows' ExtCreatePen End Cap Styles
     * and must match the values in <WINGDI.h>
     */
    protected static final long PS_ENDCAP_ROUND  = 0x00000000;
    protected static final long PS_ENDCAP_SQUARE   = 0x00000100;
    protected static final long PS_ENDCAP_FLAT   =   0x00000200;

    /**
     * These are Windows' ExtCreatePen Line Join Styles
     * and must match the values in <WINGDI.h>
     */
    protected static final long PS_JOIN_ROUND   =    0x00000000;
    protected static final long PS_JOIN_BEVEL   =    0x00001000;
    protected static final long PS_JOIN_MITER   =    0x00002000;

    /**
     * This is the Window's Polygon fill rule which
     * Selects alternate mode (fills the area between odd-numbered
     * and even-numbered polygon sides on each scan line).
     * It must match the value in <WINGDI.h> It can be passed
     * to setPolyFillMode().
     */
    protected static final int POLYFILL_ALTERNATE = 1;

    /**
     * This is the Window's Polygon fill rule which
     * Selects winding mode which fills any region
     * with a nonzero winding value). It must match
     * the value in <WINGDI.h> It can be passed
     * to setPolyFillMode().
     */
    protected static final int POLYFILL_WINDING = 2;

    /**
     * The maximum value for a Window's color component
     * as passed to selectSolidBrush.
     */
    private static final int MAX_WCOLOR = 255;

    /**
     * Flags for setting values from devmode in native code.
     * Values must match those defined in awt_PrintControl.cpp
     */
    private static final int SET_DUP_VERTICAL = 0x00000010;
    private static final int SET_DUP_HORIZONTAL = 0x00000020;
    private static final int SET_RES_HIGH = 0x00000040;
    private static final int SET_RES_LOW = 0x00000080;
    private static final int SET_COLOR = 0x00000200;
    private static final int SET_ORIENTATION = 0x00004000;
    private static final int SET_COLLATED    = 0x00008000;

    /**
     * Values must match those defined in wingdi.h & commdlg.h
     */
    private static final int PD_COLLATE = 0x00000010;
    private static final int PD_PRINTTOFILE = 0x00000020;
    private static final int DM_ORIENTATION   = 0x00000001;
    private static final int DM_PAPERSIZE     = 0x00000002;
    private static final int DM_COPIES        = 0x00000100;
    private static final int DM_DEFAULTSOURCE = 0x00000200;
    private static final int DM_PRINTQUALITY  = 0x00000400;
    private static final int DM_COLOR         = 0x00000800;
    private static final int DM_DUPLEX        = 0x00001000;
    private static final int DM_YRESOLUTION   = 0x00002000;
    private static final int DM_COLLATE       = 0x00008000;

    private static final short DMCOLLATE_FALSE  = 0;
    private static final short DMCOLLATE_TRUE   = 1;

    private static final short DMORIENT_PORTRAIT  = 1;
    private static final short DMORIENT_LANDSCAPE = 2;

    private static final short DMCOLOR_MONOCHROME = 1;
    private static final short DMCOLOR_COLOR      = 2;

    private static final short DMRES_DRAFT  = -1;
    private static final short DMRES_LOW    = -2;
    private static final short DMRES_MEDIUM = -3;
    private static final short DMRES_HIGH   = -4;

    private static final short DMDUP_SIMPLEX    = 1;
    private static final short DMDUP_VERTICAL   = 2;
    private static final short DMDUP_HORIZONTAL = 3;

    /**
     * Pageable MAX pages
     */
    private static final int MAX_UNKNOWN_PAGES = 9999;


    /* Collation and copy flags.
     * The Windows PRINTDLG struct has a nCopies field which on return
     * indicates how many copies of a print job an application must render.
     * There is also a PD_COLLATE member of the flags field which if
     * set on return indicates the application generated copies should be
     * collated.
     * Windows printer drivers typically - but not always - support
     * generating multiple copies themselves, but uncollated is more
     * universal than collated copies.
     * When they do, they read the initial values from the PRINTDLG structure
     * and set them into the driver's DEVMODE structure and intialise
     * the printer DC based on that, so that when printed those settings
     * will be used.
     * For drivers supporting both these capabilities via DEVMODE, then on
     * return from the Print Dialog, nCopies is set to 1 and the PD_COLLATE is
     * cleared, so that the application will only render 1 copy and the
     * driver takes care of the rest.
     *
     * Applications which want to know what's going on have to be DEVMODE
     * savvy and peek at that.
     * DM_COPIES flag indicates support for multiple driver copies
     * and dmCopies is the number of copies the driver will print
     * DM_COLLATE flag indicates support for collated driver copies and
     * dmCollate == DMCOLLATE_TRUE indicates the option is in effect.
     *
     * Multiple copies from Java applications:
     * We provide API to get & set the number of copies as well as allowing the
     * user to choose it, so we need to be savvy about DEVMODE, so that
     * we can accurately report back the number of copies selected by
     * the user, as well as make use of the driver to render multiple copies.
     *
     * Collation and Java applications:
     * We presently provide no API for specifying collation, but its
     * present on the Windows Print Dialog, and when a user checks it
     * they expect it to be obeyed.
     * The best thing to do is to detect exactly the cases where the
     * driver doesn't support this and render multiple copies ourselves.
     * To support all this we need several flags which signal the
     * printer's capabilities and the user's requests.
     * Its questionable if we (yet) need to make a distinction between
     * the user requesting collation and the driver supporting it.
     * Since for now we only need to know whether we need to render the
     * copies. However it allows the logic to be clearer.
     * These fields are changed by native code which detects the driver's
     * capabilities and the user's choices.
     */

    //initialize to false because the Flags that we initialized in PRINTDLG
    // tells GDI that we can handle our own collation and multiple copies
     private boolean driverDoesMultipleCopies = false;
     private boolean driverDoesCollation = false;
     private boolean userRequestedCollation = false;
     private boolean noDefaultPrinter = false;

    /* The HandleRecord holds the native resources that need to be freed
     * when this WPrinterJob is GC'd.
     */
    static class HandleRecord implements DisposerRecord {
        /**
         * The Windows device context we will print into.
         * This variable is set after the Print dialog
         * is okayed by the user. If the user cancels
         * the print dialog, then this variable is 0.
         * Much of the configuration information for a printer is
         * obtained through printer device specific handles.
         * We need to associate these with, and free with, the mPrintDC.
         */
        private long mPrintDC;
        private long mPrintHDevMode;
        private long mPrintHDevNames;

        @Override
        public void dispose() {
            WPrinterJob.deleteDC(mPrintDC, mPrintHDevMode, mPrintHDevNames);
        }
    }

    private HandleRecord handleRecord = new HandleRecord();

    private int mPrintPaperSize;

    /* These fields are directly set in upcalls from the values
     * obtained from calling DeviceCapabilities()
     */
    private int mPrintXRes;   // pixels per inch in x direction

    private int mPrintYRes;   // pixels per inch in y direction

    private int mPrintPhysX;  // x offset in pixels of printable area

    private int mPrintPhysY;  // y offset in pixels of printable area

    private int mPrintWidth;  // width in pixels of printable area

    private int mPrintHeight; // height in pixels of printable area

    private int mPageWidth;   // width in pixels of entire page

    private int mPageHeight;  // height in pixels of entire page

    /* The values of the following variables are pulled directly
     * into native code (even bypassing getter methods) when starting a doc.
     * So these need to be synced up from any resulting native changes
     * by a user dialog.
     * But the native changes call up to into the attributeset, and that
     * should be sufficient, since before heading down to native either
     * to (re-)display a dialog, or to print the doc, these are all
     * re-populated from the AttributeSet,
     * Nonetheless having them in sync with the attributeset and native
     * state is probably safer.
     * Also whereas the startDoc native code pulls the variables directly,
     * the dialog code does use getter to pull some of these values.
     * That's an inconsistency we should fix if it causes problems.
     */
    private int mAttSides;
    private int mAttChromaticity;
    private int mAttXRes;
    private int mAttYRes;
    private int mAttQuality;
    private int mAttCollate;
    private int mAttCopies;
    private int mAttMediaSizeName;
    private int mAttMediaTray;

    private String mDestination = null;

    /**
     * The last color set into the print device context or
     * {@code null} if no color has been set.
     */
    private Color mLastColor;

    /**
     * The last text color set into the print device context or
     * {@code null} if no color has been set.
     */
    private Color mLastTextColor;

    /**
     * Define the most recent java font set as a GDI font in the printer
     * device context. mLastFontFamily will be NULL if no
     * GDI font has been set.
     */
    private String mLastFontFamily;
    private float mLastFontSize;
    private int mLastFontStyle;
    private int mLastRotation;
    private float mLastAwScale;

    // for AwtPrintControl::InitPrintDialog
    private PrinterJob pjob;

    private java.awt.peer.ComponentPeer dialogOwnerPeer = null;

    private int graphicsMode;
    private double[] worldTransform = new double[6];

 /* Static Initializations */

    static {
        // AWT has to be initialized for the native code to function correctly.
        Toolkit.getDefaultToolkit();

        initIDs();

        Win32FontManager.registerJREFontsForPrinting();
    }

    /* Constructors */

    public WPrinterJob()
    {
        Disposer.addRecord(disposerReferent,
                           handleRecord = new HandleRecord());
        initAttributeMembers();
    }

    /* Implement DisposerTarget. Weak references to an Object can delay
     * its storage reclaimation marginally.
     * It won't make the native resources be release any more quickly, but
     * by pointing the reference held by Disposer at an object which becomes
     * no longer strongly reachable when this WPrinterJob is no longer
     * strongly reachable, we allow the WPrinterJob to be freed more promptly
     * than if it were the referenced object.
     */
    private Object disposerReferent = new Object();

    @Override
    public Object getDisposerReferent() {
        return disposerReferent;
    }

/* Instance Methods */

    /**
     * Display a dialog to the user allowing the modification of a
     * PageFormat instance.
     * The {@code page} argument is used to initialize controls
     * in the page setup dialog.
     * If the user cancels the dialog, then the method returns the
     * original {@code page} object unmodified.
     * If the user okays the dialog then the method returns a new
     * PageFormat object with the indicated changes.
     * In either case the original {@code page} object will
     * not be modified.
     * @param     page    the default PageFormat presented to the user
     *                    for modification
     * @return    the original {@code page} object if the dialog
     *            is cancelled, or a new PageFormat object containing
     *            the format indicated by the user if the dialog is
     *            acknowledged
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

        if (!(getPrintService() instanceof Win32PrintService)) {
            return super.pageDialog(page);
        }

        PageFormat pageClone = (PageFormat) page.clone();
        boolean result = false;

        /*
         * Fix for 4507585: show the native modal dialog the same way printDialog() does so
         * that it won't block event dispatching when called on EventDispatchThread.
         */
        WPageDialog dialog = new WPageDialog((Frame)null, this,
                                     pageClone, null);
        dialog.setRetVal(false);
        dialog.setVisible(true);
        result = dialog.getRetVal();
        dialog.dispose();

        // myService => current PrintService
        if (result && (myService != null)) {
            // It's possible that current printer is changed through
            // the "Printer..." button so we query again from native.
            String printerName = getNativePrintService();
            if (!myService.getName().equals(printerName)) {
                // native printer is different !
                // we update the current PrintService
                try {
                    setPrintService(PrintServiceLookupProvider.
                                    getWin32PrintLUS().
                                    getPrintServiceByName(printerName));
                } catch (PrinterException e) {
                }
            }
            // Update attributes, this will preserve the page settings.
            //  - same code as in RasterPrinterJob.java
            updatePageAttributes(myService, pageClone);

            return pageClone;
        } else {
            return page;
        }
    }


    private boolean displayNativeDialog() {
        // "attributes" is required for getting the updated attributes
        if (attributes == null) {
            return false;
        }

        DialogOwner dlgOwner = (DialogOwner)attributes.get(DialogOwner.class);
        Window owner = (dlgOwner != null) ? dlgOwner.getOwner() : null;

        WPrintDialog dialog =  (owner instanceof Frame) ?
                new WPrintDialog((Frame)owner, this) :
                new WPrintDialog((Dialog)owner, this);

        dialog.setRetVal(false);
        dialog.setVisible(true);
        boolean prv = dialog.getRetVal();
        dialog.dispose();

        Destination dest =
                (Destination)attributes.get(Destination.class);
        if ((dest == null) || !prv){
                return prv;
        } else {
            String title = null;
            String strBundle = "sun.print.resources.serviceui";
            ResourceBundle rb = ResourceBundle.getBundle(strBundle);
            try {
                title = rb.getString("dialog.printtofile");
            } catch (MissingResourceException e) {
            }
            FileDialog fileDialog = (owner instanceof Frame) ?
                    new FileDialog((Frame)owner, title, FileDialog.SAVE) :
                    new FileDialog((Dialog)owner, title, FileDialog.SAVE);

            URI destURI = dest.getURI();
            // Old code destURI.getPath() would return null for "file:out.prn"
            // so we use getSchemeSpecificPart instead.
            String pathName = (destURI != null) ?
                destURI.getSchemeSpecificPart() : null;
            if (pathName != null) {
               File file = new File(pathName);
               fileDialog.setFile(file.getName());
               File parent = file.getParentFile();
               if (parent != null) {
                   fileDialog.setDirectory(parent.getPath());
               }
            } else {
                fileDialog.setFile("out.prn");
            }

            fileDialog.setVisible(true);
            String fileName = fileDialog.getFile();
            if (fileName == null) {
                fileDialog.dispose();
                return false;
            }
            String fullName = fileDialog.getDirectory() + fileName;
            File f = new File(fullName);
            File pFile = f.getParentFile();
            while ((f.exists() &&
                      (!f.isFile() || !f.canWrite())) ||
                   ((pFile != null) &&
                      (!pFile.exists() || (pFile.exists() && !pFile.canWrite())))) {

                if (owner instanceof Frame) {
                    (new PrintToFileErrorDialog((Frame)owner,
                                ServiceDialog.getMsg("dialog.owtitle"),
                                ServiceDialog.getMsg("dialog.writeerror")+" "+fullName,
                                ServiceDialog.getMsg("button.ok"))).setVisible(true);
                } else {
                    (new PrintToFileErrorDialog((Dialog)owner,
                                ServiceDialog.getMsg("dialog.owtitle"),
                                ServiceDialog.getMsg("dialog.writeerror")+" "+fullName,
                                ServiceDialog.getMsg("button.ok"))).setVisible(true);
                }

                fileDialog.setVisible(true);
                fileName = fileDialog.getFile();
                if (fileName == null) {
                    fileDialog.dispose();
                    return false;
                }
                fullName = fileDialog.getDirectory() + fileName;
                f = new File(fullName);
                pFile = f.getParentFile();
            }
            fileDialog.dispose();
            attributes.add(new Destination(f.toURI()));
            return true;
        }

    }

    /**
     * Presents the user a dialog for changing properties of the
     * print job interactively.
     * @return false if the user cancels the dialog and
     *         true otherwise.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    @Override
    public boolean printDialog() throws HeadlessException {

        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        // current request attribute set should be reflected to the print dialog.
        // If null, create new instance of HashPrintRequestAttributeSet.
        if (attributes == null) {
            attributes = new HashPrintRequestAttributeSet();
        }

        if (!(getPrintService() instanceof Win32PrintService)) {
            return super.printDialog(attributes);
        }

        if (noDefaultPrinter == true) {
            return false;
        } else {
            return displayNativeDialog();
        }
    }

     /**
     * Associate this PrinterJob with a new PrintService.
     *
     * Throws {@code PrinterException} if the specified service
     * cannot support the {@code Pageable} and
     * </code>Printable</code> interfaces necessary to support 2D printing.
     * @param service print service which supports 2D printing.
     *
     * @throws PrinterException if the specified service does not support
     * 2D printing.
     */
    @Override
    public void setPrintService(PrintService service)
        throws PrinterException {
        super.setPrintService(service);
        if (!(service instanceof Win32PrintService)) {
            return;
        }
        driverDoesMultipleCopies = false;
        driverDoesCollation = false;
        setNativePrintServiceIfNeeded(service.getName());
    }

    /* associates this job with the specified native service */
    private native void setNativePrintService(String name)
        throws PrinterException;

    private String lastNativeService = null;
    private void setNativePrintServiceIfNeeded(String name)
        throws PrinterException {

        if (name != null && !(name.equals(lastNativeService))) {
            setNativePrintService(name);
            lastNativeService = name;
        }
    }

    @Override
    public PrintService getPrintService() {
        if (myService == null) {
            String printerName = getNativePrintService();

            if (printerName != null) {
                myService = PrintServiceLookupProvider.getWin32PrintLUS().
                    getPrintServiceByName(printerName);
                // no need to call setNativePrintService as this name is
                // currently set in native
                if (myService != null) {
                    return myService;
                }
            }

            myService = PrintServiceLookup.lookupDefaultPrintService();
            if (myService instanceof Win32PrintService) {
                try {
                    setNativePrintServiceIfNeeded(myService.getName());
                } catch (Exception e) {
                    myService = null;
                }
            }

          }
          return myService;
    }

    private native String getNativePrintService();

    private void initAttributeMembers() {
            mAttSides = 0;
            mAttChromaticity = 0;
            mAttXRes = 0;
            mAttYRes = 0;
            mAttQuality = 0;
            mAttCollate = -1;
            mAttCopies = 0;
            mAttMediaTray = 0;
            mAttMediaSizeName = 0;
            mDestination = null;

    }

    /**
     * copy the attributes to the native print job
     * Note that this method, and hence the re-initialisation
     * of the GDI values is done on each entry to the print dialog since
     * an app could redisplay the print dialog for the same job and
     * 1) the application may have changed attribute settings
     * 2) the application may have changed the printer.
     * In the event that the user changes the printer using the
      dialog, then it is up to GDI to report back all changed values.
     */
    @Override
    protected void setAttributes(PrintRequestAttributeSet attributes)
        throws PrinterException {

        // initialize attribute values
        initAttributeMembers();
        super.setAttributes(attributes);

        mAttCopies = getCopiesInt();
        mDestination = destinationAttr;

        if (attributes == null) {
            return; // now always use attributes, so this shouldn't happen.
        }
        Attribute[] attrs = attributes.toArray();
        for (int i=0; i< attrs.length; i++) {
            Attribute attr = attrs[i];
            try {
                 if (attr.getCategory() == Sides.class) {
                    setSidesAttrib(attr);
                }
                else if (attr.getCategory() == Chromaticity.class) {
                    setColorAttrib(attr);
                }
                else if (attr.getCategory() == PrinterResolution.class) {
                    if (myService.isAttributeValueSupported(attr, null, null)) {
                        setResolutionAttrib(attr);
                    }
                }
                else if (attr.getCategory() == PrintQuality.class) {
                    setQualityAttrib(attr);
                }
                else if (attr.getCategory() == SheetCollate.class) {
                    setCollateAttrib(attr);
                }  else if (attr.getCategory() == Media.class ||
                            attr.getCategory() == SunAlternateMedia.class) {
                    /* SunAlternateMedia is used if its a tray, and
                     * any Media that is specified is not a tray.
                     */
                    if (attr.getCategory() == SunAlternateMedia.class) {
                        Media media = (Media)attributes.get(Media.class);
                        if (media == null ||
                            !(media instanceof MediaTray)) {
                            attr = ((SunAlternateMedia)attr).getMedia();
                        }
                    }
                    if (attr instanceof MediaSizeName) {
                        setWin32MediaAttrib(attr);
                    }
                    if (attr instanceof MediaTray) {
                        setMediaTrayAttrib(attr);
                    }
                }

            } catch (ClassCastException e) {
            }
        }
    }

    /**
     * Alters the orientation and Paper to match defaults obtained
     * from a printer.
     */
    private native void getDefaultPage(PageFormat page);

    /**
     * The passed in PageFormat will be copied and altered to describe
     * the default page size and orientation of the PrinterJob's
     * current printer.
     * Note: PageFormat.getPaper() returns a clone and getDefaultPage()
     * gets that clone so it won't overwrite the original paper.
     */
    @Override
    public PageFormat defaultPage(PageFormat page) {
        PageFormat newPage = (PageFormat)page.clone();
        getDefaultPage(newPage);
        return newPage;
    }

    /**
     * validate the paper size against the current printer.
     */
    @Override
    protected native void validatePaper(Paper origPaper, Paper newPaper );

    /**
     * Examine the metrics captured by the
     * {@code PeekGraphics} instance and
     * if capable of directly converting this
     * print job to the printer's control language
     * or the native OS's graphics primitives, then
     * return a {@code PathGraphics} to perform
     * that conversion. If there is not an object
     * capable of the conversion then return
     * {@code null}. Returning {@code null}
     * causes the print job to be rasterized.
     */

    @Override
    protected Graphics2D createPathGraphics(PeekGraphics peekGraphics,
                                            PrinterJob printerJob,
                                            Printable painter,
                                            PageFormat pageFormat,
                                            int pageIndex) {

        WPathGraphics pathGraphics;
        PeekMetrics metrics = peekGraphics.getMetrics();

        /* If the application has drawn anything that
         * out PathGraphics class can not handle then
         * return a null PathGraphics. If the property
         * to force the raster pipeline has been set then
         * we also want to avoid the path (pdl) pipeline
         * and return null.
         */
       if (forcePDL == false && (forceRaster == true
                                  || metrics.hasNonSolidColors()
                                  || metrics.hasCompositing()
                                  )) {
            pathGraphics = null;
        } else {
            BufferedImage bufferedImage = new BufferedImage(8, 8,
                                            BufferedImage.TYPE_INT_RGB);
            Graphics2D bufferedGraphics = bufferedImage.createGraphics();

            boolean canRedraw = peekGraphics.getAWTDrawingOnly() == false;
            pathGraphics =  new WPathGraphics(bufferedGraphics, printerJob,
                                              painter, pageFormat, pageIndex,
                                              canRedraw);
        }

        return pathGraphics;
    }


    @Override
    protected double getXRes() {
        if (mAttXRes != 0) {
            return mAttXRes;
        } else {
            return mPrintXRes;
        }
    }

    @Override
    protected double getYRes() {
        if (mAttYRes != 0) {
            return mAttYRes;
        } else {
            return mPrintYRes;
        }
    }

    @Override
    protected double getPhysicalPrintableX(Paper p) {
        return mPrintPhysX;
    }

    @Override
    protected double getPhysicalPrintableY(Paper p) {
        return mPrintPhysY;
    }

    @Override
    protected double getPhysicalPrintableWidth(Paper p) {
        return mPrintWidth;
    }

    @Override
    protected double getPhysicalPrintableHeight(Paper p) {
        return mPrintHeight;
    }

    @Override
    protected double getPhysicalPageWidth(Paper p) {
        return mPageWidth;
    }

    @Override
    protected double getPhysicalPageHeight(Paper p) {
        return mPageHeight;
    }

    /**
     * We don't (yet) provide API to support collation, and
     * when we do the logic here will require adjustment, but
     * this method is currently necessary to honour user-originated
     * collation requests - which can only originate from the print dialog.
     * REMIND: check if this can be deleted already.
     */
    @Override
    protected boolean isCollated() {
        return userRequestedCollation;
    }

    /**
     * Returns how many times the entire book should
     * be printed by the PrintJob. If the printer
     * itself supports collation then this method
     * should return 1 indicating that the entire
     * book need only be printed once and the copies
     * will be collated and made in the printer.
     */
    @Override
    protected int getCollatedCopies() {
        debug_println("driverDoesMultipleCopies="+driverDoesMultipleCopies
                      +" driverDoesCollation="+driverDoesCollation);
        if  (super.isCollated() && !driverDoesCollation) {
            // we will do our own collation so we need to
            // tell the printer to not collate
            mAttCollate = 0;
            mAttCopies = 1;
            return getCopies();
        }

        return 1;
    }

    /**
     * Returns how many times each page in the book
     * should be consecutively printed by PrinterJob.
     * If the underlying Window's driver will
     * generate the copies, rather than having RasterPrinterJob
     * iterate over the number of copies, this method always returns
     * 1.
     */
    @Override
    protected int getNoncollatedCopies() {
        if (driverDoesMultipleCopies || super.isCollated()) {
            return 1;
        } else {
            return getCopies();
        }
    }

    /* These getter/setters are called from native code */

    /**
     * Return the Window's device context that we are printing
     * into.
     */
    private long getPrintDC() {
        return handleRecord.mPrintDC;
    }

    private void setPrintDC(long mPrintDC) {
        handleRecord.mPrintDC = mPrintDC;
    }

    private long getDevMode() {
        return handleRecord.mPrintHDevMode;
    }

    private void setDevMode(long mPrintHDevMode) {
        handleRecord.mPrintHDevMode = mPrintHDevMode;
    }

    private long getDevNames() {
        return handleRecord.mPrintHDevNames;
    }

    private void setDevNames(long mPrintHDevNames) {
        handleRecord.mPrintHDevNames = mPrintHDevNames;
    }

    protected void beginPath() {
        beginPath(getPrintDC());
    }

    protected void endPath() {
        endPath(getPrintDC());
    }

    protected void scaleTransform(float scale) {
        graphicsMode = setAdvancedGraphicsMode();
        getWorldTransform(worldTransform);
        scale(scale, scale);
    }

    protected void restoreTransform() {
        setWorldTransform(worldTransform);
        setGraphicsMode(graphicsMode);
    }

    protected void closeFigure() {
        closeFigure(getPrintDC());
    }

    protected void fillPath() {
        fillPath(getPrintDC());
    }

    protected void moveTo(float x, float y) {
        moveTo(getPrintDC(), x, y);
    }

    protected void lineTo(float x, float y) {
        lineTo(getPrintDC(), x, y);
    }

    protected void polyBezierTo(float control1x, float control1y,
                                float control2x, float control2y,
                                float endX, float endY) {

        polyBezierTo(getPrintDC(),  control1x, control1y,
                               control2x, control2y,
                               endX, endY);
    }

    /**
     * Set the current polgon fill rule into the printer device context.
     * The {@code fillRule} should
     * be one of the following Windows constants:
     * {@code ALTERNATE} or {@code WINDING}.
     */
    protected void setPolyFillMode(int fillRule) {
        setPolyFillMode(getPrintDC(), fillRule);
    }

    /**
     * Set the GDI graphics mode to {@code GM_ADVANCED}.
     */
    private int setAdvancedGraphicsMode() {
        return setAdvancedGraphicsMode(getPrintDC());
    }

    /**
     * Set the GDI graphics mode.
     * The {@code mode} should
     * be one of the following Windows constants:
     * {@code GM_COMPATIBLE} or {@code GM_ADVANCED}.
     */
    private void setGraphicsMode(int mode) {
        setGraphicsMode(getPrintDC(), mode);
    }

    /**
     * Scale the GDI World Transform.
     */
    private void scale(double scaleX, double scaleY) {
        scale(getPrintDC(), scaleX, scaleY);
    }

    /**
     * Get the GDI World Transform.
     */
    private void getWorldTransform(double[] transform) {
        getWorldTransform(getPrintDC(), transform);
    }

    /**
     * Set the GDI World Transform.
     */
    private void setWorldTransform(double[] transform) {
        setWorldTransform(getPrintDC(), transform);
    }

    /*
     * Create a Window's solid brush for the color specified
     * by {@code (red, green, blue)}. Once the brush
     * is created, select it in the current printing device
     * context and free the old brush.
     */
    protected void selectSolidBrush(Color color) {

        /* We only need to select a brush if the color has changed.
        */
        if (color.equals(mLastColor) == false) {
            mLastColor = color;
            float[] rgb = color.getRGBColorComponents(null);

            selectSolidBrush(getPrintDC(),
                             (int) (rgb[0] * MAX_WCOLOR),
                             (int) (rgb[1] * MAX_WCOLOR),
                             (int) (rgb[2] * MAX_WCOLOR));
        }
    }

    /**
     * Return the x coordinate of the current pen
     * position in the print device context.
     */
    protected int getPenX() {

        return getPenX(getPrintDC());
    }


    /**
     * Return the y coordinate of the current pen
     * position in the print device context.
     */
    protected int getPenY() {

        return getPenY(getPrintDC());
    }

    /**
     * Set the current path in the printer device's
     * context to be clipping path.
     */
    protected void selectClipPath() {
        selectClipPath(getPrintDC());
    }


    protected void frameRect(float x, float y, float width, float height) {
        frameRect(getPrintDC(), x, y, width, height);
    }

    protected void fillRect(float x, float y, float width, float height,
                            Color color) {
        float[] rgb = color.getRGBColorComponents(null);

        fillRect(getPrintDC(), x, y, width, height,
                 (int) (rgb[0] * MAX_WCOLOR),
                 (int) (rgb[1] * MAX_WCOLOR),
                 (int) (rgb[2] * MAX_WCOLOR));
    }


    protected void selectPen(float width, Color color) {

        float[] rgb = color.getRGBColorComponents(null);

        selectPen(getPrintDC(), width,
                  (int) (rgb[0] * MAX_WCOLOR),
                  (int) (rgb[1] * MAX_WCOLOR),
                  (int) (rgb[2] * MAX_WCOLOR));
    }


    protected boolean selectStylePen(int cap, int join, float width,
                                     Color color) {

        long endCap;
        long lineJoin;

        float[] rgb = color.getRGBColorComponents(null);

        switch(cap) {
        case BasicStroke.CAP_BUTT: endCap = PS_ENDCAP_FLAT; break;
        case BasicStroke.CAP_ROUND: endCap = PS_ENDCAP_ROUND; break;
        default:
        case BasicStroke.CAP_SQUARE: endCap = PS_ENDCAP_SQUARE; break;
        }

        switch(join) {
        case BasicStroke.JOIN_BEVEL:lineJoin = PS_JOIN_BEVEL; break;
        default:
        case BasicStroke.JOIN_MITER:lineJoin = PS_JOIN_MITER; break;
        case BasicStroke.JOIN_ROUND:lineJoin = PS_JOIN_ROUND; break;
        }

        return (selectStylePen(getPrintDC(), endCap, lineJoin, width,
                               (int) (rgb[0] * MAX_WCOLOR),
                               (int) (rgb[1] * MAX_WCOLOR),
                               (int) (rgb[2] * MAX_WCOLOR)));
    }

    /**
     * Set a GDI font capable of drawing the java Font
     * passed in.
     */
    protected boolean setFont(String family, float size, int style,
                              int rotation, float awScale) {

        if (family.isEmpty()) {
            return false;
        }

        boolean didSetFont = true;

        if (!family.equals(mLastFontFamily) ||
            size     != mLastFontSize       ||
            style    != mLastFontStyle      ||
            rotation != mLastRotation       ||
            awScale  != mLastAwScale) {

            didSetFont = setFont(getPrintDC(),
                                 family,
                                 size,
                                 (style & Font.BOLD) != 0,
                                 (style & Font.ITALIC) != 0,
                                 rotation, awScale);
            if (didSetFont) {
                mLastFontFamily   = family;
                mLastFontSize     = size;
                mLastFontStyle    = style;
                mLastRotation     = rotation;
                mLastAwScale      = awScale;
            }
        }
        return didSetFont;
    }

    /**
     * Set the GDI color for text drawing.
     */
    protected void setTextColor(Color color) {

        /* We only need to select a brush if the color has changed.
        */
        if (color.equals(mLastTextColor) == false) {
            mLastTextColor = color;
            float[] rgb = color.getRGBColorComponents(null);

            setTextColor(getPrintDC(),
                         (int) (rgb[0] * MAX_WCOLOR),
                         (int) (rgb[1] * MAX_WCOLOR),
                         (int) (rgb[2] * MAX_WCOLOR));
        }
    }

    /**
     * Remove control characters.
     */
    @Override
    protected String removeControlChars(String str) {
        return super.removeControlChars(str);
    }

    /**
     * Draw the string {@code text} to the printer's
     * device context at the specified position.
     */
    protected void textOut(String str, float x, float y,
                           float[] positions) {
        /* Don't leave handling of control chars to GDI.
         * If control chars are removed,  'positions' isn't valid.
         * This means the caller needs to be aware of this and remove
         * control chars up front if supplying positions. Since the
         * caller is tightly integrated here, that's acceptable.
         */
        String text = removeControlChars(str);
        assert (positions == null) || (text.length() == str.length());
        if (text.length() == 0) {
            return;
        }
        textOut(getPrintDC(), text, text.length(), false, x, y, positions);
    }

   /**
     * Draw the glyphs {@code glyphs} to the printer's
     * device context at the specified position.
     */
    protected void glyphsOut(int []glyphs, float x, float y,
                             float[] positions) {

        /* TrueType glyph codes are 16 bit values, so can be packed
         * in a unicode string, and that's how GDI expects them.
         * A flag bit is set to indicate to GDI that these are glyphs,
         * not characters. The positions array must always be non-null
         * here for our purposes, although if not supplied, GDI should
         * just use the default advances for the glyphs.
         * Mask out upper 16 bits to remove any slot from a composite.
         */
        char[] glyphCharArray = new char[glyphs.length];
        for (int i=0;i<glyphs.length;i++) {
            glyphCharArray[i] = (char)(glyphs[i] & 0xffff);
        }
        String glyphStr = new String(glyphCharArray);
        textOut(getPrintDC(), glyphStr, glyphs.length, true, x, y, positions);
    }


    /**
     * Get the advance of this text that GDI returns for the
     * font currently selected into the GDI device context for
     * this job. Note that the removed control characters are
     * interpreted as zero-width by JDK and we remove them for
     * rendering so also remove them for measurement so that
     * this measurement can be properly compared with JDK measurement.
     */
    protected int getGDIAdvance(String text) {
        /* Don't leave handling of control chars to GDI. */
        text = removeControlChars(text);
        if (text.length() == 0) {
            return 0;
        }
        return getGDIAdvance(getPrintDC(), text);
    }

     /**
     * Draw the 24 bit BGR image buffer represented by
     * {@code image} to the GDI device context
     * {@code printDC}. The image is drawn at
     * {@code (destX, destY)} in device coordinates.
     * The image is scaled into a square of size
     * specified by {@code destWidth} and
     * {@code destHeight}. The portion of the
     * source image copied into that square is specified
     * by {@code srcX}, {@code srcY},
     * {@code srcWidth}, and srcHeight.
     */
    protected void drawImage3ByteBGR(byte[] image,
                                     float destX, float destY,
                                     float destWidth, float destHeight,
                                     float srcX, float srcY,
                                     float srcWidth, float srcHeight) {


        drawDIBImage(getPrintDC(), image,
                     destX, destY,
                     destWidth, destHeight,
                     srcX, srcY,
                     srcWidth, srcHeight,
                     24, null);

    }

    /* If 'icm' is null we expect its 24 bit (ie 3BYTE_BGR).
     * If 'icm' is non-null we expect its no more than 8 bpp and
     * specifically must be a valid DIB sizes : 1, 4 or 8 bpp.
     * Then we need to extract the colours into a byte array of the
     * format required by GDI which is an array of 'RGBQUAD'
     * RGBQUAD looks like :
     * typedef struct tagRGBQUAD {
     *    BYTE    rgbBlue;
     *    BYTE    rgbGreen;
     *    BYTE    rgbRed;
     *    BYTE    rgbReserved; // must be zero.
     * } RGBQUAD;
     * There's no alignment problem as GDI expects this to be packed
     * and each struct will start on a 4 byte boundary anyway.
     */
    protected void drawDIBImage(byte[] image,
                                float destX, float destY,
                                float destWidth, float destHeight,
                                float srcX, float srcY,
                                float srcWidth, float srcHeight,
                                int sampleBitsPerPixel,
                                IndexColorModel icm) {
        int bitCount = 24;
        byte[] bmiColors = null;

        if (icm != null) {
            bitCount = sampleBitsPerPixel;
            bmiColors = new byte[(1<<icm.getPixelSize())*4];
            for (int i=0;i<icm.getMapSize(); i++) {
                bmiColors[i*4+0]=(byte)(icm.getBlue(i)&0xff);
                bmiColors[i*4+1]=(byte)(icm.getGreen(i)&0xff);
                bmiColors[i*4+2]=(byte)(icm.getRed(i)&0xff);
            }
        }

        drawDIBImage(getPrintDC(), image,
                     destX, destY,
                     destWidth, destHeight,
                     srcX, srcY,
                     srcWidth, srcHeight,
                     bitCount, bmiColors);
    }

    /**
     * Begin a new page.
     */
    @Override
    protected void startPage(PageFormat format, Printable painter,
                             int index, boolean paperChanged) {

        /* Invalidate any device state caches we are
         * maintaining. Win95/98 resets the device
         * context attributes to default values at
         * the start of each page.
         */
        invalidateCachedState();

        deviceStartPage(format, painter, index, paperChanged);
    }

    /**
     * End a page.
     */
    @Override
    protected void endPage(PageFormat format, Printable painter,
                           int index) {

        deviceEndPage(format, painter, index);
    }

    /**
     * Forget any device state we may have cached.
     */
    private void invalidateCachedState() {
        mLastColor = null;
        mLastTextColor = null;
        mLastFontFamily = null;
    }

    private boolean defaultCopies = true;
    /**
     * Set the number of copies to be printed.
     */
    @Override
    public void setCopies(int copies) {
        super.setCopies(copies);
        defaultCopies = false;
        mAttCopies = copies;
        setNativeCopies(copies);
    }


 /* Native Methods */

    /**
     * Set copies in device.
     */
    private native void setNativeCopies(int copies);

    /**
     * Displays the print dialog and records the user's settings
     * into this object. Return false if the user cancels the
     * dialog.
     * If the dialog is to use a set of attributes, useAttributes is true.
     */
    private native boolean jobSetup(Pageable doc, boolean allowPrintToFile);

    /* Make sure printer DC is intialised and that info about the printer
     * is reflected back up to Java code
     */
    @Override
    protected native void initPrinter();

    /**
     * Call Window's StartDoc routine to begin a
     * print job. The DC from the print dialog is
     * used. If the print dialog was not displayed
     * then a DC for the default printer is created.
     * The native StartDoc returns false if the end-user cancelled
     * printing. This is possible if the printer is connected to FILE:
     * in which case windows queries the user for a destination and the
     * user may cancel out of it. Note that the implementation of
     * cancel() throws PrinterAbortException to indicate the user cancelled.
     */
    private native boolean _startDoc(String dest, String jobName)
                                     throws PrinterException;
    @Override
    protected void startDoc() throws PrinterException {
        if (!_startDoc(mDestination, getJobName())) {
            cancel();
        }
    }

    /**
     * Call Window's EndDoc routine to end a
     * print job.
     */
    @Override
    protected native void endDoc();

    /**
     * Call Window's AbortDoc routine to abort a
     * print job.
     */
    @Override
    protected native void abortDoc();

    /**
     * Call Windows native resource freeing APIs
     */
    private static native void deleteDC(long dc, long devmode, long devnames);

    /**
     * Begin a new page. This call's Window's
     * StartPage routine.
     */
    protected native void deviceStartPage(PageFormat format, Printable painter,
                                          int index, boolean paperChanged);
    /**
     * End a page. This call's Window's EndPage
     * routine.
     */
    protected native void deviceEndPage(PageFormat format, Printable painter,
                                        int index);

    /**
     * Prints the contents of the array of ints, 'data'
     * to the current page. The band is placed at the
     * location (x, y) in device coordinates on the
     * page. The width and height of the band is
     * specified by the caller.
     */
    @Override
    protected native void printBand(byte[] data, int x, int y,
                                    int width, int height);

    /**
     * Begin a Window's rendering path in the device
     * context {@code printDC}.
     */
    protected native void beginPath(long printDC);

    /**
     * End a Window's rendering path in the device
     * context {@code printDC}.
     */
    protected native void endPath(long printDC);

    /**
     * Close a subpath in a Window's rendering path in the device
     * context {@code printDC}.
     */
    protected native void closeFigure(long printDC);

    /**
     * Fill a defined Window's rendering path in the device
     * context {@code printDC}.
     */
    protected native void fillPath(long printDC);

    /**
     * Move the Window's pen position to {@code (x,y)}
     * in the device context {@code printDC}.
     */
    protected native void moveTo(long printDC, float x, float y);

    /**
     * Draw a line from the current pen position to
     * {@code (x,y)} in the device context {@code printDC}.
     */
    protected native void lineTo(long printDC, float x, float y);

    protected native void polyBezierTo(long printDC,
                                       float control1x, float control1y,
                                       float control2x, float control2y,
                                       float endX, float endY);

    /**
     * Set the current polgon fill rule into the device context
     * {@code printDC}. The {@code fillRule} should
     * be one of the following Windows constants:
     * {@code ALTERNATE} or {@code WINDING}.
     */
    protected native void setPolyFillMode(long printDC, int fillRule);

    /**
     * Set the GDI graphics mode to {@code GM_ADVANCED}
     * into the device context {@code printDC}.
     */
    protected native int setAdvancedGraphicsMode(long printDC);

    /**
     * Set the GDI graphics {@code mode}
     * into the device context {@code printDC}.
     * The {@code mode} should
     * be one of the following Windows constants:
     * {@code GM_COMPATIBLE} or {@code GM_ADVANCED}.
     */
    protected native void setGraphicsMode(long printDC, int mode);

    /**
     * Scale the GDI World Transform
     * of the device context {@code printDC}.
     */
    protected native void scale(long printDC, double scaleX, double scaleY);

    /**
     * Get the GDI World Transform
     * from the device context {@code printDC}.
     */
    protected native void getWorldTransform(long printDC, double[] transform);

    /**
     * Set the GDI World Transform
     * into the device context {@code printDC}.
     */
    protected native void setWorldTransform(long printDC, double[] transform);

    /**
     * Create a Window's solid brush for the color specified
     * by {@code (red, green, blue)}. Once the brush
     * is created, select it in the device
     * context {@code printDC} and free the old brush.
     */
    protected native void selectSolidBrush(long printDC,
                                           int red, int green, int blue);

    /**
     * Return the x coordinate of the current pen
     * position in the device context
     * {@code printDC}.
     */
    protected native int getPenX(long printDC);

    /**
     * Return the y coordinate of the current pen
     * position in the device context
     * {@code printDC}.
     */
    protected native int getPenY(long printDC);

    /**
     * Select the device context's current path
     * to be the clipping path.
     */
    protected native void selectClipPath(long printDC);

    /**
     * Draw a rectangle using specified brush.
     */
    protected native void frameRect(long printDC, float x, float y,
                                    float width, float height);

    /**
     * Fill a rectangle specified by the coordinates using
     * specified brush.
     */
    protected native void fillRect(long printDC, float x, float y,
                                   float width, float height,
                                   int red, int green, int blue);

    /**
     * Create a solid brush using the RG & B colors and width.
     * Select this brush and delete the old one.
     */
    protected native void selectPen(long printDC, float width,
                                    int red, int green, int blue);

    /**
     * Create a solid brush using the RG & B colors and specified
     * pen styles.  Select this created brush and delete the old one.
     */
    protected native boolean selectStylePen(long printDC, long cap,
                                            long join, float width,
                                            int red, int green, int blue);

    /**
     * Set a GDI font capable of drawing the java Font
     * passed in.
     */
    protected native boolean setFont(long printDC, String familyName,
                                     float fontSize,
                                     boolean bold,
                                     boolean italic,
                                     int rotation,
                                     float awScale);


    /**
     * Set the GDI color for text drawing.
     */
    protected native void setTextColor(long printDC,
                                       int red, int green, int blue);


    /**
     * Draw the string {@code text} into the device
     * context {@code printDC} at the specified
     * position.
     */
    protected native void textOut(long printDC, String text,
                                  int strlen, boolean glyphs,
                                  float x, float y, float[] positions);


    private native int getGDIAdvance(long printDC, String text);

     /**
     * Draw the DIB compatible image buffer represented by
     * {@code image} to the GDI device context
     * {@code printDC}. The image is drawn at
     * {@code (destX, destY)} in device coordinates.
     * The image is scaled into a square of size
     * specified by {@code destWidth} and
     * {@code destHeight}. The portion of the
     * source image copied into that square is specified
     * by {@code srcX}, {@code srcY},
     * {@code srcWidth}, and srcHeight.
     * Note that the image isn't completely compatible with DIB format.
     * At the very least it needs to be padded so each scanline is
     * DWORD aligned. Also we "flip" the image to make it a bottom-up DIB.
     */
    private native void drawDIBImage(long printDC, byte[] image,
                                     float destX, float destY,
                                     float destWidth, float destHeight,
                                     float srcX, float srcY,
                                     float srcWidth, float srcHeight,
                                     int bitCount, byte[] bmiColors);


    //** BEGIN Functions called by native code for querying/updating attributes

    private String getPrinterAttrib() {
        // getPrintService will get current print service or default if none
        PrintService service = this.getPrintService();
        String name = (service != null) ? service.getName() : null;
        return name;
    }

    /* SheetCollate */
    private int getCollateAttrib() {
        // -1 means unset, 0 uncollated, 1 collated.
        return mAttCollate;
    }

    private void setCollateAttrib(Attribute attr) {
        if (attr == SheetCollate.COLLATED) {
            mAttCollate = 1; // DMCOLLATE_TRUE
        } else {
            mAttCollate = 0; // DMCOLLATE_FALSE
        }
    }

    private void setCollateAttrib(Attribute attr,
                                  PrintRequestAttributeSet set) {
        setCollateAttrib(attr);
        set.add(attr);
    }

    /* Orientation */

    private int getOrientAttrib() {
        int orient = PageFormat.PORTRAIT;
        OrientationRequested orientReq = (attributes == null) ? null :
            (OrientationRequested)attributes.get(OrientationRequested.class);
        if (orientReq == null) {
            orientReq = (OrientationRequested)
               myService.getDefaultAttributeValue(OrientationRequested.class);
        }
        if (orientReq != null) {
            if (orientReq == OrientationRequested.REVERSE_LANDSCAPE) {
                orient = PageFormat.REVERSE_LANDSCAPE;
            } else if (orientReq == OrientationRequested.LANDSCAPE) {
                orient = PageFormat.LANDSCAPE;
            }
        }

        return orient;
    }

    private void setOrientAttrib(Attribute attr,
                                 PrintRequestAttributeSet set) {
        if (set != null) {
            set.add(attr);
        }
    }

    /* Copies and Page Range. */
    private int getCopiesAttrib() {
        if (defaultCopies) {
            return 0;
        } else {
            return getCopiesInt();
        }
     }

    private void setRangeCopiesAttribute(int from, int to, boolean isRangeSet,
                                         int copies) {
        if (attributes != null) {
            if (isRangeSet) {
                attributes.add(new PageRanges(from, to));
                setPageRange(from, to);
            }
            defaultCopies = false;
            attributes.add(new Copies(copies));
            /* Since this is called from native to tell Java to sync
             * up with native, we don't call this class's own setCopies()
             * method which is mainly to send the value down to native
             */
            super.setCopies(copies);
            mAttCopies = copies;
        }
    }



    private boolean getDestAttrib() {
        return (mDestination != null);
    }

    /* Quality */
    private int getQualityAttrib() {
        return mAttQuality;
    }

    private void setQualityAttrib(Attribute attr) {
        if (attr == PrintQuality.HIGH) {
            mAttQuality = -4; // DMRES_HIGH
        } else if (attr == PrintQuality.NORMAL) {
            mAttQuality = -3; // DMRES_MEDIUM
        } else {
            mAttQuality = -2; // DMRES_LOW
        }
    }

    private void setQualityAttrib(Attribute attr,
                                  PrintRequestAttributeSet set) {
        setQualityAttrib(attr);
        set.add(attr);
    }

    /* Color/Chromaticity */
    private int getColorAttrib() {
        return mAttChromaticity;
    }

    private void setColorAttrib(Attribute attr) {
        if (attr == Chromaticity.COLOR) {
            mAttChromaticity = 2; // DMCOLOR_COLOR
        } else {
            mAttChromaticity = 1; // DMCOLOR_MONOCHROME
        }
    }

    private void setColorAttrib(Attribute attr,
                                  PrintRequestAttributeSet set) {
        setColorAttrib(attr);
        set.add(attr);
    }

    /* Sides */
    private int getSidesAttrib() {
        return mAttSides;
    }

    private void setSidesAttrib(Attribute attr) {
        if (attr == Sides.TWO_SIDED_LONG_EDGE) {
            mAttSides = 2; // DMDUP_VERTICAL
        } else if (attr == Sides.TWO_SIDED_SHORT_EDGE) {
            mAttSides = 3; // DMDUP_HORIZONTAL
        } else { // Sides.ONE_SIDED
            mAttSides = 1;
        }
    }

    private void setSidesAttrib(Attribute attr,
                                PrintRequestAttributeSet set) {
        setSidesAttrib(attr);
        set.add(attr);
    }

    /** MediaSizeName / dmPaper */
    private int[] getWin32MediaAttrib() {
        int[] wid_ht = {0, 0};
        if (attributes != null) {
            Media media = (Media)attributes.get(Media.class);
            if (media instanceof MediaSizeName) {
                MediaSizeName msn = (MediaSizeName)media;
                MediaSize ms = MediaSize.getMediaSizeForName(msn);
                if (ms != null) {
                    wid_ht[0] = (int)(ms.getX(MediaSize.INCH) * 72.0);
                    wid_ht[1] = (int)(ms.getY(MediaSize.INCH) * 72.0);
                }
            }
        }
        return wid_ht;
    }

    private void setWin32MediaAttrib(Attribute attr) {
        if (!(attr instanceof MediaSizeName)) {
            return;
        }
        MediaSizeName msn = (MediaSizeName)attr;
        mAttMediaSizeName = ((Win32PrintService)myService).findPaperID(msn);
    }

    private void addPaperSize(PrintRequestAttributeSet aset,
                              int dmIndex, int width, int length) {

        if (aset == null) {
            return;
        }
        MediaSizeName msn =
           ((Win32PrintService)myService).findWin32Media(dmIndex);
        if (msn == null) {
            msn = ((Win32PrintService)myService).
                findMatchingMediaSizeNameMM((float)width, (float)length);
        }

        if (msn != null) {
            aset.add(msn);
        }
    }

    private void setWin32MediaAttrib(int dmIndex, int width, int length) {
        addPaperSize(attributes, dmIndex, width, length);
        mAttMediaSizeName = dmIndex;
    }

    /* MediaTray / dmTray */
    private void setMediaTrayAttrib(Attribute attr) {
        if (attr == MediaTray.BOTTOM) {
            mAttMediaTray = 2;    // DMBIN_LOWER
        } else if (attr == MediaTray.ENVELOPE) {
            mAttMediaTray = 5;    // DMBIN_ENVELOPE
        } else if (attr == MediaTray.LARGE_CAPACITY) {
            mAttMediaTray = 11;      // DMBIN_LARGECAPACITY
        } else if (attr == MediaTray.MAIN) {
            mAttMediaTray =1;               // DMBIN_UPPER
        } else if (attr == MediaTray.MANUAL) {
            mAttMediaTray = 4;              // DMBIN_MANUAL
        } else if (attr == MediaTray.MIDDLE) {
            mAttMediaTray = 3;              // DMBIN_MIDDLE
        } else if (attr == MediaTray.SIDE) {
            // no equivalent predefined value
            mAttMediaTray = 7;              // DMBIN_AUTO
        } else if (attr == MediaTray.TOP) {
            mAttMediaTray = 1;              // DMBIN_UPPER
        } else {
            if (attr instanceof Win32MediaTray) {
                mAttMediaTray = ((Win32MediaTray)attr).winID;
            } else {
                mAttMediaTray = 1;  // default
            }
        }
    }

    private void setMediaTrayAttrib(int dmBinID) {
        mAttMediaTray = dmBinID;
        MediaTray tray = ((Win32PrintService)myService).findMediaTray(dmBinID);
    }

    private int getMediaTrayAttrib() {
        return mAttMediaTray;
    }



    private boolean getPrintToFileEnabled() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            FilePermission printToFilePermission =
                new FilePermission("<<ALL FILES>>", "read,write");
            try {
                security.checkPermission(printToFilePermission);
            } catch (SecurityException e) {
                return false;
            }
        }
        return true;
    }

    private void setNativeAttributes(int flags, int fields, int values) {
        if (attributes == null) {
            return;
        }
        if ((flags & PD_PRINTTOFILE) != 0) {
            Destination destPrn = (Destination)attributes.get(
                                                 Destination.class);
            if (destPrn == null) {
                try {
                    attributes.add(new Destination(
                                               new File("./out.prn").toURI()));
                } catch (SecurityException se) {
                    try {
                        attributes.add(new Destination(
                                                new URI("file:out.prn")));
                    } catch (URISyntaxException e) {
                    }
                }
            }
        } else {
            attributes.remove(Destination.class);
        }

        if ((flags & PD_COLLATE) != 0) {
            setCollateAttrib(SheetCollate.COLLATED, attributes);
        } else {
            setCollateAttrib(SheetCollate.UNCOLLATED, attributes);
        }

        if ((flags & PD_NOSELECTION) != PD_NOSELECTION) {
            if ((flags & PD_PAGENUMS) != 0) {
                attributes.add(SunPageSelection.RANGE);
            } else if ((flags & PD_SELECTION) != 0) {
                attributes.add(SunPageSelection.SELECTION);
            } else {
                attributes.add(SunPageSelection.ALL);
            }
        }

        if ((fields & DM_ORIENTATION) != 0) {
            if ((values & SET_ORIENTATION) != 0) {
                setOrientAttrib(OrientationRequested.LANDSCAPE, attributes);
            } else {
                setOrientAttrib(OrientationRequested.PORTRAIT, attributes);
            }
        }

        if ((fields & DM_COLOR) != 0) {
            if ((values & SET_COLOR) != 0) {
                setColorAttrib(Chromaticity.COLOR, attributes);
            } else {
                setColorAttrib(Chromaticity.MONOCHROME, attributes);
            }
        }

        if ((fields & DM_PRINTQUALITY) != 0) {
            PrintQuality quality;
            if ((values & SET_RES_LOW) != 0) {
                quality = PrintQuality.DRAFT;
            } else if ((fields & SET_RES_HIGH) != 0) {
                quality = PrintQuality.HIGH;
            } else {
                quality = PrintQuality.NORMAL;
            }
            setQualityAttrib(quality, attributes);
        }

        if ((fields & DM_DUPLEX) != 0) {
            Sides sides;
            if ((values & SET_DUP_VERTICAL) != 0) {
                sides = Sides.TWO_SIDED_LONG_EDGE;
            } else if ((values & SET_DUP_HORIZONTAL) != 0) {
                sides = Sides.TWO_SIDED_SHORT_EDGE;
            } else {
                sides = Sides.ONE_SIDED;
            }
            setSidesAttrib(sides, attributes);
        }
    }

    private static final class DevModeValues {
        int dmFields;
        short copies;
        short collate;
        short color;
        short duplex;
        short orient;
        short paper;
        short bin;
        short xres_quality;
        short yres;
    }

    private void getDevModeValues(PrintRequestAttributeSet aset,
                                  DevModeValues info) {

        Copies c = (Copies)aset.get(Copies.class);
        if (c != null) {
            info.dmFields |= DM_COPIES;
            info.copies = (short)c.getValue();
        }

        SheetCollate sc = (SheetCollate)aset.get(SheetCollate.class);
        if (sc != null) {
            info.dmFields |= DM_COLLATE;
            info.collate = (sc == SheetCollate.COLLATED) ?
                DMCOLLATE_TRUE : DMCOLLATE_FALSE;
        }

        Chromaticity ch = (Chromaticity)aset.get(Chromaticity.class);
        if (ch != null) {
            info.dmFields |= DM_COLOR;
            if (ch == Chromaticity.COLOR) {
                info.color = DMCOLOR_COLOR;
            } else {
                info.color = DMCOLOR_MONOCHROME;
            }
        }

        Sides s = (Sides)aset.get(Sides.class);
        if (s != null) {
            info.dmFields |= DM_DUPLEX;
            if (s == Sides.TWO_SIDED_LONG_EDGE) {
                info.duplex = DMDUP_VERTICAL;
            } else if (s == Sides.TWO_SIDED_SHORT_EDGE) {
                info.duplex = DMDUP_HORIZONTAL;
            } else { // Sides.ONE_SIDED
                info.duplex = DMDUP_SIMPLEX;
            }
        }

        OrientationRequested or =
            (OrientationRequested)aset.get(OrientationRequested.class);
        if (or != null) {
            info.dmFields |= DM_ORIENTATION;
            info.orient = (or == OrientationRequested.LANDSCAPE)
                ? DMORIENT_LANDSCAPE : DMORIENT_PORTRAIT;
        }

        Media m = (Media)aset.get(Media.class);
        if (m instanceof MediaSizeName) {
            info.dmFields |= DM_PAPERSIZE;
            MediaSizeName msn = (MediaSizeName)m;
            info.paper =
                (short)((Win32PrintService)myService).findPaperID(msn);
        }

        MediaTray mt = null;
        if (m instanceof MediaTray) {
            mt = (MediaTray)m;
        }
        if (mt == null) {
            SunAlternateMedia sam =
                (SunAlternateMedia)aset.get(SunAlternateMedia.class);
            if (sam != null && (sam.getMedia() instanceof MediaTray)) {
                mt = (MediaTray)sam.getMedia();
            }
        }

        if (mt != null) {
            info.dmFields |= DM_DEFAULTSOURCE;
            info.bin = (short)(((Win32PrintService)myService).findTrayID(mt));
        }

        PrintQuality q = (PrintQuality)aset.get(PrintQuality.class);
        if (q != null) {
            info.dmFields |= DM_PRINTQUALITY;
            if (q == PrintQuality.DRAFT) {
                info.xres_quality = DMRES_DRAFT;
            } else if (q == PrintQuality.HIGH) {
                info.xres_quality = DMRES_HIGH;
            } else {
                info.xres_quality = DMRES_MEDIUM;
            }
        }

        PrinterResolution r =
            (PrinterResolution)aset.get(PrinterResolution.class);
        if (r != null) {
            info.dmFields |= DM_PRINTQUALITY | DM_YRESOLUTION;
            info.xres_quality =
                (short)r.getCrossFeedResolution(PrinterResolution.DPI);
            info.yres = (short)r.getFeedResolution(PrinterResolution.DPI);
        }
    }

    /* This method is called from native to update the values in the
     * attribute set which originates from the cross-platform dialog,
     * but updated by the native DocumentPropertiesUI which updates the
     * devmode. This syncs the devmode back in to the attributes so that
     * we can update the cross-platform dialog.
     * The attribute set here is a temporary one installed whilst this
     * happens,
     */
    private void setJobAttributes(PrintRequestAttributeSet attributes,
                                        int fields, int values,
                                        short copies,
                                        short dmPaperSize,
                                        short dmPaperWidth,
                                        short dmPaperLength,
                                        short dmDefaultSource,
                                        short xRes,
                                        short yRes) {

        if (attributes == null) {
            return;
        }

        if ((fields & DM_COPIES) != 0) {
            attributes.add(new Copies(copies));
        }

        if ((fields & DM_COLLATE) != 0) {
            if ((values & SET_COLLATED) != 0) {
                attributes.add(SheetCollate.COLLATED);
            } else {
                attributes.add(SheetCollate.UNCOLLATED);
            }
        }

        if ((fields & DM_ORIENTATION) != 0) {
            if ((values & SET_ORIENTATION) != 0) {
                attributes.add(OrientationRequested.LANDSCAPE);
            } else {
                attributes.add(OrientationRequested.PORTRAIT);
            }
        }

        if ((fields & DM_COLOR) != 0) {
            if ((values & SET_COLOR) != 0) {
                attributes.add(Chromaticity.COLOR);
            } else {
                attributes.add(Chromaticity.MONOCHROME);
            }
        }

        if ((fields & DM_PRINTQUALITY) != 0) {
            /* value < 0 indicates quality setting.
             * value > 0 indicates X resolution. In that case
             * hopefully we will also find y-resolution specified.
             * If its not, assume its the same as x-res.
             * Maybe Java code should try to reconcile this against
             * the printers claimed set of supported resolutions.
             */
            if (xRes < 0) {
                PrintQuality quality;
                if ((values & SET_RES_LOW) != 0) {
                    quality = PrintQuality.DRAFT;
                } else if ((fields & SET_RES_HIGH) != 0) {
                    quality = PrintQuality.HIGH;
                } else {
                    quality = PrintQuality.NORMAL;
                }
                attributes.add(quality);
            } else if (xRes > 0 && yRes > 0) {
                attributes.add(
                    new PrinterResolution(xRes, yRes, PrinterResolution.DPI));
            }
        }

        if ((fields & DM_DUPLEX) != 0) {
            Sides sides;
            if ((values & SET_DUP_VERTICAL) != 0) {
                sides = Sides.TWO_SIDED_LONG_EDGE;
            } else if ((values & SET_DUP_HORIZONTAL) != 0) {
                sides = Sides.TWO_SIDED_SHORT_EDGE;
            } else {
                sides = Sides.ONE_SIDED;
            }
            attributes.add(sides);
        }

        if ((fields & DM_PAPERSIZE) != 0) {
            addPaperSize(attributes, dmPaperSize, dmPaperWidth, dmPaperLength);
        }

        if ((fields & DM_DEFAULTSOURCE) != 0) {
            MediaTray tray =
                ((Win32PrintService)myService).findMediaTray(dmDefaultSource);
            attributes.add(new SunAlternateMedia(tray));
        }
    }

    private native boolean showDocProperties(long hWnd,
                                             PrintRequestAttributeSet aset,
                                             int dmFields,
                                             short copies,
                                             short collate,
                                             short color,
                                             short duplex,
                                             short orient,
                                             short paper,
                                             short bin,
                                             short xres_quality,
                                             short yres);

    public PrintRequestAttributeSet
        showDocumentProperties(Window owner,
                               PrintService service,
                               PrintRequestAttributeSet aset)
    {
        try {
            setNativePrintServiceIfNeeded(service.getName());
        } catch (PrinterException e) {
        }
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        long hWnd = acc.<WComponentPeer>getPeer(owner).getHWnd();
        DevModeValues info = new DevModeValues();
        getDevModeValues(aset, info);
        boolean ok =
            showDocProperties(hWnd, aset,
                              info.dmFields,
                              info.copies,
                              info.collate,
                              info.color,
                              info.duplex,
                              info.orient,
                              info.paper,
                              info.bin,
                              info.xres_quality,
                              info.yres);

        if (ok) {
            return aset;
        } else {
            return null;
        }
    }

    /* Printer Resolution. See also getXRes() and getYRes() */
    private void setResolutionDPI(int xres, int yres) {
        if (attributes != null) {
            PrinterResolution res =
                new PrinterResolution(xres, yres, PrinterResolution.DPI);
            attributes.add(res);
        }
        mAttXRes = xres;
        mAttYRes = yres;
    }

    private void setResolutionAttrib(Attribute attr) {
        PrinterResolution pr = (PrinterResolution)attr;
        mAttXRes = pr.getCrossFeedResolution(PrinterResolution.DPI);
        mAttYRes = pr.getFeedResolution(PrinterResolution.DPI);
    }

    private void setPrinterNameAttrib(String printerName) {
        PrintService service = this.getPrintService();

        if (printerName == null) {
            return;
        }

        if (service != null && printerName.equals(service.getName())) {
            return;
        } else {
            PrintService []services = PrinterJob.lookupPrintServices();
            for (int i=0; i<services.length; i++) {
                if (printerName.equals(services[i].getName())) {

                    try {
                        this.setPrintService(services[i]);
                    } catch (PrinterException e) {
                    }
                    return;
                }
            }
        }
    //** END Functions called by native code for querying/updating attributes

    }

@SuppressWarnings("serial") // JDK-implementation class
class PrintToFileErrorDialog extends Dialog implements ActionListener{
    public PrintToFileErrorDialog(Frame parent, String title, String message,
                           String buttonText) {
        super(parent, title, true);
        init (parent, title, message, buttonText);
    }

    public PrintToFileErrorDialog(Dialog parent, String title, String message,
                           String buttonText) {
        super(parent, title, true);
        init (parent, title, message, buttonText);
    }

    private void init(Component parent, String  title, String message,
                      String buttonText) {
        Panel p = new Panel();
        add("Center", new Label(message));
        Button btn = new Button(buttonText);
        btn.addActionListener(this);
        p.add(btn);
        add("South", p);
        pack();

        Dimension dDim = getSize();
        if (parent != null) {
            Rectangle fRect = parent.getBounds();
            setLocation(fRect.x + ((fRect.width - dDim.width) / 2),
                        fRect.y + ((fRect.height - dDim.height) / 2));
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        setVisible(false);
        dispose();
        return;
    }
}




    /**
     * Initialize JNI field and method ids
     */
    private static native void initIDs();

}
