/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8266496
 * @summary Test that Whitebox.isClassAlive() works with hidden classes.
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI TestHiddenClassIsAlive
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import sun.hotspot.WhiteBox;

final class MyClass {
    MyClass() {}
    public Object get(int i) { return null; }
}

public class TestHiddenClassIsAlive {


    public static void main(String[] args) throws Throwable {
        byte[] classBytes = readClassFile("MyClass.class");
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineHiddenClass(classBytes, true).lookupClass();
        if (!WhiteBox.getWhiteBox().isClassAlive("MyClass")) {
            throw new RuntimeException("Hidden class should be alive");
        }
    }

    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(System.getProperty("test.classes") + File.separator + classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }
}
