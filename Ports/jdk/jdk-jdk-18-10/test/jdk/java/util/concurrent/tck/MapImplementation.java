/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Map;

/** Allows tests to work with different Map implementations. */
public interface MapImplementation {
    /** Returns the Map implementation class. */
    public Class<?> klazz();
    /** Returns an empty map. */
    public Map emptyMap();

    // General purpose implementations can use Integers for key and value
    default Object makeKey(int i) { return i; }
    default Object makeValue(int i) { return i; }
    default int keyToInt(Object key) { return (Integer) key; }
    default int valueToInt(Object value) { return (Integer) value; }

    public boolean isConcurrent();
    default boolean remappingFunctionCalledAtMostOnce() { return true; };
    public boolean permitsNullKeys();
    public boolean permitsNullValues();
    public boolean supportsSetValue();
}
