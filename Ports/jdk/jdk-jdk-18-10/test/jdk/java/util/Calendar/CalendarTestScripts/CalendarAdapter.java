/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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


import java.util.Calendar;
import sun.util.calendar.CalendarUtils;

public class CalendarAdapter extends Calendar {
    public static enum Type {
        GREGORIAN, BUDDHIST, JAPANESE;
    }

    static final String[] FIELD_NAMES = {
        "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
        "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
        "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
        "DST_OFFSET"
    };

    Calendar cal;
    GregorianAdapter gcal;
    private Type type;

    public CalendarAdapter(Calendar cal) {
        if (cal == null)
            throw new NullPointerException();

        this.cal = cal;
        if (cal instanceof sun.util.BuddhistCalendar) {
            type = Type.BUDDHIST;
        } else if (cal instanceof GregorianAdapter) {
            type = Type.GREGORIAN;
            gcal = (GregorianAdapter) cal;
        } else {
            type = Type.JAPANESE;
        }
    }

    public void setFirstDayOfWeek(int w) {
        cal.setFirstDayOfWeek(w);
    }

    public int getFirstDayOfWeek() {
        return cal.getFirstDayOfWeek();
    }

    public void setMinimalDaysInFirstWeek(int value) {
        cal.setMinimalDaysInFirstWeek(value);
    }

    public int getMinimalDaysInFirstWeek() {
        return getMinimalDaysInFirstWeek();
    }

    public long getTimeInMillis() {
        return cal.getTimeInMillis();
    }

    public void setTimeInMillis(long millis) {
        cal.setTimeInMillis(millis);
    }

    public int get(int field) {
        return cal.get(field);
    }

    public void set(int field, int value) {
        cal.set(field, value);
    }

    public void add(int field, int amount) {
        cal.add(field, amount);
    }

    public void roll(int field, boolean dir) {
        cal.roll(field, dir);
    }

    public void roll(int field, int amount) {
        cal.roll(field, amount);
    }

    public void setDate(int year, int month, int date)
    {
        cal.set(year, month, date);
    }

    public void setDate(int era, int year, int month, int date) {
        cal.set(ERA, era);
        cal.set(year, month, date);
    }

    public void setDateTime(int year, int month, int date, int hourOfDay, int minute, int second)
    {
        cal.set(year, month, date, hourOfDay, minute, second);
    }

    public void clearAll() {
        cal.clear();
    }

    public void clearField(int field)
    {
        cal.clear(field);
    }

    public boolean isSetField(int field)
    {
        return cal.isSet(field);
    }

    public int getMaximum(int field) {
        return cal.getMaximum(field);
    }

    public int getLeastMaximum(int field) {
        return cal.getLeastMaximum(field);
    }

    public int getActualMaximum(int field) {
        return cal.getActualMaximum(field);
    }

    public int getMinimum(int field) {
        return cal.getMinimum(field);
    }

    public int getGreatestMinimum(int field) {
        return cal.getGreatestMinimum(field);
    }

    public int getActualMinimum(int field) {
        return cal.getActualMinimum(field);
    }

    public void setLenient(boolean lenient) {
        cal.setLenient(lenient);
    }

    public String toString() {
        return cal.toString();
    }

    protected void computeFields() {
    }

    protected void computeTime() {
    }

    void setTimeOfDay(int hourOfDay, int minute, int second, int ms) {
        cal.set(HOUR_OF_DAY, hourOfDay);
        cal.set(MINUTE, minute);
        cal.set(SECOND, second);
        cal.set(MILLISECOND, ms);
    }

    // GregorianAdapter specific methods

    // When gcal is null, the methods throw a NPE.

    int getSetStateFields() {
        return gcal.getSetStateFields();
    }

    int[] getFields() {
        return gcal.getFields();
    }

    boolean checkInternalDate(int year, int month, int dayOfMonth) {
        return gcal.checkInternalDate(year, month, dayOfMonth);
    }

    boolean checkInternalDate(int year, int month, int dayOfMonth, int dayOfWeek) {
        return gcal.checkInternalDate(year, month, dayOfMonth, dayOfWeek);
    }

    boolean checkInternalField(int field, int expectedValue) {
        return checkInternalField(field, expectedValue);
    }

    // check methods

    boolean checkAllSet() {
        initTest();
        for (int i = 0; i < FIELD_COUNT; i++) {
            checkFieldState(i, true);
        }
        return getStatus();
    }

    boolean checkMaximum(int field, int expectedValue) {
        int val;
        if ((val = getMaximum(field)) != expectedValue) {
            appendMessage("getMaximum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkActualMaximum(int field, int expectedValue) {
        int val;
        if ((val = getActualMaximum(field)) != expectedValue) {
            appendMessage("getActualMaximum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkLeastMaximum(int field, int expectedValue) {
        int val;
        if ((val = getLeastMaximum(field)) != expectedValue) {
            appendMessage("getLeastMaximum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkMinimum(int field, int expectedValue) {
        int val;
        if ((val = getMinimum(field)) != expectedValue) {
            appendMessage("getMinimum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkActualMinimum(int field, int expectedValue) {
        int val;
        if ((val = getActualMinimum(field)) != expectedValue) {
            appendMessage("getActualMinimum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkGreatestMinimum(int field, int expectedValue) {
        int val;
        if ((val = getGreatestMinimum(field)) != expectedValue) {
            appendMessage("getGreatestMinimum("+FIELD_NAMES[field]+"): got " + val
                          + " expected " + expectedValue);
        }
        return getStatus();
    }

    boolean checkDate(int year, int month, int dayOfMonth) {
        initTest();
        checkField(YEAR, year);
        checkField(MONTH, month);
        checkField(DAY_OF_MONTH, dayOfMonth);
        return getStatus();
    }

    boolean checkDate(int era, int year, int month, int dayOfMonth) {
        initTest();
        checkField(ERA, era);
        checkField(YEAR, year);
        checkField(MONTH, month);
        checkField(DAY_OF_MONTH, dayOfMonth);
        return getStatus();
    }

    boolean checkDateTime(int year, int month, int dayOfMonth,
                          int hourOfDay, int minute, int second) {
        initTest();
        checkField(YEAR, year);
        checkField(MONTH, month);
        checkField(DAY_OF_MONTH, dayOfMonth);
        checkField(HOUR_OF_DAY, hourOfDay);
        checkField(MINUTE, minute);
        checkField(SECOND, second);
        return getStatus();
    }

    boolean checkDateTime(int year, int month, int dayOfMonth,
                          int hourOfDay, int minute, int second, int ms) {
        initTest();
        checkField(YEAR, year);
        checkField(MONTH, month);
        checkField(DAY_OF_MONTH, dayOfMonth);
        checkField(HOUR_OF_DAY, hourOfDay);
        checkField(MINUTE, minute);
        checkField(SECOND, second);
        checkField(MILLISECOND, ms);
        return getStatus();
    }

    boolean checkTimeOfDay(int hourOfDay, int minute, int second, int ms) {
        initTest();
        checkField(HOUR_OF_DAY, hourOfDay);
        checkField(MINUTE, minute);
        checkField(SECOND, second);
        checkField(MILLISECOND, ms);
        return getStatus();
    }

    boolean checkMillis(long millis) {
        initTest();
        long t = cal.getTimeInMillis();
        if (t != millis) {
            appendMessage("wrong millis: got " + t + ", expected " + millis);
            return false;
        }
        return true;
    }

    boolean checkFieldState(int field, boolean expectedState) {
        if (isSet(field) != expectedState) {
            appendMessage(FIELD_NAMES[field] + " state is not " + expectedState + "; ");
            return false;
        }
        return true;
    }

    boolean checkField(int field, int expectedValue) {
        int val;
        if ((val = get(field)) != expectedValue) {
            appendMessage("get(" + FIELD_NAMES[field] + "): got " + val +
                          ", expected " + expectedValue + "; ");
            return false;
        }
        return true;
    }

    static final String fieldName(int field) {
        return FIELD_NAMES[field];
    }

    String toDateString() {
        StringBuffer sb = new StringBuffer();
        String[] eraNames = null;
        switch (type) {
        case GREGORIAN:
            eraNames = new String[] { "BCE", "" };
            break;

        case BUDDHIST:
            eraNames = new String[] { "Before BE", "BE"};
            break;

        case JAPANESE:
            eraNames = new String[] {
                "BeforeMeiji",
                "Meiji",
                "Taisho",
                "Showa",
                "Heisei",
                "Reiwa"
            };
            break;
        }

        sb.append(eraNames[cal.get(ERA)]);
        if (sb.length() > 0)
            sb.append(' ');
        CalendarUtils.sprintf0d(sb, cal.get(YEAR), 4).append('-');
        CalendarUtils.sprintf0d(sb, cal.get(MONTH)+1, 2).append('-');
        CalendarUtils.sprintf0d(sb, cal.get(DAY_OF_MONTH), 2);
        return sb.toString();
    }

    String toTimeString() {
        StringBuffer sb = new StringBuffer();
        CalendarUtils.sprintf0d(sb, cal.get(HOUR_OF_DAY), 2).append(':');
        CalendarUtils.sprintf0d(sb, cal.get(MINUTE), 2).append(':');
        CalendarUtils.sprintf0d(sb, cal.get(SECOND),2 ).append('.');
        CalendarUtils.sprintf0d(sb, cal.get(MILLISECOND), 3);
        int zoneOffset = cal.get(ZONE_OFFSET) + cal.get(DST_OFFSET);
        if (zoneOffset == 0) {
            sb.append('Z');
        } else {
            int offset;
            char sign;
            if (zoneOffset > 0) {
                offset = zoneOffset;
                sign = '+';
            } else {
                offset = -zoneOffset;
                sign = '-';
            }
            offset /= 60000;
            sb.append(sign);
            CalendarUtils.sprintf0d(sb, offset / 60, 2);
            CalendarUtils.sprintf0d(sb, offset % 60, 2);
        }
        return sb.toString();
    }

    String toDateTimeString() {
        return toDateString() + "T" + toTimeString();
    }

    // Message handling

    StringBuffer msg = new StringBuffer();

    private void initTest() {
        msg = new StringBuffer();
    }

    String getMessage() {
        String s = msg.toString();
        msg = new StringBuffer();
        return "    " + s;
    }

    private void setMessage(String msg) {
        this.msg = new StringBuffer(msg);
    }

    private void appendMessage(String msg) {
        this.msg.append(msg);
    }

    boolean getStatus() {
        return msg.length() == 0;
    }
}
