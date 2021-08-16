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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobOriginatingUserName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;

import java.awt.print.*;

public class Win32PrintJob implements CancelablePrintJob {

    private transient Vector<PrintJobListener> jobListeners;
    private transient Vector<PrintJobAttributeListener> attrListeners;
    private transient Vector<PrintJobAttributeSet> listenedAttributeSets;

    private Win32PrintService service;
    private boolean fidelity;
    private boolean printing = false;
    private boolean printReturned = false;
    private PrintRequestAttributeSet reqAttrSet = null;
    private PrintJobAttributeSet jobAttrSet = null;
    private PrinterJob job;
    private Doc doc;
    private String mDestination = null;

    /* these variables used globally to store reference to the print
     * data retrieved as a stream. On completion these are always closed
     * if non-null.
     */
    private InputStream instream = null;
    private Reader reader = null;

    /* default values overridden by those extracted from the attributes */
    private String jobName = "Java Printing";
    private int copies = 0;
    private MediaSizeName mediaName = null;
    private MediaSize     mediaSize = null;
    private OrientationRequested orient = null;

    /* print job handle used by native code */
    private long hPrintJob;

    /* buffer length for printing raw data */
    private static final int PRINTBUFFERLEN = 8192;

    Win32PrintJob(Win32PrintService service) {
        this.service = service;
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

                        case PrintJobEvent.JOB_COMPLETE :
                            listener.printJobCompleted(event);
                            break;

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

        PrinterState prnState = service.getAttribute(PrinterState.class);
        if (prnState == PrinterState.STOPPED) {
            PrinterStateReasons prnStateReasons =
                service.getAttribute(PrinterStateReasons.class);
                if ((prnStateReasons != null) &&
                    (prnStateReasons.containsKey(PrinterStateReason.SHUTDOWN)))
                {
                    throw new PrintException("PrintService is no longer available.");
                }
        }

        if (service.getAttribute(PrinterIsAcceptingJobs.class) ==
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

        String repClassName = flavor.getRepresentationClassName();

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
                printableJob(new ImagePrinter(instream));
                service.wakeNotifier();
                return;
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
                printableJob(new ImagePrinter((URL)data));
                service.wakeNotifier();
                return;
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            }
        } else if (repClassName.equals("java.awt.print.Pageable")) {
            try {
                pageableJob((Pageable)doc.getPrintData());
                service.wakeNotifier();
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
                service.wakeNotifier();
                return;
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            } catch (IOException ioe) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(ioe);
            }
        } else if (repClassName.equals("[B") ||
                   repClassName.equals("java.io.InputStream") ||
                   repClassName.equals("java.net.URL")) {

            if (repClassName.equals("java.net.URL")) {
                URL url = (URL)data;
                try {
                    instream = url.openStream();
                } catch (IOException e) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException(e.toString());
                }
            } else {
                try {
                    instream = doc.getStreamForBytes();
                } catch (IOException ioe) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException(ioe.toString());
                }
            }

            if (instream == null) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException("No stream for data");
            }

            if (mDestination != null) { // if destination attribute is set
                try {
                    Files.copy(instream, Path.of(mDestination), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException(ioe.toString());
                }
                notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);
                notifyEvent(PrintJobEvent.JOB_COMPLETE);
                service.wakeNotifier();
                return;
            }

            if (!startPrintRawData(service.getName(), jobName)) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException("Print job failed to start.");
            }
            BufferedInputStream  bin = new BufferedInputStream(instream);
            int bread = 0;
            try {
                byte[] buffer = new byte[PRINTBUFFERLEN];

                while ((bread = bin.read(buffer, 0, PRINTBUFFERLEN)) >=0) {
                    if (!printRawData(buffer, bread)) {
                        bin.close();
                        notifyEvent(PrintJobEvent.JOB_FAILED);
                        throw new PrintException ("Problem while spooling data");
                    }
                }
                bin.close();
                if (!endPrintRawData()) {
                    notifyEvent(PrintJobEvent.JOB_FAILED);
                    throw new PrintException("Print job failed to close properly.");
                }
                notifyEvent(PrintJobEvent.DATA_TRANSFER_COMPLETE);
            } catch (IOException e) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException (e.toString());
            } finally {
                notifyEvent(PrintJobEvent.NO_MORE_EVENTS);
            }
        } else {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException("unrecognized class: "+repClassName);
        }
        service.wakeNotifier();
    }

    public void printableJob(Printable printable) throws PrintException {
        try {
            synchronized(this) {
                if (job != null) { // shouldn't happen
                    throw new PrintException("already printing");
                } else {
                    job = new sun.awt.windows.WPrinterJob();
                }
            }
            PrintService svc = getPrintService();
            job.setPrintService(svc);
            if (copies == 0) {
                Copies c = (Copies)svc.getDefaultAttributeValue(Copies.class);
                copies = c.getValue();
            }

            if (mediaName == null) {
                Object media = svc.getDefaultAttributeValue(Media.class);
                if (media instanceof MediaSizeName) {
                    mediaName = (MediaSizeName) media;
                    mediaSize = MediaSize.getMediaSizeForName(mediaName);
                }
            }

            if (orient == null) {
                orient =
                    (OrientationRequested)svc.getDefaultAttributeValue(OrientationRequested.class);
            }

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
                    job = new sun.awt.windows.WPrinterJob();
                }
            }
            PrintService svc = getPrintService();
            job.setPrintService(svc);
            if (copies == 0) {
                Copies c = (Copies)svc.getDefaultAttributeValue(Copies.class);
                copies = c.getValue();
            }
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

        if (reqAttrSet.get(Fidelity.class) == Fidelity.FIDELITY_TRUE) {
            fidelity = true;
        } else {
            fidelity = false;
        }

        Class<? extends Attribute> category;
        Attribute [] attrs = reqAttrSet.toArray();
        for (int i=0; i<attrs.length; i++) {
            Attribute attr = attrs[i];
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
            } else if (category == JobName.class) {
                jobName = ((JobName)attr).getValue();
            } else if (category == Copies.class) {
                copies = ((Copies)attr).getValue();
            } else if (category == Media.class) {
              if (attr instanceof MediaSizeName) {
                    mediaName = (MediaSizeName)attr;
                    // If requested MediaSizeName is not supported,
                    // get the corresponding media size - this will
                    // be used to create a new PageFormat.
                    if (!service.isAttributeValueSupported(attr, null, null)) {
                        mediaSize = MediaSize.getMediaSizeForName(mediaName);
                    }
                }
            } else if (category == OrientationRequested.class) {
                orient = (OrientationRequested)attr;
            }
        }
    }

    private native boolean startPrintRawData(String printerName,
                                             String jobName);
    private native boolean printRawData(byte[] data, int count);
    private native boolean endPrintRawData();

    /* Cancel PrinterJob jobs that haven't yet completed. */
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
