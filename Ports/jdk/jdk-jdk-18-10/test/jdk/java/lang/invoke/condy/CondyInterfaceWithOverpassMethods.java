/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186046
 * @summary Test for an interface using condy with default overpass methods
 * @library /lib/testlibrary/bytecode
 * @build jdk.experimental.bytecode.BasicClassBuilder
 * @run testng CondyInterfaceWithOverpassMethods
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyInterfaceWithOverpassMethods
 */

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.TypedCodeBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Test
public class CondyInterfaceWithOverpassMethods {
    interface A {
        int a();

        default int x() {
            return 1;
        }
    }


    // Generated class with methods containing condy ldc
    Class<?> gc;

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type) {
        return name;
    }

    @BeforeClass
    public void generateClass() throws Exception {
//        interface B extends A {
//            // Overpass for method A.a
//
//            default void y() {
//                // ldc to Dynamic
//            }
//        }
        Class<?> thisClass = CondyInterfaceWithOverpassMethods.class;

        String genClassName = thisClass.getSimpleName() + "$Code";
        String bsmClassName = thisClass.getCanonicalName().replace('.', '/');
        String bsmMethodName = "bsm";
        String bsmDescriptor = MethodType.methodType(Object.class, MethodHandles.Lookup.class,
                                                     String.class, Class.class).toMethodDescriptorString();

        byte[] byteArray = new BasicClassBuilder(genClassName, 55, 0)
                .withFlags(Flag.ACC_INTERFACE, Flag.ACC_ABSTRACT)
                .withSuperclass("java/lang/Object")
                .withSuperinterface(thisClass.getCanonicalName().replace('.', '/') + "$" + A.class.getSimpleName())
                .withMethod("y", "()Ljava/lang/String;", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.ldc("String", "Ljava/lang/String;", bsmClassName, bsmMethodName, bsmDescriptor,
                                              S -> {})
                                                .areturn()
                                ))
                .build();

        gc = MethodHandles.lookup().defineClass(byteArray);
    }

    @Test
    public void testClass() throws Exception {
        // Trigger initialization
        Class.forName(gc.getName());
    }
}
