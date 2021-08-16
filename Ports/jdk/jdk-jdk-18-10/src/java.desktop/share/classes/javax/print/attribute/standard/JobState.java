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

package javax.print.attribute.standard;

import java.io.Serial;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * {@code JobState} is a printing attribute class, an enumeration, that
 * identifies the current state of a print job. Class {@code JobState} defines
 * standard job state values. A Print Service implementation only needs to
 * report those job states which are appropriate for the particular
 * implementation; it does not have to report every defined job state. The
 * {@link JobStateReasons JobStateReasons} attribute augments the
 * {@code JobState} attribute to give more detailed information about the job in
 * the given job state.
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public class JobState extends EnumSyntax implements PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 400465010094018920L;

    /**
     * The job state is unknown.
     */
    public static final JobState UNKNOWN = new JobState(0);

    /**
     * The job is a candidate to start processing, but is not yet processing.
     */
    public static final JobState PENDING = new JobState(3);

    /**
     * The job is not a candidate for processing for any number of reasons but
     * will return to the {@code PENDING} state as soon as the reasons are no
     * longer present. The job's {@link JobStateReasons JobStateReasons}
     * attribute must indicate why the job is no longer a candidate for
     * processing.
     */
    public static final JobState PENDING_HELD = new JobState(4);

    /**
     * The job is processing. One or more of the following activities is
     * occurring:
     * <ol type=1>
     *   <li>The job is using, or is attempting to use, one or more purely
     *   software processes that are analyzing, creating, or interpreting a PDL,
     *   etc.
     *   <li>The job is using, or is attempting to use, one or more hardware
     *   devices that are interpreting a PDL, making marks on a medium, and/or
     *   performing finishing, such as stapling, etc.
     *   <li>The printer has made the job ready for printing, but the output
     *   device is not yet printing it, either because the job hasn't reached
     *   the output device or because the job is queued in the output device or
     *   some other spooler, awaiting the output device to print it.
     * </ol>
     * When the job is in the {@code PROCESSING} state, the entire job state
     * includes the detailed status represented in the printer's
     * {@link PrinterState PrinterState} and
     * {@link PrinterStateReasons PrinterStateReasons} attributes.
     * <p>
     * Implementations may, though they need not, include additional values in
     * the job's {@link JobStateReasons JobStateReasons} attribute to indicate
     * the progress of the job, such as adding the {@code JOB_PRINTING} value to
     * indicate when the output device is actually making marks on paper and/or
     * the {@code PROCESSING_TO_STOP_POINT} value to indicate that the printer
     * is in the process of canceling or aborting the job.
     */
    public static final JobState PROCESSING = new JobState (5);

    /**
     * The job has stopped while processing for any number of reasons and will
     * return to the {@code PROCESSING} state as soon as the reasons are no
     * longer present.
     * <p>
     * The job's {@link JobStateReasons JobStateReasons} attribute may indicate
     * why the job has stopped processing. For example, if the output device is
     * stopped, the {@code PRINTER_STOPPED} value may be included in the job's
     * {@link JobStateReasons JobStateReasons} attribute.
     * <p>
     * <i>Note:</i> When an output device is stopped, the device usually
     * indicates its condition in human readable form locally at the device. A
     * client can obtain more complete device status remotely by querying the
     * printer's {@link PrinterState PrinterState} and
     * {@link PrinterStateReasons PrinterStateReasons} attributes.
     */
    public static final JobState PROCESSING_STOPPED = new JobState (6);

    /**
     * The job has been canceled by some human agency, the printer has completed
     * canceling the job, and all job status attributes have reached their final
     * values for the job. While the printer is canceling the job, the job
     * remains in its current state, but the job's {@link JobStateReasons
     * JobStateReasons} attribute should contain the
     * {@code PROCESSING_TO_STOP_POINT} value and one of the
     * {@code CANCELED_BY_USER}, {@code CANCELED_BY_OPERATOR}, or
     * {@code CANCELED_AT_DEVICE} values. When the job moves to the
     * {@code CANCELED} state, the {@code PROCESSING_TO_STOP_POINT} value, if
     * present, must be removed, but the CANCELED_BY_<i>xxx</i> value, if
     * present, must remain.
     */
    public static final JobState CANCELED = new JobState (7);

    /**
     * The job has been aborted by the system (usually while the job was in the
     * {@code PROCESSING} or {@code PROCESSING_STOPPED} state), the printer has
     * completed aborting the job, and all job status attributes have reached
     * their final values for the job. While the printer is aborting the job,
     * the job remains in its current state, but the job's
     * {@link JobStateReasons JobStateReasons} attribute should contain the
     * {@code PROCESSING_TO_STOP_POINT} and {@code ABORTED_BY_SYSTEM} values.
     * When the job moves to the {@code ABORTED} state, the
     * {@code PROCESSING_TO_STOP_POINT} value, if present, must be removed, but
     * the {@code ABORTED_BY_SYSTEM} value, if present, must remain.
     */
    public static final JobState ABORTED = new JobState (8);

    /**
     * The job has completed successfully or with warnings or errors after
     * processing, all of the job media sheets have been successfully stacked in
     * the appropriate output bin(s), and all job status attributes have reached
     * their final values for the job. The job's
     * {@link JobStateReasons JobStateReasons} attribute should contain one of
     * these values: {@code COMPLETED_SUCCESSFULLY},
     * {@code COMPLETED_WITH_WARNINGS}, or {@code COMPLETED_WITH_ERRORS}.
     */
    public static final JobState COMPLETED = new JobState (9);

    // Hidden constructors.

    /**
     * Construct a new job state enumeration value with the given integer value.
     *
     * @param  value Integer value
     */
    protected JobState(int value) {
        super (value);
    }

    /**
     * The string table for class {@code JobState}.
     */
    private static final String[] myStringTable =
    {"unknown",
     null,
     null,
     "pending",
     "pending-held",
     "processing",
     "processing-stopped",
     "canceled",
     "aborted",
     "completed"};

    /**
     * The enumeration value table for class {@code JobState}.
     */
    private static final JobState[] myEnumValueTable =
    {UNKNOWN,
     null,
     null,
     PENDING,
     PENDING_HELD,
     PROCESSING,
     PROCESSING_STOPPED,
     CANCELED,
     ABORTED,
     COMPLETED};

    /**
     * Returns the string table for class {@code JobState}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code JobState}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobState} and any vendor-defined subclasses, the
     * category is class {@code JobState} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobState.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobState} and any vendor-defined subclasses, the
     * category name is {@code "job-state"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-state";
    }
}
