/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8239083
 * @summary Test invocation of static interface method with and without method handle with C1.
 *
 * @run main/othervm -Xbatch -XX:TieredStopAtLevel=3 compiler.c1.TestStaticInterfaceMethodCall
 */

package compiler.c1;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestStaticInterfaceMethodCall {

     static final MethodHandle MH_m;

     static {
         try {
             MH_m = MethodHandles.lookup().findStatic(MyInterface.class, "m", MethodType.methodType(void.class));
         } catch (ReflectiveOperationException e) {
             throw new BootstrapMethodError(e);
         }
     }

     public static void main(String[] args) throws Throwable {
         for (int i = 0; i < 20_000; i++) {
             test_call_by_method_handle();
             test_direct_call();
         }
     }

     static void test_call_by_method_handle() throws Throwable {
         MH_m.invokeExact();
     }

     static void test_direct_call() {
         MyInterface.m();
     }

}

interface MyInterface {
     static void m() {}
}
