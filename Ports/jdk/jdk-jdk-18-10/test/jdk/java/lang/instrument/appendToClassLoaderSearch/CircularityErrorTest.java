/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Unit test for Instrumentation appendToBootstrapClassLoaderSearch:
 *
 * 1. Reference class A. Resolving this class should fail with
 *    ClassCircularityError.
 *
 * 2. Add JAR file to boot class path which contains a "good"
 *    version of A.
 *
 * 3. Re-run the code for 1 again - it should fail with
 *    ClassCircularityError again.
 */
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class CircularityErrorTest {

    static Instrumentation ins;

    static void resolve() {
        try {
            Class c = A.class;
            throw new RuntimeException("Test failed - class A loaded by: " +
                c.getClassLoader());
        } catch (ClassCircularityError e) {
            System.err.println(e);
        }
    }

    public static void main(String args[]) throws Exception {
        resolve();
        ins.appendToBootstrapClassLoaderSearch(new JarFile("A.jar"));
        resolve();
    }

    public static void premain(String args, Instrumentation i) {
        ins = i;
    }
}
