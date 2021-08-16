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

/**
 * Class {@code JobStateReason} is a printing attribute class, an enumeration,
 * that provides additional information about the job's current state, i.e.,
 * information that augments the value of the job's {@link JobState JobState}
 * attribute. Class {@code JobStateReason} defines standard job state reason
 * values. A Print Service implementation only needs to report those job state
 * reasons which are appropriate for the particular implementation; it does not
 * have to report every defined job state reason.
 * <p>
 * Instances of {@code JobStateReason} do not appear in a Print Job's attribute
 * set directly. Rather, a {@link JobStateReasons JobStateReasons} attribute
 * appears in the Print Job's attribute set. The
 * {@link JobStateReasons JobStateReasons} attribute contains zero, one, or more
 * than one {@code JobStateReason} objects which pertain to the Print Job's
 * status. The printer adds a JobStateReason object to the Print Job's
 * {@link JobStateReasons JobStateReasons} attribute when the corresponding
 * condition becomes true of the Print Job, and the printer removes the
 * {@code JobStateReason} object again when the corresponding condition becomes
 * false, regardless of whether the Print Job's overall
 * {@link JobState JobState} also changed.
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public class JobStateReason extends EnumSyntax implements Attribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8765894420449009168L;

    /**
     * The printer has created the Print Job, but the printer has not finished
     * accessing or accepting all the print data yet.
     */
    public static final JobStateReason
        JOB_INCOMING = new JobStateReason(0);

    /**
     * The printer has created the Print Job, but the printer is expecting
     * additional print data before it can move the job into the
     * {@code PROCESSING} state. If a printer starts processing before it has
     * received all data, the printer removes the {@code JOB_DATA_INSUFFICIENT}
     * reason, but the {@code JOB_INCOMING} reason remains. If a printer starts
     * processing after it has received all data, the printer removes the
     * {@code JOB_DATA_INSUFFICIENT} and {@code JOB_INCOMING} reasons at the
     * same time.
     */
    public static final JobStateReason
        JOB_DATA_INSUFFICIENT = new JobStateReason(1);

    /**
     * The printer could not access one or more documents passed by reference
     * (i.e., the print data representation object is a {@code URL}). This
     * reason is intended to cover any file access problem,including file does
     * not exist and access denied because of an access control problem. Whether
     * the printer aborts the job and moves the job to the {@code ABORTED} job
     * state or prints all documents that are accessible and moves the job to
     * the {@code COMPLETED} job state and adds the
     * {@code COMPLETED_WITH_ERRORS} reason to the job's
     * {@link JobStateReasons JobStateReasons} attribute depends on
     * implementation and/or site policy. This value should be supported if the
     * printer supports doc flavors with {@code URL} print data representation
     * objects.
     */
    public static final JobStateReason
        DOCUMENT_ACCESS_ERROR = new JobStateReason(2);

    /**
     * The job was not completely submitted for some unforeseen reason.
     * Possibilities include (1) the printer has crashed before the job was
     * fully submitted by the client, (2) the printer or the document transfer
     * method has crashed in some non-recoverable way before the document data
     * was entirely transferred to the printer, (3) the client crashed before
     * the job was fully submitted.
     */
    public static final JobStateReason
        SUBMISSION_INTERRUPTED = new JobStateReason(3);

    /**
     * The printer is transmitting the job to the output device.
     */
    public static final JobStateReason
        JOB_OUTGOING = new JobStateReason(4);

    /**
     * The value of the job's {@link JobHoldUntil JobHoldUntil} attribute was
     * specified with a date-time that is still in the future. The job must not
     * be a candidate for processing until this reason is removed and there are
     * no other reasons to hold the job. This value should be supported if the
     * {@link JobHoldUntil JobHoldUntil} job template attribute is supported.
     */
    public static final JobStateReason
        JOB_HOLD_UNTIL_SPECIFIED = new JobStateReason(5);

    /**
     * At least one of the resources needed by the job, such as media, fonts,
     * resource objects, etc., is not ready on any of the physical printers for
     * which the job is a candidate. This condition may be detected when the job
     * is accepted, or subsequently while the job is pending or processing,
     * depending on implementation. The job may remain in its current state or
     * be moved to the {@code PENDING_HELD} state, depending on implementation
     * and/or job scheduling policy.
     */
    public static final JobStateReason
        RESOURCES_ARE_NOT_READY = new JobStateReason(6);

    /**
     * The value of the printer's {@link PrinterStateReasons
     * PrinterStateReasons} attribute contains a
     * {@link PrinterStateReason PrinterStateReason} value of
     * {@code STOPPED_PARTLY}.
     */
    public static final JobStateReason
        PRINTER_STOPPED_PARTLY = new JobStateReason(7);

    /**
     * The value of the printer's {@link PrinterState PrinterState} attribute ia
     * {@code STOPPED}.
     */
    public static final JobStateReason
        PRINTER_STOPPED = new JobStateReason(8);

    /**
     * The job is in the {@code PROCESSING} state, but more specifically, the
     * printer ia interpreting the document data.
     */
    public static final JobStateReason
        JOB_INTERPRETING = new JobStateReason(9);

    /**
     * The job is in the {@code PROCESSING} state, but more specifically, the
     * printer has queued the document data.
     */
    public static final JobStateReason JOB_QUEUED = new JobStateReason(10);

    /**
     * The job is in the {@code PROCESSING} state, but more specifically, the
     * printer is interpreting document data and producing another electronic
     * representation.
     */
    public static final JobStateReason
        JOB_TRANSFORMING = new JobStateReason(11);

    /**
     * The job is in the {@code PENDING_HELD}, {@code PENDING}, or
     * {@code PROCESSING} state, but more specifically, the printer has
     * completed enough processing of the document to be able to start marking
     * and the job is waiting for the marker. Systems that require human
     * intervention to release jobs put the job into the {@code PENDING_HELD}
     * job state. Systems that automatically select a job to use the marker put
     * the job into the {@code PENDING} job state or keep the job in the
     * {@code PROCESSING} job state while waiting for the marker, depending on
     * implementation. All implementations put the job into (or back into) the
     * {@code PROCESSING} state when marking does begin.
     */
    public static final JobStateReason
        JOB_QUEUED_FOR_MARKER = new JobStateReason(12);

    /**
     * The output device is marking media. This value is useful for printers
     * which spend a great deal of time processing (1) when no marking is
     * happening and then want to show that marking is now happening or (2) when
     * the job is in the process of being canceled or aborted while the job
     * remains in the {@code PROCESSING} state, but the marking has not yet
     * stopped so that impression or sheet counts are still increasing for the
     * job.
     */
    public static final JobStateReason
        JOB_PRINTING = new JobStateReason(13);

    /**
     * The job was canceled by the owner of the job, i.e., by a user whose
     * authenticated identity is the same as the value of the originating user
     * that created the Print Job, or by some other authorized end-user, such as
     * a member of the job owner's security group. This value should be
     * supported.
     */
    public static final JobStateReason
        JOB_CANCELED_BY_USER = new JobStateReason(14);

    /**
     * The job was canceled by the operator, i.e., by a user who has been
     * authenticated as having operator privileges (whether local or remote). If
     * the security policy is to allow anyone to cancel anyone's job, then this
     * value may be used when the job is canceled by someone other than the
     * owner of the job. For such a security policy, in effect, everyone is an
     * operator as far as canceling jobs is concerned. This value should be
     * supported if the implementation permits canceling by someone other than
     * the owner of the job.
     */
    public static final JobStateReason
        JOB_CANCELED_BY_OPERATOR = new JobStateReason(15);

    /**
     * The job was canceled by an unidentified local user, i.e., a user at a
     * console at the device. This value should be supported if the
     * implementation supports canceling jobs at the console.
     */
    public static final JobStateReason
        JOB_CANCELED_AT_DEVICE = new JobStateReason(16);

    /**
     * The job was aborted by the system. Either the job (1) is in the process
     * of being aborted, (2) has been aborted by the system and placed in the
     * {@code ABORTED} state, or (3) has been aborted by the system and placed
     * in the {@code PENDING_HELD} state, so that a user or operator can
     * manually try the job again. This value should be supported.
     */
    public static final JobStateReason
        ABORTED_BY_SYSTEM = new JobStateReason(17);

    /**
     * The job was aborted by the system because the printer determined while
     * attempting to decompress the document's data that the compression is
     * actually not among those supported by the printer. This value must be
     * supported, since {@link Compression Compression} is a required doc
     * description attribute.
     */
    public static final JobStateReason
        UNSUPPORTED_COMPRESSION = new JobStateReason(18);

    /**
     * The job was aborted by the system because the printer encountered an
     * error in the document data while decompressing it. If the printer posts
     * this reason, the document data has already passed any tests that would
     * have led to the {@code UNSUPPORTED_COMPRESSION} job state reason.
     */
    public static final JobStateReason
        COMPRESSION_ERROR = new JobStateReason(19);

    /**
     * The job was aborted by the system because the document data's document
     * format (doc flavor) is not among those supported by the printer. If the
     * client specifies a doc flavor with a MIME type of
     * {@code "application/octet-stream"}, the printer may abort the job if the
     * printer cannot determine the document data's actual format through
     * auto-sensing (even if the printer supports the document format if
     * specified explicitly). This value must be supported, since a doc flavor
     * is required to be specified for each doc.
     */
    public static final JobStateReason
        UNSUPPORTED_DOCUMENT_FORMAT = new JobStateReason(20);

    /**
     * The job was aborted by the system because the printer encountered an
     * error in the document data while processing it. If the printer posts this
     * reason, the document data has already passed any tests that would have
     * led to the {@code UNSUPPORTED_DOCUMENT_FORMAT} job state reason.
     */
    public static final JobStateReason
        DOCUMENT_FORMAT_ERROR = new JobStateReason(21);

    /**
     * The requester has canceled the job or the printer has aborted the job,
     * but the printer is still performing some actions on the job until a
     * specified stop point occurs or job termination/cleanup is completed.
     * <p>
     * If the implementation requires some measurable time to cancel the job in
     * the {@code PROCESSING} or {@code PROCESSING_STOPPED} job states, the
     * printer must use this reason to indicate that the printer is still
     * performing some actions on the job while the job remains in the
     * {@code PROCESSING} or {@code PROCESSING_STOPPED} state. After all the
     * job's job description attributes have stopped incrementing, the printer
     * moves the job from the PROCESSING state to the {@code CANCELED} or
     * {@code ABORTED} job states.
     */
    public static final JobStateReason
        PROCESSING_TO_STOP_POINT = new JobStateReason(22);

    /**
     * The printer is off-line and accepting no jobs. All {@code PENDING} jobs
     * are put into the {@code PENDING_HELD} state. This situation could be true
     * if the service's or document transform's input is impaired or broken.
     */
    public static final JobStateReason
        SERVICE_OFF_LINE = new JobStateReason(23);

    /**
     * The job completed successfully. This value should be supported.
     */
    public static final JobStateReason
        JOB_COMPLETED_SUCCESSFULLY = new JobStateReason(24);

    /**
     * The job completed with warnings. This value should be supported if the
     * implementation detects warnings.
     */
    public static final JobStateReason
        JOB_COMPLETED_WITH_WARNINGS = new JobStateReason(25);

    /**
     * The job completed with errors (and possibly warnings too). This value
     * should be supported if the implementation detects errors.
     */
    public static final JobStateReason
        JOB_COMPLETED_WITH_ERRORS = new JobStateReason(26);

    /**
     * This job is retained and is currently able to be restarted. If
     * {@code JOB_RESTARTABLE} is contained in the job's
     * {@link JobStateReasons JobStateReasons} attribute, then the printer must
     * accept a request to restart that job. This value should be supported if
     * restarting jobs is supported. <i>[The capability for restarting jobs is
     * not in the Java Print Service API at present.]</i>
     */
    public static final JobStateReason
        JOB_RESTARTABLE = new JobStateReason(27);

    /**
     * The job has been forwarded to a device or print system that is unable to
     * send back status. The printer sets the job's {@link JobState JobState}
     * attribute to {@code COMPLETED} and adds the {@code QUEUED_IN_DEVICE}
     * reason to the job's {@link JobStateReasons JobStateReasons} attribute to
     * indicate that the printer has no additional information about the job and
     * never will have any better information.
     */
    public static final JobStateReason
        QUEUED_IN_DEVICE = new JobStateReason(28);

    /**
     * Construct a new job state reason enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected JobStateReason(int value) {
        super (value);
    }

    /**
     * The string table for class {@code JobStateReason}.
     */
    private static final String[] myStringTable = {
        "job-incoming",
        "job-data-insufficient",
        "document-access-error",
        "submission-interrupted",
        "job-outgoing",
        "job-hold-until-specified",
        "resources-are-not-ready",
        "printer-stopped-partly",
        "printer-stopped",
        "job-interpreting",
        "job-queued",
        "job-transforming",
        "job-queued-for-marker",
        "job-printing",
        "job-canceled-by-user",
        "job-canceled-by-operator",
        "job-canceled-at-device",
        "aborted-by-system",
        "unsupported-compression",
        "compression-error",
        "unsupported-document-format",
        "document-format-error",
        "processing-to-stop-point",
        "service-off-line",
        "job-completed-successfully",
        "job-completed-with-warnings",
        "job-completed-with-errors",
        "job-restartable",
        "queued-in-device"};

    /**
     * The enumeration value table for class {@code JobStateReason}.
     */
    private static final JobStateReason[] myEnumValueTable = {
        JOB_INCOMING,
        JOB_DATA_INSUFFICIENT,
        DOCUMENT_ACCESS_ERROR,
        SUBMISSION_INTERRUPTED,
        JOB_OUTGOING,
        JOB_HOLD_UNTIL_SPECIFIED,
        RESOURCES_ARE_NOT_READY,
        PRINTER_STOPPED_PARTLY,
        PRINTER_STOPPED,
        JOB_INTERPRETING,
        JOB_QUEUED,
        JOB_TRANSFORMING,
        JOB_QUEUED_FOR_MARKER,
        JOB_PRINTING,
        JOB_CANCELED_BY_USER,
        JOB_CANCELED_BY_OPERATOR,
        JOB_CANCELED_AT_DEVICE,
        ABORTED_BY_SYSTEM,
        UNSUPPORTED_COMPRESSION,
        COMPRESSION_ERROR,
        UNSUPPORTED_DOCUMENT_FORMAT,
        DOCUMENT_FORMAT_ERROR,
        PROCESSING_TO_STOP_POINT,
        SERVICE_OFF_LINE,
        JOB_COMPLETED_SUCCESSFULLY,
        JOB_COMPLETED_WITH_WARNINGS,
        JOB_COMPLETED_WITH_ERRORS,
        JOB_RESTARTABLE,
        QUEUED_IN_DEVICE};

    /**
     * Returns the string table for class {@code JobStateReason}.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class {@code JobStateReason}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobStateReason} and any vendor-defined subclasses, the
     * category is class {@code JobStateReason} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobStateReason.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobStateReason} and any vendor-defined subclasses, the
     * category name is {@code "job-state-reason"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-state-reason";
    }
}
