/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8007464
 * @summary Add graph inference support
 *          check that new wildcards inference strategy doesn't run into 7190296
 * @compile TargetType61.java
 */
class TargetType61 {

    interface Stream<T> {
        void forEach(Sink<? super T> sink);
    }

    interface Sink<T> {
        void put(T t);
    }

    public boolean add(CharSequence s) { return false; }

    public void addAll(Stream<? extends CharSequence> stream) {
        stream.forEach(this::add);
        stream.forEach(e -> { add(e); });
    }
}
