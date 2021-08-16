/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

/**
 * NativeLibrary represents a loaded native library instance.
 */
public interface NativeLibrary {
    String name();

    /**
     * Finds the address of the entry of the given name.  Returns 0
     * if not found.
     *
     * @param name the name of the symbol to be found
     */
    long find(String name);

    /**
     * Finds the address of the entry of the given name.
     *
     * @param name the name of the symbol to be found
     * @throws NoSuchMethodException if the named entry is not found.
     */
    default long lookup(String name) throws NoSuchMethodException {
        long addr = find(name);
        if (0 == addr) {
            throw new NoSuchMethodException("Cannot find symbol " + name + " in library " + name());
        }
        return addr;
    }
}
