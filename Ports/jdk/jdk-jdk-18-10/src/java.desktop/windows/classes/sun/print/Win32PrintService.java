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

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.print.attribute.standard.Severity;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.event.PrintServiceAttributeListener;
import sun.awt.windows.WPrinterJob;

public class Win32PrintService implements PrintService, AttributeUpdater,
                                          SunPrinterJobService {

    public static MediaSize[] predefMedia = Win32MediaSize.getPredefMedia();

    private static final DocFlavor[] supportedFlavors = {
        DocFlavor.BYTE_ARRAY.GIF,
        DocFlavor.INPUT_STREAM.GIF,
        DocFlavor.URL.GIF,
        DocFlavor.BYTE_ARRAY.JPEG,
        DocFlavor.INPUT_STREAM.JPEG,
        DocFlavor.URL.JPEG,
        DocFlavor.BYTE_ARRAY.PNG,
        DocFlavor.INPUT_STREAM.PNG,
        DocFlavor.URL.PNG,
        DocFlavor.SERVICE_FORMATTED.PAGEABLE,
        DocFlavor.SERVICE_FORMATTED.PRINTABLE,
        DocFlavor.BYTE_ARRAY.AUTOSENSE,
        DocFlavor.URL.AUTOSENSE,
        DocFlavor.INPUT_STREAM.AUTOSENSE
    };

    /* let's try to support a few of these */
    private static final Class<?>[] serviceAttrCats = {
        PrinterName.class,
        PrinterIsAcceptingJobs.class,
        QueuedJobCount.class,
        ColorSupported.class,
    };

    /*  it turns out to be inconvenient to store the other categories
     *  separately because many attributes are in multiple categories.
     */
    private static Class<?>[] otherAttrCats = {
        JobName.class,
        RequestingUserName.class,
        Copies.class,
        Destination.class,
        OrientationRequested.class,
        PageRanges.class,
        Media.class,
        MediaPrintableArea.class,
        Fidelity.class,
        // We support collation on 2D printer jobs, even if the driver can't.
        SheetCollate.class,
        SunAlternateMedia.class,
        Chromaticity.class
    };


    /*
     * This table together with methods findWin32Media and
     * findMatchingMediaSizeNameMM are declared public as these are also
     * used in WPrinterJob.java.
     */
    public static final MediaSizeName[] dmPaperToPrintService = {
      MediaSizeName.NA_LETTER, MediaSizeName.NA_LETTER,
      MediaSizeName.TABLOID, MediaSizeName.LEDGER,
      MediaSizeName.NA_LEGAL, MediaSizeName.INVOICE,
      MediaSizeName.EXECUTIVE, MediaSizeName.ISO_A3,
      MediaSizeName.ISO_A4, MediaSizeName.ISO_A4,
      MediaSizeName.ISO_A5, MediaSizeName.JIS_B4,
      MediaSizeName.JIS_B5, MediaSizeName.FOLIO,
      MediaSizeName.QUARTO, MediaSizeName.NA_10X14_ENVELOPE,
      MediaSizeName.B, MediaSizeName.NA_LETTER,
      MediaSizeName.NA_NUMBER_9_ENVELOPE, MediaSizeName.NA_NUMBER_10_ENVELOPE,
      MediaSizeName.NA_NUMBER_11_ENVELOPE, MediaSizeName.NA_NUMBER_12_ENVELOPE,
      MediaSizeName.NA_NUMBER_14_ENVELOPE, MediaSizeName.C,
      MediaSizeName.D, MediaSizeName.E,
      MediaSizeName.ISO_DESIGNATED_LONG, MediaSizeName.ISO_C5,
      MediaSizeName.ISO_C3, MediaSizeName.ISO_C4,
      MediaSizeName.ISO_C6, MediaSizeName.ITALY_ENVELOPE,
      MediaSizeName.ISO_B4, MediaSizeName.ISO_B5,
      MediaSizeName.ISO_B6, MediaSizeName.ITALY_ENVELOPE,
      MediaSizeName.MONARCH_ENVELOPE, MediaSizeName.PERSONAL_ENVELOPE,
      MediaSizeName.NA_10X15_ENVELOPE, MediaSizeName.NA_9X12_ENVELOPE,
      MediaSizeName.FOLIO, MediaSizeName.ISO_B4,
      MediaSizeName.JAPANESE_POSTCARD, MediaSizeName.NA_9X11_ENVELOPE,
    };

    private static final MediaTray[] dmPaperBinToPrintService = {
      MediaTray.TOP, MediaTray.BOTTOM, MediaTray.MIDDLE,
      MediaTray.MANUAL, MediaTray.ENVELOPE, Win32MediaTray.ENVELOPE_MANUAL,
      Win32MediaTray.AUTO, Win32MediaTray.TRACTOR,
      Win32MediaTray.SMALL_FORMAT, Win32MediaTray.LARGE_FORMAT,
      MediaTray.LARGE_CAPACITY, null, null,
      MediaTray.MAIN, Win32MediaTray.FORMSOURCE,
    };

    // from wingdi.h
    private static int DM_PAPERSIZE = 0x2;
    private static int DM_PRINTQUALITY = 0x400;
    private static int DM_YRESOLUTION = 0x2000;
    private static final int DMRES_MEDIUM = -3;
    private static final int DMRES_HIGH = -4;
    private static final int DMORIENT_LANDSCAPE = 2;
    private static final int DMDUP_VERTICAL = 2;
    private static final int DMDUP_HORIZONTAL = 3;
    private static final int DMCOLLATE_TRUE = 1;
    private static final int DMCOLOR_MONOCHROME = 1;
    private static final int DMCOLOR_COLOR = 2;


    // media sizes with indices above dmPaperToPrintService' length
    private static final int DMPAPER_A2 = 66;
    private static final int DMPAPER_A6 = 70;
    private static final int DMPAPER_B6_JIS = 88;


    // Bit settings for getPrinterCapabilities which matches that
    // of native getCapabilities in WPrinterJob.cpp
    private static final int DEVCAP_COLOR = 0x0001;
    private static final int DEVCAP_DUPLEX = 0x0002;
    private static final int DEVCAP_COLLATE = 0x0004;
    private static final int DEVCAP_QUALITY = 0x0008;
    private static final int DEVCAP_POSTSCRIPT = 0x0010;

    private String printer;
    private PrinterName name;
    private String port;

    private transient PrintServiceAttributeSet lastSet;
    private transient ServiceNotifier notifier = null;

    private MediaSizeName[] mediaSizeNames;
    private MediaPrintableArea[] mediaPrintables;
    private MediaTray[] mediaTrays;
    private PrinterResolution[] printRes;
    private HashMap<MediaSizeName, MediaPrintableArea> mpaMap;
    private int nCopies;
    private int prnCaps;
    private int[] defaultSettings;

    private boolean gotTrays;
    private boolean gotCopies;
    private boolean mediaInitialized;
    private boolean mpaListInitialized;

    private ArrayList<Integer> idList;
    private MediaSize[] mediaSizes;

    private boolean isInvalid;

    Win32PrintService(String name) {
        if (name == null) {
            throw new IllegalArgumentException("null printer name");
        }
        printer = name;

        // initialize flags
        mediaInitialized = false;
        gotTrays = false;
        gotCopies = false;
        isInvalid = false;
        printRes = null;
        prnCaps = 0;
        defaultSettings = null;
        port = null;
    }

    public void invalidateService() {
        isInvalid = true;
    }

    public String getName() {
        return printer;
    }

    private PrinterName getPrinterName() {
        if (name == null) {
            name = new PrinterName(printer, null);
        }
        return name;
    }

    public int findPaperID(MediaSizeName msn) {
        if (msn instanceof Win32MediaSize) {
            Win32MediaSize winMedia = (Win32MediaSize)msn;
            return winMedia.getDMPaper();
        } else {
            for (int id=0; id<dmPaperToPrintService.length;id++) {
                if (dmPaperToPrintService[id].equals(msn)) {
                    return id+1; // DMPAPER_LETTER == 1
                }
            }
            if (msn.equals(MediaSizeName.ISO_A2)) {
                return DMPAPER_A2;
            }
            else if (msn.equals(MediaSizeName.ISO_A6)) {
                return DMPAPER_A6;
            }
            else if (msn.equals(MediaSizeName.JIS_B6)) {
                return DMPAPER_B6_JIS;
            }
        }

        // If not found in predefined Windows ID, then we search through
        // the returned IDs of the driver because they can define their own
        // unique IDs.
        initMedia();

        if ((idList != null) && (mediaSizes != null) &&
            (idList.size() == mediaSizes.length)) {
            for (int i=0; i< idList.size(); i++) {
                if (mediaSizes[i].getMediaSizeName() == msn) {
                    return idList.get(i).intValue();
                }
            }
        }
        return 0;
    }

    public int findTrayID(MediaTray tray) {

        getMediaTrays(); // make sure they are initialised.

        if (tray instanceof Win32MediaTray) {
            Win32MediaTray winTray = (Win32MediaTray)tray;
            return winTray.getDMBinID();
        }
        for (int id=0; id<dmPaperBinToPrintService.length; id++) {
            if (tray.equals(dmPaperBinToPrintService[id])) {
                return id+1; // DMBIN_FIRST = 1;
            }
        }
        return 0; // didn't find the tray
    }

    public MediaTray findMediaTray(int dmBin) {
        if (dmBin >= 1 && dmBin <= dmPaperBinToPrintService.length) {
            return dmPaperBinToPrintService[dmBin-1];
        }
        MediaTray[] trays = getMediaTrays();
        if (trays != null) {
            for (int i=0;i<trays.length;i++) {
                if(trays[i] instanceof Win32MediaTray) {
                    Win32MediaTray win32Tray = (Win32MediaTray)trays[i];
                    if (win32Tray.winID == dmBin) {
                        return win32Tray;
                    }
                }
            }
        }
        return Win32MediaTray.AUTO;
    }

    public MediaSizeName findWin32Media(int dmIndex) {
        if (dmIndex >= 1 && dmIndex <= dmPaperToPrintService.length) {
            return dmPaperToPrintService[dmIndex - 1];
        }
        switch(dmIndex) {
            /* matching media sizes with indices beyond
               dmPaperToPrintService's length */
            case DMPAPER_A2:
                return MediaSizeName.ISO_A2;
            case DMPAPER_A6:
                return MediaSizeName.ISO_A6;
            case DMPAPER_B6_JIS:
                return MediaSizeName.JIS_B6;
            default:
                return null;
        }
    }

    private boolean addToUniqueList(ArrayList<MediaSizeName> msnList,
                                    MediaSizeName mediaName) {
        MediaSizeName msn;
        for (int i=0; i< msnList.size(); i++) {
            msn = msnList.get(i);
            if (msn == mediaName) {
                return false;
            }
        }
        msnList.add(mediaName);
        return true;
    }

    private synchronized void initMedia() {
        if (mediaInitialized == true) {
            return;
        }
        mediaInitialized = true;
        int[] media = getAllMediaIDs(printer, getPort());
        if (media == null) {
            return;
        }

        ArrayList<MediaSizeName> msnList = new ArrayList<>();
        ArrayList<Win32MediaSize> trailingWmsList = new ArrayList<Win32MediaSize>();
        MediaSizeName mediaName;
        boolean added;
        boolean queryFailure = false;
        float[] prnArea;

        // Get all mediaSizes supported by the printer.
        // We convert media to ArrayList idList and pass this to the
        // function for getting mediaSizes.
        // This is to ensure that mediaSizes and media IDs have 1-1 correspondence.
        // We remove from ID list any invalid mediaSize.  Though this is rare,
        // it happens in HP 4050 German driver.

        idList = new ArrayList<>();
        for (int i=0; i < media.length; i++) {
            idList.add(Integer.valueOf(media[i]));
        }

        ArrayList<String> dmPaperNameList = new ArrayList<String>();
        mediaSizes = getMediaSizes(idList, media, dmPaperNameList);
        for (int i = 0; i < idList.size(); i++) {

            // match Win ID with our predefined ID using table
            mediaName = findWin32Media(idList.get(i).intValue());
            // Verify that this standard size is the same size as that
            // reported by the driver. This should be the case except when
            // the driver is mis-using a standard windows paper ID.
            if (mediaName != null &&
                idList.size() == mediaSizes.length) {
                MediaSize win32Size = MediaSize.getMediaSizeForName(mediaName);
                MediaSize driverSize = mediaSizes[i];
                int error = 2540; // == 1/10"
                if (Math.abs(win32Size.getX(1)-driverSize.getX(1)) > error ||
                    Math.abs(win32Size.getY(1)-driverSize.getY(1)) > error)
                {
                   mediaName = null;
                }
            }
            boolean dmPaperIDMatched = (mediaName != null);

            // No match found, then we get the MediaSizeName out of the MediaSize
            // This requires 1-1 correspondence, lengths must be checked.
            if ((mediaName == null) && (idList.size() == mediaSizes.length)) {
                mediaName = mediaSizes[i].getMediaSizeName();
            }

            // Add mediaName to the msnList
            added = false;
            if (mediaName != null) {
                added = addToUniqueList(msnList, mediaName);
            }
            if ((!dmPaperIDMatched || !added) && (idList.size() == dmPaperNameList.size())) {
                /* The following block allows to add such media names to the list, whose sizes
                 * matched with media sizes predefined in JDK, while whose paper IDs did not,
                 * or whose sizes and paper IDs both did not match with any predefined in JDK.
                 */
                Win32MediaSize wms = Win32MediaSize.findMediaName(dmPaperNameList.get(i));
                if ((wms == null) && (idList.size() == mediaSizes.length)) {
                    wms = new Win32MediaSize(dmPaperNameList.get(i), idList.get(i));
                    mediaSizes[i] = new MediaSize(mediaSizes[i].getX(MediaSize.MM),
                        mediaSizes[i].getY(MediaSize.MM), MediaSize.MM, wms);
                }
                if ((wms != null) && (wms != mediaName)) {
                    if (!added) {
                        added = addToUniqueList(msnList, mediaName = wms);
                    } else {
                        trailingWmsList.add(wms);
                    }
                }
            }
        }
        for (Win32MediaSize wms : trailingWmsList) {
            added = addToUniqueList(msnList, wms);
        }

        // init mediaSizeNames
        mediaSizeNames = new MediaSizeName[msnList.size()];
        msnList.toArray(mediaSizeNames);
    }


    /*
     * Gets a list of MediaPrintableAreas using a call to native function.
     *  msn is MediaSizeName used to get a specific printable area.  If null,
     *  it will get all the supported MediPrintableAreas.
     */
    private synchronized MediaPrintableArea[] getMediaPrintables(MediaSizeName msn)
    {
        if (msn == null)  {
            if (mpaListInitialized == true) {
                return mediaPrintables;
            }
        } else {
            // get from cached mapping of MPAs
            if (mpaMap != null && (mpaMap.get(msn) != null)) {
                MediaPrintableArea[] mpaArr = new MediaPrintableArea[1];
                mpaArr[0] = mpaMap.get(msn);
                return mpaArr;
            }
        }

        initMedia();

        if ((mediaSizeNames == null) || (mediaSizeNames.length == 0)) {
            return null;
        }

        MediaSizeName[] loopNames;
        if (msn != null) {
            loopNames = new MediaSizeName[1];
            loopNames[0] = msn;
        } else {
            loopNames = mediaSizeNames;
        }

        if (mpaMap == null) {
            mpaMap = new HashMap<>();
        }

        for (int i=0; i < loopNames.length; i++) {
            MediaSizeName mediaName = loopNames[i];

            if (mpaMap.get(mediaName) != null) {
                continue;
             }

            if (mediaName != null) {
                int defPaper = findPaperID(mediaName);
                float[] prnArea = (defPaper != 0) ? getMediaPrintableArea(printer, defPaper) : null;
                MediaPrintableArea printableArea = null;
                if (prnArea != null) {
                    try {
                        printableArea = new MediaPrintableArea(prnArea[0],
                                                               prnArea[1],
                                                               prnArea[2],
                                                               prnArea[3],
                                                 MediaPrintableArea.INCH);

                        mpaMap.put(mediaName, printableArea);
                    }
                    catch (IllegalArgumentException e) {
                    }
                } else {
                    // if getting  MPA failed, we use MediaSize
                    MediaSize ms = MediaSize.getMediaSizeForName(mediaName);

                    if (ms != null) {
                        try {
                            printableArea = new MediaPrintableArea(0, 0,
                                                     ms.getX(MediaSize.INCH),
                                                     ms.getY(MediaSize.INCH),
                                                     MediaPrintableArea.INCH);
                            mpaMap.put(mediaName, printableArea);
                        } catch (IllegalArgumentException e) {
                        }
                    }
                }
            } //mediaName != null
        }

       if (mpaMap.size() == 0) {
           return null;
       }

       if (msn != null) {
           if (mpaMap.get(msn) == null) {
               return null;
           }
           MediaPrintableArea[] mpaArr = new MediaPrintableArea[1];
           // by this time, we've already gotten the desired MPA
           mpaArr[0] = mpaMap.get(msn);
           return mpaArr;
       } else {
           mediaPrintables = mpaMap.values().toArray(new MediaPrintableArea[0]);
           mpaListInitialized = true;
           return mediaPrintables;
       }
    }


    private synchronized MediaTray[] getMediaTrays() {
        if (gotTrays == true && mediaTrays != null) {
            return mediaTrays;
        }
        String prnPort = getPort();
        int[] mediaTr = getAllMediaTrays(printer, prnPort);
        String[] winMediaTrayNames = getAllMediaTrayNames(printer, prnPort);

        if ((mediaTr == null) || (winMediaTrayNames == null)){
            return null;
        }

        /* first count how many valid bins there are so we can allocate
         * an array of the correct size
         */
        int nTray = 0;
        for (int i=0; i < mediaTr.length ; i++) {
            if (mediaTr[i] > 0) nTray++;
        }

        MediaTray[] arr = new MediaTray[nTray];
        int dmBin;

        /* Some drivers in Win 7 don't have the same length for DC_BINS and
         * DC_BINNAMES so there is no guarantee that lengths of mediaTr and
         * winMediaTrayNames are equal. To avoid getting ArrayIndexOutOfBounds,
         * we need to make sure we get the minimum of the two.
         */

        for (int i = 0, j=0; i < Math.min(mediaTr.length, winMediaTrayNames.length); i++) {
            dmBin = mediaTr[i];
            if (dmBin > 0) {
                // check for unsupported DMBINs and create new Win32MediaTray
                if ((dmBin > dmPaperBinToPrintService.length)
                    || (dmPaperBinToPrintService[dmBin-1] == null)) {
                    arr[j++] = new Win32MediaTray(dmBin, winMediaTrayNames[i]);
                } else {
                    arr[j++] = dmPaperBinToPrintService[dmBin-1];
                }
            }
            // no else - For invalid ids, just ignore it because assigning a "default"
            // value may result in duplicate trays.
        }
        mediaTrays = arr;
        gotTrays = true;
        return mediaTrays;
    }

    private boolean isSameSize(float w1, float h1, float w2, float h2) {
        float diffX = w1 - w2;
        float diffY = h1 - h2;
        // Get diff of reverse dimensions
        // EPSON Stylus COLOR 860 reverses envelope's width & height
        float diffXrev = w1 - h2;
        float diffYrev = h1 - w2;

        if (((Math.abs(diffX)<=1) && (Math.abs(diffY)<=1)) ||
            ((Math.abs(diffXrev)<=1) && (Math.abs(diffYrev)<=1))){
          return true;
        } else {
          return false;
        }
    }

    public MediaSizeName findMatchingMediaSizeNameMM (float w, float h){
        if (predefMedia != null) {
            for (int k=0; k<predefMedia.length;k++) {
                if (predefMedia[k] == null) {
                    continue;
                }

                if (isSameSize(predefMedia[k].getX(MediaSize.MM),
                               predefMedia[k].getY(MediaSize.MM),
                               w, h)) {
                  return predefMedia[k].getMediaSizeName();
                }
            }
        }
        return null;
    }


    private MediaSize[] getMediaSizes(ArrayList<Integer> idList, int[] media,
                                      ArrayList<String> dmPaperNameList) {
        if (dmPaperNameList == null) {
            dmPaperNameList = new ArrayList<String>();
        }

        String prnPort = getPort();
        int[] mediaSz = getAllMediaSizes(printer, prnPort);
        String[] winMediaNames = getAllMediaNames(printer, prnPort);
        MediaSizeName msn = null;
        MediaSize ms = null;
        float wid, ht;

        if ((mediaSz == null) || (winMediaNames == null)) {
            return null;
        }

        int nMedia = mediaSz.length/2;
        ArrayList<MediaSize> msList = new ArrayList<>();

        for (int i = 0; i < nMedia; i++, ms=null) {
            wid = mediaSz[i*2]/10f;
            ht = mediaSz[i*2+1]/10f;

            // Make sure to validate wid & ht.
            // HP LJ 4050 (german) causes IAE in Sonderformat paper, wid & ht
            // returned is not constant.
            if ((wid <= 0) || (ht <= 0)) {
                //Remove corresponding ID from list
                if (nMedia == media.length) {
                    Integer remObj = Integer.valueOf(media[i]);
                    idList.remove(idList.indexOf(remObj));
                }
                continue;
            }
            // Find matching media using dimensions.
            // This call matches only with our own predefined sizes.
            msn = findMatchingMediaSizeNameMM(wid, ht);
            if (msn != null) {
                ms = MediaSize.getMediaSizeForName(msn);
            }

            if (ms != null) {
                msList.add(ms);
                dmPaperNameList.add(winMediaNames[i]);
            } else {
                Win32MediaSize wms = Win32MediaSize.findMediaName(winMediaNames[i]);
                if (wms == null) {
                    wms = new Win32MediaSize(winMediaNames[i], media[i]);
                }
                try {
                    ms = new MediaSize(wid, ht, MediaSize.MM, wms);
                    msList.add(ms);
                    dmPaperNameList.add(winMediaNames[i]);
                } catch(IllegalArgumentException e) {
                    if (nMedia == media.length) {
                        Integer remObj = Integer.valueOf(media[i]);
                        idList.remove(idList.indexOf(remObj));
                    }
                }
            }
        }

        MediaSize[] arr2 = new MediaSize[msList.size()];
        msList.toArray(arr2);

        return arr2;
    }

    private PrinterIsAcceptingJobs getPrinterIsAcceptingJobs() {
        if (getJobStatus(printer, 2) != 1) {
            return PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS;
        }
        else {
            return PrinterIsAcceptingJobs.ACCEPTING_JOBS;
        }
    }

    private PrinterState getPrinterState() {
        if (isInvalid) {
            return PrinterState.STOPPED;
        } else {
            return null;
        }
    }

    private PrinterStateReasons getPrinterStateReasons() {
        if (isInvalid) {
            PrinterStateReasons psr = new PrinterStateReasons();
            psr.put(PrinterStateReason.SHUTDOWN, Severity.ERROR);
            return psr;
        } else {
            return null;
        }
    }

    private QueuedJobCount getQueuedJobCount() {

        int count = getJobStatus(printer, 1);
        if (count != -1) {
            return new QueuedJobCount(count);
        }
        else {
            return new QueuedJobCount(0);
        }
    }

    private boolean isSupportedCopies(Copies copies) {
        synchronized (this) {
            if (gotCopies == false) {
                nCopies = getCopiesSupported(printer, getPort());
                gotCopies = true;
            }
        }
        int numCopies = copies.getValue();
        return (numCopies > 0 && numCopies <= nCopies);
    }

    private boolean isSupportedMedia(MediaSizeName msn) {

        initMedia();

        if (mediaSizeNames != null) {
            for (int i=0; i<mediaSizeNames.length; i++) {
                if (msn.equals(mediaSizeNames[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSupportedMediaPrintableArea(MediaPrintableArea mpa) {

        getMediaPrintables(null);
        int units = MediaPrintableArea.INCH;

        if (mediaPrintables != null) {
            for (int i=0; i<mediaPrintables.length; i++) {
                if ((mpa.getX(units) >= mediaPrintables[i].getX(units)) &&
                    (mpa.getY(units) >= mediaPrintables[i].getY(units)) &&
                    (mpa.getX(units) + mpa.getWidth(units) <=
                            mediaPrintables[i].getX(units) +
                            mediaPrintables[i].getWidth(units)) &&
                    (mpa.getY(units) + mpa.getHeight(units) <=
                            mediaPrintables[i].getY(units) +
                            mediaPrintables[i].getHeight(units))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSupportedMediaTray(MediaTray msn) {
        MediaTray[] trays = getMediaTrays();

        if (trays != null) {
            for (int i=0; i<trays.length; i++) {
                if (msn.equals(trays[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getPrinterCapabilities() {
        if (prnCaps == 0) {
            prnCaps = getCapabilities(printer, getPort());
        }
        return prnCaps;
    }

    private String getPort() {
        if (port == null) {
            port = getPrinterPort(printer);
        }
        return port;
    }

   /*
    * NOTE: defaults indices must match those in WPrinterJob.cpp
    */
    private int[] getDefaultPrinterSettings() {
        if (defaultSettings == null) {
            defaultSettings = getDefaultSettings(printer, getPort());
        }
        return defaultSettings;
    }

    private PrinterResolution[] getPrintResolutions() {
        if (printRes == null) {
            int[] prnRes = getAllResolutions(printer, getPort());
            if (prnRes == null) {
                printRes = new PrinterResolution[0];
            } else {
                int nRes = prnRes.length/2;

                ArrayList<PrinterResolution> arrList = new ArrayList<>();
                PrinterResolution pr;

                for (int i=0; i<nRes; i++) {
                  try {
                        pr = new PrinterResolution(prnRes[i*2],
                                       prnRes[i*2+1], PrinterResolution.DPI);
                        arrList.add(pr);
                    } catch (IllegalArgumentException e) {
                    }
                }

                printRes = arrList.toArray(new PrinterResolution[arrList.size()]);
            }
        }
        return printRes;
    }

    private boolean isSupportedResolution(PrinterResolution res) {
        PrinterResolution[] supportedRes = getPrintResolutions();
        if (supportedRes != null) {
            for (int i=0; i<supportedRes.length; i++) {
                if (res.equals(supportedRes[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public DocPrintJob createPrintJob() {
      @SuppressWarnings("removal")
      SecurityManager security = System.getSecurityManager();
      if (security != null) {
        security.checkPrintJobAccess();
      }
        return new Win32PrintJob(this);
    }

    private PrintServiceAttributeSet getDynamicAttributes() {
        PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
        attrs.add(getPrinterIsAcceptingJobs());
        attrs.add(getQueuedJobCount());
        return attrs;
    }

    public PrintServiceAttributeSet getUpdatedAttributes() {
        PrintServiceAttributeSet currSet = getDynamicAttributes();
        if (lastSet == null) {
            lastSet = currSet;
            return AttributeSetUtilities.unmodifiableView(currSet);
        } else {
            PrintServiceAttributeSet updates =
                new HashPrintServiceAttributeSet();
            Attribute []attrs =  currSet.toArray();
            for (int i=0; i<attrs.length; i++) {
                Attribute attr = attrs[i];
                if (!lastSet.containsValue(attr)) {
                    updates.add(attr);
                }
            }
            lastSet = currSet;
            return AttributeSetUtilities.unmodifiableView(updates);
        }
    }

    public void wakeNotifier() {
        synchronized (this) {
            if (notifier != null) {
                notifier.wake();
            }
        }
    }

    public void addPrintServiceAttributeListener(PrintServiceAttributeListener
                                                 listener) {
        synchronized (this) {
            if (listener == null) {
                return;
            }
            if (notifier == null) {
                notifier = new ServiceNotifier(this);
            }
            notifier.addListener(listener);
        }
    }

    public void removePrintServiceAttributeListener(
                                      PrintServiceAttributeListener listener) {
        synchronized (this) {
            if (listener == null || notifier == null ) {
                return;
            }
            notifier.removeListener(listener);
            if (notifier.isEmpty()) {
                notifier.stopNotifier();
                notifier = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PrintServiceAttribute> T
        getAttribute(Class<T> category)
    {
        if (category == null) {
            throw new NullPointerException("category");
        }
        if (!(PrintServiceAttribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException("Not a PrintServiceAttribute");
        }
        if (category == ColorSupported.class) {
            int caps = getPrinterCapabilities();
            if ((caps & DEVCAP_COLOR) != 0) {
                return (T)ColorSupported.SUPPORTED;
            } else {
                return (T)ColorSupported.NOT_SUPPORTED;
            }
        } else if (category == PrinterName.class) {
            return (T)getPrinterName();
        } else if (category == PrinterState.class) {
            return (T)getPrinterState();
        } else if (category == PrinterStateReasons.class) {
            return (T)getPrinterStateReasons();
        } else if (category == QueuedJobCount.class) {
            return (T)getQueuedJobCount();
        } else if (category == PrinterIsAcceptingJobs.class) {
            return (T)getPrinterIsAcceptingJobs();
        } else {
            return null;
        }
    }

    public PrintServiceAttributeSet getAttributes() {

        PrintServiceAttributeSet attrs = new  HashPrintServiceAttributeSet();
        attrs.add(getPrinterName());
        attrs.add(getPrinterIsAcceptingJobs());
        PrinterState prnState = getPrinterState();
        if (prnState != null) {
            attrs.add(prnState);
        }
        PrinterStateReasons prnStateReasons = getPrinterStateReasons();
        if (prnStateReasons != null) {
            attrs.add(prnStateReasons);
        }
        attrs.add(getQueuedJobCount());
        int caps = getPrinterCapabilities();
        if ((caps & DEVCAP_COLOR) != 0) {
            attrs.add(ColorSupported.SUPPORTED);
        } else {
            attrs.add(ColorSupported.NOT_SUPPORTED);
        }

        return AttributeSetUtilities.unmodifiableView(attrs);
    }

    public DocFlavor[] getSupportedDocFlavors() {
        int len = supportedFlavors.length;
        DocFlavor[] supportedDocFlavors;
        int caps = getPrinterCapabilities();
        // doc flavors supported
        // if PostScript is supported
        if ((caps & DEVCAP_POSTSCRIPT) != 0) {
            supportedDocFlavors = new DocFlavor[len+3];
            System.arraycopy(supportedFlavors, 0, supportedDocFlavors, 0, len);
            supportedDocFlavors[len] = DocFlavor.BYTE_ARRAY.POSTSCRIPT;
            supportedDocFlavors[len+1] = DocFlavor.INPUT_STREAM.POSTSCRIPT;
            supportedDocFlavors[len+2] = DocFlavor.URL.POSTSCRIPT;
        } else {
            supportedDocFlavors = new DocFlavor[len];
            System.arraycopy(supportedFlavors, 0, supportedDocFlavors, 0, len);
        }
        return supportedDocFlavors;
    }

    public boolean isDocFlavorSupported(DocFlavor flavor) {
        /* To avoid a native query which may be time-consuming
         * do not invoke native unless postscript support is being queried.
         * Instead just check the ones we 'always' support
         */
        DocFlavor[] supportedDocFlavors;
        if (isPostScriptFlavor(flavor)) {
            supportedDocFlavors = getSupportedDocFlavors();
        } else {
            supportedDocFlavors = supportedFlavors;
        }
        for (int f=0; f<supportedDocFlavors.length; f++) {
            if (flavor.equals(supportedDocFlavors[f])) {
                return true;
            }
        }
        return false;
    }

    public Class<?>[] getSupportedAttributeCategories() {
        ArrayList<Class<?>> categList = new ArrayList<>(otherAttrCats.length+3);
        for (int i=0; i < otherAttrCats.length; i++) {
            categList.add(otherAttrCats[i]);
        }

        int caps = getPrinterCapabilities();

        if ((caps & DEVCAP_DUPLEX) != 0) {
            categList.add(Sides.class);
        }

        if ((caps & DEVCAP_QUALITY) != 0) {
            int[] defaults = getDefaultPrinterSettings();
            // Added check: if supported, we should be able to get the default.
            if ((defaults[3] >= DMRES_HIGH) && (defaults[3] < 0)) {
                categList.add(PrintQuality.class);
            }
        }

        PrinterResolution[] supportedRes = getPrintResolutions();
        if ((supportedRes!=null) && (supportedRes.length>0)) {
            categList.add(PrinterResolution.class);
        }

        if (GraphicsEnvironment.isHeadless() == false) {
            categList.add(DialogOwner.class);
            categList.add(DialogTypeSelection.class);
        }
        return categList.toArray(new Class<?>[categList.size()]);
    }

    public boolean
        isAttributeCategorySupported(Class<? extends Attribute> category)
    {

        if (category == null) {
            throw new NullPointerException("null category");
        }

        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(category +
                                               " is not an Attribute");
        }

        Class<?>[] classList = getSupportedAttributeCategories();
        for (int i = 0; i < classList.length; i++) {
            if (category.equals(classList[i])) {
                return true;
            }
        }

        return false;
    }

    public Object
        getDefaultAttributeValue(Class<? extends Attribute> category)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (!Attribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category +
                                               " is not an Attribute");
        }

        if (!isAttributeCategorySupported(category)) {
            return null;
        }

        int[] defaults = getDefaultPrinterSettings();
        // indices must match those in WPrinterJob.cpp
        int defPaper = defaults[0];
        int defYRes = defaults[2];
        int defQuality = defaults[3];
        int defCopies = defaults[4];
        int defOrient = defaults[5];
        int defSides = defaults[6];
        int defCollate = defaults[7];
        int defColor = defaults[8];

        if (category == Copies.class) {
            if (defCopies > 0) {
                return new Copies(defCopies);
            } else {
                return new Copies(1);
            }
        } else if (category == Chromaticity.class) {
            if (defColor == DMCOLOR_COLOR) {
                return Chromaticity.COLOR;
            } else {
                return Chromaticity.MONOCHROME;
            }
        } else if (category == JobName.class) {
            return new JobName("Java Printing", null);
        } else if (category == OrientationRequested.class) {
            if (defOrient == DMORIENT_LANDSCAPE) {
                return OrientationRequested.LANDSCAPE;
            } else {
                return OrientationRequested.PORTRAIT;
            }
        } else if (category == PageRanges.class) {
            return new PageRanges(1, Integer.MAX_VALUE);
        } else if (category == Media.class) {
            MediaSizeName msn = findWin32Media(defPaper);
            if (msn != null) {
                if (!isSupportedMedia(msn) && mediaSizeNames != null) {
                    msn = mediaSizeNames[0];
                    defPaper = findPaperID(msn);
                }
                return msn;
             } else {
                 initMedia();
                 if ((mediaSizeNames != null) && (mediaSizeNames.length > 0)) {
                     // if 'mediaSizeNames' is not null, idList and mediaSizes
                     // cannot be null but to be safe, add a check
                     if ((idList != null) && (mediaSizes != null) &&
                         (idList.size() == mediaSizes.length)) {
                         Integer defIdObj = Integer.valueOf(defPaper);
                         int index = idList.indexOf(defIdObj);
                         if (index>=0 && index<mediaSizes.length) {
                             return mediaSizes[index].getMediaSizeName();
                         }
                     }

                     return mediaSizeNames[0];
                 }
             }
        } else if (category == MediaPrintableArea.class) {
            /* Verify defPaper */
            MediaSizeName msn = findWin32Media(defPaper);
            if (msn != null &&
                !isSupportedMedia(msn) && mediaSizeNames != null) {
                defPaper = findPaperID(mediaSizeNames[0]);
            }
            float[] prnArea = getMediaPrintableArea(printer, defPaper);
            if (prnArea != null) {
                MediaPrintableArea printableArea = null;
                try {
                    printableArea = new MediaPrintableArea(prnArea[0],
                                                           prnArea[1],
                                                           prnArea[2],
                                                           prnArea[3],
                                                           MediaPrintableArea.INCH);
                } catch (IllegalArgumentException e) {
                }
                return printableArea;
            }
            return null;
        } else if (category == SunAlternateMedia.class) {
            return null;
        } else if (category == Destination.class) {
            try {
                return new Destination((new File("out.prn")).toURI());
            } catch (SecurityException se) {
                try {
                    return new Destination(new URI("file:out.prn"));
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        } else if (category == Sides.class) {
            switch(defSides) {
            case DMDUP_VERTICAL :
                return Sides.TWO_SIDED_LONG_EDGE;
            case DMDUP_HORIZONTAL :
                return Sides.TWO_SIDED_SHORT_EDGE;
            default :
                return Sides.ONE_SIDED;
            }
        } else if (category == PrinterResolution.class) {
            int yRes = defYRes;
            int xRes = defQuality;
            if ((xRes < 0) || (yRes < 0)) {
                int res = (yRes > xRes) ? yRes : xRes;
                if (res > 0) {
                 return new PrinterResolution(res, res, PrinterResolution.DPI);
                }
            }
            else {
               return new PrinterResolution(xRes, yRes, PrinterResolution.DPI);
            }
        } else if (category == ColorSupported.class) {
            int caps = getPrinterCapabilities();
            if ((caps & DEVCAP_COLOR) != 0) {
                return ColorSupported.SUPPORTED;
            } else {
                return ColorSupported.NOT_SUPPORTED;
            }
        } else if (category == PrintQuality.class) {
            if ((defQuality < 0) && (defQuality >= DMRES_HIGH)) {
                switch (defQuality) {
                case DMRES_HIGH:
                    return PrintQuality.HIGH;
                case DMRES_MEDIUM:
                    return PrintQuality.NORMAL;
                default:
                    return PrintQuality.DRAFT;
                }
            }
        } else if (category == RequestingUserName.class) {
            String userName = "";
            try {
              userName = System.getProperty("user.name", "");
            } catch (SecurityException se) {
            }
            return new RequestingUserName(userName, null);
        } else if (category == SheetCollate.class) {
            if (defCollate == DMCOLLATE_TRUE) {
                return SheetCollate.COLLATED;
            } else {
                return SheetCollate.UNCOLLATED;
            }
        } else if (category == Fidelity.class) {
            return Fidelity.FIDELITY_FALSE;
        }
        return null;
    }

    private boolean isPostScriptFlavor(DocFlavor flavor) {
        if (flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT) ||
            flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
            flavor.equals(DocFlavor.URL.POSTSCRIPT)) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean isPSDocAttr(Class<?> category) {
        if (category == OrientationRequested.class || category == Copies.class) {
                return true;
        }
        else {
            return false;
        }
    }

    private boolean isAutoSense(DocFlavor flavor) {
        if (flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
            flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
            flavor.equals(DocFlavor.URL.AUTOSENSE)) {
            return true;
        }
        else {
            return false;
        }
    }

    public Object
        getSupportedAttributeValues(Class<? extends Attribute> category,
                                    DocFlavor flavor,
                                    AttributeSet attributes)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (!Attribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category +
                                             " does not implement Attribute");
        }
        if (flavor != null) {
            if (!isDocFlavorSupported(flavor)) {
                throw new IllegalArgumentException(flavor +
                                                  " is an unsupported flavor");
                // if postscript & category is already specified within the
                //  PostScript data we return null
            } else if (isAutoSense(flavor) ||(isPostScriptFlavor(flavor) &&
                       (isPSDocAttr(category)))){
                return null;
            }
        }
        if (!isAttributeCategorySupported(category)) {
            return null;
        }

        if (category == JobName.class) {
            return new JobName("Java Printing", null);
        } else if (category == RequestingUserName.class) {
          String userName = "";
          try {
            userName = System.getProperty("user.name", "");
          } catch (SecurityException se) {
          }
            return new RequestingUserName(userName, null);
        } else if (category == ColorSupported.class) {
            int caps = getPrinterCapabilities();
            if ((caps & DEVCAP_COLOR) != 0) {
                return ColorSupported.SUPPORTED;
            } else {
                return ColorSupported.NOT_SUPPORTED;
            }
        } else if (category == Chromaticity.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                flavor.equals(DocFlavor.URL.GIF) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                flavor.equals(DocFlavor.URL.JPEG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                flavor.equals(DocFlavor.URL.PNG)) {
                int caps = getPrinterCapabilities();
                if ((caps & DEVCAP_COLOR) == 0) {
                    Chromaticity []arr = new Chromaticity[1];
                    arr[0] = Chromaticity.MONOCHROME;
                    return (arr);
                } else {
                    Chromaticity []arr = new Chromaticity[2];
                    arr[0] = Chromaticity.MONOCHROME;
                    arr[1] = Chromaticity.COLOR;
                    return (arr);
                }
            } else {
                return null;
            }
        } else if (category == Destination.class) {
            try {
                return new Destination((new File("out.prn")).toURI());
            } catch (SecurityException se) {
                try {
                    return new Destination(new URI("file:out.prn"));
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        } else if (category == OrientationRequested.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
                flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
                flavor.equals(DocFlavor.URL.GIF) ||
                flavor.equals(DocFlavor.URL.JPEG) ||
                flavor.equals(DocFlavor.URL.PNG)) {
                OrientationRequested []arr = new OrientationRequested[3];
                arr[0] = OrientationRequested.PORTRAIT;
                arr[1] = OrientationRequested.LANDSCAPE;
                arr[2] = OrientationRequested.REVERSE_LANDSCAPE;
                return arr;
            } else {
                return null;
            }
        } else if ((category == Copies.class) ||
                   (category == CopiesSupported.class)) {
            synchronized (this) {
                if (gotCopies == false) {
                    nCopies = getCopiesSupported(printer, getPort());
                    gotCopies = true;
                }
            }
            return new CopiesSupported(1, nCopies);
        } else if (category == Media.class) {

            initMedia();

            int len = (mediaSizeNames == null) ? 0 : mediaSizeNames.length;

            MediaTray[] trays = getMediaTrays();

            len += (trays == null) ? 0 : trays.length;

            Media []arr = new Media[len];
            if (mediaSizeNames != null) {
                System.arraycopy(mediaSizeNames, 0, arr,
                                 0, mediaSizeNames.length);
            }
            if (trays != null) {
                System.arraycopy(trays, 0, arr,
                                 len - trays.length, trays.length);
            }
            return arr;
        } else if (category == MediaPrintableArea.class) {
            // if getting printable area for a specific media size
            Media mediaName = null;
            if ((attributes != null) &&
                ((mediaName =
                  (Media)attributes.get(Media.class)) != null)) {

                if (!(mediaName instanceof MediaSizeName)) {
                    // if an instance of MediaTray, fall thru returning
                    // all MediaPrintableAreas
                    mediaName = null;
                }
            }

            MediaPrintableArea[] mpas =
                                  getMediaPrintables((MediaSizeName)mediaName);
            if (mpas != null) {
                MediaPrintableArea[] arr = new MediaPrintableArea[mpas.length];
                System.arraycopy(mpas, 0, arr, 0, mpas.length);
                return arr;
            } else {
                return null;
            }
        } else if (category == SunAlternateMedia.class) {
            return new SunAlternateMedia(
                              (Media)getDefaultAttributeValue(Media.class));
        } else if (category == PageRanges.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                PageRanges []arr = new PageRanges[1];
                arr[0] = new PageRanges(1, Integer.MAX_VALUE);
                return arr;
            } else {
                return null;
            }
        } else if (category == PrinterResolution.class) {
            PrinterResolution[] supportedRes = getPrintResolutions();
            if (supportedRes == null) {
                return null;
            }
            PrinterResolution []arr =
                new PrinterResolution[supportedRes.length];
            System.arraycopy(supportedRes, 0, arr, 0, supportedRes.length);
            return arr;
        } else if (category == Sides.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                Sides []arr = new Sides[3];
                arr[0] = Sides.ONE_SIDED;
                arr[1] = Sides.TWO_SIDED_LONG_EDGE;
                arr[2] = Sides.TWO_SIDED_SHORT_EDGE;
                return arr;
            } else {
                return null;
            }
        } else if (category == PrintQuality.class) {
            PrintQuality []arr = new PrintQuality[3];
            arr[0] = PrintQuality.DRAFT;
            arr[1] = PrintQuality.HIGH;
            arr[2] = PrintQuality.NORMAL;
            return arr;
        } else if (category == SheetCollate.class) {
            if (flavor == null ||
                (flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                 flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                SheetCollate []arr = new SheetCollate[2];
                arr[0] = SheetCollate.COLLATED;
                arr[1] = SheetCollate.UNCOLLATED;
                return arr;
            } else {
                return null;
            }
        } else if (category == Fidelity.class) {
            Fidelity []arr = new Fidelity[2];
            arr[0] = Fidelity.FIDELITY_FALSE;
            arr[1] = Fidelity.FIDELITY_TRUE;
            return arr;
        } else {
            return null;
        }
    }

    public boolean isAttributeValueSupported(Attribute attr,
                                             DocFlavor flavor,
                                             AttributeSet attributes) {

        if (attr == null) {
            throw new NullPointerException("null attribute");
        }
        Class<? extends Attribute> category = attr.getCategory();
        if (flavor != null) {
            if (!isDocFlavorSupported(flavor)) {
                throw new IllegalArgumentException(flavor +
                                                   " is an unsupported flavor");
                // if postscript & category is already specified within the PostScript data
                // we return false
            } else if (isAutoSense(flavor) || (isPostScriptFlavor(flavor) &&
                       (isPSDocAttr(category)))) {
                return false;
            }
        }

        if (!isAttributeCategorySupported(category)) {
            return false;
        }
        else if (category == Chromaticity.class) {
            if ((flavor == null) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                flavor.equals(DocFlavor.URL.GIF) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                flavor.equals(DocFlavor.URL.JPEG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                flavor.equals(DocFlavor.URL.PNG)) {
                int caps = getPrinterCapabilities();
                if ((caps & DEVCAP_COLOR) != 0) {
                    return true;
                } else {
                    return attr == Chromaticity.MONOCHROME;
                }
            } else {
                return false;
            }
        } else if (category == Copies.class) {
            return isSupportedCopies((Copies)attr);

        } else if (category == Destination.class) {
            URI uri = ((Destination)attr).getURI();
            if ("file".equals(uri.getScheme()) &&
                !uri.getSchemeSpecificPart().isEmpty()) {
                return true;
            } else {
            return false;
            }

        } else if (category == Media.class) {
            if (attr instanceof MediaSizeName) {
                return isSupportedMedia((MediaSizeName)attr);
            }
            if (attr instanceof MediaTray) {
                return isSupportedMediaTray((MediaTray)attr);
            }

        } else if (category == MediaPrintableArea.class) {
            return isSupportedMediaPrintableArea((MediaPrintableArea)attr);

        } else if (category == SunAlternateMedia.class) {
            Media media = ((SunAlternateMedia)attr).getMedia();
            return isAttributeValueSupported(media, flavor, attributes);

        } else if (category == PageRanges.class ||
                   category == SheetCollate.class ||
                   category == Sides.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (category == PrinterResolution.class) {
            if (attr instanceof PrinterResolution) {
                return isSupportedResolution((PrinterResolution)attr);
            }
        } else if (category == OrientationRequested.class) {
            if (attr == OrientationRequested.REVERSE_PORTRAIT ||
                (flavor != null) &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
                flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
                flavor.equals(DocFlavor.URL.GIF) ||
                flavor.equals(DocFlavor.URL.JPEG) ||
                flavor.equals(DocFlavor.URL.PNG))) {
                return false;
            }

        } else if (category == ColorSupported.class) {
            int caps = getPrinterCapabilities();
            boolean isColorSup = ((caps & DEVCAP_COLOR) != 0);
            if  ((!isColorSup && (attr == ColorSupported.SUPPORTED)) ||
                (isColorSup && (attr == ColorSupported.NOT_SUPPORTED))) {
                return false;
            }
        } else if (category == DialogTypeSelection.class) {
            return true; // isHeadless was checked by category support
        } else if (category == DialogOwner.class) {
            DialogOwner owner = (DialogOwner)attr;
            DialogTypeSelection dts = (attributes == null) ? null :
                (DialogTypeSelection)attributes.get(DialogTypeSelection.class);
            if (dts == DialogTypeSelection.NATIVE) {
                return DialogOwnerAccessor.getID(owner) != 0;
            } else {
               if (DialogOwnerAccessor.getID(owner) != 0) {
                  return false;
               } else if (owner.getOwner() != null) {
                   return true;
               } else {
                   return Toolkit.getDefaultToolkit().isAlwaysOnTopSupported();
               }
            }
        }
        return true;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
                                                 AttributeSet attributes) {

        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("flavor " + flavor +
                                               " is not supported");
        }

        if (attributes == null) {
            return null;
        }

        Attribute attr;
        AttributeSet unsupp = new HashAttributeSet();
        Attribute []attrs = attributes.toArray();
        for (int i=0; i<attrs.length; i++) {
            try {
                attr = attrs[i];
                if (!isAttributeCategorySupported(attr.getCategory())) {
                    unsupp.add(attr);
                }
                else if (!isAttributeValueSupported(attr, flavor, attributes)) {
                    unsupp.add(attr);
                }
            } catch (ClassCastException e) {
            }
        }
        if (unsupp.isEmpty()) {
            return null;
        } else {
            return unsupp;
        }
    }

    private Win32DocumentPropertiesUI docPropertiesUI = null;

    private static class Win32DocumentPropertiesUI
        extends DocumentPropertiesUI {

        Win32PrintService service;

        private Win32DocumentPropertiesUI(Win32PrintService s) {
            service = s;
        }

        public PrintRequestAttributeSet
            showDocumentProperties(PrinterJob job,
                                   Window owner,
                                   PrintService service,
                                   PrintRequestAttributeSet aset) {

            if (!(job instanceof WPrinterJob)) {
                return null;
            }
            WPrinterJob wJob = (WPrinterJob)job;
            return wJob.showDocumentProperties(owner, service, aset);
        }
    }

    private synchronized DocumentPropertiesUI getDocumentPropertiesUI() {
        return new Win32DocumentPropertiesUI(this);
    }

    private static class Win32ServiceUIFactory extends ServiceUIFactory {

        Win32PrintService service;

        Win32ServiceUIFactory(Win32PrintService s) {
            service = s;
        }

        public Object getUI(int role, String ui) {
            if (role <= ServiceUIFactory.MAIN_UIROLE) {
                return null;
            }
            if (role == DocumentPropertiesUI.DOCUMENTPROPERTIES_ROLE &&
                DocumentPropertiesUI.DOCPROPERTIESCLASSNAME.equals(ui))
            {
                return service.getDocumentPropertiesUI();
            }
            throw new IllegalArgumentException("Unsupported role");
        }

        public String[] getUIClassNamesForRole(int role) {

            if (role <= ServiceUIFactory.MAIN_UIROLE) {
                return null;
            }
            if (role == DocumentPropertiesUI.DOCUMENTPROPERTIES_ROLE) {
                String[] names = new String[0];
                names[0] = DocumentPropertiesUI.DOCPROPERTIESCLASSNAME;
                return names;
            }
            throw new IllegalArgumentException("Unsupported role");
        }
    }

    private Win32ServiceUIFactory uiFactory = null;

    public synchronized ServiceUIFactory getServiceUIFactory() {
        if (uiFactory == null) {
            uiFactory = new Win32ServiceUIFactory(this);
        }
        return uiFactory;
    }

    public String toString() {
        return "Win32 Printer : " + getName();
    }

    public boolean equals(Object obj) {
        return  (obj == this ||
                 (obj instanceof Win32PrintService &&
                  ((Win32PrintService)obj).getName().equals(getName())));
    }

   public int hashCode() {
        return this.getClass().hashCode()+getName().hashCode();
    }

    public boolean usesClass(Class<?> c) {
        return (c == sun.awt.windows.WPrinterJob.class);
    }

    private native int[] getAllMediaIDs(String printerName, String port);
    private native int[] getAllMediaSizes(String printerName, String port);
    private native int[] getAllMediaTrays(String printerName, String port);
    private native float[] getMediaPrintableArea(String printerName,
                                                 int paperSize);
    private native String[] getAllMediaNames(String printerName, String port);
    private native String[] getAllMediaTrayNames(String printerName, String port);
    private native int getCopiesSupported(String printerName, String port);
    private native int[] getAllResolutions(String printerName, String port);
    private native int getCapabilities(String printerName, String port);

    private native int[] getDefaultSettings(String printerName, String port);
    private native int getJobStatus(String printerName, int type);
    private native String getPrinterPort(String printerName);
}

@SuppressWarnings("serial") // JDK implementation class
class Win32MediaSize extends MediaSizeName {
    private static ArrayList<String> winStringTable = new ArrayList<>();
    private static ArrayList<Win32MediaSize> winEnumTable = new ArrayList<>();
    private static MediaSize[] predefMedia;

    private int dmPaperID; // driver ID for this paper.

    private Win32MediaSize(int x) {
        super(x);

    }

    private static synchronized int nextValue(String name) {
      winStringTable.add(name);
      return (winStringTable.size()-1);
    }

    public static synchronized Win32MediaSize findMediaName(String name) {
        int nameIndex = winStringTable.indexOf(name);
        if (nameIndex != -1) {
            return winEnumTable.get(nameIndex);
        }
        return null;
    }

    public static MediaSize[] getPredefMedia() {
        return predefMedia;
    }

    public Win32MediaSize(String name, int dmPaper) {
        super(nextValue(name));
        dmPaperID = dmPaper;
        winEnumTable.add(this);
    }

    private MediaSizeName[] getSuperEnumTable() {
      return (MediaSizeName[])super.getEnumValueTable();
    }

    static {
         /* initialize predefMedia */
        {
            Win32MediaSize winMedia = new Win32MediaSize(-1);

            // cannot call getSuperEnumTable directly because of static context
            MediaSizeName[] enumMedia = winMedia.getSuperEnumTable();
            if (enumMedia != null) {
                predefMedia = new MediaSize[enumMedia.length];

                for (int i=0; i<enumMedia.length; i++) {
                    predefMedia[i] = MediaSize.getMediaSizeForName(enumMedia[i]);
                }
            }
        }
    }

    int getDMPaper() {
        return dmPaperID;
    }

    protected String[] getStringTable() {
      String[] nameTable = new String[winStringTable.size()];
      return winStringTable.toArray(nameTable);
    }

    protected EnumSyntax[] getEnumValueTable() {
      MediaSizeName[] enumTable = new MediaSizeName[winEnumTable.size()];
      return winEnumTable.toArray(enumTable);
    }

}
