/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The module view of a ResourcePool.
 */
public interface ResourcePoolModuleView {
    /**
     * Find the module, if any, of the given name.
     *
     * @param name name of the module
     * @return Optional containing the module of the given name.
     */
    public Optional<ResourcePoolModule> findModule(String name);

    /**
     * Find the module, if any, of the given ResourcePoolEntry
     *
     * @param entry The ResourcePoolEntry whose module is looked up.
     * @return Optional containing the module of the given ResourcePoolEntry
     */
    public default Optional<ResourcePoolModule> findModule(ResourcePoolEntry entry) {
        String name = Objects.requireNonNull(entry).moduleName();
        return name != null? findModule(name) : Optional.empty();
    }

    /**
     * The stream of modules contained in this ResourcePool.
     *
     * @return The stream of modules.
     */
    public Stream<ResourcePoolModule> modules();

    /**
     * Return the number of ResourcePoolModule count in this ResourcePool.
     *
     * @return the module count.
     */
    public int moduleCount();
}
