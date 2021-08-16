/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import java.util.ArrayList;

import com.sun.tools.classfile.*;

/*
 * @test
 * @bug 8136419 8200301
 * @summary test that type annotations on entities in static initializers are emitted to classfile
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDdeduplicateLambdas=false StaticInitializer.java
 * @run main StaticInitializer
 */

public class StaticInitializer extends ClassfileTestHelper {
    public static void main(String[] args) throws Exception {
        new StaticInitializer().run();
    }

    public void run() throws Exception {
        expected_tinvisibles = 4;
        expected_tvisibles = 0;

        ClassFile cf = getClassFile("StaticInitializer$Test.class");
        test(cf);
        for (Field f : cf.fields) {
            test(cf, f);
        }
        for (Method m: cf.methods) {
            test(cf, m, true);
        }

        countAnnotations();

        if (errors > 0)
            throw new Exception(errors + " errors found");
        System.out.println("PASSED");
    }

    /*********************** Test class *************************/
    static class Test {
        @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
        @interface T {}

        static {
            @T String s = null;
            Runnable r = () -> new ArrayList<@T String>();
        }
        @T static String s = null;
        static Runnable r = () -> new ArrayList<@T String>();
    }
}
