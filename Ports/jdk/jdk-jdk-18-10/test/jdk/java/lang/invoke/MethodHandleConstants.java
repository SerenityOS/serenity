/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8022066
 * @summary smoke test for method handle constants
 * @build indify.Indify
 * @compile MethodHandleConstants.java
 * @run main/othervm
 *      indify.Indify
 *      --verify-specifier-count=4
 *      --expand-properties --classpath ${test.classes}
 *      --java test.java.lang.invoke.MethodHandleConstants --check-output
 * @run main/othervm
 *      -Djava.security.manager=allow
 *      indify.Indify
 *      --expand-properties --classpath ${test.classes}
 *      --java test.java.lang.invoke.MethodHandleConstants --security-manager
 */

package test.java.lang.invoke;

import java.util.*;
import java.io.*;
import java.lang.invoke.*;
import java.security.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class MethodHandleConstants {
    public static void main(String... av) throws Throwable {
        if (av.length > 0 && av[0].equals("--check-output"))  openBuf();
        if (av.length > 0 && av[0].equals("--security-manager"))  setSM();
        System.out.println("Obtaining method handle constants:");
        testCase(MH_String_replace_C2(), String.class, "replace", String.class, String.class, char.class, char.class);
        testCase(MH_MethodHandle_invokeExact_SC2(), MethodHandle.class, "invokeExact", String.class, MethodHandle.class, String.class, char.class, char.class);
        testCase(MH_MethodHandle_invoke_SC2(), MethodHandle.class, "invoke", String.class, MethodHandle.class, String.class, char.class, char.class);
        testCase(MH_Class_forName_S(), Class.class, "forName", Class.class, String.class);
        testCase(MH_Class_forName_SbCL(), Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class);
        System.out.println("Done.");
        closeBuf();
    }

    private static void testCase(MethodHandle mh, Class<?> defc, String name, Class<?> rtype, Class<?>... ptypes) throws Throwable {
        System.out.println(mh);
        // we include defc, because we assume it is a non-static MH:
        MethodType mt = methodType(rtype, ptypes);
        assertEquals(mh.type(), mt);
        // FIXME: Use revealDirect to find out more
    }
    private static void assertEquals(Object exp, Object act) {
        if (exp == act || (exp != null && exp.equals(act)))  return;
        throw new AssertionError("not equal: "+exp+", "+act);
    }

    private static void setSM() {
        Policy.setPolicy(new TestPolicy());
        System.setSecurityManager(new SecurityManager());
    }

    private static PrintStream oldOut;
    private static ByteArrayOutputStream buf;
    private static void openBuf() {
        oldOut = System.out;
        buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
    }
    private static void closeBuf() {
        if (buf == null)  return;
        System.out.flush();
        System.setOut(oldOut);
        String[] haveLines = new String(buf.toByteArray()).split("[\n\r]+");
        for (String line : haveLines)  System.out.println(line);
        Iterator<String> iter = Arrays.asList(haveLines).iterator();
        for (String want : EXPECT_OUTPUT) {
            String have = iter.hasNext() ? iter.next() : "[EOF]";
            if (want.equals(have))  continue;
            System.err.println("want line: "+want);
            System.err.println("have line: "+have);
            throw new AssertionError("unexpected output: "+have);
        }
        if (iter.hasNext())
            throw new AssertionError("unexpected output: "+iter.next());
    }
    private static final String[] EXPECT_OUTPUT = {
        "Obtaining method handle constants:",
        "MethodHandle(String,char,char)String",
        "MethodHandle(MethodHandle,String,char,char)String",
        "MethodHandle(MethodHandle,String,char,char)String",
        "MethodHandle(String)Class",
        "MethodHandle(String,boolean,ClassLoader)Class",
        "Done."
    };

    // String.replace(String, char, char)
    private static MethodType MT_String_replace_C2() {
        shouldNotCallThis();
        return methodType(String.class, char.class, char.class);
    }
    private static MethodHandle MH_String_replace_C2() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findVirtual(String.class, "replace", MT_String_replace_C2());
    }

    // MethodHandle.invokeExact(...)
    private static MethodType MT_MethodHandle_invokeExact_SC2() {
        shouldNotCallThis();
        return methodType(String.class, String.class, char.class, char.class);
    }
    private static MethodHandle MH_MethodHandle_invokeExact_SC2() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findVirtual(MethodHandle.class, "invokeExact", MT_MethodHandle_invokeExact_SC2());
    }

    // MethodHandle.invoke(...)
    private static MethodType MT_MethodHandle_invoke_SC2() {
        shouldNotCallThis();
        return methodType(String.class, String.class, char.class, char.class);
    }
    private static MethodHandle MH_MethodHandle_invoke_SC2() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findVirtual(MethodHandle.class, "invoke", MT_MethodHandle_invoke_SC2());
    }

    // Class.forName(String)
    private static MethodType MT_Class_forName_S() {
        shouldNotCallThis();
        return methodType(Class.class, String.class);
    }
    private static MethodHandle MH_Class_forName_S() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findStatic(Class.class, "forName", MT_Class_forName_S());
    }

    // Class.forName(String, boolean, ClassLoader)
    private static MethodType MT_Class_forName_SbCL() {
        shouldNotCallThis();
        return methodType(Class.class, String.class, boolean.class, ClassLoader.class);
    }
    private static MethodHandle MH_Class_forName_SbCL() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findStatic(Class.class, "forName", MT_Class_forName_SbCL());
    }

    private static void shouldNotCallThis() {
        // if this gets called, the transformation has not taken place
        if (System.getProperty("MethodHandleConstants.allow-untransformed") != null)  return;
        throw new AssertionError("this code should be statically transformed away by Indify");
    }

    static class TestPolicy extends Policy {
        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        final PermissionCollection permissions = new Permissions();
        TestPolicy() {
            permissions.add(new java.io.FilePermission("<<ALL FILES>>", "read"));
        }
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return permissions;
        }

        public PermissionCollection getPermissions(CodeSource codesource) {
            return permissions;
        }

        public boolean implies(ProtectionDomain domain, Permission perm) {
            return permissions.implies(perm) || DEFAULT_POLICY.implies(domain, perm);
        }
    }
}
