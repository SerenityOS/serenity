/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.helpers;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Create a new instance of a class by name.
 *
 *
 * <p>This class contains a static method for creating an instance of a
 * class from an explicit class name.  It tries to use the thread's context
 * ClassLoader if possible and falls back to using
 * Class.forName(String).</p>
 *
 * <p>This code is designed to compile and run on JDK version 1.1 and later
 * including versions of Java 2.</p>
 *
 * @author Edwin Goei, David Brownell
 * @version 2.0.1 (sax2r2)
 */
class NewInstance {
    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xerces.internal";
    private static final String DEFAULT_CLASS = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
    /**
     * Creates a new instance of the specified class name
     *
     * Package private so this code is not exposed at the API level.
     */
    @SuppressWarnings("removal")
    static <T> T newInstance (Class<T> type, ClassLoader loader, String clsName)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException
    {
        ClassLoader classLoader = Objects.requireNonNull(loader);
        String className = Objects.requireNonNull(clsName);

        // Instantiate directly for the SAX default parser
        if (className.equals(DEFAULT_CLASS)) {
            return type.cast(new com.sun.org.apache.xerces.internal.parsers.SAXParser());
        }

        // make sure we have access to restricted packages
        boolean internal = false;
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                internal = true;
            }
        }

        Class<?> driverClass;
        if (classLoader == null || internal) {
            driverClass = Class.forName(className);
        } else {
            driverClass = classLoader.loadClass(className);
        }

        try {
            return type.cast(driverClass.getConstructor().newInstance());
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            throw new InstantiationException(ex.getMessage());
        }
    }

}
