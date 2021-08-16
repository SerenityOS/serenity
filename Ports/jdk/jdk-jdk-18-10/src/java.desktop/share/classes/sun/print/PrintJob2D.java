/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.PrintJob;
import java.awt.JobAttributes;
import java.awt.JobAttributes.*;
import java.awt.PageAttributes;
import java.awt.PageAttributes.*;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Properties;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PageRanges;

import sun.print.SunPageSelection;
import sun.print.SunMinMaxPage;

/**
 * A class which initiates and executes a print job using
 * the underlying PrinterJob graphics conversions.
 *
 * @see java.awt.Toolkit#getPrintJob
 *
 */
public class PrintJob2D extends PrintJob implements Printable, Runnable {

    private static final MediaType[] SIZES = {
        MediaType.ISO_4A0, MediaType.ISO_2A0, MediaType.ISO_A0,
        MediaType.ISO_A1, MediaType.ISO_A2, MediaType.ISO_A3,
        MediaType.ISO_A4, MediaType.ISO_A5, MediaType.ISO_A6,
        MediaType.ISO_A7, MediaType.ISO_A8, MediaType.ISO_A9,
        MediaType.ISO_A10, MediaType.ISO_B0, MediaType.ISO_B1,
        MediaType.ISO_B2, MediaType.ISO_B3, MediaType.ISO_B4,
        MediaType.ISO_B5, MediaType.ISO_B6, MediaType.ISO_B7,
        MediaType.ISO_B8, MediaType.ISO_B9, MediaType.ISO_B10,
        MediaType.JIS_B0, MediaType.JIS_B1, MediaType.JIS_B2,
        MediaType.JIS_B3, MediaType.JIS_B4, MediaType.JIS_B5,
        MediaType.JIS_B6, MediaType.JIS_B7, MediaType.JIS_B8,
        MediaType.JIS_B9, MediaType.JIS_B10, MediaType.ISO_C0,
        MediaType.ISO_C1, MediaType.ISO_C2, MediaType.ISO_C3,
        MediaType.ISO_C4, MediaType.ISO_C5, MediaType.ISO_C6,
        MediaType.ISO_C7, MediaType.ISO_C8, MediaType.ISO_C9,
        MediaType.ISO_C10, MediaType.ISO_DESIGNATED_LONG,
        MediaType.EXECUTIVE, MediaType.FOLIO, MediaType.INVOICE,
        MediaType.LEDGER, MediaType.NA_LETTER, MediaType.NA_LEGAL,
        MediaType.QUARTO, MediaType.A, MediaType.B,
        MediaType.C, MediaType.D, MediaType.E,
        MediaType.NA_10X15_ENVELOPE, MediaType.NA_10X14_ENVELOPE,
        MediaType.NA_10X13_ENVELOPE, MediaType.NA_9X12_ENVELOPE,
        MediaType.NA_9X11_ENVELOPE, MediaType.NA_7X9_ENVELOPE,
        MediaType.NA_6X9_ENVELOPE, MediaType.NA_NUMBER_9_ENVELOPE,
        MediaType.NA_NUMBER_10_ENVELOPE, MediaType.NA_NUMBER_11_ENVELOPE,
        MediaType.NA_NUMBER_12_ENVELOPE, MediaType.NA_NUMBER_14_ENVELOPE,
        MediaType.INVITE_ENVELOPE, MediaType.ITALY_ENVELOPE,
        MediaType.MONARCH_ENVELOPE, MediaType.PERSONAL_ENVELOPE
    };

    /* This array maps the above array to the objects used by the
     * javax.print APIs
         */
    private static final MediaSizeName[] JAVAXSIZES = {
        null, null, MediaSizeName.ISO_A0,
        MediaSizeName.ISO_A1, MediaSizeName.ISO_A2, MediaSizeName.ISO_A3,
        MediaSizeName.ISO_A4, MediaSizeName.ISO_A5, MediaSizeName.ISO_A6,
        MediaSizeName.ISO_A7, MediaSizeName.ISO_A8, MediaSizeName.ISO_A9,
        MediaSizeName.ISO_A10, MediaSizeName.ISO_B0, MediaSizeName.ISO_B1,
        MediaSizeName.ISO_B2, MediaSizeName.ISO_B3, MediaSizeName.ISO_B4,
        MediaSizeName.ISO_B5,  MediaSizeName.ISO_B6, MediaSizeName.ISO_B7,
        MediaSizeName.ISO_B8, MediaSizeName.ISO_B9, MediaSizeName.ISO_B10,
        MediaSizeName.JIS_B0, MediaSizeName.JIS_B1, MediaSizeName.JIS_B2,
        MediaSizeName.JIS_B3, MediaSizeName.JIS_B4, MediaSizeName.JIS_B5,
        MediaSizeName.JIS_B6, MediaSizeName.JIS_B7, MediaSizeName.JIS_B8,
        MediaSizeName.JIS_B9, MediaSizeName.JIS_B10, MediaSizeName.ISO_C0,
        MediaSizeName.ISO_C1, MediaSizeName.ISO_C2, MediaSizeName.ISO_C3,
        MediaSizeName.ISO_C4, MediaSizeName.ISO_C5, MediaSizeName.ISO_C6,
        null, null, null, null,
        MediaSizeName.ISO_DESIGNATED_LONG, MediaSizeName.EXECUTIVE,
        MediaSizeName.FOLIO, MediaSizeName.INVOICE, MediaSizeName.LEDGER,
        MediaSizeName.NA_LETTER, MediaSizeName.NA_LEGAL,
        MediaSizeName.QUARTO, MediaSizeName.A, MediaSizeName.B,
        MediaSizeName.C, MediaSizeName.D, MediaSizeName.E,
        MediaSizeName.NA_10X15_ENVELOPE, MediaSizeName.NA_10X14_ENVELOPE,
        MediaSizeName.NA_10X13_ENVELOPE, MediaSizeName.NA_9X12_ENVELOPE,
        MediaSizeName.NA_9X11_ENVELOPE, MediaSizeName.NA_7X9_ENVELOPE,
        MediaSizeName.NA_6X9_ENVELOPE,
        MediaSizeName.NA_NUMBER_9_ENVELOPE,
        MediaSizeName.NA_NUMBER_10_ENVELOPE,
        MediaSizeName.NA_NUMBER_11_ENVELOPE,
        MediaSizeName.NA_NUMBER_12_ENVELOPE,
        MediaSizeName.NA_NUMBER_14_ENVELOPE,
        null, MediaSizeName.ITALY_ENVELOPE,
        MediaSizeName.MONARCH_ENVELOPE, MediaSizeName.PERSONAL_ENVELOPE,
    };


    // widths and lengths in PostScript points (1/72 in.)
    private static final int[] WIDTHS = {
        /*iso-4a0*/ 4768, /*iso-2a0*/ 3370, /*iso-a0*/ 2384, /*iso-a1*/ 1684,
        /*iso-a2*/ 1191, /*iso-a3*/ 842, /*iso-a4*/ 595, /*iso-a5*/ 420,
        /*iso-a6*/ 298, /*iso-a7*/ 210, /*iso-a8*/ 147, /*iso-a9*/ 105,
        /*iso-a10*/ 74, /*iso-b0*/ 2835, /*iso-b1*/ 2004, /*iso-b2*/ 1417,
        /*iso-b3*/ 1001, /*iso-b4*/ 709, /*iso-b5*/ 499, /*iso-b6*/ 354,
        /*iso-b7*/ 249, /*iso-b8*/ 176, /*iso-b9*/ 125, /*iso-b10*/ 88,
        /*jis-b0*/ 2920, /*jis-b1*/ 2064, /*jis-b2*/ 1460, /*jis-b3*/ 1032,
        /*jis-b4*/ 729, /*jis-b5*/ 516, /*jis-b6*/ 363, /*jis-b7*/ 258,
        /*jis-b8*/ 181, /*jis-b9*/ 128, /*jis-b10*/ 91, /*iso-c0*/ 2599,
        /*iso-c1*/ 1837, /*iso-c2*/ 1298, /*iso-c3*/ 918, /*iso-c4*/ 649,
        /*iso-c5*/ 459, /*iso-c6*/ 323, /*iso-c7*/ 230, /*iso-c8*/ 162,
        /*iso-c9*/ 113, /*iso-c10*/ 79, /*iso-designated-long*/ 312,
        /*executive*/ 522, /*folio*/ 612, /*invoice*/ 396, /*ledger*/ 792,
        /*na-letter*/ 612, /*na-legal*/ 612, /*quarto*/ 609, /*a*/ 612,
        /*b*/ 792, /*c*/ 1224, /*d*/ 1584, /*e*/ 2448,
        /*na-10x15-envelope*/ 720, /*na-10x14-envelope*/ 720,
        /*na-10x13-envelope*/ 720, /*na-9x12-envelope*/ 648,
        /*na-9x11-envelope*/ 648, /*na-7x9-envelope*/ 504,
        /*na-6x9-envelope*/ 432, /*na-number-9-envelope*/ 279,
        /*na-number-10-envelope*/ 297, /*na-number-11-envelope*/ 324,
        /*na-number-12-envelope*/ 342, /*na-number-14-envelope*/ 360,
        /*invite-envelope*/ 624, /*italy-envelope*/ 312,
        /*monarch-envelope*/ 279, /*personal-envelope*/ 261
    };
    private static final int[] LENGTHS = {
        /*iso-4a0*/ 6741, /*iso-2a0*/ 4768, /*iso-a0*/ 3370, /*iso-a1*/ 2384,
        /*iso-a2*/ 1684, /*iso-a3*/ 1191, /*iso-a4*/ 842, /*iso-a5*/ 595,
        /*iso-a6*/ 420, /*iso-a7*/ 298, /*iso-a8*/ 210, /*iso-a9*/ 147,
        /*iso-a10*/ 105, /*iso-b0*/ 4008, /*iso-b1*/ 2835, /*iso-b2*/ 2004,
        /*iso-b3*/ 1417, /*iso-b4*/ 1001, /*iso-b5*/ 729, /*iso-b6*/ 499,
        /*iso-b7*/ 354, /*iso-b8*/ 249, /*iso-b9*/ 176, /*iso-b10*/ 125,
        /*jis-b0*/ 4127, /*jis-b1*/ 2920, /*jis-b2*/ 2064, /*jis-b3*/ 1460,
        /*jis-b4*/ 1032, /*jis-b5*/ 729, /*jis-b6*/ 516, /*jis-b7*/ 363,
        /*jis-b8*/ 258, /*jis-b9*/ 181, /*jis-b10*/ 128, /*iso-c0*/ 3677,
        /*iso-c1*/ 2599, /*iso-c2*/ 1837, /*iso-c3*/ 1298, /*iso-c4*/ 918,
        /*iso-c5*/ 649, /*iso-c6*/ 459, /*iso-c7*/ 323, /*iso-c8*/ 230,
        /*iso-c9*/ 162, /*iso-c10*/ 113, /*iso-designated-long*/ 624,
        /*executive*/ 756, /*folio*/ 936, /*invoice*/ 612, /*ledger*/ 1224,
        /*na-letter*/ 792, /*na-legal*/ 1008, /*quarto*/ 780, /*a*/ 792,
        /*b*/ 1224, /*c*/ 1584, /*d*/ 2448, /*e*/ 3168,
        /*na-10x15-envelope*/ 1080, /*na-10x14-envelope*/ 1008,
        /*na-10x13-envelope*/ 936, /*na-9x12-envelope*/ 864,
        /*na-9x11-envelope*/ 792, /*na-7x9-envelope*/ 648,
        /*na-6x9-envelope*/ 648, /*na-number-9-envelope*/ 639,
        /*na-number-10-envelope*/ 684, /*na-number-11-envelope*/ 747,
        /*na-number-12-envelope*/ 792, /*na-number-14-envelope*/ 828,
        /*invite-envelope*/ 624, /*italy-envelope*/ 652,
        /*monarch-envelope*/ 540, /*personal-envelope*/ 468
    };


    private Frame frame;
    private String docTitle = "";
    private JobAttributes jobAttributes;
    private PageAttributes pageAttributes;
    private PrintRequestAttributeSet attributes;

    /*
     * Displays the native or cross-platform dialog and allows the
     * user to update job & page attributes
     */

    /**
     * The PrinterJob being uses to implement the PrintJob.
     */
    private PrinterJob printerJob;

    /**
     * The size of the page being used for the PrintJob.
     */
    private PageFormat pageFormat;

    /**
     * The PrinterJob and the application run on different
     * threads and communicate through a pair of message
     * queues. This queue is the list of Graphics that
     * the PrinterJob has requested rendering for, but
     * for which the application has not yet called getGraphics().
     * In practice the length of this message queue is always
     * 0 or 1.
     */
    private MessageQ graphicsToBeDrawn = new MessageQ("tobedrawn");

    /**
     * Used to communicate between the application's thread
     * and the PrinterJob's thread this message queue holds
     * the list of Graphics into which the application has
     * finished drawing, but that have not yet been returned
     * to the PrinterJob thread. Again, in practice, the
     * length of this message queue is always 0 or 1.
     */
    private MessageQ graphicsDrawn = new MessageQ("drawn");

    /**
     * The last Graphics returned to the application via
     * getGraphics. This is the Graphics into which the
     * application is currently drawing.
     */
    private Graphics2D currentGraphics;

    /**
     * The zero based index of the page currently being rendered
     * by the application.
     */
    private int pageIndex = -1;

    // The following Strings are maintained for backward-compatibility with
    // Properties based print control.
    private static final String DEST_PROP = "awt.print.destination";
    private static final String PRINTER = "printer";
    private static final String FILE = "file";

    private static final String PRINTER_PROP = "awt.print.printer";

    private static final String FILENAME_PROP = "awt.print.fileName";

    private static final String NUMCOPIES_PROP = "awt.print.numCopies";

    private static final String OPTIONS_PROP = "awt.print.options";

    private static final String ORIENT_PROP = "awt.print.orientation";
    private static final String PORTRAIT = "portrait";
    private static final String LANDSCAPE = "landscape";

    private static final String PAPERSIZE_PROP = "awt.print.paperSize";
    private static final String LETTER = "letter";
    private static final String LEGAL = "legal";
    private static final String EXECUTIVE = "executive";
    private static final String A4 = "a4";

    private Properties props;

    private String options = ""; // REMIND: needs implementation

    /**
     * The thread on which PrinterJob is running.
     * This is different than the applications thread.
     */
    private Thread printerJobThread;

    public PrintJob2D(Frame frame,  String doctitle,
                      final Properties props) {
        this.props = props;
        this.jobAttributes = new JobAttributes();
        this.pageAttributes = new PageAttributes();
        translateInputProps();
        initPrintJob2D(frame, doctitle,
                       this.jobAttributes, this.pageAttributes);
    }

    public PrintJob2D(Frame frame,  String doctitle,
                      JobAttributes jobAttributes,
                      PageAttributes pageAttributes) {
        initPrintJob2D(frame, doctitle, jobAttributes, pageAttributes);
    }

    private void initPrintJob2D(Frame frame,  String doctitle,
                                JobAttributes jobAttributes,
                                PageAttributes pageAttributes) {

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }

        if (frame == null &&
            (jobAttributes == null ||
             jobAttributes.getDialog() == DialogType.NATIVE)) {
            throw new NullPointerException("Frame must not be null");
        }
        this.frame = frame;

        this.docTitle = (doctitle == null) ? "" : doctitle;
        this.jobAttributes = (jobAttributes != null)
            ? jobAttributes : new JobAttributes();
        this.pageAttributes = (pageAttributes != null)
            ? pageAttributes : new PageAttributes();

        // Currently, we always reduce page ranges to xxx or xxx-xxx
        int[][] pageRanges = this.jobAttributes.getPageRanges();
        int first = pageRanges[0][0];
        int last = pageRanges[pageRanges.length - 1][1];
        this.jobAttributes.setPageRanges(new int[][] {
            new int[] { first, last }
        });
        this.jobAttributes.setToPage(last);
        this.jobAttributes.setFromPage(first);


        // Verify that the cross feed and feed resolutions are the same
        int[] res = this.pageAttributes.getPrinterResolution();
        if (res[0] != res[1]) {
            throw new IllegalArgumentException("Differing cross feed and feed"+
                                               " resolutions not supported.");
        }

        // Verify that the app has access to the file system
        DestinationType dest= this.jobAttributes.getDestination();
        if (dest == DestinationType.FILE) {
            throwPrintToFile();

            // check if given filename is valid
            String destStr = jobAttributes.getFileName();
            if ((destStr != null) &&
                (jobAttributes.getDialog() == JobAttributes.DialogType.NONE)) {

                File f = new File(destStr);
                try {
                    // check if this is a new file and if filename chars are valid
                    // createNewFile returns false if file exists
                    if (f.createNewFile()) {
                        f.delete();
                    }
                } catch (IOException ioe) {
                    throw new IllegalArgumentException("Cannot write to file:"+
                                                       destStr);
                } catch (SecurityException se) {
                    //There is already file read/write access so at this point
                    // only delete access is denied.  Just ignore it because in
                    // most cases the file created in createNewFile gets overwritten
                    // anyway.
                }

                 File pFile = f.getParentFile();
                 if ((f.exists() &&
                      (!f.isFile() || !f.canWrite())) ||
                     ((pFile != null) &&
                      (!pFile.exists() || (pFile.exists() && !pFile.canWrite())))) {
                     throw new IllegalArgumentException("Cannot write to file:"+
                                                        destStr);
                 }
            }
        }
    }

    public boolean printDialog() {

        boolean proceedWithPrint = false;

        printerJob = PrinterJob.getPrinterJob();
        if (printerJob == null) {
            return false;
        }
        DialogType d = this.jobAttributes.getDialog();
        PrintService pServ = printerJob.getPrintService();
        if ((pServ == null) &&  (d == DialogType.NONE)){
            return false;
        }
        copyAttributes(pServ);

        DefaultSelectionType select =
            this.jobAttributes.getDefaultSelection();
        if (select == DefaultSelectionType.RANGE) {
            attributes.add(SunPageSelection.RANGE);
        } else if (select == DefaultSelectionType.SELECTION) {
            attributes.add(SunPageSelection.SELECTION);
        } else {
            attributes.add(SunPageSelection.ALL);
        }

        if (frame != null) {
             attributes.add(new DialogOwner(frame));
         }

        if ( d == DialogType.NONE) {
            proceedWithPrint = true;
        } else {
            if (d == DialogType.NATIVE) {
                attributes.add(DialogTypeSelection.NATIVE);
            }  else { //  (d == DialogType.COMMON)
                attributes.add(DialogTypeSelection.COMMON);
            }
            if (proceedWithPrint = printerJob.printDialog(attributes)) {
                if (pServ == null) {
                    // Windows gives an option to install a service
                    // when it detects there are no printers so
                    // we make sure we get the updated print service.
                    pServ = printerJob.getPrintService();
                    if (pServ == null) {
                        return false;
                    }
                }
                updateAttributes();
                translateOutputProps();
            }
        }

        if (proceedWithPrint) {

            JobName jname = (JobName)attributes.get(JobName.class);
            if (jname != null) {
                printerJob.setJobName(jname.toString());
            }

            pageFormat = new PageFormat();

            Media media = (Media)attributes.get(Media.class);
            MediaSize mediaSize =  null;
            if (media != null  && media instanceof MediaSizeName) {
                mediaSize = MediaSize.getMediaSizeForName((MediaSizeName)media);
            }

            Paper p = pageFormat.getPaper();
            if (mediaSize != null) {
                p.setSize(mediaSize.getX(MediaSize.INCH)*72.0,
                          mediaSize.getY(MediaSize.INCH)*72.0);
            }

            if (pageAttributes.getOrigin()==OriginType.PRINTABLE) {
                // AWT uses 1/4" borders by default
                p.setImageableArea(18.0, 18.0,
                                   p.getWidth()-36.0,
                                   p.getHeight()-36.0);
            } else {
                p.setImageableArea(0.0,0.0,p.getWidth(),p.getHeight());
            }

            pageFormat.setPaper(p);

            OrientationRequested orient =
               (OrientationRequested)attributes.get(OrientationRequested.class);
            if (orient!= null &&
                orient == OrientationRequested.REVERSE_LANDSCAPE) {
                pageFormat.setOrientation(PageFormat.REVERSE_LANDSCAPE);
            } else if (orient == OrientationRequested.LANDSCAPE) {
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
            } else {
                pageFormat.setOrientation(PageFormat.PORTRAIT);
            }

            PageRanges pageRangesAttr
                    = (PageRanges) attributes.get(PageRanges.class);
            if (pageRangesAttr != null) {
                // Get the PageRanges from print dialog.
                int[][] range = pageRangesAttr.getMembers();

                int prevFromPage = this.jobAttributes.getFromPage();
                int prevToPage = this.jobAttributes.getToPage();

                int currFromPage = range[0][0];
                int currToPage = range[range.length - 1][1];

                // if from < to update fromPage first followed by toPage
                // else update toPage first followed by fromPage
                if (currFromPage < prevToPage) {
                    this.jobAttributes.setFromPage(currFromPage);
                    this.jobAttributes.setToPage(currToPage);
                } else {
                    this.jobAttributes.setToPage(currToPage);
                    this.jobAttributes.setFromPage(currFromPage);
                }
            }
            printerJob.setPrintable(this, pageFormat);

        }

        return proceedWithPrint;
    }

    private void updateAttributes() {
        Copies c = (Copies)attributes.get(Copies.class);
        jobAttributes.setCopies(c.getValue());

        SunPageSelection sel =
            (SunPageSelection)attributes.get(SunPageSelection.class);
        if (sel == SunPageSelection.RANGE) {
            jobAttributes.setDefaultSelection(DefaultSelectionType.RANGE);
        } else if (sel == SunPageSelection.SELECTION) {
            jobAttributes.setDefaultSelection(DefaultSelectionType.SELECTION);
        } else {
            jobAttributes.setDefaultSelection(DefaultSelectionType.ALL);
        }

        Destination dest = (Destination)attributes.get(Destination.class);
        if (dest != null) {
            jobAttributes.setDestination(DestinationType.FILE);
            jobAttributes.setFileName(dest.getURI().getPath());
        } else {
            jobAttributes.setDestination(DestinationType.PRINTER);
        }

        PrintService serv = printerJob.getPrintService();
        if (serv != null) {
            jobAttributes.setPrinter(serv.getName());
        }

        PageRanges range = (PageRanges)attributes.get(PageRanges.class);
        int[][] members = range.getMembers();
        jobAttributes.setPageRanges(members);

        SheetCollate collation =
            (SheetCollate)attributes.get(SheetCollate.class);
        if (collation == SheetCollate.COLLATED) {
            jobAttributes.setMultipleDocumentHandling(
            MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES);
        } else {
            jobAttributes.setMultipleDocumentHandling(
            MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);
        }

        Sides sides = (Sides)attributes.get(Sides.class);
        if (sides == Sides.TWO_SIDED_LONG_EDGE) {
            jobAttributes.setSides(SidesType.TWO_SIDED_LONG_EDGE);
        } else if (sides == Sides.TWO_SIDED_SHORT_EDGE) {
            jobAttributes.setSides(SidesType.TWO_SIDED_SHORT_EDGE);
        } else {
            jobAttributes.setSides(SidesType.ONE_SIDED);
        }

        // PageAttributes

        Chromaticity color =
            (Chromaticity)attributes.get(Chromaticity.class);
        if (color == Chromaticity.COLOR) {
            pageAttributes.setColor(ColorType.COLOR);
        } else {
            pageAttributes.setColor(ColorType.MONOCHROME);
        }

        OrientationRequested orient =
            (OrientationRequested)attributes.get(OrientationRequested.class);
        if (orient == OrientationRequested.LANDSCAPE) {
            pageAttributes.setOrientationRequested(
                                       OrientationRequestedType.LANDSCAPE);
        } else {
            pageAttributes.setOrientationRequested(
                                       OrientationRequestedType.PORTRAIT);
        }

        PrintQuality qual = (PrintQuality)attributes.get(PrintQuality.class);
        if (qual == PrintQuality.DRAFT) {
            pageAttributes.setPrintQuality(PrintQualityType.DRAFT);
        } else if (qual == PrintQuality.HIGH) {
            pageAttributes.setPrintQuality(PrintQualityType.HIGH);
        } else { // NORMAL
            pageAttributes.setPrintQuality(PrintQualityType.NORMAL);
        }

        Media msn = (Media)attributes.get(Media.class);
        if (msn != null && msn instanceof MediaSizeName) {
            MediaType mType = unMapMedia((MediaSizeName)msn);

            if (mType != null) {
                pageAttributes.setMedia(mType);
            }
        }
        debugPrintAttributes(false, false);
    }

    private void debugPrintAttributes(boolean ja, boolean pa ) {
        if (ja) {
            System.out.println("new Attributes\ncopies = "+
                               jobAttributes.getCopies()+
                               "\nselection = "+
                               jobAttributes.getDefaultSelection()+
                               "\ndest "+jobAttributes.getDestination()+
                               "\nfile "+jobAttributes.getFileName()+
                               "\nfromPage "+jobAttributes.getFromPage()+
                               "\ntoPage "+jobAttributes.getToPage()+
                               "\ncollation "+
                               jobAttributes.getMultipleDocumentHandling()+
                               "\nPrinter "+jobAttributes.getPrinter()+
                               "\nSides2 "+jobAttributes.getSides()
                               );
        }

        if (pa) {
            System.out.println("new Attributes\ncolor = "+
                               pageAttributes.getColor()+
                               "\norientation = "+
                               pageAttributes.getOrientationRequested()+
                               "\nquality "+pageAttributes.getPrintQuality()+
                               "\nMedia2 "+pageAttributes.getMedia()
                               );
        }
    }


    /* From JobAttributes we will copy job name and duplex printing
     * and destination.
     * The majority of the rest of the attributes are reflected
     * attributes.
     *
     * From PageAttributes we copy color, media size, orientation,
     * origin type, resolution and print quality.
     * We use the media, orientation in creating the page format, and
     * the origin type to set its imageable area.
     *
     * REMIND: Interpretation of resolution, additional media sizes.
     */
    private void copyAttributes(PrintService printServ) {

        attributes = new HashPrintRequestAttributeSet();
        attributes.add(new JobName(docTitle, null));
        PrintService pServ = printServ;

        String printerName = jobAttributes.getPrinter();
        if (printerName != null && printerName != ""
            && pServ != null && !printerName.equals(pServ.getName())) {

            // Search for the given printerName in the list of PrintServices
            PrintService []services = PrinterJob.lookupPrintServices();
            try {
                for (int i=0; i<services.length; i++) {
                    if (printerName.equals(services[i].getName())) {
                        printerJob.setPrintService(services[i]);
                        pServ = services[i];
                        break;
                    }
                }
            } catch (PrinterException pe) {
            }
        }

        DestinationType dest = jobAttributes.getDestination();
        if (dest == DestinationType.FILE && pServ != null &&
            pServ.isAttributeCategorySupported(Destination.class)) {

            String fileName = jobAttributes.getFileName();

            Destination defaultDest;
            if (fileName == null && (defaultDest = (Destination)pServ.
                    getDefaultAttributeValue(Destination.class)) != null) {
                attributes.add(defaultDest);
            } else {
                URI uri = null;
                try {
                    if (fileName != null) {
                        if (fileName.isEmpty()) {
                            fileName = ".";
                        }
                    } else {
                        // defaultDest should not be null.  The following code
                        // is only added to safeguard against a possible
                        // buggy implementation of a PrintService having a
                        // null default Destination.
                        fileName = "out.prn";
                    }
                    uri = (new File(fileName)).toURI();
                } catch (SecurityException se) {
                    try {
                        // '\\' file separator is illegal character in opaque
                        // part and causes URISyntaxException, so we replace
                        // it with '/'
                        fileName = fileName.replace('\\', '/');
                        uri = new URI("file:"+fileName);
                    } catch (URISyntaxException e) {
                    }
                }
                if (uri != null) {
                    attributes.add(new Destination(uri));
                }
            }
        }
        attributes.add(new SunMinMaxPage(jobAttributes.getMinPage(),
                                         jobAttributes.getMaxPage()));
        SidesType sType = jobAttributes.getSides();
        if (sType == SidesType.TWO_SIDED_LONG_EDGE) {
            attributes.add(Sides.TWO_SIDED_LONG_EDGE);
        } else if (sType == SidesType.TWO_SIDED_SHORT_EDGE) {
            attributes.add(Sides.TWO_SIDED_SHORT_EDGE);
        } else if (sType == SidesType.ONE_SIDED) {
            attributes.add(Sides.ONE_SIDED);
        }

        MultipleDocumentHandlingType hType =
          jobAttributes.getMultipleDocumentHandling();
        if (hType ==
            MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES) {
          attributes.add(SheetCollate.COLLATED);
        } else {
          attributes.add(SheetCollate.UNCOLLATED);
        }

        attributes.add(new Copies(jobAttributes.getCopies()));

        attributes.add(new PageRanges(jobAttributes.getFromPage(),
                                      jobAttributes.getToPage()));

        if (pageAttributes.getColor() == ColorType.COLOR) {
            attributes.add(Chromaticity.COLOR);
        } else {
            attributes.add(Chromaticity.MONOCHROME);
        }

        pageFormat = printerJob.defaultPage();
        if (pageAttributes.getOrientationRequested() ==
            OrientationRequestedType.LANDSCAPE) {
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
                attributes.add(OrientationRequested.LANDSCAPE);
        } else {
                pageFormat.setOrientation(PageFormat.PORTRAIT);
                attributes.add(OrientationRequested.PORTRAIT);
        }

        MediaType media = pageAttributes.getMedia();
        MediaSizeName msn = mapMedia(media);
        if (msn != null) {
            attributes.add(msn);
        }

        PrintQualityType qType =
            pageAttributes.getPrintQuality();
        if (qType == PrintQualityType.DRAFT) {
            attributes.add(PrintQuality.DRAFT);
        } else if (qType == PrintQualityType.NORMAL) {
            attributes.add(PrintQuality.NORMAL);
        } else if (qType == PrintQualityType.HIGH) {
            attributes.add(PrintQuality.HIGH);
        }
    }

    /**
     * Gets a Graphics object that will draw to the next page.
     * The page is sent to the printer when the graphics
     * object is disposed.  This graphics object will also implement
     * the PrintGraphics interface.
     * @see java.awt.PrintGraphics
     */
    public Graphics getGraphics() {

        Graphics printGraphics = null;

        synchronized (this) {
            ++pageIndex;

            // Thread should not be created after end has been called.
            // One way to detect this is if any of the graphics queue
            //  has been closed.
            if (pageIndex == 0 && !graphicsToBeDrawn.isClosed()) {

            /* We start a thread on which the PrinterJob will run.
             * The PrinterJob will ask for pages on that thread
             * and will use a message queue to fulfill the application's
             * requests for a Graphics on the application's
             * thread.
             */

                startPrinterJobThread();

            }
            notify();
        }

        /* If the application has already been handed back
         * a graphics then we need to put that graphics into
         * the drawn queue so that the PrinterJob thread can
         * return to the print system.
         */
        if (currentGraphics != null) {
            graphicsDrawn.append(currentGraphics);
            currentGraphics = null;
        }

        /* We'll block here until a new graphics becomes
         * available.
         */

        currentGraphics = graphicsToBeDrawn.pop();

        if (currentGraphics instanceof PeekGraphics) {
            ( (PeekGraphics) currentGraphics).setAWTDrawingOnly();
            graphicsDrawn.append(currentGraphics);
            currentGraphics = graphicsToBeDrawn.pop();
        }


        if (currentGraphics != null) {

            /* In the PrintJob API, the origin is at the upper-
             * left of the imageable area when using the new "printable"
             * origin attribute, otherwise its the physical origin (for
             * backwards compatibility. We emulate this by createing
             * a PageFormat which matches and then performing the
             * translate to the origin. This is a no-op if physical
             * origin is specified.
             */
            currentGraphics.translate(pageFormat.getImageableX(),
                                      pageFormat.getImageableY());

            /* Scale to accommodate AWT's notion of printer resolution */
            double awtScale = 72.0/getPageResolutionInternal();
            currentGraphics.scale(awtScale, awtScale);

            /* The caller wants a Graphics instance but we do
             * not want them to make 2D calls. We can't hand
             * back a Graphics2D. The returned Graphics also
             * needs to implement PrintGraphics, so we wrap
             * the Graphics2D instance. The PrintJob API has
             * the application dispose of the Graphics so
             * we create a copy of the one returned by PrinterJob.
             */
            printGraphics = new ProxyPrintGraphics(currentGraphics.create(),
                                                   this);

        }

        return printGraphics;
    }

    /**
     * Returns the dimensions of the page in pixels.
     * The resolution of the page is chosen so that it
     * is similar to the screen resolution.
     * Except (since 1.3) when the application specifies a resolution.
     * In that case it is scaled accordingly.
     */
    public Dimension getPageDimension() {
        double wid, hgt, scale;
        if (pageAttributes != null &&
            pageAttributes.getOrigin()==OriginType.PRINTABLE) {
            wid = pageFormat.getImageableWidth();
            hgt = pageFormat.getImageableHeight();
        } else {
            wid = pageFormat.getWidth();
            hgt = pageFormat.getHeight();
        }
        scale = getPageResolutionInternal() / 72.0;
        return new Dimension((int)(wid * scale), (int)(hgt * scale));
    }

     private double getPageResolutionInternal() {
        if (pageAttributes != null) {
            int []res = pageAttributes.getPrinterResolution();
            if (res[2] == 3) {
                return res[0];
            } else /* if (res[2] == 4) */ {
                return (res[0] * 2.54);
            }
        } else {
            return 72.0;
        }
    }

    /**
     * Returns the resolution of the page in pixels per inch.
     * Note that this doesn't have to correspond to the physical
     * resolution of the printer.
     */
    public int getPageResolution() {
        return (int)getPageResolutionInternal();
    }

    /**
     * Returns true if the last page will be printed first.
     */
    public boolean lastPageFirst() {
        return false;
    }

    /**
     * Ends the print job and does any necessary cleanup.
     */
    public synchronized void end() {

        /* Prevent the PrinterJob thread from appending any more
         * graphics to the to-be-drawn queue
         */
        graphicsToBeDrawn.close();

        /* If we have a currentGraphics it was the last one returned to the
         * PrintJob client. Append it to the drawn queue so that print()
         * will return allowing the page to be flushed.
         * This really ought to happen in dispose() but for whatever reason
         * that isn't how the old PrintJob worked even though its spec
         * said dispose() flushed the page.
         */
        if (currentGraphics != null) {
            graphicsDrawn.append(currentGraphics);
        }
        graphicsDrawn.closeWhenEmpty();

        /* Wait for the PrinterJob.print() thread to terminate, ensuring
         * that RasterPrinterJob has made its end doc call, and resources
         * are released, files closed etc.
         */
        if( printerJobThread != null && printerJobThread.isAlive() ){
            try {
                printerJobThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Ends this print job once it is no longer referenced.
     * @see #end
     */
    @SuppressWarnings("deprecation")
    public void finalize() {
        end();
    }

    /**
     * Prints the page at the specified index into the specified
     * {@link Graphics} context in the specified
     * format.  A {@code PrinterJob} calls the
     * {@code Printable} interface to request that a page be
     * rendered into the context specified by
     * {@code graphics}.  The format of the page to be drawn is
     * specified by {@code pageFormat}.  The zero based index
     * of the requested page is specified by {@code pageIndex}.
     * If the requested page does not exist then this method returns
     * NO_SUCH_PAGE; otherwise PAGE_EXISTS is returned.
     * The {@code Graphics} class or subclass implements the
     * {@link java.awt.PrintGraphics} interface to provide additional
     * information.  If the {@code Printable} object
     * aborts the print job then it throws a {@link PrinterException}.
     * @param graphics the context into which the page is drawn
     * @param pageFormat the size and orientation of the page being drawn
     * @param pageIndex the zero based index of the page to be drawn
     * @return PAGE_EXISTS if the page is rendered successfully
     *         or NO_SUCH_PAGE if {@code pageIndex} specifies a
     *         non-existent page.
     * @exception java.awt.print.PrinterException
     *         thrown when the print job is terminated.
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                 throws PrinterException {

        int result;

        /* This method will be called by the PrinterJob on a thread other
         * that the application's thread. We hold on to the graphics
         * until we can rendevous with the application's thread and
         * hand over the graphics. The application then does all the
         * drawing. When the application is done drawing we rendevous
         * again with the PrinterJob thread and release the Graphics
         * so that it knows we are done.
         */

        /* Add the graphics to the message queue of graphics to
         * be rendered. This is really a one slot queue. The
         * application's thread will come along and remove the
         * graphics from the queue when the app asks for a graphics.
         */
        graphicsToBeDrawn.append( (Graphics2D) graphics);

        /* We now wait for the app's thread to finish drawing on
         * the Graphics. This thread will sleep until the application
         * release the graphics by placing it in the graphics drawn
         * message queue. If the application signals that it is
         * finished drawing the entire document then we'll get null
         * returned when we try and pop a finished graphic.
         */
        if (graphicsDrawn.pop() != null) {
            result = PAGE_EXISTS;
        } else {
            result = NO_SUCH_PAGE;
        }

        return result;
    }

    private void startPrinterJobThread() {
        printerJobThread =
            new Thread(null, this, "printerJobThread", 0, false);
        printerJobThread.start();
    }


    public void run() {

        try {
            attributes.remove(PageRanges.class);
            printerJob.print(attributes);
        } catch (PrinterException e) {
            //REMIND: need to store this away and not rethrow it.
        }

        /* Close the message queues so that nobody is stuck
         * waiting for one.
         */
        graphicsToBeDrawn.closeWhenEmpty();
        graphicsDrawn.close();
    }

    private class MessageQ {

        private String qid="noname";

        private ArrayList<Graphics2D> queue = new ArrayList<>();

        MessageQ(String id) {
          qid = id;
        }

        synchronized void closeWhenEmpty() {

            while (queue != null && queue.size() > 0) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }

            queue = null;
            notifyAll();
        }

        synchronized void close() {
            queue = null;
            notifyAll();
        }

        synchronized boolean append(Graphics2D g) {

            boolean queued = false;

            if (queue != null) {
                queue.add(g);
                queued = true;
                notify();
            }

            return queued;
        }

        synchronized Graphics2D pop() {
            Graphics2D g = null;

            while (g == null && queue != null) {

                if (queue.size() > 0) {
                    g = queue.remove(0);
                    notify();

                } else {
                    try {
                        wait(2000);
                    } catch (InterruptedException e) {
                        // do nothing.
                    }
                }
            }

            return g;
        }

        synchronized boolean isClosed() {
            return queue == null;
        }

    }


    private static int[] getSize(MediaType mType) {
        int []dim = new int[2];
        dim[0] = 612;
        dim[1] = 792;

        for (int i=0; i < SIZES.length; i++) {
            if (SIZES[i] == mType) {
                dim[0] = WIDTHS[i];
                dim[1] = LENGTHS[i];
                break;
            }
        }
        return dim;
    }

    public static MediaSizeName mapMedia(MediaType mType) {
        MediaSizeName media = null;

        // JAVAXSIZES.length and SIZES.length must be equal!
        // Attempt to recover by getting the smaller size.
        int length = Math.min(SIZES.length, JAVAXSIZES.length);

        for (int i=0; i < length; i++) {
            if (SIZES[i] == mType) {
                if ((JAVAXSIZES[i] != null) &&
                    MediaSize.getMediaSizeForName(JAVAXSIZES[i]) != null) {
                    media = JAVAXSIZES[i];
                    break;
                } else {
                    /* create Custom Media */
                    media = new CustomMediaSizeName(SIZES[i].toString());

                    float w = (float)Math.rint(WIDTHS[i]  / 72.0);
                    float h = (float)Math.rint(LENGTHS[i] / 72.0);
                    if (w > 0.0 && h > 0.0) {
                        // add new created MediaSize to our static map
                        // so it will be found when we call findMedia
                        new MediaSize(w, h, Size2DSyntax.INCH, media);
                    }

                    break;
                }
            }
        }
        return media;
    }


    public static MediaType unMapMedia(MediaSizeName mSize) {
        MediaType media = null;

        // JAVAXSIZES.length and SIZES.length must be equal!
        // Attempt to recover by getting the smaller size.
        int length = Math.min(SIZES.length, JAVAXSIZES.length);

        for (int i=0; i < length; i++) {
            if (JAVAXSIZES[i] == mSize) {
                if (SIZES[i] != null) {
                    media = SIZES[i];
                    break;
                }
            }
        }
        return media;
    }

    private void translateInputProps() {
        if (props == null) {
            return;
        }

        String str;

        str = props.getProperty(DEST_PROP);
        if (str != null) {
            if (str.equals(PRINTER)) {
                jobAttributes.setDestination(DestinationType.PRINTER);
            } else if (str.equals(FILE)) {
                jobAttributes.setDestination(DestinationType.FILE);
            }
        }
        str = props.getProperty(PRINTER_PROP);
        if (str != null) {
            jobAttributes.setPrinter(str);
        }
        str = props.getProperty(FILENAME_PROP);
        if (str != null) {
            jobAttributes.setFileName(str);
        }
        str = props.getProperty(NUMCOPIES_PROP);
        if (str != null) {
            jobAttributes.setCopies(Integer.parseInt(str));
        }

        this.options = props.getProperty(OPTIONS_PROP, "");

        str = props.getProperty(ORIENT_PROP);
        if (str != null) {
            if (str.equals(PORTRAIT)) {
                pageAttributes.setOrientationRequested(
                                        OrientationRequestedType.PORTRAIT);
            } else if (str.equals(LANDSCAPE)) {
                pageAttributes.setOrientationRequested(
                                        OrientationRequestedType.LANDSCAPE);
            }
        }
        str = props.getProperty(PAPERSIZE_PROP);
        if (str != null) {
            if (str.equals(LETTER)) {
                pageAttributes.setMedia(SIZES[MediaType.LETTER.hashCode()]);
            } else if (str.equals(LEGAL)) {
                pageAttributes.setMedia(SIZES[MediaType.LEGAL.hashCode()]);
            } else if (str.equals(EXECUTIVE)) {
                pageAttributes.setMedia(SIZES[MediaType.EXECUTIVE.hashCode()]);
            } else if (str.equals(A4)) {
                pageAttributes.setMedia(SIZES[MediaType.A4.hashCode()]);
            }
        }
    }

    private void translateOutputProps() {
        if (props == null) {
            return;
        }

        String str;

        props.setProperty(DEST_PROP,
            (jobAttributes.getDestination() == DestinationType.PRINTER) ?
                          PRINTER : FILE);
        str = jobAttributes.getPrinter();
        if (str != null && !str.isEmpty()) {
            props.setProperty(PRINTER_PROP, str);
        }
        str = jobAttributes.getFileName();
        if (str != null && !str.isEmpty()) {
            props.setProperty(FILENAME_PROP, str);
        }
        int copies = jobAttributes.getCopies();
        if (copies > 0) {
            props.setProperty(NUMCOPIES_PROP, "" + copies);
        }
        str = this.options;
        if (str != null && !str.isEmpty()) {
            props.setProperty(OPTIONS_PROP, str);
        }
        props.setProperty(ORIENT_PROP,
            (pageAttributes.getOrientationRequested() ==
             OrientationRequestedType.PORTRAIT)
                          ? PORTRAIT : LANDSCAPE);
        MediaType media = SIZES[pageAttributes.getMedia().hashCode()];
        if (media == MediaType.LETTER) {
            str = LETTER;
        } else if (media == MediaType.LEGAL) {
            str = LEGAL;
        } else if (media == MediaType.EXECUTIVE) {
            str = EXECUTIVE;
        } else if (media == MediaType.A4) {
            str = A4;
        } else {
            str = media.toString();
        }
        props.setProperty(PAPERSIZE_PROP, str);
    }

    private void throwPrintToFile() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        FilePermission printToFilePermission = null;
        if (security != null) {
            if (printToFilePermission == null) {
                printToFilePermission =
                    new FilePermission("<<ALL FILES>>", "read,write");
            }
            security.checkPermission(printToFilePermission);
        }
    }

}
