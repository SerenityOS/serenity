/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4892571
 * @summary generics: type cast with instance of
 * @author gafter
 *
 * @compile  -Werror InstanceOf.java
 */

class InstanceOf<T> {
    static interface I1<T> {
        public T[] toArray();
    }
    static interface I2<T> extends I1<T> {
        public T[] toArray();
    }
    static interface I3<T> extends I2<T> {    }
    InstanceOf() {
        I1<T> inv = null;
        I1<? extends T> cov = null;
        I1<? super T> con = null;
        boolean b;
        b = inv instanceof I3; // <<pass>>
        b = cov instanceof I3; // <<pass>>
        b = con instanceof I3; // <<pass>>
    }
}
