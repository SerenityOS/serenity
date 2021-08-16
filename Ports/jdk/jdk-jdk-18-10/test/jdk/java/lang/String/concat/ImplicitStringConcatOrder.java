/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test implicit string concat calls argument conversions in the right order
 * @bug 8200118
 *
 * @compile ImplicitStringConcatOrder.java
 * @run main/othervm -Xverify:all ImplicitStringConcatOrder
 *
 * @compile -XDstringConcat=inline ImplicitStringConcatOrder.java
 * @run main/othervm -Xverify:all ImplicitStringConcatOrder
 *
 * @compile -XDstringConcat=indy ImplicitStringConcatOrder.java
 * @run main/othervm -Xverify:all ImplicitStringConcatOrder
 *
 * @compile -XDstringConcat=indyWithConstants ImplicitStringConcatOrder.java
 * @run main/othervm -Xverify:all ImplicitStringConcatOrder
*/
import java.lang.StringBuilder;

public class ImplicitStringConcatOrder {

    static MyClass c = new MyClass();

    public static void main(String[] args) throws Exception {
        test("foo123bar",    "foo" + c + c + c + "bar");
        test("bazxyz456abc", "baz" + ("xyz" + c + c) + c + "abc");
        test("caf7eba89be",  "caf" + c + ("eba" + c + c) + "be");
    }

    public static void test(String expected, String actual) {
       if (!expected.equals(actual)) {
           StringBuilder sb = new StringBuilder();
           sb.append("Expected = ");
           sb.append(expected);
           sb.append(", actual = ");
           sb.append(actual);
           throw new IllegalStateException(sb.toString());
       }
    }

    static class MyClass {
        int x;
        public String toString() {
            return String.valueOf(++x);
        }
    }
}
