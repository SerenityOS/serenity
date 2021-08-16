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

/*
 * @test
 * @bug 8008762
 * @summary Type annotation on inner class in anonymous class
 *          shows up as regular annotation
 * @modules jdk.jdeps/com.sun.tools.classfile
 */
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

import com.sun.tools.classfile.*;

public class T8008762 extends ClassfileTestHelper{
    public static void main(String[] args) throws Exception {
        new T8008762().run();
    }

    public void run() throws Exception {
        expected_tinvisibles = 0;
        expected_tvisibles = 4;

        ClassFile cf = getClassFile("T8008762$Test$1$InnerAnon.class");
        test(cf);
        for (Field f : cf.fields) {
            test(cf, f, false);
        }
        for (Method m : cf.methods) {
            test(cf, m, false);
        }
        countAnnotations();

        if (errors > 0)
            throw new Exception(errors + " errors found");
        System.out.println("PASSED");
    }

    /*********************** Test class *************************/
    static class Test {
        Object mtest( Test t){ return null; }
        public void test() {
          mtest( new Test() {
                class InnerAnon { // Test1$1$InnerAnon.class
                  @A @B String ai_data = null;
                  @A @B String ai_m(){ return null; };
                }
               InnerAnon IA = new InnerAnon();
            });
        }
        @Retention(RUNTIME) @Target(TYPE_USE) @interface A { }
        @Retention(RUNTIME) @Target(TYPE_USE) @interface B { }
    }
}
