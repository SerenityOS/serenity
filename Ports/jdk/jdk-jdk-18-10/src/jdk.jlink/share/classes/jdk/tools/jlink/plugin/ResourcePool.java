/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteOrder;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A Pool of Java resources.
 */
public interface ResourcePool {
    /**
     * Return the module view of this resource pool.
     *
     * @return a module based view of this resource pool.
     */
    public ResourcePoolModuleView moduleView();

    /**
     * Get all ResourcePoolEntry contained in this ResourcePool instance.
     *
     * @return The stream of ResourcePoolEntries.
     */
    public Stream<ResourcePoolEntry> entries();

    /**
     * Return the number of ResourcePoolEntry count in this ResourcePool.
     *
     * @return the entry count.
     */
    public int entryCount();

    /**
     * Get the ResourcePoolEntry for the passed path.
     *
     * @param path A data path
     * @return A ResourcePoolEntry instance or null if the data is not found
     */
    public Optional<ResourcePoolEntry> findEntry(String path);

    /**
     * Get the ModuleEntry for the passed path restricted to supplied context.
     *
     * @param path A data path
     * @param context A context of the search
     * @return A ModuleEntry instance or null if the data is not found
     */
    public Optional<ResourcePoolEntry> findEntryInContext(String path, ResourcePoolEntry context);

    /**
     * Check if the ResourcePool contains the given ResourcePoolEntry.
     *
     * @param data The module data to check existence for.
     * @return The module data or null if not found.
     */
    public boolean contains(ResourcePoolEntry data);

    /**
     * Check if the ResourcePool contains some content at all.
     *
     * @return True, no content, false otherwise.
     */
    public boolean isEmpty();

    /**
     * The ByteOrder currently in use when generating the jimage file.
     *
     * @return The ByteOrder.
     */
    public ByteOrder byteOrder();

    /**
     * Visit each ResourcePoolEntry in this ResourcePool to transform it and copy
     * the transformed ResourcePoolEntry to the output ResourcePoolBuilder.
     *
     * @param transform The function called for each ResourcePoolEntry found in the
     * ResourcePool. The transform function should return a ResourcePoolEntry
     * instance which will be added to the output or it should return null if
     * the passed ResourcePoolEntry is to be ignored for the output.
     *
     * @param outBuilder The ResourcePoolBuilder to be filled with Visitor returned
     * ResourcePoolEntries.
     */
    public default void transformAndCopy(
            Function<ResourcePoolEntry, ResourcePoolEntry> transform,
            ResourcePoolBuilder outBuilder) {
        entries().forEach(resource -> {
            ResourcePoolEntry res = transform.apply(resource);
            if (res != null) {
                outBuilder.add(res);
            }
        });
    }
}
