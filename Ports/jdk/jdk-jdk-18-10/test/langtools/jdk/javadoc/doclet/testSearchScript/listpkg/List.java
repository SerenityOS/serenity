/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package listpkg;


/**
 * Example class containing "list" matching full name.
 */
public interface List<E> {

    List add(E e);

    void remove(int i);

    int size();

    static <E> List<E> of() {
        return null;
    }
    static <E> List<E> of(E e1) {
        return null;
    }
    static <E> List<E> of(E e1, E e2) {
        return null;
    }
    static <E> List<E> of(E e1, E e2, E e3) {
        return null;
    }
    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return null;
    }
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return null;
    }
    static <E> List<E> of(E... elements) {
        return null;
    }
}
