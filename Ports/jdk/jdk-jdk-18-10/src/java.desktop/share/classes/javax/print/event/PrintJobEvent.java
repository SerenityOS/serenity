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

package javax.print.event;

import java.io.Serial;

import javax.print.DocPrintJob;

/**
 * Class {@code PrintJobEvent} encapsulates common events a print job reports to
 * let a listener know of progress in the processing of the {@link DocPrintJob}.
 */
public class PrintJobEvent extends PrintEvent {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1711656903622072997L;

    /**
     * The reason of this event.
     */
    private int reason;

    /**
     * The job was canceled by the
     * {@link javax.print.PrintService PrintService}.
     */
    public static final int JOB_CANCELED = 101;

    /**
     * The document is completely printed.
     */
    public static final int JOB_COMPLETE = 102;

    /**
     * The print service reports that the job cannot be completed. The
     * application must resubmit the job.
     */
    public static final int JOB_FAILED = 103;

    /**
     * The print service indicates that a - possibly transient - problem may
     * require external intervention before the print service can continue. One
     * example of an event that can generate this message is when the printer
     * runs out of paper.
     */
    public static final int REQUIRES_ATTENTION = 104;

    /**
     * Not all print services may be capable of delivering interesting events,
     * or even telling when a job is complete. This message indicates the print
     * job has no further information or communication with the print service.
     * This message should always be delivered if a terminal event
     * (completed/failed/canceled) is not delivered. For example, if messages
     * such as {@code JOB_COMPLETE} have NOT been received before receiving this
     * message, the only inference that should be drawn is that the print
     * service does not support delivering such an event.
     */
    public static final int NO_MORE_EVENTS = 105;

    /**
     * The job is not necessarily printed yet, but the data has been transferred
     * successfully from the client to the print service. The client may free
     * data resources.
     */
    public static final int DATA_TRANSFER_COMPLETE = 106;

    /**
     * Constructs a {@code PrintJobEvent} object.
     *
     * @param  source a {@code DocPrintJob} object
     * @param  reason an int specifying the reason
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public PrintJobEvent( DocPrintJob source, int reason) {

        super(source);
        this.reason = reason;
    }

    /**
     * Gets the reason for this event.
     *
     * @return reason int
     */
    public int getPrintEventType() {
        return reason;
    }

    /**
     * Determines the {@code DocPrintJob} to which this print job event
     * pertains.
     *
     * @return the {@code DocPrintJob} object that represents the print job that
     *         reports the events encapsulated by this {@code PrintJobEvent}
     */
    public DocPrintJob getPrintJob() {
        return (DocPrintJob) getSource();
    }
}
