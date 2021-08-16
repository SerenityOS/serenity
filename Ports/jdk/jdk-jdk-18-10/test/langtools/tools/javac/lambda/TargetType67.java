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
 * @bug 8010303
 * @summary Graph inference: missing incorporation step causes spurious inference error
 * @compile TargetType67.java
 */
class TargetType67 {

    interface Func<A, B> {
        B f(A a);
    }

    class List<X> {

        <M> List<M> map(Func<X, M> f) {
            return null;
        }

        <A> List<A> apply(final List<Func<X, A>> lf) {
            return null;
        }

        <B, C> List<C> bind(final List<B> lb, final Func<X, Func<B, C>> f) {
            return lb.apply(map(f));
        }
    }
}
