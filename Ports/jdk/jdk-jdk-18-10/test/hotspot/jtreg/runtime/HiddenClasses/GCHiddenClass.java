/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that hidden classes get garbage collected.
 * @library /test/lib
 * @modules jdk.compiler
 * @run main GCHiddenClass
 */


import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class GCHiddenClass {

    static byte klassbuf[] = InMemoryJavaCompiler.compile("TestClass",
        "public class TestClass { " +
        "    public TestClass() { " +
        "        System.out.println(\"Hello\"); " +
        " } } ");

    // A private method is great to keep hidden Class reference local to make it
    // GCed on the next cycle
    private PhantomReference<Class<?>> createClass(ReferenceQueue<Class<?>> refQueue) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        Class<?> cl = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
        return new PhantomReference<Class<?>>(cl, refQueue);
    }

    public boolean run() throws Exception {
        ReferenceQueue<Class<?>> refQueue = new ReferenceQueue<Class<?>>();
        PhantomReference<Class<?>> hiddenClassRef = createClass(refQueue);
        System.gc();
        Reference<? extends Class<?>> deletedObject = refQueue.remove();
        return hiddenClassRef.equals(deletedObject);
    }

    public static void main(String[] args) throws Throwable {
        GCHiddenClass gcHC = new GCHiddenClass();
        if (!gcHC.run()) {
            throw new RuntimeException("Test failed");
        }
    }
}
