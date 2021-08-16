/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package java.lang;

import java.lang.module.ModuleDescriptor;

/**
 * A helper class intended to be injected into java.lang using the
 * java --patch-module option. The helper class provides access to package private
 * methods in java.lang.Module.
 */

public final class ModuleHelper {

    private ModuleHelper() { }

    /**
     * Creates a named module but without defining the module to the VM.
     */
    public static Module newModule(ClassLoader loader, ModuleDescriptor descriptor) {
        return new Module(loader, descriptor);
    }

    /**
     * Updates module {@code from} to that it reads module {@code to} without
     * notifying the VM.
     */
    public static void addReadsNoSync(Module from, Module to) {
        from.implAddReadsNoSync(to);
    }

    /**
     * Updates module {@code from} so that it exports package {@code pkg}
     * to module {@code to} but without notifying the VM. If {@code to} is
     * {@code null} then the package is exported unconditionally.
     */
    public static void addExportsNoSync(Module from, String pkg, Module to) {
        if (to == null) {
            from.implAddExportsNoSync(pkg);
        } else {
            from.implAddExportsNoSync(pkg, to);
        }
    }

}
