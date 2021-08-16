/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139551
 * @summary Scalability problem with redefinition - multiple code cache walks
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm/timeout=180 -javaagent:redefineagent.jar -XX:CompileThreshold=100 -Xlog:redefine+class+nmethod=debug TestMultipleClasses
 */

import java.lang.instrument.*;
import java.lang.reflect.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class TestMultipleClasses extends ClassLoader {

    public static String B(int count) {
        return new String("public class B" + count + " {" +
                "   public static void compiledMethod() { " +
                "       try{" +
                "          Thread.sleep(1); " +
                "       } catch(InterruptedException ie) {" +
                "       }" +
                "   }" +
                "}");
    }

    static String newB(int count) {
        return new String("public class B" + count + " {" +
                "   public static void compiledMethod() { " +
                "       System.out.println(\"compiledMethod called " + count + "\");" +
                "   }" +
                "}");
    }

    static int index = 0;

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals("B" + index)) {
            byte[] b = InMemoryJavaCompiler.compile(name, B(index));
            return defineClass(name, b, 0, b.length);
        } else {
            return super.findClass(name);
        }
    }

    static void runCompiledMethodMethods(Class c, int count) throws Exception {
        // Run for a while so they compile.
        Object o = c.newInstance();
        Method m = c.getMethod("compiledMethod");
        for (int i = 0; i < count; i++) {
            m.invoke(o);
        }
    }

    public static void main(String[] args) throws Exception {

        final int numberOfClasses = 20;
        Class[] classes = new Class[numberOfClasses];
        byte[][] newClass = new byte[numberOfClasses][];
        ClassDefinition[] defs = new ClassDefinition[numberOfClasses];

        TestMultipleClasses loader = new TestMultipleClasses();

        // Load and start all the classes.
        for (index = 0; index < numberOfClasses; index++) {
            String name = new String("B" + index);
            Class c = loader.findClass(name);

            runCompiledMethodMethods(c, 500);
            // Make class definition for redefinition
            classes[index] = c;
            newClass[index] = InMemoryJavaCompiler.compile(c.getName(), newB(index));
            defs[index] = new ClassDefinition(c, newClass[index]);
        }

        long startTime = System.currentTimeMillis();

        // Redefine all classes.
        RedefineClassHelper.instrumentation.redefineClasses(defs);

        long endTime = System.currentTimeMillis();

        System.out.println("Redefinition took " + (endTime - startTime) + " milliseconds");

        System.gc();

        // Run all new classes.
        for (index = 0; index < numberOfClasses; index++) {
            runCompiledMethodMethods(classes[index], 1);
        }
    }
}
