/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8155028
 * @summary javac crashes in silly do-while loop
 * @compile UnreachableLoopCond.java
 */

class UnreachableLoopCond {

    public void foo() {
        Integer i = 100;
        do {
            return;
        } while (i++ < 10);
    }

    public void goo() {
        Integer i = 100;
        do {
            break;
        } while (i++ < 10);
    }

    public void zoo() {
        Integer i = 100;
        do {
            throw new RuntimeException();
        } while (i++ < 10);
    }

    public void loo() {
        Integer i = 100;
        Integer j = 100;
        do {
           do {
               return;
           } while (i++ < 10);
        } while (j++ < 10);
    }

    public void moo() {
        Integer i = 100;
        do {
            if (true) {
                return;
            } else {
                return;
            }
        } while (i++ < 10);
    }

    public void moo(boolean cond) {
        Integer i = 100;
        do {
            if (cond) {
                return;
            } else {
                return;
            }
        } while (i++ < 10);
    }
}
