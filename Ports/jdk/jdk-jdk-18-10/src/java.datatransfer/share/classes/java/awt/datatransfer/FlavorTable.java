/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * A FlavorMap which relaxes the traditional 1-to-1 restriction of a Map. A
 * flavor is permitted to map to any number of natives, and likewise a native is
 * permitted to map to any number of flavors. FlavorTables need not be
 * symmetric, but typically are.
 *
 * @author David Mendenhall
 * @since 1.4
 */
public interface FlavorTable extends FlavorMap {

    /**
     * Returns a {@code List} of {@code String} natives to which the specified
     * {@code DataFlavor} corresponds. The {@code List} will be sorted from best
     * native to worst. That is, the first native will best reflect data in the
     * specified flavor to the underlying native platform. The returned
     * {@code List} is a modifiable copy of this {@code FlavorTable}'s internal
     * data. Client code is free to modify the {@code List} without affecting
     * this object.
     *
     * @param  flav the {@code DataFlavor} whose corresponding natives should be
     *         returned. If {@code null} is specified, all natives currently
     *         known to this {@code FlavorTable} are returned in a
     *         non-deterministic order.
     * @return a {@code java.util.List} of {@code java.lang.String} objects
     *         which are platform-specific representations of platform-specific
     *         data formats
     */
    List<String> getNativesForFlavor(DataFlavor flav);

    /**
     * Returns a {@code List} of {@code DataFlavor}s to which the specified
     * {@code String} corresponds. The {@code List} will be sorted from best
     * {@code DataFlavor} to worst. That is, the first {@code DataFlavor} will
     * best reflect data in the specified native to a Java application. The
     * returned {@code List} is a modifiable copy of this {@code FlavorTable}'s
     * internal data. Client code is free to modify the {@code List} without
     * affecting this object.
     *
     * @param  nat the native whose corresponding {@code DataFlavor}s should be
     *         returned. If {@code null} is specified, all {@code DataFlavor}s
     *         currently known to this {@code FlavorTable} are returned in a
     *         non-deterministic order.
     * @return a {@code java.util.List} of {@code DataFlavor} objects into which
     *         platform-specific data in the specified, platform-specific native
     *         can be translated
     */
    List<DataFlavor> getFlavorsForNative(String nat);
}
