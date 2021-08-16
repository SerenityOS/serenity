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
 * @bug     4958003
 * @summary Range static factory fails to compute size in Jumbo enum set
 * @author  Josh Bloch
 */

import java.util.*;

public class JumboRange {
    public static void main(String[] args) {
        test(Test127.class, Test127.T2, Test127.T6);
        test(Test127.class, Test127.T126, Test127.T126);
        test(Test127.class, Test127.T0, Test127.T126);
    }

    static <T extends Enum<T>> void test(Class<T> enumClass, T e0,T e1) {
        EnumSet<T> range = EnumSet.range(e0, e1);
        if (range.size() != e1.ordinal() - e0.ordinal() + 1)
            throw new RuntimeException(range.size() + " != " +
                                       (e1.ordinal() - e0.ordinal() + 1));
    }

    public enum Test127 {
        T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15,
        T16, T17, T18, T19, T20, T21, T22, T23, T24, T25, T26, T27, T28, T29,
        T30, T31, T32, T33, T34, T35, T36, T37, T38, T39, T40, T41, T42, T43,
        T44, T45, T46, T47, T48, T49, T50, T51, T52, T53, T54, T55, T56, T57,
        T58, T59, T60, T61, T62, T63, T64, T65, T66, T67, T68, T69, T70, T71,
        T72, T73, T74, T75, T76, T77, T78, T79, T80, T81, T82, T83, T84, T85,
        T86, T87, T88, T89, T90, T91, T92, T93, T94, T95, T96, T97, T98, T99,
        T100, T101, T102, T103, T104, T105, T106, T107, T108, T109, T110, T111,
        T112, T113, T114, T115, T116, T117, T118, T119, T120, T121, T122, T123,
        T124, T125, T126
    }
}
