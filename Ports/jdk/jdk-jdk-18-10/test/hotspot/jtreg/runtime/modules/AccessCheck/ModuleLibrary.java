/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.module.ModuleReference;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A container of modules that acts as a ModuleFinder for testing
 * purposes.
 */

class ModuleLibrary implements ModuleFinder {
    private final Map<String, ModuleReference> namesToReference = new HashMap<>();

    private ModuleLibrary() { }

    void add(ModuleDescriptor... descriptors) {
        for (ModuleDescriptor descriptor: descriptors) {
            String name = descriptor.name();
            if (!namesToReference.containsKey(name)) {
                //modules.add(descriptor);

                URI uri = URI.create("module:/" + descriptor.name());

                ModuleReference mref = new ModuleReference(descriptor, uri) {
                    @Override
                    public ModuleReader open() {
                        throw new UnsupportedOperationException();
                    }
                };

                namesToReference.put(name, mref);
            }
        }
    }

    static ModuleLibrary of(ModuleDescriptor... descriptors) {
        ModuleLibrary ml = new ModuleLibrary();
        ml.add(descriptors);
        return ml;
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        return Optional.ofNullable(namesToReference.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
        return new HashSet<>(namesToReference.values());
    }
}

