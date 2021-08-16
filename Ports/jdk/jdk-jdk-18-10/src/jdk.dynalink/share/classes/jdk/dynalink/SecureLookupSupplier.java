/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Objects;

/**
 * Provides security-checked access to a {@code MethodHandles.Lookup} object.
 * See {@link #getLookup()} for details.
 */
public class SecureLookupSupplier {
    /**
     * The name of a runtime permission required to successfully invoke the
     * {@link #getLookup()} method.
     */
    public static final String GET_LOOKUP_PERMISSION_NAME = "dynalink.getLookup";

    private static final RuntimePermission GET_LOOKUP_PERMISSION = new RuntimePermission(SecureLookupSupplier.GET_LOOKUP_PERMISSION_NAME);

    private final MethodHandles.Lookup lookup;

    /**
     * Creates a new secure lookup supplier, securing the passed lookup.
     * @param lookup the lookup to secure. Can not be null.
     * @throws NullPointerException if null is passed.
     */
    public SecureLookupSupplier(final MethodHandles.Lookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /**
     * Returns the lookup secured by this {@code SecureLookupSupplier}.
     * @return the lookup secured by this {@code SecureLookupSupplier}.
     * @throws SecurityException if the secured lookup isn't the
     * {@link MethodHandles#publicLookup()}, and a security manager is present,
     * and a check for {@code RuntimePermission("dynalink.getLookup")} fails.
     */
    public final Lookup getLookup() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null && lookup != MethodHandles.publicLookup()) {
            sm.checkPermission(GET_LOOKUP_PERMISSION);
        }
        return lookup;
    }

    /**
     * Returns the value of {@link #getLookup()} without a security check. Can
     * be used by subclasses to access the lookup quickly.
     * @return same as returned value of {@link #getLookup()}.
     */
    protected final Lookup getLookupPrivileged() {
        return lookup;
    }
}
