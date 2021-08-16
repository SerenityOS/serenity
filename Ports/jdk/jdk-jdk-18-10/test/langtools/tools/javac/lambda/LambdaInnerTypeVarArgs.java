/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005653
 * @summary A lambda containing an inner class referencing an external type var in class parameter type
 * @author  Robert Field
 * @run main LambdaInnerTypeVarArgs
 */

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class LambdaInnerTypeVarArgs {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface I {
        C doit();
    }

    abstract class C {
        abstract Object it();
    }

    class TV {
        C go() {
            List<String> ls = new ArrayList<>();
            ls.add("Oh");
            ls.add("my");
            return foo(ls).doit();
        }

        <RRRRR> I foo(List<RRRRR> r) {
            return () -> new C() {
                List<RRRRR> xxxxx = r;
                @Override
                    Object it() { return xxxxx; };
            };
        }
    }

    void test1() {
        assertTrue(((List<String>)(new TV().go().it())).get(0).equals("Oh"));
    }

    public static void main(String[] args) {
        LambdaInnerTypeVarArgs t = new LambdaInnerTypeVarArgs();
        t.test1();
        assertTrue(assertionCount == 1);
    }
}
