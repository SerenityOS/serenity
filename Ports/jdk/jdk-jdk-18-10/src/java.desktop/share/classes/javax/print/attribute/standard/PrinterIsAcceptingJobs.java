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
 * Class {@code PrinterIsAcceptingJobs} is a printing attribute class, an
 * enumeration, that indicates whether the printer is currently able to accept
 * jobs. This value is independent of the {@link PrinterState PrinterState} and
 * {@link PrinterStateReasons PrinterStateReasons} attributes because its value
 * does not affect the current job; rather it affects future jobs. If the value
 * is {@code NOT_ACCEPTING_JOBS}, the printer will reject jobs even when the
 * {@link PrinterState PrinterState} is {@code IDLE}. If value is
 * {@code ACCEPTING_JOBS}, the Printer will accept jobs even when the
 * {@link PrinterState PrinterState} is {@code STOPPED}.
 * <p>
 * <b>IPP Compatibility:</b> The IPP boolean value is "true" for
 * {@code ACCEPTING_JOBS} and "false" for {@code NOT_ACCEPTING_JOBS}. The
 * category name returned by {@code getName()} is the IPP attribute name. The
 * enumeration's integer value is the IPP enum value. The {@code toString()}
 * method returns the IPP string representation of the attribute value.
 *
 * @author Alan Kaminsky
 */
public final class PrinterIsAcceptingJobs extends EnumSyntax
        implements PrintServiceAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5052010680537678061L;

    /**
     * The printer is currently rejecting any jobs sent to it.
     */
    public static final PrinterIsAcceptingJobs
        NOT_ACCEPTING_JOBS = new PrinterIsAcceptingJobs(0);

    /**
     * The printer is currently accepting jobs.
     */
    public static final PrinterIsAcceptingJobs
        ACCEPTING_JOBS = new PrinterIsAcceptingJobs(1);

    /**
     * Construct a new printer is accepting jobs enumeration value with the
     * given integer value.
     *
     * @param  value Integer value
     */
    protected PrinterIsAcceptingJobs(int value) {
        super (value);
    }

    /**
     * The string table for class {@code PrinterIsAcceptingJobs}.
     */
    private static final String[] myStringTable = {
        "not-accepting-jobs",
        "accepting-jobs"
    };

    /**
     * The enumeration value table for class {@code PrinterIsAcceptingJobs}.
     */
    private static final PrinterIsAcceptingJobs[] myEnumValueTable = {
        NOT_ACCEPTING_JOBS,
        ACCEPTING_JOBS
    };

    /**
     * Returns the string table for class {@code PrinterIsAcceptingJobs}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class
     * {@code PrinterIsAcceptingJobs}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PrinterIsAcceptingJobs}, the category is class
     * {@code PrinterIsAcceptingJobs} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PrinterIsAcceptingJobs.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PrinterIsAcceptingJobs}, the category name is
     * {@code "printer-is-accepting-jobs"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "printer-is-accepting-jobs";
    }
}
