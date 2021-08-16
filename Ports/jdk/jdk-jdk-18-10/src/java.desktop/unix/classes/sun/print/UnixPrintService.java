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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.print.attribute.standard.Severity;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintServiceAttributeListener;


public class UnixPrintService implements PrintService, AttributeUpdater,
                                         SunPrinterJobService {

    /* define doc flavors for text types in the default encoding of
     * this platform since we can always read those.
     */
    private static DocFlavor textByteFlavor;

    private static DocFlavor[] supportedDocFlavors = null;
    private static final DocFlavor[] supportedDocFlavorsInit = {
         DocFlavor.BYTE_ARRAY.POSTSCRIPT,
         DocFlavor.INPUT_STREAM.POSTSCRIPT,
         DocFlavor.URL.POSTSCRIPT,
         DocFlavor.BYTE_ARRAY.GIF,
         DocFlavor.INPUT_STREAM.GIF,
         DocFlavor.URL.GIF,
         DocFlavor.BYTE_ARRAY.JPEG,
         DocFlavor.INPUT_STREAM.JPEG,
         DocFlavor.URL.JPEG,
         DocFlavor.BYTE_ARRAY.PNG,
         DocFlavor.INPUT_STREAM.PNG,
         DocFlavor.URL.PNG,

         DocFlavor.CHAR_ARRAY.TEXT_PLAIN,
         DocFlavor.READER.TEXT_PLAIN,
         DocFlavor.STRING.TEXT_PLAIN,

         DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
         DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16,
         DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16BE,
         DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16LE,
         DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,


         DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8,
         DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16,
         DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16BE,
         DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16LE,
         DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII,


         DocFlavor.URL.TEXT_PLAIN_UTF_8,
         DocFlavor.URL.TEXT_PLAIN_UTF_16,
         DocFlavor.URL.TEXT_PLAIN_UTF_16BE,
         DocFlavor.URL.TEXT_PLAIN_UTF_16LE,
         DocFlavor.URL.TEXT_PLAIN_US_ASCII,

         DocFlavor.SERVICE_FORMATTED.PAGEABLE,
         DocFlavor.SERVICE_FORMATTED.PRINTABLE,

         DocFlavor.BYTE_ARRAY.AUTOSENSE,
         DocFlavor.URL.AUTOSENSE,
         DocFlavor.INPUT_STREAM.AUTOSENSE
    };

    private static final DocFlavor[] supportedHostDocFlavors = {
        DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
        DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,
        DocFlavor.URL.TEXT_PLAIN_HOST
    };

    String[] lpcStatusCom = {
      "",
      "| grep -E '^[ 0-9a-zA-Z_-]*@' | awk '{print $2, $3}'"
    };

    String[] lpcQueueCom = {
      "",
      "| grep -E '^[ 0-9a-zA-Z_-]*@' | awk '{print $4}'"
    };

    @SuppressWarnings("removal")
    private static String encoding = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("file.encoding"));

    /* let's try to support a few of these */
    private static final Class<?>[] serviceAttrCats = {
        PrinterName.class,
        PrinterIsAcceptingJobs.class,
        QueuedJobCount.class,
    };

    /*  it turns out to be inconvenient to store the other categories
     *  separately because many attributes are in multiple categories.
     */
    private static final Class<?>[] otherAttrCats = {
        Chromaticity.class,
        Copies.class,
        Destination.class,
        Fidelity.class,
        JobName.class,
        JobSheets.class,
        Media.class, /* have to support this somehow ... */
        MediaPrintableArea.class,
        OrientationRequested.class,
        PageRanges.class,
        RequestingUserName.class,
        SheetCollate.class,
        Sides.class,
    };

    private static int MAXCOPIES = 1000;

    private static final MediaSizeName[] mediaSizes = {
        MediaSizeName.NA_LETTER,
        MediaSizeName.TABLOID,
        MediaSizeName.LEDGER,
        MediaSizeName.NA_LEGAL,
        MediaSizeName.EXECUTIVE,
        MediaSizeName.ISO_A3,
        MediaSizeName.ISO_A4,
        MediaSizeName.ISO_A5,
        MediaSizeName.ISO_B4,
        MediaSizeName.ISO_B5,
    };

    private String printer;
    private PrinterName name;
    private boolean isInvalid;

    private transient PrintServiceAttributeSet lastSet;
    private transient ServiceNotifier notifier = null;

    UnixPrintService(String name) {
        if (name == null) {
            throw new IllegalArgumentException("null printer name");
        }
        printer = name;
        isInvalid = false;
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

    private PrinterIsAcceptingJobs getPrinterIsAcceptingJobsBSD() {
        if (PrintServiceLookupProvider.cmdIndex ==
            PrintServiceLookupProvider.UNINITIALIZED) {

            PrintServiceLookupProvider.cmdIndex =
                PrintServiceLookupProvider.getBSDCommandIndex();
        }

        String command = "/usr/sbin/lpc status " + printer
            + lpcStatusCom[PrintServiceLookupProvider.cmdIndex];
        String[] results= PrintServiceLookupProvider.execCmd(command);

        if (results != null && results.length > 0) {
            if (PrintServiceLookupProvider.cmdIndex ==
                PrintServiceLookupProvider.BSD_LPD_NG) {
                if (results[0].startsWith("enabled enabled")) {
                    return PrinterIsAcceptingJobs.ACCEPTING_JOBS ;
                }
            } else {
                if ((results[1].trim().startsWith("queuing is enabled") &&
                    results[2].trim().startsWith("printing is enabled")) ||
                    (results.length >= 4 &&
                     results[2].trim().startsWith("queuing is enabled") &&
                     results[3].trim().startsWith("printing is enabled"))) {
                    return PrinterIsAcceptingJobs.ACCEPTING_JOBS ;
                }
            }
        }
        return PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS ;
    }

    // Filter the list of possible AIX Printers and remove header lines
    // and extra lines which have been added for remote printers.
    // 'protected' because this method is also used from PrintServiceLookupProvider.
    protected static String[] filterPrinterNamesAIX(String[] posPrinters) {
        ArrayList<String> printers = new ArrayList<>();
        String [] splitPart;

        for(int i = 0; i < posPrinters.length; i++) {
            // Remove the header lines
            if (posPrinters[i].startsWith("---") ||
                posPrinters[i].startsWith("Queue") ||
                posPrinters[i].isEmpty()) continue;

            // Check if there is a ":" in the end of the first colomn.
            // This means that it is not a valid printer definition.
            splitPart = posPrinters[i].split(" ");
            if(splitPart.length >= 1 && !splitPart[0].trim().endsWith(":")) {
                printers.add(posPrinters[i]);
            }
        }

        return printers.toArray(new String[printers.size()]);
    }

    private PrinterIsAcceptingJobs getPrinterIsAcceptingJobsAIX() {
        // On AIX there should not be a blank after '-a'.
        String command = "/usr/bin/lpstat -a" + printer;
        String[] results= PrintServiceLookupProvider.execCmd(command);

        // Remove headers and bogus entries added by remote printers.
        results = filterPrinterNamesAIX(results);

        if (results != null && results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                if (results[i].contains("READY") ||
                    results[i].contains("RUNNING")) {
                    return PrinterIsAcceptingJobs.ACCEPTING_JOBS;
                }
            }
        }

        return PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS;

    }

    private PrinterIsAcceptingJobs getPrinterIsAcceptingJobs() {
        if (PrintServiceLookupProvider.isBSD()) {
            return getPrinterIsAcceptingJobsBSD();
        } else if (PrintServiceLookupProvider.isAIX()) {
            return getPrinterIsAcceptingJobsAIX();
        } else {
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

    private QueuedJobCount getQueuedJobCountBSD() {
        if (PrintServiceLookupProvider.cmdIndex ==
            PrintServiceLookupProvider.UNINITIALIZED) {

            PrintServiceLookupProvider.cmdIndex =
                PrintServiceLookupProvider.getBSDCommandIndex();
        }

        int qlen = 0;
        String command = "/usr/sbin/lpc status " + printer
            + lpcQueueCom[PrintServiceLookupProvider.cmdIndex];
        String[] results = PrintServiceLookupProvider.execCmd(command);

        if (results != null && results.length > 0) {
            String queued;
            if (PrintServiceLookupProvider.cmdIndex ==
                PrintServiceLookupProvider.BSD_LPD_NG) {
                queued = results[0];
            } else {
                queued = results[3].trim();
                if (queued.startsWith("no")) {
                    return new QueuedJobCount(0);
                } else {
                    queued = queued.substring(0, queued.indexOf(' '));
                }
            }

            try {
                qlen = Integer.parseInt(queued);
            } catch (NumberFormatException e) {
            }
        }

        return new QueuedJobCount(qlen);
    }

    private QueuedJobCount getQueuedJobCountAIX() {
        // On AIX there should not be a blank after '-a'.
        String command = "/usr/bin/lpstat -a" + printer;
        String[] results=  PrintServiceLookupProvider.execCmd(command);

        // Remove headers and bogus entries added by remote printers.
        results = filterPrinterNamesAIX(results);

        int qlen = 0;
        if (results != null && results.length > 0){
            for (int i = 0; i < results.length; i++) {
                if (results[i].contains("QUEUED")){
                    qlen ++;
                }
            }
        }
        return new QueuedJobCount(qlen);
    }

    private QueuedJobCount getQueuedJobCount() {
        if (PrintServiceLookupProvider.isBSD()) {
            return getQueuedJobCountBSD();
        } else if (PrintServiceLookupProvider.isAIX()) {
            return getQueuedJobCountAIX();
        } else {
            return new QueuedJobCount(0);
        }
    }

    private PrintServiceAttributeSet getBSDServiceAttributes() {
        PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
        attrs.add(getQueuedJobCountBSD());
        attrs.add(getPrinterIsAcceptingJobsBSD());
        return attrs;
    }

    private PrintServiceAttributeSet getAIXServiceAttributes() {
        PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
        attrs.add(getQueuedJobCountAIX());
        attrs.add(getPrinterIsAcceptingJobsAIX());
        return attrs;
    }

    private boolean isSupportedCopies(Copies copies) {
        int numCopies = copies.getValue();
        return (numCopies > 0 && numCopies < MAXCOPIES);
    }

    private boolean isSupportedMedia(MediaSizeName msn) {
        for (int i=0; i<mediaSizes.length; i++) {
            if (msn.equals(mediaSizes[i])) {
                return true;
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
        return new UnixPrintJob(this);
    }

    private PrintServiceAttributeSet getDynamicAttributes() {
        if (PrintServiceLookupProvider.isAIX()) {
            return getAIXServiceAttributes();
        } else {
            return getBSDServiceAttributes();
        }
    }

    public PrintServiceAttributeSet getUpdatedAttributes() {
        PrintServiceAttributeSet currSet = getDynamicAttributes();
        if (lastSet == null) {
            lastSet = currSet;
            return AttributeSetUtilities.unmodifiableView(currSet);
        } else {
            PrintServiceAttributeSet updates =
                new HashPrintServiceAttributeSet();
            Attribute []attrs = currSet.toArray();
            Attribute attr;
            for (int i=0; i<attrs.length; i++) {
                attr = attrs[i];
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

    public void addPrintServiceAttributeListener(
                                 PrintServiceAttributeListener listener) {
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
    public <T extends PrintServiceAttribute>
        T getAttribute(Class<T> category)
    {
        if (category == null) {
            throw new NullPointerException("category");
        }
        if (!(PrintServiceAttribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException("Not a PrintServiceAttribute");
        }

        if (category == PrinterName.class) {
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
        PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
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
        return AttributeSetUtilities.unmodifiableView(attrs);
    }

    private void initSupportedDocFlavors() {
        String hostEnc = DocFlavor.hostEncoding.toLowerCase(Locale.ENGLISH);
        if (!hostEnc.equals("utf-8") && !hostEnc.equals("utf-16") &&
            !hostEnc.equals("utf-16be") && !hostEnc.equals("utf-16le") &&
            !hostEnc.equals("us-ascii")) {

            int len = supportedDocFlavorsInit.length;
            DocFlavor[] flavors =
                new DocFlavor[len + supportedHostDocFlavors.length];
            // copy host encoding flavors
            System.arraycopy(supportedHostDocFlavors, 0, flavors,
                             len, supportedHostDocFlavors.length);
            System.arraycopy(supportedDocFlavorsInit, 0, flavors, 0, len);

            supportedDocFlavors = flavors;
        } else {
            supportedDocFlavors = supportedDocFlavorsInit;
        }
    }

    public DocFlavor[] getSupportedDocFlavors() {
        if (supportedDocFlavors == null) {
            initSupportedDocFlavors();
        }
        int len = supportedDocFlavors.length;
        DocFlavor[] flavors = new DocFlavor[len];
        System.arraycopy(supportedDocFlavors, 0, flavors, 0, len);

        return flavors;
    }

    public boolean isDocFlavorSupported(DocFlavor flavor) {
        if (supportedDocFlavors == null) {
            initSupportedDocFlavors();
        }
        for (int f=0; f<supportedDocFlavors.length; f++) {
            if (flavor.equals(supportedDocFlavors[f])) {
                return true;
            }
        }
        return false;
    }

    public Class<?>[] getSupportedAttributeCategories() {
        ArrayList<Class<?>> categList = new ArrayList<>(otherAttrCats.length);
        for (Class<?> c : otherAttrCats) {
            categList.add(c);
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

        for (int i=0;i<otherAttrCats.length;i++) {
            if (category == otherAttrCats[i]) {
                return true;
            }
        }
        return false;
    }

    /* return defaults for all attributes for which there is a default
     * value
     */
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

        if (category == Copies.class) {
            return new Copies(1);
        } else if (category == Chromaticity.class) {
            return Chromaticity.COLOR;
        } else if (category == Destination.class) {
            try {
                return new Destination((new File("out.ps")).toURI());
            } catch (SecurityException se) {
                try {
                    return new Destination(new URI("file:out.ps"));
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        } else if (category == Fidelity.class) {
            return Fidelity.FIDELITY_FALSE;
        } else if (category == JobName.class) {
            return new JobName("Java Printing", null);
        } else if (category == JobSheets.class) {
            return JobSheets.STANDARD;
        } else if (category == Media.class) {
            String defaultCountry = Locale.getDefault().getCountry();
            if (defaultCountry != null &&
                (defaultCountry.isEmpty() ||
                 defaultCountry.equals(Locale.US.getCountry()) ||
                 defaultCountry.equals(Locale.CANADA.getCountry()))) {
                return MediaSizeName.NA_LETTER;
            } else {
                 return MediaSizeName.ISO_A4;
            }
        } else if (category == MediaPrintableArea.class) {
            String defaultCountry = Locale.getDefault().getCountry();
            float iw, ih;
            if (defaultCountry != null &&
                (defaultCountry.isEmpty() ||
                 defaultCountry.equals(Locale.US.getCountry()) ||
                 defaultCountry.equals(Locale.CANADA.getCountry()))) {
                iw = MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) - 0.5f;
                ih = MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) - 0.5f;
            } else {
                iw = MediaSize.ISO.A4.getX(Size2DSyntax.INCH) - 0.5f;
                ih = MediaSize.ISO.A4.getY(Size2DSyntax.INCH) - 0.5f;
            }
            return new MediaPrintableArea(0.25f, 0.25f, iw, ih,
                                          MediaPrintableArea.INCH);
        } else if (category == OrientationRequested.class) {
            return OrientationRequested.PORTRAIT;
        } else if (category == PageRanges.class) {
            return new PageRanges(1, Integer.MAX_VALUE);
        } else if (category == RequestingUserName.class) {
            String userName = "";
            try {
              userName = System.getProperty("user.name", "");
            } catch (SecurityException se) {
            }
            return new RequestingUserName(userName, null);
        } else if (category == SheetCollate.class) {
            return SheetCollate.UNCOLLATED;
        } else if (category == Sides.class) {
            return Sides.ONE_SIDED;
        } else
            return null;
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
            } else if (isAutoSense(flavor)) {
                return null;
            }
        }

        if (!isAttributeCategorySupported(category)) {
            return null;
        }

        if (category == Chromaticity.class) {
            if (flavor == null || isServiceFormattedFlavor(flavor)) {
                Chromaticity[]arr = new Chromaticity[1];
                arr[0] = Chromaticity.COLOR;
                return (arr);
            } else {
                return null;
            }
        } else if (category == Destination.class) {
            try {
                return new Destination((new File("out.ps")).toURI());
            } catch (SecurityException se) {
                try {
                    return new Destination(new URI("file:out.ps"));
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        } else if (category == JobName.class) {
            return new JobName("Java Printing", null);
        } else if (category == JobSheets.class) {
            JobSheets[] arr = new JobSheets[2];
            arr[0] = JobSheets.NONE;
            arr[1] = JobSheets.STANDARD;
            return arr;
        } else if (category == RequestingUserName.class) {
            String userName = "";
            try {
              userName = System.getProperty("user.name", "");
            } catch (SecurityException se) {
            }
            return new RequestingUserName(userName, null);
        } else if (category == OrientationRequested.class) {
            if (flavor == null || isServiceFormattedFlavor(flavor)) {
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
            if (flavor == null ||
                !(flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
                  flavor.equals(DocFlavor.URL.POSTSCRIPT) ||
                  flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT))) {
                return new CopiesSupported(1, MAXCOPIES);
            } else {
                return null;
            }
        } else if (category == Media.class) {
            Media []arr = new Media[mediaSizes.length];
            System.arraycopy(mediaSizes, 0, arr, 0, mediaSizes.length);
            return arr;
        } else if (category == Fidelity.class) {
            Fidelity []arr = new Fidelity[2];
            arr[0] = Fidelity.FIDELITY_FALSE;
            arr[1] = Fidelity.FIDELITY_TRUE;
            return arr;
        } else if (category == MediaPrintableArea.class) {
            /* The code below implements the behaviour that if no Media or
             * MediaSize attribute is specified, return an array of
             * MediaPrintableArea, one for each supported Media.
             * If a MediaSize is specified, return a MPA consistent for that,
             * and if a Media is specified locate its MediaSize and return
             * its MPA, and if none is found, return an MPA for the default
             * Media for this service.
             */
            if (attributes == null) {
                return getAllPrintableAreas();
            }
            MediaSize mediaSize = (MediaSize)attributes.get(MediaSize.class);
            Media media = (Media)attributes.get(Media.class);
            MediaPrintableArea []arr = new MediaPrintableArea[1];
            if (mediaSize == null) {
                if (media instanceof MediaSizeName) {
                    MediaSizeName msn = (MediaSizeName)media;
                    mediaSize = MediaSize.getMediaSizeForName(msn);
                    if (mediaSize == null) {
                        /* try to get a size from the default media */
                        media = (Media)getDefaultAttributeValue(Media.class);
                        if (media instanceof MediaSizeName) {
                            msn = (MediaSizeName)media;
                            mediaSize = MediaSize.getMediaSizeForName(msn);
                        }
                        if (mediaSize == null) {
                            /* shouldn't happen, return a default */
                            arr[0] = new MediaPrintableArea(0.25f, 0.25f,
                                                            8f, 10.5f,
                                                            MediaSize.INCH);
                            return arr;
                        }
                    }
                } else {
                    return getAllPrintableAreas();
                }
            }
            /* If reach here MediaSize is non-null */
            assert mediaSize != null;
            arr[0] = new MediaPrintableArea(0.25f, 0.25f,
                                mediaSize.getX(MediaSize.INCH)-0.5f,
                                mediaSize.getY(MediaSize.INCH)-0.5f,
                                MediaSize.INCH);
            return arr;
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
        } else if (category == SheetCollate.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                SheetCollate []arr = new SheetCollate[2];
                arr[0] = SheetCollate.UNCOLLATED;
                arr[1] = SheetCollate.COLLATED;
                return arr;
            } else {
                return null;
            }
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
        } else {
            return null;
        }
    }

    private static MediaPrintableArea[] mpas = null;
    private MediaPrintableArea[] getAllPrintableAreas() {

        if (mpas == null) {
            Media[] media = (Media[])getSupportedAttributeValues(Media.class,
                                                                 null, null);
            mpas = new MediaPrintableArea[media.length];
            for (int i=0; i< mpas.length; i++) {
                if (media[i] instanceof MediaSizeName) {
                    MediaSizeName msn = (MediaSizeName)media[i];
                    MediaSize mediaSize = MediaSize.getMediaSizeForName(msn);
                    if (mediaSize == null) {
                        mpas[i] = (MediaPrintableArea)
                            getDefaultAttributeValue(MediaPrintableArea.class);
                    } else {
                        mpas[i] = new MediaPrintableArea(0.25f, 0.25f,
                                        mediaSize.getX(MediaSize.INCH)-0.5f,
                                        mediaSize.getY(MediaSize.INCH)-0.5f,
                                        MediaSize.INCH);
                    }
                }
            }
        }
        MediaPrintableArea[] mpasCopy = new MediaPrintableArea[mpas.length];
        System.arraycopy(mpas, 0, mpasCopy, 0, mpas.length);
        return mpasCopy;
    }

    /* Is this one of the flavors that this service explicitly
     * generates postscript for, and so can control how it is rendered?
     */
    private boolean isServiceFormattedFlavor(DocFlavor flavor) {
        return
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
            flavor.equals(DocFlavor.URL.PNG);
    }

    public boolean isAttributeValueSupported(Attribute attr,
                                             DocFlavor flavor,
                                             AttributeSet attributes) {
        if (attr == null) {
            throw new NullPointerException("null attribute");
        }
        if (flavor != null) {
            if (!isDocFlavorSupported(flavor)) {
                throw new IllegalArgumentException(flavor +
                                               " is an unsupported flavor");
            } else if (isAutoSense(flavor)) {
                return false;
            }
        }
        Class<? extends Attribute> category = attr.getCategory();
        if (!isAttributeCategorySupported(category)) {
            return false;
        }
        else if (attr.getCategory() == Chromaticity.class) {
            if (flavor == null || isServiceFormattedFlavor(flavor)) {
                return attr == Chromaticity.COLOR;
            } else {
                return false;
            }
        }
        else if (attr.getCategory() == Copies.class) {
            return (flavor == null ||
                   !(flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
                     flavor.equals(DocFlavor.URL.POSTSCRIPT) ||
                     flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT))) &&
                isSupportedCopies((Copies)attr);
        } else if (attr.getCategory() == Destination.class) {
            URI uri = ((Destination)attr).getURI();
                if ("file".equals(uri.getScheme()) &&
                    !uri.getSchemeSpecificPart().isEmpty()) {
                return true;
            } else {
            return false;
            }
        } else if (attr.getCategory() == Media.class) {
            if (attr instanceof MediaSizeName) {
                return isSupportedMedia((MediaSizeName)attr);
            } else {
                return false;
            }
        } else if (attr.getCategory() == OrientationRequested.class) {
            if (attr == OrientationRequested.REVERSE_PORTRAIT ||
                (flavor != null) &&
                !isServiceFormattedFlavor(flavor)) {
                return false;
            }
        } else if (attr.getCategory() == PageRanges.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == SheetCollate.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == Sides.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == DialogOwner.class) {
            DialogOwner owner = (DialogOwner)attr;
            // ID not supported on any dialog type on Unix platforms.
            if (DialogOwnerAccessor.getID(owner) != 0) {
                return false;
            }
            // UnixPrintService is not used on Mac, so this is
            // always some Unix system that does not have CUPS/IPP
            // Which means we always use a Swing dialog and we need
            // only check if alwaysOnTop is supported by the toolkit.
            if (owner.getOwner() != null) {
                return true;
            } else {
                return Toolkit.getDefaultToolkit().isAlwaysOnTopSupported();
            }
        } else if (attr.getCategory() == DialogTypeSelection.class) {
            DialogTypeSelection dts = (DialogTypeSelection)attr;
            return dts == DialogTypeSelection.COMMON;
        }
        return true;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
                                                 AttributeSet attributes) {

        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("flavor " + flavor +
                                               "is not supported");
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
                } else if (!isAttributeValueSupported(attr, flavor,
                                                      attributes)) {
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

    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }

    public String toString() {
        return "Unix Printer : " + getName();
    }

    public boolean equals(Object obj) {
        return  (obj == this ||
                 (obj instanceof UnixPrintService &&
                  ((UnixPrintService)obj).getName().equals(getName())));
    }

    public int hashCode() {
        return this.getClass().hashCode()+getName().hashCode();
    }

    public boolean usesClass(Class<?> c) {
        return (c == sun.print.PSPrinterJob.class);
    }

}
