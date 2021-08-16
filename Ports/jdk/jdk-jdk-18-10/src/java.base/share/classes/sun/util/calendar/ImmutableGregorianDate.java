/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.calendar;

import java.util.Locale;
import java.util.TimeZone;

class ImmutableGregorianDate extends BaseCalendar.Date {
    private final BaseCalendar.Date date;

    ImmutableGregorianDate(BaseCalendar.Date date) {
        if (date == null) {
            throw new NullPointerException();
        }
        this.date = date;
    }

    public Era getEra() {
        return date.getEra();
    }

    public CalendarDate setEra(Era era) {
        unsupported(); return this;
    }

    public int getYear() {
        return date.getYear();
    }

    public CalendarDate setYear(int year) {
        unsupported(); return this;
    }

    public CalendarDate addYear(int n) {
        unsupported(); return this;
    }

    public boolean isLeapYear() {
        return date.isLeapYear();
    }

    void setLeapYear(boolean leapYear) {
        unsupported();
    }

    public int getMonth() {
        return date.getMonth();
    }

    public CalendarDate setMonth(int month) {
        unsupported(); return this;
    }

    public CalendarDate addMonth(int n) {
        unsupported(); return this;
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public CalendarDate setDayOfMonth(int date) {
        unsupported(); return this;
    }

    public CalendarDate addDayOfMonth(int n) {
        unsupported(); return this;
    }

    public int getDayOfWeek() {
        return date.getDayOfWeek();
    }

    public int getHours() {
        return date.getHours();
    }

    public CalendarDate setHours(int hours) {
        unsupported(); return this;
    }

    public CalendarDate addHours(int n) {
        unsupported(); return this;
    }

    public int getMinutes() {
        return date.getMinutes();
    }

    public CalendarDate setMinutes(int minutes) {
        unsupported(); return this;
    }

    public CalendarDate addMinutes(int n) {
        unsupported(); return this;
    }

    public int getSeconds() {
        return date.getSeconds();
    }

    public CalendarDate setSeconds(int seconds) {
        unsupported(); return this;
    }

    public CalendarDate addSeconds(int n) {
        unsupported(); return this;
    }

    public int getMillis() {
        return date.getMillis();
    }

    public CalendarDate setMillis(int millis) {
        unsupported(); return this;
    }

    public CalendarDate addMillis(int n) {
        unsupported(); return this;
    }

    public long getTimeOfDay() {
        return date.getTimeOfDay();
    }

    public CalendarDate setDate(int year, int month, int dayOfMonth) {
        unsupported(); return this;
    }

    public CalendarDate addDate(int year, int month, int dayOfMonth) {
        unsupported(); return this;
    }

    public CalendarDate setTimeOfDay(int hours, int minutes, int seconds, int millis) {
        unsupported(); return this;
    }

    public CalendarDate addTimeOfDay(int hours, int minutes, int seconds, int millis) {
        unsupported(); return this;
    }

    protected void setTimeOfDay(long fraction) {
        unsupported();
    }

    public boolean isNormalized() {
        return date.isNormalized();
    }

    public boolean isStandardTime() {
        return date.isStandardTime();
    }

    public void setStandardTime(boolean standardTime) {
        unsupported();
    }

    public boolean isDaylightTime() {
        return date.isDaylightTime();
    }

    protected void setLocale(Locale loc) {
        unsupported();
    }

    public TimeZone getZone() {
        return date.getZone();
    }

    public CalendarDate setZone(TimeZone zoneinfo) {
        unsupported(); return this;
    }

    public boolean isSameDate(CalendarDate date) {
        return date.isSameDate(date);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ImmutableGregorianDate)) {
            return false;
        }
        return date.equals(((ImmutableGregorianDate) obj).date);
    }

    public int hashCode() {
        return date.hashCode();
    }

    public Object clone() {
        return super.clone();
    }

    public String toString() {
        return date.toString();
    }

    protected void setDayOfWeek(int dayOfWeek) {
        unsupported();
    }

    protected void setNormalized(boolean normalized) {
        unsupported();
    }

    public int getZoneOffset() {
        return date.getZoneOffset();
    }

    protected void setZoneOffset(int offset) {
        unsupported();
    }

    public int getDaylightSaving() {
        return date.getDaylightSaving();
    }

    protected void setDaylightSaving(int daylightSaving) {
        unsupported();
    }

    public int getNormalizedYear() {
        return date.getNormalizedYear();
    }

    public void setNormalizedYear(int normalizedYear) {
        unsupported();
    }

    private void unsupported() {
        throw new UnsupportedOperationException();
    }
}
