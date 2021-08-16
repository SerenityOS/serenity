/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

public class GregorianAdapter extends GregorianCalendar {
    static final int ALL_FIELDS = (1 << FIELD_COUNT) - 1;

    public GregorianAdapter() {
        super();
    }

    public GregorianAdapter(TimeZone tz) {
        super(tz);
    }

    public GregorianAdapter(Locale loc) {
        super(loc);
    }

    public GregorianAdapter(TimeZone tz, Locale loc) {
        super(tz, loc);
    }

    public void computeTime() {
        super.computeTime();
    }

    public void computeFields() {
        super.computeFields();
    }

    public void complete() {
        super.complete();
    }

    StringBuffer msg = new StringBuffer();

    void initTest() {
        msg = new StringBuffer();
    }

    String getMessage() {
        String s = msg.toString();
        msg = new StringBuffer();
        return "    " + s;
    }

    void setMessage(String msg) {
        this.msg = new StringBuffer(msg);
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

    boolean checkInternalDate(int year, int month, int dayOfMonth) {
        initTest();
        checkInternalField(YEAR, year);
        checkInternalField(MONTH, month);
        checkInternalField(DAY_OF_MONTH, dayOfMonth);
        return getStatus();
    }

    boolean checkInternalDate(int year, int month, int dayOfMonth, int dayOfWeek) {
        initTest();
        checkInternalField(YEAR, year);
        checkInternalField(MONTH, month);
        checkInternalField(DAY_OF_MONTH, dayOfMonth);
        checkInternalField(DAY_OF_WEEK, dayOfWeek);
        return getStatus();
    }

    boolean checkInternalField(int field, int expectedValue) {
        int val;
        if ((val = internalGet(field)) != expectedValue) {
            appendMessage("internalGet(" + CalendarAdapter.FIELD_NAMES[field] + "): got " + val +
                          ", expected " + expectedValue + "; ");
            return false;
        }
        return true;
    }
}
