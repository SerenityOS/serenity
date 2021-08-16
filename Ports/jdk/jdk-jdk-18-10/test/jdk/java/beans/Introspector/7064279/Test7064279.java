/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7064279
 * @summary Tests that Introspector does not have strong references to context class loader
 * @author Sergey Malenkov
 * @run main/othervm -Xmx128m Test7064279
 */

import java.beans.Introspector;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;

public class Test7064279 {

    public static void main(String[] args) throws Exception {
        WeakReference ref = new WeakReference(test("test.jar", "test.Test"));
        try {
            int[] array = new int[1024];
            while (true) {
                array = new int[array.length << 1];
            }
        }
        catch (OutOfMemoryError error) {
            System.gc();
        }
        if (null != ref.get()) {
            throw new Error("ClassLoader is not released");
        }
    }

    private static Object test(String jarName, String className) throws Exception {
        StringBuilder sb = new StringBuilder(256);
        sb.append("file:");
        sb.append(System.getProperty("test.src", "."));
        sb.append(File.separatorChar);
        sb.append(jarName);

        ClassLoader newLoader = new URLClassLoader(new URL[] { new URL(sb.toString()) });
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(newLoader);
        test(newLoader.loadClass(className));
        Thread.currentThread().setContextClassLoader(oldLoader);

        return newLoader;
    }

    private static void test(Class type) throws Exception {
        Introspector.getBeanInfo(type);
    }
}
