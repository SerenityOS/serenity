/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.net.URL;

/**
 * Class to simplify use of internationalization message strings.
 * Property files are constructed in terms of content as for JAI with
 * one "key=value" pair per line. All such files however have the same
 * name "properties". The resource extractor resolves the extraction of
 * the file from the jar as the package name is included automatically.
 *
 * <p>Extenders need only provide a static method
 * {@code getString(String)} which calls the static method in this
 * class with the name of the invoking class and returns a
 * {@code String}.
 */
public class I18NImpl {
    /**
     * Returns the message string with the specified key from the
     * "properties" file in the package containing the class with
     * the specified name.
     */
    protected static final String getString(String className, String resource_name, String key) {
        PropertyResourceBundle bundle = null;
        try {
            InputStream stream =
                Class.forName(className).getResourceAsStream(resource_name);
            bundle = new PropertyResourceBundle(stream);
        } catch(Throwable e) {
            throw new RuntimeException(e); // Chain the exception.
        }

        return (String)bundle.handleGetObject(key);
    }
}
