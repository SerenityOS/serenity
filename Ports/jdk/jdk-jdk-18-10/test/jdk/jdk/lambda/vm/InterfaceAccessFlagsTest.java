/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm;

import java.io.*;

import org.testng.annotations.Test;
import separate.*;
import separate.Compiler;

import static org.testng.Assert.*;
import static separate.SourceModel.*;
import static separate.SourceModel.Class;

public class InterfaceAccessFlagsTest extends TestHarness {
    public InterfaceAccessFlagsTest() {
        super(false, false);
    }

    public void testMethodCallWithFlag(AccessFlag ... flags) {
        Class I = new Class("I",
            new ConcreteMethod("int", "m", "return priv();", AccessFlag.PUBLIC),
            new ConcreteMethod("int", "priv", "return 99;", flags));
        Interface Istub = new Interface("I",
            new DefaultMethod("int", "m", "return 0;"));
        Class C = new Class("C", Istub,
            new ConcreteMethod("int", "foo", "return (new C()).m();",
                AccessFlag.PUBLIC, AccessFlag.STATIC));
        C.addCompilationDependency(Istub.findMethod("m"));

        Compiler compiler = new Compiler(/*Compiler.Flags.VERBOSE*/);
        compiler.addPostprocessor(new ClassToInterfaceConverter("I"));
        // Turns I from a class into an interface when loading

        ClassLoader cl = compiler.compile(C, I);
        try {
            java.lang.Class<?> C_class =
                java.lang.Class.forName("C", true, cl);
            assertNotNull(C_class);
            java.lang.reflect.Method m = C_class.getMethod("foo");
            assertNotNull(m);
            Integer res = (Integer)m.invoke(null);
            assertEquals(res.intValue(), 99);
        } catch (java.lang.reflect.InvocationTargetException e) {
            fail("Unexpected exception: " + e.getCause());
        } catch (Throwable e) {
            fail("Unexpected exception: " + e);
        } finally {
            compiler.cleanup();
        }
    }

    /* excluded: 8187655 */
    @Test(enabled=false, groups = "vm_prototype")
    public void testPrivateMethodCall() {
        testMethodCallWithFlag(AccessFlag.PRIVATE);
    }

    @Test(groups = "vm_prototype")
    public void testStaticMethodCall() {
        testMethodCallWithFlag(AccessFlag.PUBLIC, AccessFlag.STATIC);
    }

    @Test(groups = "vm_prototype")
    public void testPrivateStaticMethodCall() {
        testMethodCallWithFlag(AccessFlag.PRIVATE, AccessFlag.STATIC);
    }

    // Test other combos?  Protected?
}
