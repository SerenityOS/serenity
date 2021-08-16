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

package vm.mlvm.mixed.func.regression.b7127687;

import nsk.share.Consts;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.Env;
import vm.mlvm.share.CustomClassLoaders;

import java.lang.invoke.MethodType;

import java.util.List;
import java.util.ArrayList;

import jdk.internal.org.objectweb.asm.ClassWriter;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class Test extends MlvmTest {

    final static int CLASSES_COUNT = 1000;

    public static void main(String[] args) { MlvmTest.launch(args); }

    @Override
    public boolean run() throws Throwable {
        List<Class> classes = new ArrayList<Class>();

        //generating list of unique classes
        for (int i = 0; i < CLASSES_COUNT; i++) {
            classes.add(generateClass("Class" + i));
        }

        for (Class a : classes) {
            for (Class b : classes) {
                Env.traceNormal("Perform call MethodType.methodType(" + a + ", " + b + ")");
                MethodType.methodType(a, b);
            }
        }

        return true;
    }


    private static Class generateClass(String name) throws ClassNotFoundException{
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC, name, null, "java/lang/Object", null);
        return CustomClassLoaders.makeClassBytesLoader(cw.toByteArray(), name).loadClass(name);
    }

}
