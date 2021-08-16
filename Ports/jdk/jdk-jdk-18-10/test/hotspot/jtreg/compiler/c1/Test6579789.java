/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6579789
 * @summary Internal error "c1_LinearScan.cpp:1429 Error: assert(false,"")" in debuggee with fastdebug VM
 *
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:UseSSE=0
 *      -XX:CompileCommand=compileonly,compiler.c1.Test6579789::bug
 *      compiler.c1.Test6579789
 */

package compiler.c1;

public class Test6579789 {
    public static void main(String[] args) {
        bug(4);
    }
    public static void bug(int n) {
        float f = 1;
        int i = 1;
        try {
            int x = 1 / n; // instruction that can trap
            f = 2;
            i = 2;
            int y = 2 / n; // instruction that can trap
        } catch (Exception ex) {
            f++;
            i++;
        }
    }
}
