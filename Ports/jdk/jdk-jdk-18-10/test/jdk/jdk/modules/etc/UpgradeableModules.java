/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Verify that upgradeable modules are not hashed in java.base
 *          whereas non-upgradeable modules are.
 * @modules java.base/jdk.internal.module
 * @run main UpgradeableModules
 */

import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleReferenceImpl;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UpgradeableModules {
    private static final List<String> UPGRADEABLE_MODULES =
        List.of("java.compiler",
                "jdk.internal.vm.compiler",
                "jdk.internal.vm.compiler.management");


    public static void main(String... args) {
        Set<String> hashedModules = hashedModules();
        if (hashedModules.isEmpty())
            return;

        if (UPGRADEABLE_MODULES.stream().anyMatch(hashedModules::contains)) {
            throw new RuntimeException("upgradeable modules are hashed: " +
                UPGRADEABLE_MODULES.stream()
                    .filter(hashedModules::contains)
                    .collect(Collectors.joining(" ")));
        }

        Set<String> nonUpgradeableModules =
            ModuleFinder.ofSystem().findAll().stream()
                .map(mref -> mref.descriptor().name())
                .filter(mn -> !UPGRADEABLE_MODULES.contains(mn))
                .collect(Collectors.toSet());

        if (nonUpgradeableModules.stream().anyMatch(mn -> !hashedModules.contains(mn))) {
            throw new RuntimeException("non-upgradeable modules are not hashed: " +
                nonUpgradeableModules.stream()
                    .filter(mn -> !hashedModules.contains(mn))
                    .collect(Collectors.joining(" ")));
        }
    }

    private static Set<String> hashedModules() {
        Optional<ResolvedModule> resolvedModule = ModuleLayer.boot()
            .configuration()
            .findModule("java.base");
        assert resolvedModule.isPresent();
        ModuleReference mref = resolvedModule.get().reference();
        assert mref instanceof ModuleReferenceImpl;
        ModuleHashes hashes = ((ModuleReferenceImpl) mref).recordedHashes();
        if (hashes != null) {
            Set<String> names = new HashSet<>(hashes.names());
            names.add("java.base");
            return names;
        }

        return Set.of();
    }
}
