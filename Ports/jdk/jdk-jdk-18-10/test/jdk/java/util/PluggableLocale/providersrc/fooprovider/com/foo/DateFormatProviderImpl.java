/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
/*
 *
 */

package com.foo;

import java.text.*;
import java.text.spi.*;
import java.util.*;

import com.foobar.Utils;

public class DateFormatProviderImpl extends DateFormatProvider {

    static Locale[] avail = {
        Locale.JAPAN,
        new Locale("ja", "JP", "osaka"),
        new Locale("ja", "JP", "kyoto"),
        new Locale("yy")};

    static String[] datePattern = {
        "yyyy'\u5e74'M'\u6708'd'\u65e5'", // full date pattern
        "yyyy/MMM/dd", // long date pattern
        "yyyy/MM/dd", // medium date pattern
        "yy/MM/dd" // short date pattern
    };

    static String[] timePattern = {
        "H'\u6642'mm'\u5206'ss'\u79d2' z", // full time pattern
        "H:mm:ss z", // long time pattern
        "H:mm:ss", // medium time pattern
        "H:mm" // short time pattern
    };

    static String[] dialect = {
        "\u3067\u3059\u3002",
        "\u3084\u3002",
        "\u3069\u3059\u3002",
        "\u308f\u3044\u308f\u3044"
    };

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public DateFormat getDateInstance(int style, Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooDateFormat(datePattern[style]+dialect[i], locale);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    public DateFormat getTimeInstance(int style, Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooDateFormat(timePattern[style]+dialect[i], locale);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    public DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooDateFormat(
                    datePattern[dateStyle]+" "+timePattern[timeStyle]+dialect[i], locale);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }
}
