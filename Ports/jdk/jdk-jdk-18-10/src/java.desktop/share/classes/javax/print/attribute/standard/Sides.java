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
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code Sides} is a printing attribute class, an enumeration, that
 * specifies how print-stream pages are to be imposed upon the sides of an
 * instance of a selected medium, i.e., an impression.
 * <p>
 * The effect of a {@code Sides} attribute on a multidoc print job (a job with
 * multiple documents) depends on whether all the docs have the same sides
 * values specified or whether different docs have different sides values
 * specified, and on the (perhaps defaulted) value of the
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute.
 * <ul>
 *   <li>If all the docs have the same sides value <i>n</i> specified, then any
 *   value of {@link MultipleDocumentHandling MultipleDocumentHandling} makes
 *   sense, and the printer's processing depends on the
 *   {@link MultipleDocumentHandling MultipleDocumentHandling} value:
 *   <ul>
 *     <li>{@code SINGLE_DOCUMENT} -- All the input docs will be combined
 *     together into one output document. Each media sheet will consist of
 *     <i>n</i> impressions from the output document.
 *     <li>{@code SINGLE_DOCUMENT_NEW_SHEET} -- All the input docs will be
 *     combined together into one output document. Each media sheet will consist
 *     of <i>n</i> impressions from the output document. However, the first
 *     impression of each input doc will always start on a new media sheet; this
 *     means the last media sheet of an input doc may have only one impression
 *     on it.
 *     <li>{@code SEPARATE_DOCUMENTS_UNCOLLATED_COPIES} -- The input docs will
 *     remain separate. Each media sheet will consist of <i>n</i> impressions
 *     from the input doc. Since the input docs are separate, the first
 *     impression of each input doc will always start on a new media sheet; this
 *     means the last media sheet of an input doc may have only one impression
 *     on it.
 *     <li>{@code SEPARATE_DOCUMENTS_COLLATED_COPIES} -- The input docs will
 *     remain separate. Each media sheet will consist of <i>n</i> impressions
 *     from the input doc. Since the input docs are separate, the first
 *     impression of each input doc will always start on a new media sheet; this
 *     means the last media sheet of an input doc may have only one impression
 *     on it.
 *   </ul>
 *   <ul>
 *     <li>{@code SINGLE_DOCUMENT} -- All the input docs will be combined
 *     together into one output document. Each media sheet will consist of
 *     <i>n<sub>i</sub></i> impressions from the output document, where <i>i</i>
 *     is the number of the input doc corresponding to that point in the output
 *     document. When the next input doc has a different sides value from the
 *     previous input doc, the first print-stream page of the next input doc
 *     goes at the start of the next media sheet, possibly leaving only one
 *     impression on the previous media sheet.
 *     <li>{@code SINGLE_DOCUMENT_NEW_SHEET} -- All the input docs will be
 *     combined together into one output document. Each media sheet will consist
 *     of <i>n</i> impressions from the output document. However, the first
 *     impression of each input doc will always start on a new media sheet; this
 *     means the last impression of an input doc may have only one impression on
 *     it.
 *     <li>{@code SEPARATE_DOCUMENTS_UNCOLLATED_COPIES} -- The input docs will
 *     remain separate. For input doc <i>i,</i> each media sheet will consist of
 *     <i>n<sub>i</sub></i> impressions from the input doc. Since the input docs
 *     are separate, the first impression of each input doc will always start on
 *     a new media sheet; this means the last media sheet of an input doc may
 *     have only one impression on it.
 *     <li>{@code SEPARATE_DOCUMENTS_COLLATED_COPIES} -- The input docs will
 *     remain separate. For input doc <i>i,</i> each media sheet will consist of
 *     <i>n<sub>i</sub></i> impressions from the input doc. Since the input docs
 *     are separate, the first impression of each input doc will always start on
 *     a new media sheet; this means the last media sheet of an input doc may
 *     have only one impression on it.
 *   </ul>
 * </ul>
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public final class Sides extends EnumSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -6890309414893262822L;

    /**
     * Imposes each consecutive print-stream page upon the same side of
     * consecutive media sheets.
     */
    public static final Sides ONE_SIDED = new Sides(0);

    /**
     * Imposes each consecutive pair of print-stream pages upon front and back
     * sides of consecutive media sheets, such that the orientation of each pair
     * of print-stream pages on the medium would be correct for the reader as if
     * for binding on the long edge. This imposition is also known as "duplex"
     * (see {@link #DUPLEX DUPLEX}).
     */
    public static final Sides TWO_SIDED_LONG_EDGE = new Sides(1);

    /**
     * Imposes each consecutive pair of print-stream pages upon front and back
     * sides of consecutive media sheets, such that the orientation of each pair
     * of print-stream pages on the medium would be correct for the reader as if
     * for binding on the short edge. This imposition is also known as "tumble"
     * (see {@link #TUMBLE TUMBLE}).
     */
    public static final Sides TWO_SIDED_SHORT_EDGE = new Sides(2);

    /**
     * An alias for "two sided long edge" (see
     * {@link #TWO_SIDED_LONG_EDGE TWO_SIDED_LONG_EDGE}).
     */
    public static final Sides DUPLEX = TWO_SIDED_LONG_EDGE;

    /**
     * An alias for "two sided short edge" (see
     * {@link #TWO_SIDED_SHORT_EDGE TWO_SIDED_SHORT_EDGE}).
     */
    public static final Sides TUMBLE = TWO_SIDED_SHORT_EDGE;

    /**
     * Construct a new sides enumeration value with the given integer value.
     *
     * @param  value Integer value
     */
    protected Sides(int value) {
        super (value);
    }

    /**
     * The string table for class {@code Sides}.
     */
    private static final String[] myStringTable = {
        "one-sided",
        "two-sided-long-edge",
        "two-sided-short-edge"
    };

    /**
     * The enumeration value table for class {@code Sides}.
     */
    private static final Sides[] myEnumValueTable = {
        ONE_SIDED,
        TWO_SIDED_LONG_EDGE,
        TWO_SIDED_SHORT_EDGE
    };

    /**
     * Returns the string table for class {@code Sides}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code Sides}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code Sides}, the category is class {@code Sides} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return Sides.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code Sides}, the category name is {@code "sides"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "sides";
    }
}
