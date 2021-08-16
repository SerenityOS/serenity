/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code PresentationDirection} is a printing attribute class, an
 * enumeration, that is used in conjunction with the {@link NumberUp NumberUp}
 * attribute to indicate the layout of multiple print-stream pages to impose
 * upon a single side of an instance of a selected medium. This is useful to
 * mirror the text layout conventions of different scripts. For example, English
 * is "toright-tobottom", Hebrew is "toleft-tobottom" and Japanese is usually
 * "tobottom-toleft".
 * <p>
 * <b>IPP Compatibility:</b> This attribute is not an IPP 1.1 attribute; it is
 * an attribute in the Production Printing Extension
 * (<a href="ftp://ftp.pwg.org/pub/pwg/standards/temp_archive/pwg5100.3.pdf">
 * PDF</a>) of IPP 1.1. The category name returned by {@code getName()} is the
 * IPP attribute name. The enumeration's integer value is the IPP enum value.
 * The {@code toString()} method returns the IPP string representation of the
 * attribute value.
 *
 * @author Phil Race
 */
public final class PresentationDirection extends EnumSyntax
       implements PrintJobAttribute, PrintRequestAttribute  {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 8294728067230931780L;

    /**
     * Pages are laid out in columns starting at the top left, proceeding
     * towards the bottom {@literal &} right.
     */
    public static final PresentationDirection TOBOTTOM_TORIGHT =
        new PresentationDirection(0);

    /**
     * Pages are laid out in columns starting at the top right, proceeding
     * towards the bottom {@literal &} left.
     */
    public static final PresentationDirection TOBOTTOM_TOLEFT =
        new PresentationDirection(1);

    /**
     * Pages are laid out in columns starting at the bottom left, proceeding
     * towards the top {@literal &} right.
     */
    public static final PresentationDirection TOTOP_TORIGHT =
        new PresentationDirection(2);

    /**
     * Pages are laid out in columns starting at the bottom right, proceeding
     * towards the top {@literal &} left.
     */
    public static final PresentationDirection TOTOP_TOLEFT =
        new PresentationDirection(3);

    /**
     * Pages are laid out in rows starting at the top left, proceeding towards
     * the right {@literal &} bottom.
     */
    public static final PresentationDirection TORIGHT_TOBOTTOM =
        new PresentationDirection(4);

    /**
     * Pages are laid out in rows starting at the bottom left, proceeding
     * towards the right {@literal &} top.
     */
    public static final PresentationDirection TORIGHT_TOTOP =
        new PresentationDirection(5);

    /**
     * Pages are laid out in rows starting at the top right, proceeding towards
     * the left {@literal &} bottom.
     */
    public static final PresentationDirection TOLEFT_TOBOTTOM =
        new PresentationDirection(6);

    /**
     * Pages are laid out in rows starting at the bottom right, proceeding
     * towards the left {@literal &} top.
     */
    public static final PresentationDirection TOLEFT_TOTOP =
        new PresentationDirection(7);

    /**
     * Construct a new presentation direction enumeration value with the given
     * integer value.
     *
     * @param  value Integer value
     */
    private PresentationDirection(int value) {
        super (value);
    }

    /**
     * The string table for class {@code PresentationDirection}.
     */
    private static final String[] myStringTable = {
        "tobottom-toright",
        "tobottom-toleft",
        "totop-toright",
        "totop-toleft",
        "toright-tobottom",
        "toright-totop",
        "toleft-tobottom",
        "toleft-totop",
    };

    /**
     * The enumeration value table for class {@code PresentationDirection}.
     */
    private static final PresentationDirection[] myEnumValueTable = {
        TOBOTTOM_TORIGHT,
        TOBOTTOM_TOLEFT,
        TOTOP_TORIGHT,
        TOTOP_TOLEFT,
        TORIGHT_TOBOTTOM,
        TORIGHT_TOTOP,
        TOLEFT_TOBOTTOM,
        TOLEFT_TOTOP,
    };

    /**
     * Returns the string table for class {@code PresentationDirection}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class
     * {@code PresentationDirection}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PresentationDirection} the category is class
     * {@code PresentationDirection} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PresentationDirection.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PresentationDirection} the category name is
     * {@code "presentation-direction"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "presentation-direction";
    }
}
