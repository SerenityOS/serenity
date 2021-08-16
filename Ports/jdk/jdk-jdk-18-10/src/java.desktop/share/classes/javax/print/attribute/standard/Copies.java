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
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code Copies} is an integer valued printing attribute class that
 * specifies the number of copies to be printed.
 * <p>
 * On many devices the supported number of collated copies will be limited by
 * the number of physical output bins on the device, and may be different from
 * the number of uncollated copies which can be supported.
 * <p>
 * The effect of a {@code Copies} attribute with a value of <i>n</i> on a
 * multidoc print job (a job with multiple documents) depends on the (perhaps
 * defaulted) value of the
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute:
 * <ul>
 *   <li>{@code SINGLE_DOCUMENT} -- The result will be <i>n</i> copies of a
 *   single output document comprising all the input docs.
 *   <li>{@code SINGLE_DOCUMENT_NEW_SHEET} -- The result will be <i>n</i> copies
 *   of a single output document comprising all the input docs, and the first
 *   impression of each input doc will always start on a new media sheet.
 *   <li>{@code SEPARATE_DOCUMENTS_UNCOLLATED_COPIES} -- The result will be
 *   <i>n</i> copies of the first input document, followed by <i>n</i> copies of
 *   the second input document, . . . followed by <i>n</i> copies of the last
 *   input document.
 *   <li>{@code SEPARATE_DOCUMENTS_COLLATED_COPIES} -- The result will be the
 *   first input document, the second input document, . . . the last input
 *   document, the group of documents being repeated <i>n</i> times.
 * </ul>
 * <p>
 * <b>IPP Compatibility:</b> The integer value gives the IPP integer value. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author David Mendenhall
 * @author Alan Kamihensky
 */
public final class Copies extends IntegerSyntax
        implements PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -6426631521680023833L;

    /**
     * Construct a new copies attribute with the given integer value.
     *
     * @param  value Integer value
     * @throws IllegalArgumentException if {@code value < 1}
     */
    public Copies(int value) {
        super (value, 1, Integer.MAX_VALUE);
    }

    /**
     * Returns whether this copies attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code Copies}.
     *   <li>This copies attribute's value and {@code object}'s value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this copies
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return super.equals (object) && object instanceof Copies;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code Copies}, the category is class {@code Copies} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return Copies.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code Copies}, the category name is {@code "copies"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "copies";
    }
}
