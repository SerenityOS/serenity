/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink.internal;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.stream.Stream;

/**
 * Utility class for creating permission-restricting {@link AccessControlContext}s.
 */
public final class AccessControlContextFactory {
    private AccessControlContextFactory () {
    }

    /**
     * Creates an access control context with no permissions.
     * @return an access control context with no permissions.
     */
    @SuppressWarnings("removal")
    public static AccessControlContext createAccessControlContext() {
        return createAccessControlContext(new Permission[0]);
    }

    /**
     * Creates an access control context limited to only the specified permissions.
     * @param permissions the permissions for the newly created access control context.
     * @return a new access control context limited to only the specified permissions.
     */
    @SuppressWarnings("removal")
    public static AccessControlContext createAccessControlContext(final Permission... permissions) {
        final Permissions perms = new Permissions();
        for(final Permission permission: permissions) {
            perms.add(permission);
        }
        return new AccessControlContext(new ProtectionDomain[] { new ProtectionDomain(null, perms) });
    }

    /**
     * Creates an access control context limited to only the {@link RuntimePermission}s
     * of the given names.
     * @param runtimePermissionNames the names of runtime permissions for the
     * newly created access control context.
     * @return a new access control context limited to only the runtime
     * permissions with the specified names.
     */
    @SuppressWarnings("removal")
    public static AccessControlContext createAccessControlContext(final String... runtimePermissionNames) {
        return createAccessControlContext(makeRuntimePermissions(runtimePermissionNames));
    }

    private static Permission[] makeRuntimePermissions(final String... runtimePermissionNames) {
        return Stream.of(runtimePermissionNames).map(RuntimePermission::new).toArray(Permission[]::new);
    }
}
