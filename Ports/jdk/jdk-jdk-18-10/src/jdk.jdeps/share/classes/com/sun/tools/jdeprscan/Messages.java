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

package com.sun.tools.jdeprscan;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Message handling class for localization.
 */
public class Messages {
    /** Indicates whether line separators in messages need replacement. */
    static final boolean REPLACE_LINESEP = ! System.lineSeparator().equals("\n");

    /** The resource bundle, must be non-null. */
    static final ResourceBundle bundle;

    static {
        Locale locale = Locale.getDefault();
        try {
            bundle = ResourceBundle.getBundle("com.sun.tools.jdeprscan.resources.jdeprscan", locale);
        } catch (MissingResourceException e) {
            throw new InternalError("Cannot find jdeprscan resource bundle for locale " + locale, e);
        }
    }

    /**
     * Gets a message from the resource bundle. If necessary, translates "\n",
     * the line break string used in the message file, to the system-specific
     * line break string.
     *
     * @param key the message key
     * @param args the message arguments
     */
    public static String get(String key, Object... args) {
        try {
            String msg = MessageFormat.format(bundle.getString(key), args);
            if (REPLACE_LINESEP) {
                msg = msg.replace("\n", System.lineSeparator());
            }
            return msg;
        } catch (MissingResourceException e) {
            throw new InternalError("Missing message: " + key, e);
        }
    }
}
