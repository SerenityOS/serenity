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
 * @summary Repeated type-annotations on type parm of local variable
 *          are not written to classfile.
 * @bug 8008769
 * @modules jdk.jdeps/com.sun.tools.classfile
 */
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
import com.sun.tools.classfile.*;

public class T8008769 extends ClassfileTestHelper{
    public static void main(String[] args) throws Exception {
        new T8008769().run();
    }

    public void run() throws Exception {
        expected_tvisibles = 4;
        ClassFile cf = getClassFile("T8008769$Test.class");
        for (Method m : cf.methods) {
            test(cf, m, true);
        }
        countAnnotations();

        if (errors > 0)
            throw new Exception(errors + " errors found");
        System.out.println("PASSED");
    }

    /*********************** Test class *************************/
    static class Test<T> {
        public void test() {
            Test<@A @B String>    t0 = new Test<>(); // 2 ok
            Test<@B @B String>    t1 = new Test<>(); // 1 missing
            Test<@A @A @A String> t2 = new Test<>(); // 1 missing
       }
    }
    @Retention(RUNTIME) @Target(TYPE_USE) @Repeatable( AC.class ) @interface A { }
    @Retention(RUNTIME) @Target(TYPE_USE) @Repeatable( BC.class ) @interface B { }
    @Retention(RUNTIME) @Target(TYPE_USE) @interface AC { A[] value(); }
    @Retention(RUNTIME) @Target(TYPE_USE) @interface BC { B[] value(); }
}
