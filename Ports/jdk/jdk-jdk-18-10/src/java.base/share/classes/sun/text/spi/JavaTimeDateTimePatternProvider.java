/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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



package sun.text.spi;

import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * Service Provider Interface for retrieving DateTime patterns from
 * specified Locale provider for java.time.
 */

public abstract class JavaTimeDateTimePatternProvider extends LocaleServiceProvider {

    protected JavaTimeDateTimePatternProvider() {
    }

    /**
     * Gets the formatting pattern for a timeStyle
     * dateStyle, calendarType and locale.
     * Concrete implementation of this method will retrieve
     * a java.time specific dateTime Pattern from selected Locale Provider.
     *
     * @param timeStyle an {@code int} value representing FormatStyle constant, -1
     * for date-only pattern
     * @param dateStyle an {@code int} value,representing FormatStyle constant, -1
     * for time-only pattern
     * @param locale {@code locale}, non-null
     * @param calType a {@code String},non-null representing CalendarType such as "japanese",
     * "iso8601"
     * @return  formatting pattern {@code String}
     * @see java.time.format.DateTimeFormatterBuilder#convertStyle(java.time.format.FormatStyle)
     * @since 9
     */
    public abstract String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType, Locale locale);
}
