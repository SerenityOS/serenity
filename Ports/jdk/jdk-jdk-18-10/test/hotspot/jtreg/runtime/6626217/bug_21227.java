/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6626217
 * @summary Loader-constraint table allows arrays instead of only the base-classes
 * @library /test/lib
 * @compile bug_21227.java from_loader2.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller from_loader2
 * @compile impl2/many_loader.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller many_loader
 * @compile many_loader.java
 * @run main/othervm -Xverify -Xint bug_21227
 */

import java.lang.reflect.*;
import java.security.*;

abstract public class bug_21227 {

    // Jam anything you want in here, it will be cast to a You_Have_Been_P0wned.
    public static Object _p0wnee;

    public static void main(String argv[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
            System.out.println("Warmup");

            // Make a Class 'many_loader' under the default loader.
            bug_21227 bug = new many_loader();

            // Some classes under a new Loader, LOADER2, including another version of 'many_loader'.
            ClassLoader LOADER2 = new Loader2();
            Class clazz2 = LOADER2.loadClass("from_loader2");
            IFace iface = (IFace)clazz2.newInstance();

            // Set the victim, a String of length 6.
            String s = "victim";
            _p0wnee = s;

            // Go cast '_p0wnee' to type You_Have_Been_P0wned.
            many_loader[] x2 = bug.make(iface);

            many_loader b = x2[0];

            // Make it clear that the runtime type many_loader (what we get from the
            // array X2) varies from the static type of many_loader.
            Class cl1 = b.getClass();
            ClassLoader ld1 = cl1.getClassLoader();
            Class cl2 = many_loader.class;
            ClassLoader ld2 = cl2.getClassLoader();
            System.out.println("bug.make()  "+ld1+":"+cl1);
            System.out.println("many_loader "+ld2+":"+cl2);

            // Read the victims guts out.
            You_Have_Been_P0wned q = b._p0wnee;
            System.out.println("q._a = 0x"+Integer.toHexString(q._a));
            System.out.println("q._b = 0x"+Integer.toHexString(q._b));
            System.out.println("q._c = 0x"+Integer.toHexString(q._c));
            System.out.println("q._d = 0x"+Integer.toHexString(q._d));

            System.out.println("I will now crash the VM:");
            // On 32-bit HotSpot Java6 this sets the victim String length shorter, then crashes the VM.
            //q._c = 3;
            q._a = -1;

            System.out.println(s);
            throw new RuntimeException("Expected LinkageError was not thrown.");
        } catch (LinkageError e) {
            String errorMsg = e.getMessage();
            if (!errorMsg.contains("loader constraint")) {
                throw new RuntimeException("Error message of LinkageError does not contain \"loader constraint\":" +
                                           errorMsg);
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    // I need to compile (hence call in a loop) a function which returns a value
    // loaded from classloader other than the system one.  The point of this
    // call is to give me an abstract 'hook' into a function loaded with a
    // foreign loader.
    public abstract many_loader[] make(IFace iface); // abstract factory
}

