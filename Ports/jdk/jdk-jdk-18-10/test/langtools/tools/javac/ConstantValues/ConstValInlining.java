/*
 * Copyright (c) 1999, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4253590 4255513 4115099
 * @summary Verify that non-static fields with a ConstValue attribute inlined only when used as a simple name.
 * @author maddox, gafter (adapted from leo's bug report)
 *
 * @run compile test_ff1.java
 * @run compile ConstValInlining.java
 * @run compile test_ff2.java
 * @run main ConstValInlining
 */

public class ConstValInlining {

    void test() throws Exception {
        Class checksClass = Class.forName("test_ff");
        test_ff obj = new test_ff();
        String reflected_fnl_str = (String)checksClass.getField("fnl_str").get(obj);

        T t = new T();

        // even when used by a qualified name, the compiler must inline
        if (t.fnl_str.equals(reflected_fnl_str))
            throw new Exception("FAILED: compiler failed to inline a qualified nonstatic constant");

        // when used by an unqualified name, they must not differ
        t.test(reflected_fnl_str);
    }

    static class T extends test_ff {
        void test(String reflected) throws Exception {
            // when used by an unqualified qualified name, they must differ
            if (fnl_str.equals(reflected))
                throw new Exception("FAILED: compiler failed to inline an unqualified nonstatic constant");
        }
    };

    public static void main(String args[]) throws Exception {
        new ConstValInlining().test();
    }
}
