/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003720
 * @summary Method in interpreter stack frame can be deallocated
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @compile -XDignore.symbol.file Victim.java
 * @run main/othervm -Xverify:all -Xint Test8003720
 */

// Attempts to make the JVM unload a class while still executing one of its methods.
public class Test8003720 {
    final static String VICTIM_CLASS_NAME = "Victim";
    final static boolean QUIET = true;
    final static long DURATION = 30000;

    public interface CallMe { void callme(); }

    public static void main(String... av) throws Throwable {
        newVictimClassLoader();
        System.gc();

        newVictimClass();
        System.gc();

        newVictimInstance();
        System.gc();

        ((CallMe)newVictimInstance()).callme();
    }

    public static Object newVictimInstance() throws Throwable {
        return newVictimClass().newInstance();
    }

    public static Class<?> newVictimClass() throws Throwable {
        return Class.forName(VICTIM_CLASS_NAME, true, new VictimClassLoader());
    }

    public static ClassLoader newVictimClassLoader() throws Throwable {
        return new VictimClassLoader();
    }

    public static void println(String line) {
        if (!QUIET) {
            System.out.println(line);
        }
    }
}
