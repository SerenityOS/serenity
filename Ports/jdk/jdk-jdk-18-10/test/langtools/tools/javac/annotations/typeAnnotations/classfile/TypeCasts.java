/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.net.URL;
import java.util.List;

import com.sun.tools.classfile.*;

/*
 * @test
 * @bug 6843077 8006775
 * @summary test that typecasts annotation are emitted if only the cast
 *          expression is optimized away
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

public class TypeCasts extends ClassfileTestHelper{
    public static void main(String[] args) throws Exception {
        new TypeCasts().run();
    }

    public void run() throws Exception {
        expected_tinvisibles = 4;
        expected_tvisibles = 0;

        ClassFile cf = getClassFile("TypeCasts$Test.class");
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
        @interface A {}

        void emit() {
            Object o = null;
            String s = null;

            String a0 = (@A String)o;
            Object a1 = (@A Object)o;

            String b0 = (@A String)s;
            Object b1 = (@A Object)s;
        }

        void alldeadcode() {
            Object o = null;

            if (false) {
                String a0 = (@A String)o;
            }
        }
    }
}
