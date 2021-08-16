/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048838
 * @summary type inference performance regression
 * @compile T8048838.java
 */
class T8048838 {

    <T1 extends T2, T2 extends T3, T3 extends T4, T4 extends T5, T5 extends T6, T6 extends T7,
            T7 extends T8, T8 extends T9, T9 extends T10, T10 extends T11, T11 extends T12,
            T12 extends T13, T13 extends T14, T14 extends T15, T15 extends T16, T16 extends T17,
            T17 extends T18, T18 extends T19, T19 extends T20, T20 extends T21, T21 extends T22,
            T22 extends T23, T23 extends T24, T24 extends T25, T25 extends T26, T26 extends T27,
            T27 extends T28, T28 extends T29, T29 extends T30, T30 extends T31, T31 extends T32,
            T32 extends T33, T33 extends T34, T34 extends T35, T35 extends T36, T36 extends T37,
            T37 extends T38, T38 extends T39, T39 extends T40, T40 extends T41, T41 extends T42,
            T42 extends T43, T43 extends T44, T44 extends T45, T45 extends T46, T46 extends T47,
            T47 extends T48, T48 extends T49, T49 extends T50, T50 extends T51, T51 extends T52,
            T52 extends T53, T53 extends T54, T54 extends T55, T55 extends T56, T56 extends T57,
            T57 extends T58, T58 extends T59, T59 extends T60, T60 extends T61, T61 extends T62,
            T62 extends T63, T63 extends T64, T64 extends T65, T65 extends T66, T66 extends T67,
            T67 extends T68, T68 extends T69, T69 extends T70, T70 extends T71, T71 extends T72,
            T72 extends T73, T73 extends T74, T74 extends T75, T75 extends T76, T76 extends T77,
            T77 extends T78, T78 extends T79, T79 extends T80, T80 extends T81, T81 extends T82,
            T82 extends T83, T83 extends T84, T84 extends T85, T85 extends T86, T86 extends T87,
            T87 extends T88, T88 extends T89, T89 extends T90, T90 extends T91, T91 extends T92,
            T92 extends T93, T93 extends T94, T94 extends T95, T95 extends T96, T96 extends T97,
            T97 extends T98, T98 extends T99, T99 extends T100, T100 extends Integer>
    T1 foo(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5, T6 x6, T7 x7, T8 x8, T9 x9, T10 x10, T11 x11, T12 x12,
           T13 x13, T14 x14, T15 x15, T16 x16, T17 x17, T18 x18, T19 x19, T20 x20, T21 x21, T22 x22,
           T23 x23, T24 x24, T25 x25, T26 x26, T27 x27, T28 x28, T29 x29, T30 x30, T31 x31, T32 x32,
           T33 x33, T34 x34, T35 x35, T36 x36, T37 x37, T38 x38, T39 x39, T40 x40, T41 x41, T42 x42,
           T43 x43, T44 x44, T45 x45, T46 x46, T47 x47, T48 x48, T49 x49, T50 x50, T51 x51, T52 x52,
           T53 x53, T54 x54, T55 x55, T56 x56, T57 x57, T58 x58, T59 x59, T60 x60, T61 x61, T62 x62,
           T63 x63, T64 x64, T65 x65, T66 x66, T67 x67, T68 x68, T69 x69, T70 x70, T71 x71, T72 x72,
           T73 x73, T74 x74, T75 x75, T76 x76, T77 x77, T78 x78, T79 x79, T80 x80, T81 x81, T82 x82,
           T83 x83, T84 x84, T85 x85, T86 x86, T87 x87, T88 x88, T89 x89, T90 x90, T91 x91, T92 x92,
           T93 x93, T94 x94, T95 x95, T96 x96, T97 x97, T98 x98, T99 x99, T100 x100) { return null; }

    Object test() {
        return foo(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
                22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
                43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
                64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84,
                85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100); // type inference expected
    }
}
