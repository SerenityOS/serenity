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
import java.util.Calendar;
import java.util.Date;

import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code JobHoldUntil} is a printing attribute class, a date-time
 * attribute, that specifies the exact date and time at which the job must
 * become a candidate for printing.
 * <p>
 * If the value of this attribute specifies a date-time that is in the future,
 * the printer should add the {@link JobStateReason JobStateReason} value of
 * {@code JOB_HOLD_UNTIL_SPECIFIED} to the job's
 * {@link JobStateReasons JobStateReasons} attribute, must move the job to the
 * {@code PENDING_HELD} state, and must not schedule the job for printing until
 * the specified date-time arrives.
 * <p>
 * When the specified date-time arrives, the printer must remove the
 * {@link JobStateReason JobStateReason} value of
 * {@code JOB_HOLD_UNTIL_SPECIFIED} from the job's
 * {@link JobStateReasons JobStateReasons} attribute, if present. If there are
 * no other job state reasons that keep the job in the {@code PENDING_HELD}
 * state, the printer must consider the job as a candidate for processing by
 * moving the job to the PENDING state.
 * <p>
 * If the specified date-time has already passed, the job must be a candidate
 * for processing immediately. Thus, one way to make the job immediately become
 * a candidate for processing is to specify a {@code JobHoldUntil} attribute
 * constructed like this
 * (denoting a date-time of January 1, 1970, 00:00:00 GMT):
 * <pre>
 *     JobHoldUntil immediately = new JobHoldUntil (new Date (0L));
 * </pre>
 * <p>
 * If the client does not supply this attribute in a Print Request and the
 * printer supports this attribute, the printer must use its
 * (implementation-dependent) default {@code JobHoldUntil} value at job
 * submission time (unlike most job template attributes that are used if
 * necessary at job processing time).
 * <p>
 * To construct a {@code JobHoldUntil} attribute from separate values of the
 * year, month, day, hour, minute, and so on, use a {@link Calendar Calendar}
 * object to construct a {@link Date Date} object, then use the
 * {@link Date Date} object to construct the {@code JobHoldUntil} attribute. To
 * convert a {@code JobHoldUntil} attribute to separate values of the year,
 * month, day, hour, minute, and so on, create a {@link Calendar Calendar}
 * object and set it to the {@link Date Date} from the {@code JobHoldUntil}
 * attribute.
 * <p>
 * <b>IPP Compatibility:</b> Although IPP supports a "job-hold-until" attribute
 * specified as a keyword, IPP does not at this time support a "job-hold-until"
 * attribute specified as a date and time. However, the date and time can be
 * converted to one of the standard IPP keywords with some loss of precision;
 * for example, a {@code JobHoldUntil} value with today's date and 9:00pm local
 * time might be converted to the standard IPP keyword "night". The category
 * name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class JobHoldUntil extends DateTimeSyntax
        implements PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1664471048860415024L;

    /**
     * Construct a new job hold until date-time attribute with the given
     * {@link Date Date} value.
     *
     * @param  dateTime {@link Date Date} value
     * @throws NullPointerException if {@code dateTime} is {@code null}
     */
    public JobHoldUntil(Date dateTime) {
        super (dateTime);
    }

    /**
     * Returns whether this job hold until attribute is equivalent to the passed
     * in object. To be equivalent, all of the following conditions must be
     * true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code JobHoldUntil}.
     *   <li>This job hold until attribute's {@link Date Date} value and
     *   {@code object}'s {@link Date Date} value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job hold
     *         until attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals(object) && object instanceof JobHoldUntil);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobHoldUntil}, the category is class
     * {@code JobHoldUntil} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobHoldUntil.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobHoldUntil}, the category name is
     * {@code "job-hold-until"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-hold-until";
    }
}
