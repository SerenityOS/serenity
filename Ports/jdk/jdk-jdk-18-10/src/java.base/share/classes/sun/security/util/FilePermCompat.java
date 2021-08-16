/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.action.GetPropertyAction;

import java.io.FilePermission;
import java.security.Permission;
import jdk.internal.access.SharedSecrets;

/**
 * Take care of FilePermission compatibility after JDK-8164705.
 */
public class FilePermCompat {
    /**
     * New behavior? Keep compatibility? Both default true.
     */
    public static final boolean nb;
    public static final boolean compat;

    static {
        String flag = SecurityProperties.privilegedGetOverridable(
                "jdk.io.permissionsUseCanonicalPath");
        if (flag == null) {
            flag = "false";
        }
        switch (flag) {
            case "true":
                nb = false;
                compat = false;
                break;
            case "false":
                nb = true;
                compat = true;
                break;
            default:
                throw new RuntimeException(
                        "Invalid jdk.io.permissionsUseCanonicalPath: " + flag);
        }
    }

    public static Permission newPermPlusAltPath(Permission input) {
        if (compat && input instanceof FilePermission) {
            return SharedSecrets.getJavaIOFilePermissionAccess()
                    .newPermPlusAltPath((FilePermission) input);
        }
        return input;
    }

    public static Permission newPermUsingAltPath(Permission input) {
        if (input instanceof FilePermission) {
            return SharedSecrets.getJavaIOFilePermissionAccess()
                    .newPermUsingAltPath((FilePermission) input);
        }
        return null;
    }
}
