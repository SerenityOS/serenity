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
 * Class {@code PrinterStateReason} is a printing attribute class, an
 * enumeration, that provides additional information about the printer's current
 * state, i.e., information that augments the value of the printer's
 * {@link PrinterState PrinterState} attribute. Class PrinterStateReason defines
 * standard printer state reason values. A Print Service implementation only
 * needs to report those printer state reasons which are appropriate for the
 * particular implementation; it does not have to report every defined printer
 * state reason.
 * <p>
 * Instances of {@code PrinterStateReason} do not appear in a Print Service's
 * attribute set directly. Rather, a
 * {@link PrinterStateReasons PrinterStateReasons} attribute appears in the
 * Print Service's attribute set. The
 * {@link PrinterStateReasons PrinterStateReasons} attribute contains zero, one,
 * or more than one {@code PrinterStateReason} objects which pertain to the
 * Print Service's status, and each PrinterStateReason object is associated with
 * a {@link Severity Severity} level of {@code REPORT} (least severe),
 * {@code WARNING}, or {@code ERROR} (most severe). The printer adds a
 * {@code PrinterStateReason} object to the Print Service's
 * {@link PrinterStateReasons PrinterStateReasons} attribute when the
 * corresponding condition becomes true of the printer, and the printer removes
 * the {@code PrinterStateReason} object again when the corresponding condition
 * becomes false, regardless of whether the Print Service's overall
 * {@link PrinterState PrinterState} also changed.
 * <p>
 * <b>IPP Compatibility:</b> The string values returned by each individual
 * {@link PrinterStateReason} and associated {@link Severity} object's
 * {@code toString()} methods, concatenated together with a hyphen ({@code "-"})
 * in between, gives the IPP keyword value for a {@link PrinterStateReasons}.
 * The category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public class PrinterStateReason extends EnumSyntax implements Attribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1623720656201472593L;

    /**
     * The printer has detected an error other than ones listed below.
     */
    public static final PrinterStateReason OTHER = new PrinterStateReason(0);

    /**
     * A tray has run out of media.
     */
    public static final PrinterStateReason
        MEDIA_NEEDED = new PrinterStateReason(1);

    /**
     * The device has a media jam.
     */
    public static final PrinterStateReason
        MEDIA_JAM = new PrinterStateReason(2);

    /**
     * Someone has paused the printer, but the device(s) are taking an
     * appreciable time to stop. Later, when all output has stopped, the
     * {@link PrinterState PrinterState} becomes {@code STOPPED}, and the
     * {@code PAUSED} value replaces the {@code MOVING_TO_PAUSED} value in the
     * {@link PrinterStateReasons PrinterStateReasons} attribute. This value
     * must be supported if the printer can be paused and the implementation
     * takes significant time to pause a device in certain circumstances.
     */
    public static final PrinterStateReason
        MOVING_TO_PAUSED = new PrinterStateReason(3);

    /**
     * Someone has paused the printer and the printer's
     * {@link PrinterState PrinterState} is {@code STOPPED}. In this state, a
     * printer must not produce printed output, but it must perform other
     * operations requested by a client. If a printer had been printing a job
     * when the printer was paused, the {@code Printer} must resume printing
     * that job when the printer is no longer paused and leave no evidence in
     * the printed output of such a pause. This value must be supported if the
     * printer can be paused.
     */
    public static final PrinterStateReason
        PAUSED = new PrinterStateReason(4);

    /**
     * Someone has removed a printer from service, and the device may be powered
     * down or physically removed. In this state, a printer must not produce
     * printed output, and unless the printer is realized by a print server that
     * is still active, the printer must perform no other operations requested
     * by a client. If a printer had been printing a job when it was shut down,
     * the printer need not resume printing that job when the printer is no
     * longer shut down. If the printer resumes printing such a job, it may
     * leave evidence in the printed output of such a shutdown, e.g. the part
     * printed before the shutdown may be printed a second time after the
     * shutdown.
     */
    public static final PrinterStateReason
        SHUTDOWN = new PrinterStateReason(5);

    /**
     * The printer has scheduled a job on the output device and is in the
     * process of connecting to a shared network output device (and might not be
     * able to actually start printing the job for an arbitrarily long time
     * depending on the usage of the output device by other servers on the
     * network).
     */
    public static final PrinterStateReason
        CONNECTING_TO_DEVICE = new PrinterStateReason(6);

    /**
     * The server was able to connect to the output device (or is always
     * connected), but was unable to get a response from the output device.
     */
    public static final PrinterStateReason
        TIMED_OUT = new PrinterStateReason(7);

    /**
     * The printer is in the process of stopping the device and will be stopped
     * in a while. When the device is stopped, the printer will change the
     * {@link PrinterState PrinterState} to {@code STOPPED}. The
     * {@code STOPPING} reason is never an error, even for a printer with a
     * single output device. When an output device ceases accepting jobs, the
     * printer's {@link PrinterStateReasons PrinterStateReasons} will have this
     * reason while the output device completes printing.
     */
    public static final PrinterStateReason
        STOPPING = new PrinterStateReason(8);

    /**
     * When a printer controls more than one output device, this reason
     * indicates that one or more output devices are stopped. If the reason's
     * severity is a report, fewer than half of the output devices are stopped.
     * If the reason's severity is a warning, half or more but fewer than all of
     * the output devices are stopped.
     */
    public static final PrinterStateReason
        STOPPED_PARTLY = new PrinterStateReason(9);

    /**
     * The device is low on toner.
     */
    public static final PrinterStateReason
        TONER_LOW = new PrinterStateReason(10);

    /**
     * The device is out of toner.
     */
    public static final PrinterStateReason
        TONER_EMPTY = new PrinterStateReason(11);

    /**
     * The limit of persistent storage allocated for spooling has been reached.
     * The printer is temporarily unable to accept more jobs. The printer will
     * remove this reason when it is able to accept more jobs. This value should
     * be used by a non-spooling printer that only accepts one or a small number
     * jobs at a time or a spooling printer that has filled the spool space.
     */
    public static final PrinterStateReason
        SPOOL_AREA_FULL = new PrinterStateReason(12);

    /**
     * One or more covers on the device are open.
     */
    public static final PrinterStateReason
        COVER_OPEN = new PrinterStateReason(13);

    /**
     * One or more interlock devices on the printer are unlocked.
     */
    public static final PrinterStateReason
        INTERLOCK_OPEN = new PrinterStateReason(14);

    /**
     * One or more doors on the device are open.
     */
    public static final PrinterStateReason
        DOOR_OPEN = new PrinterStateReason(15);

    /**
     * One or more input trays are not in the device.
     */
    public static final PrinterStateReason
        INPUT_TRAY_MISSING = new PrinterStateReason(16);

    /**
     * At least one input tray is low on media.
     */
    public static final PrinterStateReason
        MEDIA_LOW = new PrinterStateReason(17);

    /**
     * At least one input tray is empty.
     */
    public static final PrinterStateReason
        MEDIA_EMPTY = new PrinterStateReason(18);

    /**
     * One or more output trays are not in the device.
     */
    public static final PrinterStateReason
        OUTPUT_TRAY_MISSING = new PrinterStateReason(19);

    /**
     * One or more output areas are almost full (e.g. tray, stacker, collator).
     */
    public static final PrinterStateReason
        OUTPUT_AREA_ALMOST_FULL = new PrinterStateReason(20);

    /**
     * One or more output areas are full (e.g. tray, stacker, collator).
     */
    public static final PrinterStateReason
        OUTPUT_AREA_FULL = new PrinterStateReason(21);

    /**
     * The device is low on at least one marker supply (e.g. toner, ink,
     * ribbon).
     */
    public static final PrinterStateReason
        MARKER_SUPPLY_LOW = new PrinterStateReason(22);

    /**
     * The device is out of at least one marker supply (e.g. toner, ink,
     * ribbon).
     */
    public static final PrinterStateReason
        MARKER_SUPPLY_EMPTY = new PrinterStateReason(23);

    /**
     * The device marker supply waste receptacle is almost full.
     */
    public static final PrinterStateReason
        MARKER_WASTE_ALMOST_FULL = new PrinterStateReason(24);

    /**
     * The device marker supply waste receptacle is full.
     */
    public static final PrinterStateReason
        MARKER_WASTE_FULL = new PrinterStateReason(25);

    /**
     * The fuser temperature is above normal.
     */
    public static final PrinterStateReason
        FUSER_OVER_TEMP = new PrinterStateReason(26);

    /**
     * The fuser temperature is below normal.
     */
    public static final PrinterStateReason
        FUSER_UNDER_TEMP = new PrinterStateReason(27);

    /**
     * The optical photo conductor is near end of life.
     */
    public static final PrinterStateReason
        OPC_NEAR_EOL = new PrinterStateReason(28);

    /**
     * The optical photo conductor is no longer functioning.
     */
    public static final PrinterStateReason
        OPC_LIFE_OVER = new PrinterStateReason(29);

    /**
     * The device is low on developer.
     */
    public static final PrinterStateReason
        DEVELOPER_LOW = new PrinterStateReason(30);

    /**
     * The device is out of developer.
     */
    public static final PrinterStateReason
        DEVELOPER_EMPTY = new PrinterStateReason(31);

    /**
     * An interpreter resource is unavailable (e.g., font, form).
     */
    public static final PrinterStateReason
        INTERPRETER_RESOURCE_UNAVAILABLE = new PrinterStateReason(32);

    /**
     * Construct a new printer state reason enumeration value with the given
     * integer value.
     *
     * @param  value Integer value
     */
    protected PrinterStateReason(int value) {
        super (value);
    }

    /**
     * The string table for class {@code PrinterStateReason}.
     */
    private static final String[] myStringTable = {
        "other",
        "media-needed",
        "media-jam",
        "moving-to-paused",
        "paused",
        "shutdown",
        "connecting-to-device",
        "timed-out",
        "stopping",
        "stopped-partly",
        "toner-low",
        "toner-empty",
        "spool-area-full",
        "cover-open",
        "interlock-open",
        "door-open",
        "input-tray-missing",
        "media-low",
        "media-empty",
        "output-tray-missing",
        "output-area-almost-full",
        "output-area-full",
        "marker-supply-low",
        "marker-supply-empty",
        "marker-waste-almost-full",
        "marker-waste-full",
        "fuser-over-temp",
        "fuser-under-temp",
        "opc-near-eol",
        "opc-life-over",
        "developer-low",
        "developer-empty",
        "interpreter-resource-unavailable"
    };

    /**
     * The enumeration value table for class {@code PrinterStateReason}.
     */
    private static final PrinterStateReason[] myEnumValueTable = {
        OTHER,
        MEDIA_NEEDED,
        MEDIA_JAM,
        MOVING_TO_PAUSED,
        PAUSED,
        SHUTDOWN,
        CONNECTING_TO_DEVICE,
        TIMED_OUT,
        STOPPING,
        STOPPED_PARTLY,
        TONER_LOW,
        TONER_EMPTY,
        SPOOL_AREA_FULL,
        COVER_OPEN,
        INTERLOCK_OPEN,
        DOOR_OPEN,
        INPUT_TRAY_MISSING,
        MEDIA_LOW,
        MEDIA_EMPTY,
        OUTPUT_TRAY_MISSING,
        OUTPUT_AREA_ALMOST_FULL,
        OUTPUT_AREA_FULL,
        MARKER_SUPPLY_LOW,
        MARKER_SUPPLY_EMPTY,
        MARKER_WASTE_ALMOST_FULL,
        MARKER_WASTE_FULL,
        FUSER_OVER_TEMP,
        FUSER_UNDER_TEMP,
        OPC_NEAR_EOL,
        OPC_LIFE_OVER,
        DEVELOPER_LOW,
        DEVELOPER_EMPTY,
        INTERPRETER_RESOURCE_UNAVAILABLE
    };

    /**
     * Returns the string table for class {@code PrinterStateReason}.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class {@code PrinterStateReason}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PrinterStateReason} and any vendor-defined subclasses,
     * the category is class {@code PrinterStateReason} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PrinterStateReason.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PrinterStateReason} and any vendor-defined subclasses,
     * the category name is {@code "printer-state-reason"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "printer-state-reason";
    }
}
