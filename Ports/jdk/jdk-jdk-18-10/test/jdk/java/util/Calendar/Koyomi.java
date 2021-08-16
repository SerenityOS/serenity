/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * GregorianCalendar subclass for testing.
 */
@SuppressWarnings("serial")
public class Koyomi extends GregorianCalendar {

    static final String[] FIELD_NAMES = {
        "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
        "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
        "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
        "DST_OFFSET"
    };

    static final int ALL_FIELDS = (1 << FIELD_COUNT) - 1;

    public Koyomi() {
    }

    public Koyomi(TimeZone tz) {
        super(tz);
    }

    public Koyomi(Locale loc) {
        super(loc);
    }

    public Koyomi(TimeZone tz, Locale loc) {
        super(tz, loc);
    }

    @Override
    public void computeTime() {
        super.computeTime();
    }

    @Override
    public void computeFields() {
        super.computeFields();
    }

    @Override
    public void complete() {
        super.complete();
    }

    static String getFieldName(int field) {
        return FIELD_NAMES[field];
    }

    String toDateString() {
        StringBuilder sb = new StringBuilder();
        sb.append(internalGet(ERA) == 0 ? "BCE " : "");
        sb.append(internalGet(YEAR)).append('-');
        sb.append(internalGet(MONTH) + 1).append('-');
        sb.append(internalGet(DAY_OF_MONTH));
        return sb.toString();
    }

    String toTimeString() {
        StringBuilder sb = new StringBuilder();
        sb.append(internalGet(HOUR_OF_DAY)).append(':');
        sb.append(internalGet(MINUTE)).append(':');
        sb.append(internalGet(SECOND)).append('.');
        int ms = internalGet(MILLISECOND);
        if (ms < 100) {
            sb.append('0');
            if (ms < 10) {
                sb.append('0');
            }
        }
        sb.append(ms);
        int offset = internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET);
        offset /= 60000;
        offset = (offset / 60) * 100 + (offset % 60);
        if (offset >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
            offset = -offset;
        }
        if (offset < 1000) {
            sb.append('0');
            if (offset < 100) {
                sb.append('0');
            }
        }
        sb.append(offset);
        return sb.toString();
    }

    String toDateTimeString() {
        return toDateString() + "T" + toTimeString();
    }

    StringBuilder msg = new StringBuilder();

    void initTest() {
        msg = new StringBuilder();
    }

    String getMessage() {
        String s = msg.toString();
        msg = new StringBuilder();
        return "    " + s;
    }

    void setMessage(String msg) {
        this.msg = new StringBuilder(msg);
    }

    void appendMessage(String msg) {
        this.msg.append(msg);
    }

    boolean getStatus() {
        return msg.length() == 0;
    }

    int getSetStateFields() {
        int mask = 0;
        for (int i = 0; i < FIELD_COUNT; i++) {
            if (isSet(i)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    int[] getFields() {
        int[] fds = new int[fields.length];
        System.arraycopy(fields, 0, fds, 0, fds.length);
        return fds;
    }

    boolean checkAllSet() {
        initTest();
        for (int i = 0; i < FIELD_COUNT; i++) {
            checkFieldState(i, true);
        }
        return getStatus();
    }

    boolean checkInternalDate(int year, int month, int dayOfMonth) {
        initTest();
        checkInternalFieldValue(YEAR, year);
        checkInternalFieldValue(MONTH, month);
        checkInternalFieldValue(DAY_OF_MONTH, dayOfMonth);
        return getStatus();
    }

    boolean checkInternalDate(int year, int month, int dayOfMonth, int dayOfWeek) {
        initTest();
        checkInternalFieldValue(YEAR, year);
        checkInternalFieldValue(MONTH, month);
        checkInternalFieldValue(DAY_OF_MONTH, dayOfMonth);
        checkInternalFieldValue(DAY_OF_WEEK, dayOfWeek);
        return getStatus();
    }

    boolean checkActualMaximum(int field, int expectedValue) {
        int val;
        if ((val = getActualMaximum(field)) != expectedValue) {
            appendMessage("getActualMaximum(" + FIELD_NAMES[field] + "): got " + val
                    + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkLeastMaximum(int field, int expectedValue) {
        int val;
        if ((val = getLeastMaximum(field)) != expectedValue) {
            appendMessage("getLeastMaximum(" + FIELD_NAMES[field] + "): got " + val
                    + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkActualMinimum(int field, int expectedValue) {
        int val;
        if ((val = getActualMinimum(field)) != expectedValue) {
            appendMessage("getActualMinimum(" + FIELD_NAMES[field] + "): got " + val
                    + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkGreatestMinimum(int field, int expectedValue) {
        int val;
        if ((val = getGreatestMinimum(field)) != expectedValue) {
            appendMessage("getGreatestMinimum(" + FIELD_NAMES[field] + "): got " + val
                    + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkDate(int year, int month, int dayOfMonth) {
        initTest();
        checkFieldValue(YEAR, year);
        checkFieldValue(MONTH, month);
        checkFieldValue(DAY_OF_MONTH, dayOfMonth);
        return getStatus();
    }

    boolean checkDate(int year, int month, int dayOfMonth, int dayOfWeek) {
        initTest();
        checkFieldValue(YEAR, year);
        checkFieldValue(MONTH, month);
        checkFieldValue(DAY_OF_MONTH, dayOfMonth);
        checkFieldValue(DAY_OF_WEEK, dayOfWeek);
        return getStatus();
    }

    boolean checkDateTime(int year, int month, int dayOfMonth,
            int hourOfDay, int minute, int second, int ms) {
        initTest();
        checkFieldValue(YEAR, year);
        checkFieldValue(MONTH, month);
        checkFieldValue(DAY_OF_MONTH, dayOfMonth);
        checkFieldValue(HOUR_OF_DAY, hourOfDay);
        checkFieldValue(MINUTE, minute);
        checkFieldValue(SECOND, second);
        checkFieldValue(MILLISECOND, ms);
        return getStatus();
    }

    boolean checkTime(int hourOfDay, int minute, int second, int ms) {
        initTest();
        checkFieldValue(HOUR_OF_DAY, hourOfDay);
        checkFieldValue(MINUTE, minute);
        checkFieldValue(SECOND, second);
        checkFieldValue(MILLISECOND, ms);
        return getStatus();
    }

    boolean checkFieldState(int field, boolean expectedState) {
        if (isSet(field) != expectedState) {
            appendMessage(FIELD_NAMES[field] + " state is not " + expectedState + "; ");
            return false;
        }
        return true;
    }

    boolean checkFieldValue(int field, int expectedValue) {
        int val;
        if ((val = get(field)) != expectedValue) {
            appendMessage("get(" + FIELD_NAMES[field] + "): got " + val
                    + ", expected " + expectedValue + "; ");
            return false;
        }
        return true;
    }

    boolean checkInternalFieldValue(int field, int expectedValue) {
        int val;
        if ((val = internalGet(field)) != expectedValue) {
            appendMessage("internalGet(" + FIELD_NAMES[field] + "): got " + val
                    + ", expected " + expectedValue + "; ");
            return false;
        }
        return true;
    }
}
