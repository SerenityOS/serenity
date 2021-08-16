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
 * Class {@code PrinterState} is a printing attribute class, an enumeration,
 * that identifies the current state of a printer. Class {@code PrinterState}
 * defines standard printer state values. A Print Service implementation only
 * needs to report those printer states which are appropriate for the particular
 * implementation; it does not have to report every defined printer state. The
 * {@link PrinterStateReasons PrinterStateReasons} attribute augments the
 * {@code PrinterState} attribute to give more detailed information about the
 * printer in given printer state.
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public final class PrinterState extends EnumSyntax
implements PrintServiceAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -649578618346507718L;

    /**
     * The printer state is unknown.
     */
    public static final PrinterState UNKNOWN = new PrinterState(0);

    /**
     * Indicates that new jobs can start processing without waiting.
     */
    public static final PrinterState IDLE = new PrinterState(3);

    /**
     * Indicates that jobs are processing; new jobs will wait before processing.
     */
    public static final PrinterState PROCESSING = new PrinterState(4);

    /**
     * Indicates that no jobs can be processed and intervention is required.
     */
    public static final PrinterState STOPPED = new PrinterState(5);

    /**
     * Construct a new printer state enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected PrinterState(int value) {
        super (value);
    }

    /**
     * The string table for class {@code PrinterState}.
     */
    private static final String[] myStringTable = {
        "unknown",
        null,
        null,
        "idle",
        "processing",
        "stopped"
    };

    /**
     * The enumeration value table for class {@code PrinterState}.
     */
    private static final PrinterState[] myEnumValueTable = {
        UNKNOWN,
        null,
        null,
        IDLE,
        PROCESSING,
        STOPPED
    };

    /**
     * Returns the string table for class {@code PrinterState}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code PrinterState}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PrinterState}, the category is class
     * {@code PrinterState} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PrinterState.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PrinterState}, the category name is
     * {@code "printer-state"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "printer-state";
    }
}
