/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import javax.print.event.PrintJobAttributeListener;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
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

import java.awt.print.*;

public class PSStreamPrintJob implements CancelablePrintJob {

    private transient Vector<PrintJobListener> jobListeners;
    private transient Vector<PrintJobAttributeListener> attrListeners;
    private transient Vector<PrintJobAttributeSet> listenedAttributeSets;

    private PSStreamPrintService service;
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
    private MediaSize     mediaSize = MediaSize.NA.LETTER;
    private OrientationRequested orient = OrientationRequested.PORTRAIT;

    PSStreamPrintJob(PSStreamPrintService service) {
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

                        case PrintJobEvent.JOB_COMPLETE :
                            listener.printJobCompleted(event);
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
                printableJob(new ImagePrinter(instream), reqAttrSet);
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
                printableJob(new ImagePrinter((URL)data), reqAttrSet);
                return;
            } catch (ClassCastException cce) {
                notifyEvent(PrintJobEvent.JOB_FAILED);
                throw new PrintException(cce);
            }
        } else if (repClassName.equals("java.awt.print.Pageable")) {
            try {
                pageableJob((Pageable)doc.getPrintData(), reqAttrSet);
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
                printableJob((Printable)doc.getPrintData(), reqAttrSet);
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
    }

    public void printableJob(Printable printable,
                             PrintRequestAttributeSet attributes)
        throws PrintException {
        try {
            synchronized(this) {
                if (job != null) { // shouldn't happen
                    throw new PrintException("already printing");
                } else {
                    job = new PSPrinterJob();
                }
            }
            job.setPrintService(getPrintService());
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
            job.print(attributes);
            notifyEvent(PrintJobEvent.JOB_COMPLETE);
            return;
        } catch (PrinterException pe) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException(pe);
        } finally {
            printReturned = true;
        }
    }

    public void pageableJob(Pageable pageable,
                            PrintRequestAttributeSet attributes)
        throws PrintException {
        try {
            synchronized(this) {
                if (job != null) { // shouldn't happen
                    throw new PrintException("already printing");
                } else {
                    job = new PSPrinterJob();
                }
            }
            job.setPrintService(getPrintService());
            job.setPageable(pageable);
            job.print(attributes);
            notifyEvent(PrintJobEvent.JOB_COMPLETE);
            return;
        } catch (PrinterException pe) {
            notifyEvent(PrintJobEvent.JOB_FAILED);
            throw new PrintException(pe);
        } finally {
            printReturned = true;
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
            if (category == JobName.class) {
                jobName = ((JobName)attr).getValue();
            } else if (category == Copies.class) {
                copies = ((Copies)attr).getValue();
            } else if (category == Media.class) {
                if (attr instanceof MediaSizeName &&
                    service.isAttributeValueSupported(attr, null, null)) {
                    mediaSize =
                        MediaSize.getMediaSizeForName((MediaSizeName)attr);
                }
            } else if (category == OrientationRequested.class) {
                orient = (OrientationRequested)attr;
            }
        }
    }

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
