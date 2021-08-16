/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8267773
 * @summary Test
 *
 * @compile IntegerMinValue.java
 * @run main/othervm -Xverify:all -Xbatch IntegerMinValue
 *
 * @compile -XDstringConcat=inline IntegerMinValue.java
 * @run main/othervm -Xverify:all -Xbatch IntegerMinValue
 *
 * @compile -XDstringConcat=indy IntegerMinValue.java
 * @run main/othervm -Xverify:all -Xbatch IntegerMinValue
 *
 * @compile -XDstringConcat=indyWithConstants IntegerMinValue.java
 * @run main/othervm -Xverify:all -Xbatch IntegerMinValue
*/

public class IntegerMinValue {

    public void test() {
        int i = Integer.MIN_VALUE;
        String s = "" + i;
        if (!"-2147483648".equals(s)) {
           throw new IllegalStateException("Failed: " + s);
        }
        System.out.println(s);
    }

    public static void main(String[] strArr) {
        IntegerMinValue t = new IntegerMinValue();
        for (int i = 0; i < 100_000; i++ ) {
            t.test();
        }
    }

}
