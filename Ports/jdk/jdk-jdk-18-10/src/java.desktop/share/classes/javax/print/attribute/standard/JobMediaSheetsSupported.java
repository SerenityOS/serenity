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
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Class {@code JobMediaSheetsSupported} is a printing attribute class, a set of
 * integers, that gives the supported values for a
 * {@link JobMediaSheets JobMediaSheets} attribute. It is restricted to a single
 * contiguous range of integers; multiple non-overlapping ranges are not
 * allowed. This gives the lower and upper bounds of the total sizes of print
 * jobs in number of media sheets that the printer will accept.
 * <p>
 * <b>IPP Compatibility:</b> The {@code JobMediaSheetsSupported} attribute's
 * canonical array form gives the lower and upper bound for the range of values
 * to be included in an IPP "job-media-sheets-supported" attribute. See class
 * {@link SetOfIntegerSyntax SetOfIntegerSyntax} for an explanation of canonical
 * array form. The category name returned by {@code getName()} gives the IPP
 * attribute name.
 *
 * @author Alan Kaminsky
 */
public final class JobMediaSheetsSupported extends SetOfIntegerSyntax
        implements SupportedValuesAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 2953685470388672940L;

    /**
     * Construct a new job media sheets supported attribute containing a single
     * range of integers. That is, only those values of {@code JobMediaSheets}
     * in the one range are supported.
     *
     * @param  lowerBound lower bound of the range
     * @param  upperBound upper bound of the range
     * @throws IllegalArgumentException if a {@code null} range is specified or
     *         if a {@code non-null} range is specified with {@code lowerBound}
     *         less than zero
     */
    public JobMediaSheetsSupported(int lowerBound, int upperBound) {
        super (lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 0) {
            throw new IllegalArgumentException
                                ("Job K octets value < 0 specified");
        }
    }

    /**
     * Returns whether this job media sheets supported attribute is equivalent
     * to the passed in object. To be equivalent, all of the following
     * conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class
     *   {@code JobMediaSheetsSupported}.
     *   <li>This job media sheets supported attribute's members and
     *   {@code object}'s members are the same.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job media
     *         sheets supported attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals (object) &&
                object instanceof JobMediaSheetsSupported);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobMediaSheetsSupported}, the category is class
     * {@code JobMediaSheetsSupported} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobMediaSheetsSupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobMediaSheetsSupported}, the category name is
     * {@code "job-media-sheets-supported"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-media-sheets-supported";
    }
}
