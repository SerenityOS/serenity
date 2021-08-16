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
import javax.print.attribute.PrintServiceAttribute;

/**
 * Class {@code PDLOverrideSupported} is a printing attribute class, an
 * enumeration, that expresses the printer's ability to attempt to override
 * processing instructions embedded in documents' print data with processing
 * instructions specified as attributes outside the print data.
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public class PDLOverrideSupported extends EnumSyntax
    implements PrintServiceAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4393264467928463934L;

    /**
     * The printer makes no attempt to make the external job attribute values
     * take precedence over embedded instructions in the documents' print data.
     */
    public static final PDLOverrideSupported
        NOT_ATTEMPTED = new PDLOverrideSupported(0);

    /**
     * The printer attempts to make the external job attribute values take
     * precedence over embedded instructions in the documents' print data,
     * however there is no guarantee.
     */
    public static final PDLOverrideSupported
        ATTEMPTED = new PDLOverrideSupported(1);

    /**
     * Construct a new PDL override supported enumeration value with the given
     * integer value.
     *
     * @param  value Integer value
     */
    protected PDLOverrideSupported(int value) {
        super (value);
    }

    /**
     * The string table for class {@code PDLOverrideSupported}.
     */
    private static final String[] myStringTable = {
        "not-attempted",
        "attempted"
    };

    /**
     * The enumeration value table for class {@code PDLOverrideSupported}.
     */
    private static final PDLOverrideSupported[] myEnumValueTable = {
        NOT_ATTEMPTED,
        ATTEMPTED
    };

    /**
     * Returns the string table for class {@code PDLOverrideSupported}.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class
     * {@code PDLOverrideSupported}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PDLOverrideSupported} and any vendor-defined subclasses,
     * the category is class {@code PDLOverrideSupported} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PDLOverrideSupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PDLOverrideSupported} and any vendor-defined subclasses,
     * the category name is {@code "pdl-override-supported"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "pdl-override-supported";
    }
}
