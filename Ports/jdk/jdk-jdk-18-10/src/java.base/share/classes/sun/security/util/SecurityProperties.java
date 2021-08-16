/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;

public class SecurityProperties {

    public static final boolean INCLUDE_JAR_NAME_IN_EXCEPTIONS
        = includedInExceptions("jar");

    /**
     * Returns the value of the security property propName, which can be overridden
     * by a system property of the same name
     *
     * @param  propName the name of the system or security property
     * @return the value of the system or security property
     */
    @SuppressWarnings("removal")
    public static String privilegedGetOverridable(String propName) {
        if (System.getSecurityManager() == null) {
            return getOverridableProperty(propName);
        } else {
            return AccessController.doPrivileged((PrivilegedAction<String>) () -> getOverridableProperty(propName));
        }
    }

    private static String getOverridableProperty(String propName) {
        String val = System.getProperty(propName);
        if (val == null) {
            return Security.getProperty(propName);
        } else {
            return val;
        }
    }

    /**
     * Returns true in case the system or security property "jdk.includeInExceptions"
     * contains the category refName
     *
     * @param refName the category to check
     * @return true in case the system or security property "jdk.includeInExceptions"
     *         contains refName, false otherwise
     */
    public static boolean includedInExceptions(String refName) {
        String val = privilegedGetOverridable("jdk.includeInExceptions");
        if (val == null) {
            return false;
        }

        String[] tokens = val.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.equalsIgnoreCase(refName)) {
                return true;
            }
        }
        return false;
    }
}
