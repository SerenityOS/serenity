/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.org.objectweb.asm.*;

/*
 * @test
 * @summary Test that the JVM ignores ACC_MODULE if it is set for a version
 *          52 class file.
 * @bug 8175383
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file ACCModule52.java
 * @run main ACCModule52
 */

public class ACCModule52 {

    static final String CLASS_NAME = "ACCModule52Pkg";

    public static void main(String[] args) throws Exception {
        int ACC_MODULE = 0x8000;
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC + ACC_MODULE,
                CLASS_NAME,
                null,
                "java/lang/Object",
                null);

        cw.visitEnd();
        byte[] bytes = cw.toByteArray();


        ClassLoader loader = new ClassLoader(ACCModule52.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String cn)throws ClassNotFoundException {
                if (cn.equals(CLASS_NAME)) {
                    Class superClass = super.defineClass(cn, bytes, 0, bytes.length);
                } else {
                    throw new ClassNotFoundException(cn);
                }
                return null;
            }
        };

        Class<?> clazz = loader.loadClass(CLASS_NAME);
    }
}
