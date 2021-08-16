/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.text.spi;

import java.text.DateFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * An abstract class for service providers that
 * provide concrete implementations of the
 * {@link java.text.DateFormat DateFormat} class.
 *
 * @since        1.6
 */
public abstract class DateFormatProvider extends LocaleServiceProvider {

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected DateFormatProvider() {
    }

    /**
     * Returns a new {@code DateFormat} instance which formats time
     * with the given formatting style for the specified locale.
     * @param style the given formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @throws    IllegalArgumentException if {@code style} is invalid,
     *     or if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @throws    NullPointerException if {@code locale} is null
     * @return a time formatter.
     * @see java.text.DateFormat#getTimeInstance(int, java.util.Locale)
     */
    public abstract DateFormat getTimeInstance(int style, Locale locale);

    /**
     * Returns a new {@code DateFormat} instance which formats date
     * with the given formatting style for the specified locale.
     * @param style the given formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @throws    IllegalArgumentException if {@code style} is invalid,
     *     or if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @throws    NullPointerException if {@code locale} is null
     * @return a date formatter.
     * @see java.text.DateFormat#getDateInstance(int, java.util.Locale)
     */
    public abstract DateFormat getDateInstance(int style, Locale locale);

    /**
     * Returns a new {@code DateFormat} instance which formats date and time
     * with the given formatting style for the specified locale.
     * @param dateStyle the given date formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param timeStyle the given time formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @throws    IllegalArgumentException if {@code dateStyle} or
     *     {@code timeStyle} is invalid,
     *     or if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @throws    NullPointerException if {@code locale} is null
     * @return a date/time formatter.
     * @see java.text.DateFormat#getDateTimeInstance(int, int, java.util.Locale)
     */
    public abstract DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, Locale locale);
}
