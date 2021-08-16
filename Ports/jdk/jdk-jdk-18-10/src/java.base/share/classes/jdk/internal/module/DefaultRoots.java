/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines methods to compute the default set of root modules for the unnamed
 * module.
 */

public final class DefaultRoots {
    private DefaultRoots() { }

    /**
     * Returns the default set of root modules for the unnamed module from the
     * modules observable with the intersection of two module finders.
     *
     * The first module finder should be the module finder that finds modules on
     * the upgrade module path or among the system modules. The second module
     * finder should be the module finder that finds all modules on the module
     * path, or a subset of when using --limit-modules.
     */
    static Set<String> compute(ModuleFinder finder1, ModuleFinder finder2) {
        return finder1.findAll().stream()
                .filter(mref -> !ModuleResolution.doNotResolveByDefault(mref))
                .map(ModuleReference::descriptor)
                .filter(descriptor -> finder2.find(descriptor.name()).isPresent()
                                      && exportsAPI(descriptor))
                .map(ModuleDescriptor::name)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the default set of root modules for the unnamed module from the
     * modules observable with the given module finder.
     *
     * This method is used by the jlink system modules plugin.
     */
    public static Set<String> compute(ModuleFinder finder) {
        return compute(finder, finder);
    }

    /**
     * Returns true if the given module exports a package to all modules
     */
    private static boolean exportsAPI(ModuleDescriptor descriptor) {
        return descriptor.exports()
                .stream()
                .filter(e -> !e.isQualified())
                .findAny()
                .isPresent();
    }
}
