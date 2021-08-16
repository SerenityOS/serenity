/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8022718
 * @summary Runtime accessibility checking: protected class, if extended, should be accessible from another package
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file BogoLoader.java MethodInvoker.java Test.java anotherpkg/MethodSupplierOuter.java
 * @run main/othervm Test
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

interface MyFunctionalInterface {

    void invokeMethodReference();
}

class MakeProtected implements BogoLoader.VisitorMaker, Opcodes {

    final boolean whenVisitInner;

    MakeProtected(boolean when_visit_inner) {
        super();
        whenVisitInner = when_visit_inner;
    }

    public ClassVisitor make(ClassVisitor cv) {
        return new ClassVisitor(Opcodes.ASM7, cv) {

            @Override
            public void visitInnerClass(String name, String outerName,
                    String innerName, int access) {
                if (whenVisitInner) {
                    int access_ = (ACC_PROTECTED | access) & ~(ACC_PRIVATE | ACC_PUBLIC);
                    System.out.println("visitInnerClass: name = " + name
                            + ", outerName = " + outerName
                            + ", innerName = " + innerName
                            + ", access original = 0x" + Integer.toHexString(access)
                            + ", access modified to 0x" + Integer.toHexString(access_));
                    access = access_;
                }
                super.visitInnerClass(name, outerName, innerName, access);
            }
        };
    }
};

public class Test {

    public static void main(String argv[]) throws Exception, Throwable {
        BogoLoader.VisitorMaker makeProtectedNop = new MakeProtected(false);
        BogoLoader.VisitorMaker makeProtectedMod = new MakeProtected(true);

        int errors = 0;
        errors += tryModifiedInvocation(makeProtectedNop);
        errors += tryModifiedInvocation(makeProtectedMod);

        if (errors > 0) {
            throw new Error("FAIL; there were errors");
        }
    }

    private static int tryModifiedInvocation(BogoLoader.VisitorMaker makeProtected)
            throws Throwable, ClassNotFoundException {
        HashMap<String, BogoLoader.VisitorMaker> replace
                = new HashMap<String, BogoLoader.VisitorMaker>();
        replace.put("anotherpkg.MethodSupplierOuter$MethodSupplier", makeProtected);
        HashSet<String> in_bogus = new HashSet<String>();
        in_bogus.add("MethodInvoker");
        in_bogus.add("MyFunctionalInterface");
        in_bogus.add("anotherpkg.MethodSupplierOuter"); // seems to be never loaded
        in_bogus.add("anotherpkg.MethodSupplierOuter$MethodSupplier");

        BogoLoader bl = new BogoLoader(in_bogus, replace);
        try {
            Class<?> isw = bl.loadClass("MethodInvoker");
            Method meth = isw.getMethod("invoke");
            Object result = meth.invoke(null);
        } catch (Throwable th) {
            System.out.flush();
            Thread.sleep(250); // Let Netbeans get its I/O sorted out.
            th.printStackTrace();
            System.err.flush();
            Thread.sleep(250); // Let Netbeans get its I/O sorted out.
            return 1;
        }
        return 0;
    }
}
