/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

import java.util.Map;

/**
 * A two-way Map between "natives" (Strings), which correspond to
 * platform-specific data formats, and "flavors" (DataFlavors), which correspond
 * to platform-independent MIME types. FlavorMaps need not be symmetric, but
 * typically are.
 *
 * @since 1.2
 */
public interface FlavorMap {

    /**
     * Returns a {@code Map} of the specified {@code DataFlavor}s to their
     * corresponding {@code String} native. The returned {@code Map} is a
     * modifiable copy of this {@code FlavorMap}'s internal data. Client code is
     * free to modify the {@code Map} without affecting this object.
     *
     * @param  flavors an array of {@code DataFlavor}s which will be the key set
     *         of the returned {@code Map}. If {@code null} is specified, a
     *         mapping of all {@code DataFlavor}s currently known to this
     *         {@code FlavorMap} to their corresponding {@code String} natives
     *         will be returned.
     * @return a {@code java.util.Map} of {@code DataFlavor}s to {@code String}
     *         natives
     */
    Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors);

    /**
     * Returns a {@code Map} of the specified {@code String} natives to their
     * corresponding {@code DataFlavor}. The returned {@code Map} is a
     * modifiable copy of this {@code FlavorMap}'s internal data. Client code is
     * free to modify the {@code Map} without affecting this object.
     *
     * @param  natives an array of {@code String}s which will be the key set of
     *         the returned {@code Map}. If {@code null} is specified, a mapping
     *         of all {@code String} natives currently known to this
     *         {@code FlavorMap} to their corresponding {@code DataFlavor}s will
     *         be returned.
     * @return a {@code java.util.Map} of {@code String} natives to
     *         {@code DataFlavor}s
     */
    Map<String, DataFlavor> getFlavorsForNatives(String[] natives);
}
