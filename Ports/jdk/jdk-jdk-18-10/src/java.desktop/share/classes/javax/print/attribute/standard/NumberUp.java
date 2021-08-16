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
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code NumberUp} is an integer valued printing attribute class that
 * specifies the number of print-stream pages to impose upon a single side of an
 * instance of a selected medium. That is, if the NumberUp value is <i>n,</i>
 * the printer must place <i>n</i> print-stream pages on a single side of an
 * instance of the selected medium. To accomplish this, the printer may add some
 * sort of translation, scaling, or rotation. This attribute primarily controls
 * the translation, scaling and rotation of print-stream pages.
 * <p>
 * The effect of a {@code NumberUp} attribute on a multidoc print job (a job
 * with multiple documents) depends on whether all the docs have the same number
 * up values specified or whether different docs have different number up values
 * specified, and on the (perhaps defaulted) value of the
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute.
 * <ul>
 *   <li>If all the docs have the same number up value <i>n</i> specified, then
 *   any value of {@link MultipleDocumentHandling MultipleDocumentHandling}
 *   makes sense, and the printer's processing depends on the
 *   {@link MultipleDocumentHandling MultipleDocumentHandling} value:
 *   <ul>
 *     <li>{@code SINGLE_DOCUMENT} -- All the input docs will be combined
 *     together into one output document. Each media impression will consist of
 *     <i>n</i>m print-stream pages from the output document.
 *     <li>{@code SINGLE_DOCUMENT_NEW_SHEET} -- All the input docs will be
 *     combined together into one output document. Each media impression will
 *     consist of <i>n</i> print-stream pages from the output document. However,
 *     the first impression of each input doc will always start on a new media
 *     sheet; this means the last impression of an input doc may have fewer than
 *     <i>n</i> print-stream pages on it.
 *     <li>{@code SEPARATE_DOCUMENTS_UNCOLLATED_COPIES} -- The input docs will
 *     remain separate. Each media impression will consist of <i>n</i>
 *     print-stream pages from the input doc. Since the input docs are separate,
 *     the first impression of each input doc will always start on a new media
 *     sheet; this means the last impression of an input doc may have fewer than
 *     <i>n</i> print-stream pages on it.
 *     <li>{@code SEPARATE_DOCUMENTS_COLLATED_COPIES} -- The input docs will
 *     remain separate. Each media impression will consist of <i>n</i>
 *     print-stream pages from the input doc. Since the input docs are separate,
 *     the first impression of each input doc will always start on a new media
 *     sheet; this means the last impression of an input doc may have fewer than
 *     <i>n</i> print-stream pages on it.
 *   </ul>
 *   <ul>
 *     <li>{@code SINGLE_DOCUMENT} -- All the input docs will be combined
 *     together into one output document. Each media impression will consist of
 *     <i>n<sub>i</sub></i> print-stream pages from the output document, where
 *     <i>i</i> is the number of the input doc corresponding to that point in
 *     the output document. When the next input doc has a different number up
 *     value from the previous input doc, the first print-stream page of the
 *     next input doc goes at the start of the next media impression, possibly
 *     leaving fewer than the full number of print-stream pages on the previous
 *     media impression.
 *     <li>{@code SINGLE_DOCUMENT_NEW_SHEET} -- All the input docs will be
 *     combined together into one output document. Each media impression will
 *     consist of <i>n</i> print-stream pages from the output document. However,
 *     the first impression of each input doc will always start on a new media
 *     sheet; this means the last impression of an input doc may have fewer than
 *     <i>n</i> print-stream pages on it.
 *     <li>{@code SEPARATE_DOCUMENTS_UNCOLLATED_COPIES} -- The input docs will
 *     remain separate. For input doc <i>i,</i> each media impression will
 *     consist of <i>n<sub>i</sub></i> print-stream pages from the input doc.
 *     Since the input docs are separate, the first impression of each input doc
 *     will always start on a new media sheet; this means the last impression of
 *     an input doc may have fewer than <i>n<sub>i</sub></i> print-stream pages
 *     on it.
 *     <li>{@code SEPARATE_DOCUMENTS_COLLATED_COPIES} -- The input docs will
 *     remain separate. For input doc <i>i,</i> each media impression will
 *     consist of <i>n<sub>i</sub></i> print-stream pages from the input doc.
 *     Since the input docs are separate, the first impression of each input doc
 *     will always start on a new media sheet; this means the last impression of
 *     an input doc may have fewer than <i>n<sub>i</sub></i> print-stream pages
 *     on it.
 *   </ul>
 * </ul>
 * <b>IPP Compatibility:</b> The integer value gives the IPP integer value. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class NumberUp extends IntegerSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3040436486786527811L;

    /**
     * Construct a new number up attribute with the given integer value.
     *
     * @param  value Integer value
     * @throws IllegalArgumentException if {@code value < 1}
     */
    public NumberUp(int value) {
        super (value, 1, Integer.MAX_VALUE);
    }

    /**
     * Returns whether this number up attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code NumberUp}.
     *   <li>This number up attribute's value and {@code object}'s value are
     *   equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this number up
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals(object) && object instanceof NumberUp);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code NumberUp}, the category is class {@code NumberUp}
     * itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return NumberUp.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code NumberUp}, the category name is {@code "number-up"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "number-up";
    }
}
