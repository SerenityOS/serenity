/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262891
 * @summary Check the pattern domination error are reported correctly.
 * @compile/fail/ref=Domination.out -XDrawDiagnostics --enable-preview -source ${jdk.version} Domination.java
 */
public class Domination {

    int testDominatesError1(Object o) {
        switch (o) {
            case CharSequence cs: return 0;
            case String s: return 1;
            case Object x: return -1;
        }
    }

    int testDominatesError2(Object o) {
        switch (o) {
            case CharSequence cs: return 0;
            case String s && s.isEmpty(): return 1;
            case Object x: return -1;
        }
    }

    int testDominatesError3(Object o) {
        switch (o) {
            case CharSequence cs && true: return 0;
            case String s && s.isEmpty(): return 1;
            case Object x: return -1;
        }
    }

    int testNotDominates1(Object o) {
        switch (o) {
            case CharSequence cs && cs.length() == 0: return 0;
            case String s: return 1;
            case Object x: return -1;
        }
    }

    int testDominatesStringConstant(String str) {
        switch (str) {
            case String s: return 1;
            case "": return -1;
        }
    }

    int testDominatesIntegerConstant(Integer i) {
        switch (i) {
            case Integer j: return 1;
            case 0: return -1;
        }
    }

    int testDominatesEnumConstant() {
        enum E {
            A, B;
        }
        E e = E.A;
        switch (e) {
            case E d: return 1;
            case A: return -1;
        }
    }

}
