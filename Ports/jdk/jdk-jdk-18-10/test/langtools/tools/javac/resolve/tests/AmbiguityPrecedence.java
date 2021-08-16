/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

@TraceResolve(keys={"compiler.err.ref.ambiguous"})
class AmbiguityPrecedence {

    @Candidate(applicable=Phase.BASIC)
    static void m1(long l, int i) {}
    @Candidate(applicable=Phase.BASIC)
    static void m1(int i, long l) {}
    @Candidate
    static void m1(Integer i1, Integer i2) {}

    @Candidate(applicable=Phase.BOX)
    static void m2(Object o, Integer i) {}
    @Candidate(applicable=Phase.BOX)
    static void m2(Integer i, Object o) {}
    @Candidate
    static void m2(Integer... o) {}

    {
        m1(1, 1);
        m2(1, 1);
    }
}
