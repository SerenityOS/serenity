/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.LinkedList;

import vm.mlvm.share.ClassfileGenerator;
import vm.mlvm.share.CustomClassLoaders;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.share.options.Option;

public class ClassfileGeneratorTest extends MlvmTest {

    public static final String CLASS_NAME = "Dummy";

    @Option(name = "generator", default_value = "", description = "Class name of the generator. Must inherit from vm.mlvm.share.ClassfileGenerator")
    private String generatorClassNameOpt;

    private Class<? extends ClassfileGenerator> generatorClass;

    public ClassfileGeneratorTest() {
    }

    public ClassfileGeneratorTest(Class<? extends ClassfileGenerator> genClass) {
        generatorClass = genClass;
    }

    @Override
    public boolean run() throws Throwable {
        if (generatorClass == null) {
            generatorClass = Class.forName(generatorClassNameOpt).asSubclass(ClassfileGenerator.class);
        }

        Env.traceVerbose("Generating class");
        ClassfileGenerator gen = generatorClass.newInstance();

        gen.setClassName(null, CLASS_NAME);
        ClassfileGenerator.Klass k = gen.generateBytecodes()[0];
        k.writeClass(".");
        ClassLoader cl = CustomClassLoaders.makeClassBytesLoader(k.getBytes(), k.getClassName());

        Env.traceNormal("Loading class " + k.getClassName());
        Class<?> dummyClass = cl.loadClass(k.getClassName());

        MethodType mt = MethodType.fromMethodDescriptorString(k.getMainMethodSignature(), getClass().getClassLoader());
        MethodHandle m = MethodHandles.lookup().findStatic(dummyClass, k.getMainMethodName(), mt);

        Env.traceVerbose("Main method: " + m);

        // Generate default parameter values
        List<Object> arguments = new LinkedList<>();
        for(Class<?> t : mt.wrap().parameterArray()) {
            Object arg;
            if (t.isArray()) {
                arg = java.lang.reflect.Array.newInstance(t.getComponentType(), 0);
            } else {
                arg = t.newInstance();
            }
            arguments.add(arg);
        }

        Env.traceNormal("Invoking method " + m);
        m.invokeWithArguments(arguments);

        return true;
    }

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }

}
