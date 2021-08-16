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
 * @summary Wrong classfile attribution in inner class of lambda expression.
 * @bug 8010015
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
import com.sun.tools.classfile.*;

/*
 * A type-annotations on a field in an inner class not in a lambda expression
 * results in RuntimeTypeAnnotations_attibute and RuntimeAnnotations_attribute.
 * On a field in an innner class in a lambda expression, it leaves off the
 * RuntimeAnnotations_attribute.
 */
public class T8010015 extends ClassfileTestHelper{
    public static void main(String[] args) throws Exception {
        new T8010015().run();
    }

    public void run() throws Exception {
        expected_tvisibles = 1;
        expected_visibles = 1;
        ClassFile cf = getClassFile("T8010015$Test$1innerClass.class");
        for (Field f : cf.fields) {
            test(cf, f);
        }
        countAnnotations();

        if (errors > 0)
            throw new Exception(errors + " errors found");
        System.out.println("PASSED");
    }

    /*********************** Test class **************************/
    interface MapFun<T, R> { R m( T n); }
    static class Test {
        MapFun<Class<?>,String> cs;
        void test() {
            cs = c -> {
                     class innerClass {
                         @A Class<?> icc = null;
                         innerClass(Class<?> _c) { icc = _c; }
                         String getString() { return icc.toString(); }
                     }
                     return new innerClass(c).getString();
            };
            System.out.println("cs.m : " + cs.m(Integer.class));
        }

    public static void main(String... args) {new Test().test(); }
    }
    @Retention(RUNTIME) @Target({TYPE_USE,FIELD}) @interface A { }
}
