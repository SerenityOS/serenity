/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test multiple number of arguments to concatenate.
 *
 * @compile ImplicitStringConcatArgCount.java
 * @run main/othervm -Xverify:all ImplicitStringConcatArgCount
 *
 * @compile -XDallowStringFolding=false -XDstringConcat=inline ImplicitStringConcatArgCount.java
 * @run main/othervm -Xverify:all ImplicitStringConcatArgCount
 *
 * @compile -XDallowStringFolding=false -XDstringConcat=indy ImplicitStringConcatArgCount.java
 * @run main/othervm -Xverify:all ImplicitStringConcatArgCount
 *
 * @compile -XDallowStringFolding=false -XDstringConcat=indyWithConstants ImplicitStringConcatArgCount.java
 * @run main/othervm -Xverify:all ImplicitStringConcatArgCount
*/
public class ImplicitStringConcatArgCount {
    static final String s = "f";
    static final String s1 = "o";
    static String s2 = "o";
    static int i = 7;

    public static void main(String[] args) throws Exception {
        test("fo",          s + s1);
        test("foo",         s + s1 + s2);
        test("foo7",        s + s1 + s2 + i);
        test("foo77",       s + s1 + s2 + i + i);
        test("foo777",      s + s1 + s2 + i + i + i);
        test("foo7777",     s + s1 + s2 + i + i + i + i);
        test("foo77777",    s + s1 + s2 + i + i + i + i + i);
        test("foo777777",   s + s1 + s2 + i + i + i + i + i + i);
        test("foo7777777",  s + s1 + s2 + i + i + i + i + i + i + i);
        test("foo77777777", s + s1 + s2 + i + i + i + i + i + i + i + i);
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
}
