/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal.plugins;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class PluginsResourceBundle {

    static final String DESCRIPTION = "description";
    static final String ARGUMENT = "argument";
    static final String USAGE = "usage";
    private static final ResourceBundle pluginsBundle;

    static {
        Locale locale = Locale.getDefault();
        try {
            pluginsBundle = ResourceBundle.getBundle("jdk.tools.jlink."
                    + "resources.plugins", locale);
        } catch (MissingResourceException e) {
            throw new InternalError("Cannot find jlink resource bundle for "
                    + "locale " + locale);
        }
    }

    private PluginsResourceBundle() {
    }

    public static String getArgument(String name, Object... args) {
        return getMessage(name + "." + ARGUMENT, args);
    }

    public static String getDescription(String name) {
        return getMessage(name + "." + DESCRIPTION, name);
    }

    public static String getUsage(String name) {
        return getMessage(name + "." + USAGE, name);
    }

    public static String getOption(String name, String option) {
        return getMessage(name + "." + option);
    }

    public static String getMessage(String key, Object... args) throws MissingResourceException {
        return getMessage(pluginsBundle, key, args);
    }

    public static String getMessage(ResourceBundle bundle, String key, Object... args) throws MissingResourceException {
        String val = bundle.getString(key);
        return MessageFormat.format(val, args);
    }
}
