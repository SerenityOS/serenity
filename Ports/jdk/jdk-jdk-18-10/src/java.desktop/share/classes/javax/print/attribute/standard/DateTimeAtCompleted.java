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
 * Class {@code DateTimeAtCompleted} is a printing attribute class, a date-time
 * attribute, that indicates the date and time at which the Print Job completed
 * (or was canceled or aborted).
 * <p>
 * To construct a {@code DateTimeAtCompleted} attribute from separate values of
 * the year, month, day, hour, minute, and so on, use a
 * {@link Calendar Calendar} object to construct a {@link Date Date} object,
 * then use the {@link Date Date} object to construct the DateTimeAtCompleted
 * attribute. To convert a {@code DateTimeAtCompleted} attribute to separate
 * values of the year, month, day, hour, minute, and so on, create a
 * {@link Calendar Calendar} object and set it to the {@link Date Date} from the
 * {@code DateTimeAtCompleted} attribute.
 * <p>
 * <b>IPP Compatibility:</b> The information needed to construct an IPP
 * "date-time-at-completed" attribute can be obtained as described above. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class DateTimeAtCompleted extends DateTimeSyntax
    implements PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6497399708058490000L;

    /**
     * Construct a new date-time at completed attribute with the given
     * {@link Date Date} value.
     *
     * @param  dateTime {@link Date Date} value
     * @throws NullPointerException if {@code dateTime} is {@code null}
     */
    public DateTimeAtCompleted(Date dateTime) {
        super (dateTime);
    }

    /**
     * Returns whether this date-time at completed attribute is equivalent to
     * the passed in object. To be equivalent, all of the following conditions
     * must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code DateTimeAtCompleted}.
     *   <li>This date-time at completed attribute's {@link Date Date}
     *   value and {@code object}'s {@link Date Date} value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this date-time at
     *         completed attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return(super.equals (object) &&
               object instanceof DateTimeAtCompleted);
    }

    // Exported operations inherited and implemented from interface Attribute.

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code DateTimeAtCompleted}, the category is class
     * {@code DateTimeAtCompleted} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtCompleted.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code DateTimeAtCompleted}, the category name is
     * {@code "date-time-at-completed"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "date-time-at-completed";
    }
}
