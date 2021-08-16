/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4808962
 * @summary Unit tests for String regex methods
 */


public class Regex {

    static void ck(boolean x, boolean ans) throws Exception {
        if (x != ans)
            throw new Exception("Test failed");
    }

    static void ck(String x, String ans) throws Exception {
        if (!x.equals(ans))
            throw new Exception("Test failed");
    }

    static void ck(String[] x, String[] ans) throws Exception {
        if (x.length != ans.length)
            throw new Exception("Test failed");
        for (int i = 0; i < x.length; i++) {
            if (!x[i].equals(ans[i]))
                throw new Exception("Test failed");
        }
    }

    static void testLiteralReplacement() throws Exception {
        // Test straightforward replacement
        String data = "abcdefghi";
        String result = data.replace("def", "abc");
        if (!result.equals("abcabcghi"))
            throw new Exception("Test failed");

        // Test replacement with target that has metacharacters
        data = "abc(def)?ghi";
        result = data.replace("(def)?", "abc");
        if (!result.equals("abcabcghi"))
            throw new Exception("Test failed");

        // Test replacement with replacement that has metacharacters
        data = "abcdefghi";
        result = data.replace("def", "\\ab$c");
        if (!result.equals("abc\\ab$cghi"))
            throw new Exception("Test failed");
    }

    public static void main(String[] args) throws Exception {

        // These don't need to be thorough, they just need to check
        // that we're properly hooked up to java.util.regex

        String foo = "boo:and:foo";

        ck(foo.matches("b+"), false);
        ck(foo.matches("o+"), false);
        ck(foo.matches("b..:and:f.*"), true);

        ck(foo.replaceAll("oo", "uu"), "buu:and:fuu");
        ck(foo.replaceAll("o+", "<$0>"), "b<oo>:and:f<oo>");

        ck(foo.replaceFirst("oo", "uu"), "buu:and:foo");
        ck(foo.replaceFirst("o+", "<$0>"), "b<oo>:and:foo");

        ck(foo.split(":"), new String[] { "boo", "and", "foo" });
        ck(foo.split("o"), new String[] { "b", "", ":and:f" });

        ck(foo.split(":", 2), new String[] { "boo", "and:foo" });
        ck(foo.split("o", -2), new String[] { "b", "", ":and:f", "", "" });

        testLiteralReplacement();
    }


}
