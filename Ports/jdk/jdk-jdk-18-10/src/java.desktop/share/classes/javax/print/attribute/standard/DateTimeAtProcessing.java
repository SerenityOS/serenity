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

/**
 * Class {@code DateTimeAtProcessing} is a printing attribute class, a date-time
 * attribute, that indicates the date and time at which the Print Job first
 * began processing.
 * <p>
 * To construct a {@code DateTimeAtProcessing} attribute from separate values of
 * the year, month, day, hour, minute, and so on, use a
 * {@link Calendar Calendar} object to construct a {@link Date Date} object,
 * then use the {@link Date Date} object to construct the DateTimeAtProcessing
 * attribute. To convert a {@code DateTimeAtProcessing} attribute to separate
 * values of the year, month, day, hour, minute, and so on, create a
 * {@link Calendar Calendar} object and set it to the {@link Date Date} from the
 * {@code DateTimeAtProcessing} attribute.
 * <p>
 * <b>IPP Compatibility:</b> The information needed to construct an IPP
 * "date-time-at-processing" attribute can be obtained as described above. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class DateTimeAtProcessing extends DateTimeSyntax
        implements PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3710068197278263244L;

    /**
     * Construct a new date-time at processing attribute with the given
     * {@link Date Date} value.
     *
     * @param  dateTime {@link Date Date} value
     * @throws NullPointerException if {@code dateTime} is {@code null}
     */
    public DateTimeAtProcessing(Date dateTime) {
        super (dateTime);
    }

    /**
     * Returns whether this date-time at processing attribute is equivalent to
     * the passed in object. To be equivalent, all of the following conditions
     * must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class
     *   {@code DateTimeAtProcessing}.
     *   <li>This date-time at processing attribute's {@link Date Date}
     *   value and {@code object}'s {@link Date Date} value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this date-time at
     *         processing attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return(super.equals (object) &&
               object instanceof DateTimeAtProcessing);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code DateTimeAtProcessing}, the category is class
     * {@code DateTimeAtProcessing} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtProcessing.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code DateTimeAtProcessing}, the category name is
     * {@code "date-time-at-processing"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "date-time-at-processing";
    }
}
