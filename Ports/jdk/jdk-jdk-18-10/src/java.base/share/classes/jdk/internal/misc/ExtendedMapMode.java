/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.misc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * JDK-specific map modes implemented in java.base.
 */
public class ExtendedMapMode {

    static final MethodHandle MAP_MODE_CONSTRUCTOR;
    static {
        try {
            PrivilegedExceptionAction<Lookup> pae = () ->
                MethodHandles.privateLookupIn(MapMode.class, MethodHandles.lookup());
            @SuppressWarnings("removal")
            Lookup lookup = AccessController.doPrivileged(pae);
            var methodType = MethodType.methodType(void.class, String.class);
            MAP_MODE_CONSTRUCTOR = lookup.findConstructor(MapMode.class, methodType);
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }

    public static final MapMode READ_ONLY_SYNC = newMapMode("READ_ONLY_SYNC");

    public static final MapMode READ_WRITE_SYNC = newMapMode("READ_WRITE_SYNC");

    private static MapMode newMapMode(String name) {
        try {
            return (MapMode) MAP_MODE_CONSTRUCTOR.invoke(name);
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    private ExtendedMapMode() { }
}
