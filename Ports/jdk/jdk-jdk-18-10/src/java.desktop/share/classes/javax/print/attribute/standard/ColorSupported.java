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
 * Class {@code ColorSupported} is a printing attribute class, an enumeration,
 * that identifies whether the device is capable of any type of color printing
 * at all, including highlight color as well as full process color. All document
 * instructions having to do with color are embedded within the print data (none
 * are attributes attached to the job outside the print data).
 * <p>
 * Note: End users are able to determine the nature and details of the color
 * support by querying the
 * {@link PrinterMoreInfoManufacturer PrinterMoreInfoManufacturer} attribute.
 * <p>
 * Don't confuse the {@code ColorSupported} attribute with the
 * {@link Chromaticity Chromaticity} attribute.
 * {@link Chromaticity Chromaticity} is an attribute the client can specify for
 * a job to tell the printer whether to print a document in monochrome or color,
 * possibly causing the printer to print a color document in monochrome.
 * {@code ColorSupported} is a printer description attribute that tells whether
 * the printer can print in color regardless of how the client specifies to
 * print any particular document.
 * <p>
 * <b>IPP Compatibility:</b> The IPP boolean value is "true" for SUPPORTED and
 * "false" for NOT_SUPPORTED. The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public final class ColorSupported extends EnumSyntax
    implements PrintServiceAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2700555589688535545L;

    /**
     * The printer is not capable of any type of color printing.
     */
    public static final ColorSupported NOT_SUPPORTED = new ColorSupported(0);

    /**
     * The printer is capable of some type of color printing, such as highlight
     * color or full process color.
     */
    public static final ColorSupported SUPPORTED = new ColorSupported(1);

    /**
     * Construct a new color supported enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected ColorSupported(int value) {
        super (value);
    }

    /**
     * The string table for class {@code ColorSupported}.
     */
    private static final String[] myStringTable = {"not-supported",
                                                   "supported"};

    /**
     * The enumeration value table for class {@code ColorSupported}.
     */
    private static final ColorSupported[] myEnumValueTable = {NOT_SUPPORTED,
                                                              SUPPORTED};

    /**
     * Returns the string table for class {@code ColorSupported}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code ColorSupported}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code ColorSupported}, the category is class
     * {@code ColorSupported} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return ColorSupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code ColorSupported}, the category name is
     * {@code "color-supported"}
     *
     * @return attribute category name
     */
    public final String getName() {
        return "color-supported";
    }
}
