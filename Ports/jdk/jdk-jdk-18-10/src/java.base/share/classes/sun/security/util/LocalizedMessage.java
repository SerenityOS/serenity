/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

/**
 * This class produces formatted and localized messages describing security
 * issues. Some messages may be required when the VM is not fully booted. In
 * this case, localization resources and classes used for message formatting
 * may not be available. When the VM is not booted, the message will not be
 * localized, and it will be formatted using simplified message formatting
 * code that is contained in this class.
 */

/*
 * Some of this code is executed before the VM is fully booted. Some import
 * statements have been omitted to help prevent accidental use of classes that
 * may not be available during boot.
 */

public class LocalizedMessage {

    private static final Resources RESOURCES = new Resources();

    private final String key;

    /**
     * A LocalizedMessage can be instantiated with a key and formatted with
     * arguments later in the style of MessageFormat. This organization
     * allows the actual formatting (and associated permission checks) to be
     * avoided unless the resulting string is needed.
     * @param key
     */
    public LocalizedMessage(String key) {
        this.key = key;
    }

    /**
     * Return a localized string corresponding to the key stored in this
     * object, formatted with the provided arguments. This method should only
     * be called when the VM is booted and all resources needed to obtain
     * and format the localized message are loaded (or can be loaded).
     *
     * @param arguments The arguments that should be placed in the message
     * @return A formatted message string
     */
    public String formatLocalized(Object... arguments) {
        return getLocalized(key, arguments);
    }

    /**
     * Return a non-localized string corresponding to the key stored in this
     * object, formatted with the provided arguments. All strings are obtained
     * from sun.security.util.Resources, and the formatting only supports
     * simple positional argument replacement (e.g. {1}).
     *
     * @param arguments The arguments that should be placed in the message
     * @return A formatted message string
     */
    public String formatNonlocalized(Object... arguments) {
        return getNonlocalized(key, arguments);
    }

    /**
     * Return a non-localized string corresponding to the provided key, and
     * formatted with the provided arguments. All strings are obtained from
     * sun.security.util.Resources, and the formatting only supports
     * simple positional argument replacement (e.g. {1}).
     *
     * @param key The key of the desired string in Resources
     * @param arguments The arguments that should be placed in the message
     * @return A formatted message string
     */
    public static String getNonlocalized(String key,
                                                Object... arguments) {

        String value = RESOURCES.getString(key);
        if (arguments == null || arguments.length == 0) {
            return value;
        }
        // Classes like StringTokenizer may not be loaded, so parsing
        //   is performed with String methods
        StringBuilder sb = new StringBuilder();
        int nextBraceIndex;
        while ((nextBraceIndex = value.indexOf('{')) >= 0) {

            String firstPart = value.substring(0, nextBraceIndex);
            sb.append(firstPart);
            value = value.substring(nextBraceIndex + 1);

            // look for closing brace and argument index
            nextBraceIndex = value.indexOf('}');
            if (nextBraceIndex < 0) {
                // no closing brace
                // MessageFormat would throw IllegalArgumentException, but
                //   that exception class may not be loaded yet
                throw new RuntimeException("Unmatched braces");
            }
            String indexStr = value.substring(0, nextBraceIndex);
            try {
                int index = Integer.parseInt(indexStr);
                sb.append(arguments[index]);
            } catch (NumberFormatException e) {
                // argument index is not an integer
                throw new RuntimeException("not an integer: " + indexStr);
            }
            value = value.substring(nextBraceIndex + 1);
        }
        sb.append(value);
        return sb.toString();
    }

    /**
     * Return a localized string corresponding to the provided key, and
     * formatted with the provided arguments. This method should only be
     * called when the VM is booted and all resources needed to obtain
     * and format the localized message are loaded (or can be loaded).
     *
     * @param key The key of the desired string in the security resource bundle
     * @param arguments The arguments that should be placed in the message
     * @return A formatted message string
     */
    public static String getLocalized(String key, Object... arguments) {

        String value = ResourcesMgr.getString(key);
        if (arguments == null) {
            return value;
        }
        java.text.MessageFormat form = new java.text.MessageFormat(value);
        return form.format(arguments);
    }

}
