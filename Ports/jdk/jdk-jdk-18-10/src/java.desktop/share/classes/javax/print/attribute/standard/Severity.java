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

/**
 * Class {@code Severity} is a printing attribute class, an enumeration, that
 * denotes the severity of a {@link PrinterStateReason PrinterStateReason}
 * attribute.
 * <p>
 * Instances of {@code Severity} do not appear in a Print Service's attribute
 * set directly. Rather, a {@link PrinterStateReasons PrinterStateReasons}
 * attribute appears in the Print Service's attribute set.
 * The {@link PrinterStateReasons PrinterStateReasons} attribute contains zero,
 * one, or more than one {@link PrinterStateReason PrinterStateReason} objects
 * which pertain to the Print Service's status, and each
 * {@link PrinterStateReason PrinterStateReason} object is associated with a
 * Severity level of {@code REPORT} (least severe), {@code WARNING}, or
 * {@code ERROR} (most severe). The printer adds a
 * {@link PrinterStateReason PrinterStateReason} object to the Print Service's
 * {@link PrinterStateReasons PrinterStateReasons} attribute when the
 * corresponding condition becomes true of the printer, and the printer removes
 * the {@link PrinterStateReason PrinterStateReason} object again when the
 * corresponding condition becomes false, regardless of whether the Print
 * Service's overall {@link PrinterState PrinterState} also changed.
 * <p>
 * <b>IPP Compatibility:</b> {@code Severity.toString()} returns either "error",
 * "warning", or "report". The string values returned by each individual
 * {@link PrinterStateReason} and associated {@link Severity} object's
 * {@code toString()} methods, concatenated together with a hyphen ({@code "-"})
 * in between, gives the IPP keyword value for a {@link PrinterStateReasons}.
 * The category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class Severity extends EnumSyntax implements Attribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 8781881462717925380L;

    /**
     * Indicates that the {@link PrinterStateReason PrinterStateReason} is a
     * "report" (least severe). An implementation may choose to omit some or all
     * reports. Some reports specify finer granularity about the printer state;
     * others serve as a precursor to a warning. A report must contain nothing
     * that could affect the printed output.
     */
    public static final Severity REPORT = new Severity (0);

    /**
     * Indicates that the {@link PrinterStateReason PrinterStateReason} is a
     * "warning." An implementation may choose to omit some or all warnings.
     * Warnings serve as a precursor to an error. A warning must contain nothing
     * that prevents a job from completing, though in some cases the output may
     * be of lower quality.
     */
    public static final Severity WARNING = new Severity (1);

    /**
     * Indicates that the {@link PrinterStateReason PrinterStateReason} is an
     * "error" (most severe). An implementation must include all errors. If this
     * attribute contains one or more errors, the printer's
     * {@link PrinterState PrinterState} must be {@code STOPPED}.
     */
    public static final Severity ERROR = new Severity (2);

    /**
     * Construct a new severity enumeration value with the given integer value.
     *
     * @param  value Integer value
     */
    protected Severity(int value) {
        super (value);
    }

    /**
     * The string table for class {@code Severity}.
     */
    private static final String[] myStringTable = {
        "report",
        "warning",
        "error"
    };

    /**
     * The enumeration value table for class {@code Severity}.
     */
    private static final Severity[] myEnumValueTable = {
        REPORT,
        WARNING,
        ERROR
    };

    /**
     * Returns the string table for class {@code Severity}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code Severity}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code Severity}, the category is class
     * {@code Severity} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return Severity.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code Severity}, the category name is {@code "severity"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "severity";
    }
}
