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

/**
 * Class {@code Compression} is a printing attribute class, an enumeration, that
 * specifies how print data is compressed. {@code Compression} is an attribute
 * of the print data (the doc), not of the Print Job. If a {@code Compression}
 * attribute is not specified for a doc, the printer assumes the doc's print
 * data is uncompressed (i.e., the default Compression value is always
 * {@link #NONE NONE}).
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public class Compression extends EnumSyntax implements DocAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5716748913324997674L;

    /**
     * No compression is used.
     */
    public static final Compression NONE = new Compression(0);

    /**
     * ZIP public domain inflate/deflate compression technology.
     */
    public static final Compression DEFLATE = new Compression(1);

    /**
     * GNU zip compression technology described in
     * <a href="http://www.ietf.org/rfc/rfc1952.txt">RFC 1952</a>.
     */
    public static final Compression GZIP = new Compression(2);

    /**
     * UNIX compression technology.
     */
    public static final Compression COMPRESS = new Compression(3);

    /**
     * Construct a new compression enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected Compression(int value) {
        super(value);
    }

    /**
     * The string table for class {@code Compression}.
     */
    private static final String[] myStringTable = {"none",
                                                   "deflate",
                                                   "gzip",
                                                   "compress"};

    /**
     * The enumeration value table for class {@code Compression}.
     */
    private static final Compression[] myEnumValueTable = {NONE,
                                                           DEFLATE,
                                                           GZIP,
                                                           COMPRESS};

    /**
     * Returns the string table for class {@code Compression}.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class {@code Compression}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code Compression} and any vendor-defined subclasses, the
     * category is class {@code Compression} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return Compression.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code Compression} and any vendor-defined subclasses, the
     * category name is {@code "compression"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "compression";
    }
}
