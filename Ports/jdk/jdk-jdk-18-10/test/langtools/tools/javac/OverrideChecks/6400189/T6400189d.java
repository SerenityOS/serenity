/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6400189
 * @summary raw types and inference
 * @author  mcimadamore
 * @compile T6400189d.java
 */

import java.util.Iterator;

class T6400189c<T> {

    interface A<X> extends Iterable<X> {
        Iterator<X> iterator();
    }

    interface A2<Y> extends A<Y> {
        Iterator<Y> iterator();
    }

    static abstract class B<Z> implements A<Z> {
        public abstract Iterator<Z> iterator();
    }

    static abstract class C<W> extends B<W> implements A2<W> {
        Iterator<W> test() {
            return iterator();
        }
    }
}
