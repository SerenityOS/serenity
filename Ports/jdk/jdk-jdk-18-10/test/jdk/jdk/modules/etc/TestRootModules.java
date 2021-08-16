/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;

import jdk.internal.module.ModuleResolution;

/**
 * Test the set of modules in the boot layer includes all modules that export
 * an API. Also test that java.se is not resolved.
 */

public class TestRootModules {
    public static void main(String[] args) {
        // all modules that export an API should be resolved
        // For now, this test ignores the ModuleResolution attribute
        ModuleLayer bootLayer = ModuleLayer.boot();
        ModuleFinder.ofSystem().findAll().stream()
            .filter(mref -> !ModuleResolution.doNotResolveByDefault(mref))
            .map(ModuleReference::descriptor)
            .filter(descriptor -> descriptor.exports()
                    .stream()
                    .filter(e -> !e.isQualified())
                    .findAny()
                    .isPresent())
            .map(ModuleDescriptor::name)
            .forEach(name -> {
                if (!bootLayer.findModule(name).isPresent())
                    throw new RuntimeException(name + " not in boot layer");
            });

        // java.se should not be resolved
        ModuleLayer.boot()
                .findModule("java.se")
                .map(m -> { throw new RuntimeException("java.se should not be resolved"); });
    }
}
