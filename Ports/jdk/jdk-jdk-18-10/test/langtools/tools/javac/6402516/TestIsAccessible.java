/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

import p.A;

class Test1 {
    String ss = "      p.A      yes";
    String sa = "p.A   p.A#publ yes";
    String sq = "p.A   p.A#prot no ";
    String sr = "Test2 p.A#prot no ";
    String sx = "p.A   p.A#priv no ";

    String s2 = "      Test2      yes";
    String s3 = "Test2 Test2#stat yes";

    static class Test1a {
        String s1 = "Test2 Test2#priv no";
    }
}

class Test2 extends A {
    private int priv;
    static int stat;

    String ss = "      p.A      yes";
    String sa = "p.A   p.A#publ yes";
    String sq = "p.A   p.A#prot no ";
    String sr = "Test2 p.A#prot yes";
    String sx = "p.A   p.A#priv no ";

    static class Test2a {
        String s1 = "Test2 Test2#priv yes";
    }
}
