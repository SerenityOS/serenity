/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.catalog;

import java.net.URI;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import jdk.xml.internal.SecuritySupport;

/**
 * Catalog Error messages
 *
 * @since 9
 */
final class CatalogMessages {

    public static final String ERR_INVALID_CATALOG = "InvalidCatalog";
    public static final String ERR_INVALID_ENTRY_TYPE = "InvalidEntryType";
    public static final String ERR_URI_NOTABSOLUTE = "UriNotAbsolute";
    public static final String ERR_URI_NOTVALIDURL = "UriNotValidUrl";
    public static final String ERR_INVALID_ARGUMENT = "InvalidArgument";
    public static final String ERR_NULL_ARGUMENT = "NullArgument";
    public static final String ERR_CIRCULAR_REFERENCE = "CircularReference";
    public static final String ERR_INVALID_PATH = "InvalidPath";
    public static final String ERR_PARSER_CONF = "ParserConf";
    public static final String ERR_PARSING_FAILED = "ParsingFailed";
    public static final String ERR_NO_CATALOG = "NoCatalogFound";
    public static final String ERR_NO_MATCH = "NoMatchFound";
    public static final String ERR_NO_URI_MATCH = "NoMatchURIFound";
    public static final String ERR_CREATING_URI = "FailedCreatingURI";
    public static final String ERR_OTHER = "OtherError";

    static final String bundleName = CatalogMessages.class.getPackageName() + ".CatalogMessages";
    static ResourceBundle resourceBundle;

    /**
     * Reports an error.
     * @param key the message key
     */
    static void reportError(String key) {
        reportError(key, null);
    }

    /**
     * Reports an error.
     * @param key the message key
     * @param arguments the message replacement text arguments. The order of the
     * arguments must match that of the placeholders in the actual message.
     */
    static void reportError(String key, Object[] arguments) {
        throw new CatalogException(formatMessage(key, arguments));
    }

    /**
     * Reports a CatalogException.
     * @param key the message key
     * @param arguments the message replacement text arguments. The order of the
     * arguments must match that of the placeholders in the actual message.
     */
    static void reportRunTimeError(String key, Object[] arguments) {
        throw new CatalogException(formatMessage(key, arguments));
    }

    /**
     * Reports a CatalogException.
     * @param  key the message key
     * @param cause the cause if any
     */
    static void reportRunTimeError(String key, Throwable cause) {
        throw new CatalogException(formatMessage(key, null), cause);
    }

    /**
     * Reports a CatalogException.
     * @param  key the message key
     * @param arguments the message replacement text arguments. The order of the
     * arguments must match that of the placeholders in the actual message.
     * @param cause the cause if any
     */
    static void reportRunTimeError(String key, Object[] arguments, Throwable cause) {
        throw new CatalogException(formatMessage(key, arguments), cause);
    }

    /**
     * Reports IllegalArgumentException if the argument is null.
     *
     * @param name the name of the argument
     * @param value the value of the argument
     */
    static void reportIAEOnNull(String name, String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    formatMessage(ERR_INVALID_ARGUMENT, new Object[]{null, name}));
        }
    }

    /**
     * Reports NullPointerException if the argument is null.
     *
     * @param name the name of the argument
     * @param value the value of the argument
     */
    static void reportNPEOnNull(String name, Object value) {
        if (value == null) {
            throw new NullPointerException(
                    formatMessage(ERR_NULL_ARGUMENT, new Object[]{name}));
        }
    }

    /**
     * Reports IllegalArgumentException
     * @param arguments the arguments for formating the error message
     * @param cause the cause if any
     */
    static void reportIAE(String key, Object[] arguments, Throwable cause) {
        throw new IllegalArgumentException(
                formatMessage(key, arguments), cause);
    }

    /**
     * Format a message with the specified arguments using the default locale
     * information.
     *
     * @param key the message key
     * @param arguments the message replacement text arguments. The order of the
     * arguments must match that of the placeholders in the actual message.
     *
     * @return the formatted message
     *
     * @throws MissingResourceException If the message with the specified key
     * cannot be found
     */
    static String formatMessage(String key, Object[] arguments) {
        return formatMessage(key, arguments, Locale.getDefault());
    }

    /**
     * Format a message with the specified arguments using the given locale
     * information.
     *
     * @param key the message key
     * @param arguments the message replacement text arguments. The order of the
     * arguments must match that of the placeholders in the actual message.
     * @param locale the locale of the message
     *
     * @return the formatted message
     *
     * @throws MissingResourceException If the message with the specified key
     * cannot be found
     */
    static String formatMessage(String key, Object[] arguments, Locale locale) {
        return SecuritySupport.getErrorMessage(locale, bundleName, key, arguments);
    }

    /**
     * Returns sanitized URI.
     * @param uri a URI to be sanitized
     */
    static String sanitize(String uri) {
        if (uri == null) {
            return null;
        }
        String temp;
        int p;
        p = uri.lastIndexOf("/");
        if (p > 0 && p < uri.length()) {
            return uri.substring(p + 1);
        }
        return uri;
    }
}
