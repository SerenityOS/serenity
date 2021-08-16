/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8187222
 * @run main/othervm -Djava.system.class.loader=RecursiveSystemLoader RecursiveSystemLoader
 * @summary Test for IllegalStateException if a custom system loader recursively calls getSystemClassLoader()
 */
public class RecursiveSystemLoader extends ClassLoader {
    public static void main(String[] args) {
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        if (!(sys instanceof RecursiveSystemLoader)) {
            throw new RuntimeException("Unexpected system classloader: " + sys);
        }
    }
    public RecursiveSystemLoader(ClassLoader classLoader) {
        super("RecursiveSystemLoader", classLoader);

        // Calling ClassLoader.getSystemClassLoader() before the VM is booted
        // should throw an IllegalStateException.
        try {
            ClassLoader.getSystemClassLoader();
        } catch(IllegalStateException ise) {
            System.err.println("Caught expected exception:");
            ise.printStackTrace();
            return;
        }
        throw new RuntimeException("Expected IllegalStateException was not thrown.");
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }
}
