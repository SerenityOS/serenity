/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @compile pkg/Grand.java pkg/Parent.java pkg/ClassLoaderForParentFoo.java
 * @compile pkg/ClassLoaderForChildGrandFoo.java pkg/Child.jasm
 * @run main/othervm LdrCnstrFldMsgTest
 */

import java.lang.reflect.Method;

// Check that the LinkageError loader constraint message for fields contains the
// correct information.
//
// The test creates two class loaders.  The first class loader loads classes
// Child, Foo, and Grand.  The second class loader loads Parent and Foo.  Class
// Parent is a sub-class of Grand and class Child is a sub-class of Parent.
// Class Child tries to load Parent._field1.  This should fail because type Foo
// for Parent._field1 is a different type than Child's Foo.
//
// Grand (ldr1)   has field _field1 of type Foo(ldr1)
//  |
// Parent (ldr2)  has field _field1 of type Foo(ldr2)
//  |
// Child (ldr1)
//
// java.lang.LinkageError: loader constraint violation:
//   when resolving field "_field1" of type pkg.Foo,
//   the class loader pkg.ClassLoaderForChildGrandFoo @42b2e259 of the current class, pkg.Child,
//   and the class loader pkg.ClassLoaderForParentFoo @4b55c90f for the field's defining class, pkg.Parent,
//   have different Class objects for type pkg.Foo
//   (pkg.Child is in unnamed module of loader pkg.ClassLoaderForChildGrandFoo @42b2e259, parent loader 'app';
//    pkg.Parent is in unnamed module of loader pkg.ClassLoaderForParentFoo @4b55c90f, parent loader 'app')
//
public class LdrCnstrFldMsgTest {
    public static void main(String... args) throws Exception {
        ClassLoader l = new pkg.ClassLoaderForChildGrandFoo("pkg.Foo", "pkg.Child", "pkg.Grand");
        l.loadClass("pkg.Foo");

        // Try to call a public method in Grand.
        Runnable r = (Runnable) l.loadClass("pkg.Child").newInstance();
        try {
            r.run();
            throw new RuntimeException("Expected LinkageError exception not thrown");
        } catch (java.lang.LinkageError e) {
            if (!e.getMessage().contains("for the field's defining class, pkg.Parent,") ||
                !e.getMessage().contains("have different Class objects for type pkg.Foo")) {
                throw new RuntimeException("Wrong LinkageError exception thrown: " + e.toString());
            }
        }
    }
}
