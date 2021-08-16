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

import java.net.URI;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Vector;

import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import javax.print.event.PrintJobAttributeListener;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobOriginatingUserName;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.NumberUp;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;

import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;



public class UnixPrintJob implements CancelablePrintJob {
    private static String debugPrefix = "UnixPrintJob>> ";

    private transient Vector<PrintJobListener> jobListeners;
    private transient Vector<PrintJobAttributeListener> attrListeners;
    private transient Vector<PrintJobAttributeSet> listenedAttributeSets;

    private PrintService service;
    private boolean fidelity;
    private boolean printing = false;
    private boolean printReturned = false;
    private PrintRequestAttributeSet reqAttrSet = null;
    private PrintJobAttributeSet jobAttrSet = null;
    private PrinterJob job;
    private Doc doc;
    /* these variables used globally to store reference to the print
     * data retrieved as a stream. On completion these are always closed
     * if non-null.
     */
    private InputStream instream = null;
    private Reader reader = null;

    /* default values overridden by those extracted from the attributes */
    private String jobName = "Java Printing";
    private int copies = 1;
    private MediaSizeName mediaName = MediaSizeName.NA_LETTER;
    private MediaSize     mediaSize = MediaSize.NA.LETTER;
    private CustomMediaTray     customTray = null;
    private OrientationRequested orient = OrientationRequested.PORTRAIT;
    private NumberUp nUp = null;
    private Sides sides = null;

    UnixPrintJob(PrintService service) {
        this.service = service;
        mDestination = service.getName();
        if (PrintServiceLookupProvider.isMac()) {
            mDestination = ((IPPPrintService)service).getDest();
        }
        mDestType = UnixPrintJob.DESTPRINTER;
        JobSheets js = (JobSheets)(service.
                                      getDefaultAttributeValue(JobSheets.class));
        if (js != null && js.equals(JobSheets.NONE)) {
            mNoJobSheet = true;
        }
    }

    public PrintService getPrintService() {
        return service;
    }

    public PrintJobAttributeSet getAttributes() {
        synchronized (this) {
            if (jobAttrSet == null) {
                /* just return an empty set until the job is submitted */
                PrintJobAttributeSet jobSet = new HashPrintJobAttributeSet();
                return AttributeSetUtilities.unmodifiableView(jobSet);
            } else {
              return jobAttrSet;
            }
        }
    }

    public void addPrintJobListener(PrintJobListener listener) {
        synchronized (this) {
            if (listener == null) {
                return;
            }
            if (jobListeners == null) {
                jobListeners = new Vector<>();
            }
            jobListeners.add(listener);
        }
    }

    public void removePrintJobListener(PrintJobListener listener) {
        synchronized (this) {
            if (listener == null || jobListeners == null ) {
                return;
            }
            jobListeners.remove(listener);
            if (jobListeners.isEmpty()) {
                jobListeners = null;
            }
        }
    }


    /* Closes any stream already retrieved for the data.
     * We want to avoid unnecessarily asking the Doc to create a stream only
     * to get a reference in order to close it because the job failed.
     * If the representation class is itself a "stream", this
     * closes that stream too.
     */
    private void closeDataStreams() {

        if (doc == null) {
            return;
        }

        Object data = null;

        try {
            data = doc.getPrintData();
        } catch (IOException e) {
            return;
        }

        if (instream != null) {
            try {
                instream.close();
            } catch (IOException e) {
            } finally {
                instream = null;
            }
        }
        else if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
            } finally {
                reader = null;
            }
        }
        else if (data instanceof InputStream) {
            try {
                ((InputStream)data).close();
            } catch (IOException e) {
            }
        }
        else if (data instanceof Reader) {
            try {
                ((Reader)data).close();
            } catch (IOException e) {
            }
        }
    }

    private void notifyEvent(int reason) {

        /* since this method should always get called, here's where
         * we will perform the clean up of any data stream supplied.
         */
        switch (reason) {
            case PrintJobEvent.DATA_TRANSFER_COMPLETE:
            case PrintJobEvent.JOB_CANCELED :
            case PrintJobEvent.JOB_FAILED :
            case PrintJobEvent.NO_MORE_EVENTS :
            case PrintJobEvent.JOB_COMPLETE :
                closeDataStreams();
        }

        synchronized (this) {
            if (jobListeners != null) {
                PrintJobListener listener;
                PrintJobEvent event = new PrintJobEvent(this, reason);
                for (int i = 0; i < jobListeners.size(); i++) {
                    listener = jobListeners.elementAt(i);
                    switch (reason) {

                        case PrintJobEvent.JOB_CANCELED :
                            listener.printJobCanceled(event);
                            break;

                        case PrintJobEvent.JOB_FAILED :
                            listener.printJobFailed(event);
                            break;

                        case PrintJobEvent.DATA_TRANSFER_COMPLETE :
                            listener.printDataTransferCompleted(event);
                            break;

                        case PrintJobEvent.NO_MORE_EVENTS :
                            listener.printJobNoMoreEvents(event);
                            break;

                        default:
                            break;
                    }
                }
            }
       }
    }

    public void addPrintJobAttributeListener(
                                  PrintJobAttributeListener listener,
                                  PrintJobAttributeSet attributes) {
        synchronized (this) {
            if (listener == null) {
                return;
            }
            if (attrListeners == null) {
                attrListeners = new Vector<>();
                listenedAttributeSets = new Vector<>();
            }
            attrListeners.add(listener);
            if (attributes == null) {
                attributes = new HashPrintJobAttributeSet();
            }
            listenedAttributeSets.add(attributes);
        }
    }

    public void removePrintJobAttributeListener(
                                        PrintJobAttributeListener listener) {
        synchronized (this) {
            if (listener == null || attrListeners == null ) {
                return;
            }
            int index = attrListeners.indexOf(listener);
            if (index == -1) {
                return;
            } else {
                attrListeners.remove(index);
                listenedAttributeSets.remove(index);
                if (attrListeners.isEmpty()) {
                    attrListeners = null;
                    listenedAttributeSets = null;
                }
            }
        }
    }

    public void print(Doc doc, PrintRequestAttributeSet attributes)
        throws PrintException {

        synchronized (this) {
            if (printing) {
                throw new PrintException("already printing");
            } else {
                printing = true;
            }
        }

        if ((service.getAttribute(PrinterIsAcceptingJobs.class)) ==
                         PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
            throw new PrintException("Printer is not accepting job.");
        }

        this.doc = doc;
        /* check if the parameters are valid before doing much processing */
        DocFlavor flavor = doc.getDocFlavor();

        Object data;

        try {
            data = doc.getPrintData();
        } catch (IOException e) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException("can't get print data: " + e.toString());
        }

        if (data == null) {
            throw new PrintException("Null print data.");
        }

        if (flavor == null || (!service.isDocFlavorSupported(flavor))) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintJobFlavorException("invalid flavor", flavor);
        }

        initializeAttributeSets(doc, attributes);

        getAttributeValues(flavor);

        // set up mOptions
        if ((service instanceof IPPPrintService) &&
            CUPSPrinter.isCupsRunning()) {

             IPPPrintService.debug_println(debugPrefix+
                        "instanceof IPPPrintService");

             if (mediaName != null) {
                 CustomMediaSizeName customMedia =
                     ((IPPPrintService)service).findCustomMedia(mediaName);
                 if (customMedia != null) {
                     mOptions = " media="+ customMedia.getChoiceName();
                 }
             }

             if (customTray != null &&
                 customTray instanceof CustomMediaTray) {
                 String choice = customTray.getChoiceName();
                 if (choice != null) {
                     mOptions += " InputSlot="+choice;
                 }
             }

             if (nUp != null) {
                 mOptions += " number-up="+nUp.getValue();
             }

             if (orient != OrientationRequested.PORTRAIT &&
                 (flavor != null) &&
                 !flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE)) {
                 mOptions += " orientation-requested="+orient.getValue();
             }

             if (sides != null) {
                 mOptions += " sides="+sides;
             }

        }

        IPPPrintService.debug_println(debugPrefix+"mOptions "+mOptions);
        String repClassName = flavor.getRepresentationClassName();
        String val = flavor.getParameter("charset");
        String encoding = "us-ascii";
        if (val != null && !val.isEmpty()) {
            encoding = val;
        }

        if (flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
            flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
            flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
            flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
            flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
            flavor.equals(DocFlavor.BYTE_ARRAY.PNG)) {
            try {
                instream = doc.getStreamForBytes();
                if (instream == null) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException("No stream for data");
                }
                if (!(service instanceof IPPPrintService &&
                    ((IPPPrintService)service).isIPPSupportedImages(
                                                flavor.getMimeType()))) {
                    printableJob(new ImagePrinter(instream));
                    if (service instanceof IPPPrintService) {
                        ((IPPPrintService)service).wakeNotifier();
                    } else {
                        ((UnixPrintService)service).wakeNotifier();
                    }
                    return;
                }
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe);
            }
        } else if (flavor.equals(DocFlavor.URL.GIF) ||
                   flavor.equals(DocFlavor.URL.JPEG) ||
                   flavor.equals(DocFlavor.URL.PNG)) {
            try {
                URL url = (URL)data;
                if ((service instanceof IPPPrintService) &&
                    ((IPPPrintService)service).isIPPSupportedImages(
                                               flavor.getMimeType())) {
                    instream = url.openStream();
                } else {
                    printableJob(new ImagePrinter(url));
                    if (service instanceof IPPPrintService) {
                        ((IPPPrintService)service).wakeNotifier();
                    } else {
                        ((UnixPrintService)service).wakeNotifier();
                    }
                    return;
                }
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(e.toString());
            }
        } else if (flavor.equals(DocFlavor.CHAR_ARRAY.TEXT_PLAIN) ||
                   flavor.equals(DocFlavor.READER.TEXT_PLAIN) ||
                   flavor.equals(DocFlavor.STRING.TEXT_PLAIN)) {
            try {
                reader = doc.getReaderForText();
                if (reader == null) {
                   notifyEvent(PrintJobEvent.JOB_FAILED);
                   throw new PrintException("No reader for data");
                }
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe.toString());
            }
        } else if (repClassName.equals("[B") ||
                   repClassName.equals("java.io.InputStream")) {
            try {
                instream = doc.getStreamForBytes();
                if (instream == null) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException("No stream for data");
                }
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe.toString());
            }
        } else if  (repClassName.equals("java.net.URL")) {
            /*
             * This extracts the data from the URL and passes it the content
             * directly to the print service as a file.
             * This is appropriate for the current implementation where lp or
             * lpr is always used to spool the data. We expect to revise the
             * implementation to provide more complete IPP support (ie not just
             * CUPS) and at that time the job will be spooled via IPP
             * and the URL
             * itself should be sent to the IPP print service not the content.
             */
            URL url = (URL)data;
            try {
                instream = url.openStream();
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(e.toString());
            }
        } else if (repClassName.equals("java.awt.print.Pageable")) {
            try {
                pageableJob((Pageable)doc.getPrintData());
                if (service instanceof IPPPrintService) {
                    ((IPPPrintService)service).wakeNotifier();
                } else {
                    ((UnixPrintService)service).wakeNotifier();
                }
                return;
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe);
            }
        } else if (repClassName.equals("java.awt.print.Printable")) {
            try {
                printableJob((Printable)doc.getPrintData());
                if (service instanceof IPPPrintService) {
                    ((IPPPrintService)service).wakeNotifier();
                } else {
                    ((UnixPrintService)service).wakeNotifier();
                }
                return;
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe);
            }
        } else {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException("unrecognized class: "+repClassName);
        }

        // now spool the print data.
        PrinterOpener po = new PrinterOpener();
        @SuppressWarnings("removal")
        var dummy = java.security.AccessController.doPrivileged(po);
        if (po.pex != null) {
            throw po.pex;
        }
        OutputStream output = po.result;

        /* There are three cases:
         * 1) Text data from a Reader, just pass through.
         * 2) Text data from an input stream which we must read using the
         *    correct encoding
         * 3) Raw byte data from an InputStream we don't interpret as text,
         *    just pass through: eg postscript.
         */

        BufferedWriter bw = null;
        if ((instream == null && reader != null)) {
            BufferedReader br = new BufferedReader(reader);
            OutputStreamWriter osw = new OutputStreamWriter(output);
            bw = new BufferedWriter(osw);
            char []buffer = new char[1024];
            int cread;

            try {
                while ((cread = br.read(buffer, 0, buffer.length)) >=0) {
                    bw.write(buffer, 0, cread);
                }
                br.close();
                bw.flush();
                bw.close();
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException (e);
            }
        } else if (instream != null &&
                   flavor.getMediaType().equalsIgnoreCase("text")) {
            try {

                InputStreamReader isr = new InputStreamReader(instream,
                                                              encoding);
                BufferedReader br = new BufferedReader(isr);
                OutputStreamWriter osw = new OutputStreamWriter(output);
                bw = new BufferedWriter(osw);
                char []buffer = new char[1024];
                int cread;

                while ((cread = br.read(buffer, 0, buffer.length)) >=0) {
                    bw.write(buffer, 0, cread);
                }
                bw.flush();
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException (e);
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException e) {
                }
            }
        } else if (instream != null) {
            try (BufferedInputStream bin = new BufferedInputStream(instream);
                 BufferedOutputStream bout = new BufferedOutputStream(output)) {
                bin.transferTo(bout);
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(e);
            }
        }
        notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);

        if (mDestType == UnixPrintJob.DESTPRINTER) {
            PrinterSpooler spooler = new PrinterSpooler();
            @SuppressWarnings("removal")
            var dummy2 = java.security.AccessController.doPrivileged(spooler);
            if (spooler.pex != null) {
                throw spooler.pex;
            }
        }
        notifyEvent(PrintJobEvent.NO_MORE_EVENTS);
        if (service instanceof IPPPrintService) {
            ((IPPPrintService)service).wakeNotifier();
        } else {
            ((UnixPrintService)service).wakeNotifier();
        }
    }

    public void printableJob(Printable printable) throws PrintException {
        try {
            synchronized(this) {
                if (job != null) { // shouldn't happen
                    throw new PrintException("already printing");
                } else {
                    job = new PSPrinterJob();
                }
            }
            job.setPrintService(getPrintService());
            job.setCopies(copies);
            job.setJobName(jobName);
            PageFormat pf = new PageFormat();
            if (mediaSize != null) {
                Paper p = new Paper();
                p.setSize(mediaSize.getX(MediaSize.INCH)*72.0,
                          mediaSize.getY(MediaSize.INCH)*72.0);
                p.setImageableArea(72.0, 72.0, p.getWidth()-144.0,
                                   p.getHeight()-144.0);
                pf.setPaper(p);
            }
            if (orient == OrientationRequested.REVERSE_LANDSCAPE) {
                pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
            } else if (orient == OrientationRequested.LANDSCAPE) {
                pf.setOrientation(PageFormat.LANDSCAPE);
            }
            job.setPrintable(printable, pf);
            job.print(reqAttrSet);
            notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);
            return;
        } catch (PrinterException pe) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException(pe);
        } finally {
            printReturned = true;
            notifyEvent(PrintJobEvent.NO_MORE_EVENTS);
        }
    }

    public void pageableJob(Pageable pageable) throws PrintException {
        try {
            synchronized(this) {
                if (job != null) { // shouldn't happen
                    throw new PrintException("already printing");
                } else {
                    job = new PSPrinterJob();
                }
            }
            job.setPrintService(getPrintService());
            job.setCopies(copies);
            job.setJobName(jobName);
            job.setPageable(pageable);
            job.print(reqAttrSet);
            notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);
            return;
        } catch (PrinterException pe) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException(pe);
        } finally {
            printReturned = true;
            notifyEvent(PrintJobEvent.NO_MORE_EVENTS);
        }
    }
    /* There's some inefficiency here as the job set is created even though
     * it may never be requested.
     */
    private synchronized void
        initializeAttributeSets(Doc doc, PrintRequestAttributeSet reqSet) {

        reqAttrSet = new HashPrintRequestAttributeSet();
        jobAttrSet = new HashPrintJobAttributeSet();

        Attribute[] attrs;
        if (reqSet != null) {
            reqAttrSet.addAll(reqSet);
            attrs = reqSet.toArray();
            for (int i=0; i<attrs.length; i++) {
                if (attrs[i] instanceof PrintJobAttribute) {
                    jobAttrSet.add(attrs[i]);
                }
            }
        }

        DocAttributeSet docSet = doc.getAttributes();
        if (docSet != null) {
            attrs = docSet.toArray();
            for (int i=0; i<attrs.length; i++) {
                if (attrs[i] instanceof PrintRequestAttribute) {
                    reqAttrSet.add(attrs[i]);
                }
                if (attrs[i] instanceof PrintJobAttribute) {
                    jobAttrSet.add(attrs[i]);
                }
            }
        }

        /* add the user name to the job */
        String userName = "";
        try {
          userName = System.getProperty("user.name");
        } catch (SecurityException se) {
        }

        if (userName == null || userName.isEmpty()) {
            RequestingUserName ruName =
                (RequestingUserName)reqSet.get(RequestingUserName.class);
            if (ruName != null) {
                jobAttrSet.add(
                    new JobOriginatingUserName(ruName.getValue(),
                                               ruName.getLocale()));
            } else {
                jobAttrSet.add(new JobOriginatingUserName("", null));
            }
        } else {
            jobAttrSet.add(new JobOriginatingUserName(userName, null));
        }

        /* if no job name supplied use doc name (if supplied), if none and
         * its a URL use that, else finally anything .. */
        if (jobAttrSet.get(JobName.class) == null) {
            JobName jobName;
            if (docSet != null && docSet.get(DocumentName.class) != null) {
                DocumentName docName =
                    (DocumentName)docSet.get(DocumentName.class);
                jobName = new JobName(docName.getValue(), docName.getLocale());
                jobAttrSet.add(jobName);
            } else {
                String str = "JPS Job:" + doc;
                try {
                    Object printData = doc.getPrintData();
                    if (printData instanceof URL) {
                        str = ((URL)(doc.getPrintData())).toString();
                    }
                } catch (IOException e) {
                }
                jobName = new JobName(str, null);
                jobAttrSet.add(jobName);
            }
        }

        jobAttrSet = AttributeSetUtilities.unmodifiableView(jobAttrSet);
    }

    private void getAttributeValues(DocFlavor flavor) throws PrintException {
        Attribute attr;
        Class<? extends Attribute> category;

        if (reqAttrSet.get(Fidelity.class) == Fidelity.FIDELITY_TRUE) {
            fidelity = true;
        } else {
            fidelity = false;
        }

        Attribute []attrs = reqAttrSet.toArray();
        for (int i=0; i<attrs.length; i++) {
            attr = attrs[i];
            category = attr.getCategory();
            if (fidelity == true) {
                if (!service.isAttributeCategorySupported(category)) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintJobAttributeException(
                        "unsupported category: " + category, category, null);
                } else if
                    (!service.isAttributeValueSupported(attr, flavor, null)) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintJobAttributeException(
                        "unsupported attribute: " + attr, null, attr);
                }
            }
            if (category == Destination.class) {
                URI uri = ((Destination)attr).getURI();
                if (!"file".equals(uri.getScheme())) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException("Not a file: URI");
                } else {
                    try {
                        mDestType = DESTFILE;
                        mDestination = (new File(uri)).getPath();
                    } catch (Exception e) {
                        throw new PrintException(e);
                    }
                    // check write access
                    @SuppressWarnings("removal")
                    SecurityManager security = System.getSecurityManager();
                    if (security != null) {
                      try {
                        security.checkWrite(mDestination);
                      } catch (SecurityException se) {
                        notifyEvent(PrintJobEvent.JOB_FAILED);
                        throw new PrintException(se);
                      }
                    }
                }
            } else if (category == JobSheets.class) {
                if ((JobSheets)attr == JobSheets.NONE) {
                   mNoJobSheet = true;
                }
            } else if (category == JobName.class) {
                jobName = ((JobName)attr).getValue();
            } else if (category == Copies.class) {
                copies = ((Copies)attr).getValue();
            } else if (category == Media.class) {
                if (attr instanceof MediaSizeName) {
                    mediaName = (MediaSizeName)attr;
                    IPPPrintService.debug_println(debugPrefix+
                                                  "mediaName "+mediaName);
                if (!service.isAttributeValueSupported(attr, null, null)) {
                    mediaSize = MediaSize.getMediaSizeForName(mediaName);
                }
              } else if (attr instanceof CustomMediaTray) {
                  customTray = (CustomMediaTray)attr;
              }
            } else if (category == OrientationRequested.class) {
                orient = (OrientationRequested)attr;
            } else if (category == NumberUp.class) {
                nUp = (NumberUp)attr;
            } else if (category == Sides.class) {
                sides = (Sides)attr;
            }
        }
    }

    private String[] printExecCmd(String printer, String options,
                                 boolean noJobSheet,
                                 String jobTitle, int copies, String spoolFile) {
        int PRINTER = 0x1;
        int OPTIONS = 0x2;
        int JOBTITLE  = 0x4;
        int COPIES  = 0x8;
        int NOSHEET  = 0x10;
        int pFlags = 0;
        String[] execCmd;
        int ncomps = 2; // minimum number of print args
        int n = 0;

        // conveniently "lp" is the default destination for both lp and lpr.
        if (printer != null && !printer.isEmpty() && !printer.equals("lp")) {
            pFlags |= PRINTER;
            ncomps+=1;
        }
        if (options != null && !options.isEmpty()) {
            pFlags |= OPTIONS;
            ncomps+=1;
        }
        if (jobTitle != null && !jobTitle.isEmpty()) {
            pFlags |= JOBTITLE;
            ncomps+=1;
        }
        if (copies > 1) {
            pFlags |= COPIES;
            ncomps+=1;
        }
        if (noJobSheet) {
            pFlags |= NOSHEET;
            ncomps+=1;
        } else if (getPrintService().
                        isAttributeCategorySupported(JobSheets.class)) {
            ncomps+=1;
        }
        execCmd = new String[ncomps];
        execCmd[n++] = "/usr/bin/lpr";
        if ((pFlags & PRINTER) != 0) {
            execCmd[n++] = "-P" + printer;
        }
        if ((pFlags & JOBTITLE) != 0) {
            execCmd[n++] = "-J "  + jobTitle;
        }
        if ((pFlags & COPIES) != 0) {
            execCmd[n++] = "-#" + copies;
        }
        if ((pFlags & NOSHEET) != 0) {
            execCmd[n++] = "-h";
        } else if (getPrintService().
                   isAttributeCategorySupported(JobSheets.class)) {
            execCmd[n++] = "-o job-sheets=standard";
        }
        if ((pFlags & OPTIONS) != 0) {
            execCmd[n++] = "-o" + options;
        }
        execCmd[n++] = spoolFile;
        if (IPPPrintService.debugPrint) {
            System.out.println("UnixPrintJob>> execCmd");
            for (int i=0; i<execCmd.length; i++) {
                System.out.print(" "+execCmd[i]);
            }
            System.out.println();
        }
        return execCmd;
    }

    private static int DESTPRINTER = 1;
    private static int DESTFILE = 2;
    private int mDestType = DESTPRINTER;

    private File spoolFile;
    private String mDestination, mOptions="";
    private boolean mNoJobSheet = false;

    // Inner class to run "privileged" to open the printer output stream.

    private class PrinterOpener implements java.security.PrivilegedAction<OutputStream> {
        PrintException pex;
        OutputStream result;

        public OutputStream run() {
            try {
                if (mDestType == UnixPrintJob.DESTFILE) {
                    spoolFile = new File(mDestination);
                } else {
                    /* Write to a temporary file which will be spooled to
                     * the printer then deleted. In the case that the file
                     * is not removed for some reason, request that it is
                     * removed when the VM exits.
                     */
                    spoolFile = Files.createTempFile("javaprint", "").toFile();
                    spoolFile.deleteOnExit();
                }
                result = new FileOutputStream(spoolFile);
                return result;
            } catch (IOException ex) {
                // If there is an IOError we subvert it to a PrinterException.
                notifyEvent(PrintJobEvent.JOB_FAILED);
                pex = new PrintException(ex);
            }
            return null;
        }
    }

    // Inner class to run "privileged" to invoke the system print command

    private class PrinterSpooler implements java.security.PrivilegedAction<Object> {
        PrintException pex;

        private void handleProcessFailure(final Process failedProcess,
                final String[] execCmd, final int result) throws IOException {
            try (StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw)) {
                pw.append("error=").append(Integer.toString(result));
                pw.append(" running:");
                for (String arg: execCmd) {
                    pw.append(" '").append(arg).append("'");
                }
                try (InputStream is = failedProcess.getErrorStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr)) {
                    while (br.ready()) {
                        pw.println();
                        pw.append("\t\t").append(br.readLine());
                    }
                } finally {
                    pw.flush();
                }
                throw new IOException(sw.toString());
            }
        }

        public Object run() {
            if (spoolFile == null || !spoolFile.exists()) {
               pex = new PrintException("No spool file");
               notifyEvent(PrintJobEvent.JOB_FAILED);
               return null;
            }
            try {
                /**
                 * Spool to the printer.
                 */
                String fileName = spoolFile.getAbsolutePath();
                String[] execCmd = printExecCmd(mDestination, mOptions,
                               mNoJobSheet, jobName, copies, fileName);

                Process process = Runtime.getRuntime().exec(execCmd);
                process.waitFor();
                final int result = process.exitValue();
                if (0 != result) {
                    handleProcessFailure(process, execCmd, result);
                }
                notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);
            } catch (IOException ex) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                // REMIND : 2d printing throws PrinterException
                pex = new PrintException(ex);
            } catch (InterruptedException ie) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                pex = new PrintException(ie);
            } finally {
                spoolFile.delete();
                notifyEvent(PrintJobEvent.NO_MORE_EVENTS);
            }
            return null;
        }
    }

    public void cancel() throws PrintException {
        synchronized (this) {
            if (!printing) {
                throw new PrintException("Job is not yet submitted.");
            } else if (job != null && !printReturned) {
                job.cancel();
                notifyEvent(PrintJobEvent.JOB_CANCELED);
                return;
            } else {
                throw new PrintException("Job could not be cancelled.");
            }
        }
    }
}
