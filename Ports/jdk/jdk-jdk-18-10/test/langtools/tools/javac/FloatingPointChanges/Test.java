/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4150966 4151040 4107254 4180437
 * @summary General tests of strictfp changes and reflection
 * @author David Stoutamire (dps)
 *
 * @compile Test.java
 * @run main Test
 */

import java.lang.reflect.*;

public class Test {
    strictfp void strict() {}
    strictfp class Inner2 { void strict() {} }

    public static void main(String[] args) throws NoSuchMethodException {
        assertStrict(Test.class.getDeclaredMethod("strict", new Class[0]));
        assertStrict(Test.Inner2.class.getDeclaredMethod("strict", new Class[0]));

        assertStrict(Test2.class.getDeclaredMethod("strict", new Class[0]));
        assertStrict(Test2.class.getDeclaredMethod("strict2", new Class[0]));
        assertStrict(Test2.Inner.class.getDeclaredMethod("strict", new Class[0]));
        assertStrict(Test2.Inner2.class.getDeclaredMethod("strict", new Class[0]));
    }

    private static void assertStrict(Method m) {
        int mod = m.getModifiers();
        if (!Modifier.isStrict(mod))
            barf(m, "strict");
    }

    private static void barf(Method m, String msg) {
        System.err.println("Method "+m+" was not "+msg+": "
                           +Modifier.toString(m.getModifiers()));

    }
}


strictfp class Test2 {
    void strict() {}
    strictfp void strict2() {}
    class Inner { void strict() {} }
    strictfp class Inner2 { void strict() {} }
}

// check local inner classes are accepted
class OtherTest
{
    static double m() {
        strictfp class Inn {
            float i;
        }
        return 2.0;
    }
}
