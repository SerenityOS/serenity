/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015436
 * @summary the IK _initial_method_idnum value must be adjusted if overpass methods are added
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @build compiler.runtime.cr8015436.Test8015436
 *
 * @run driver compiler.runtime.cr8015436.Driver8015436
 */

/*
 * The test checks that a MemberName for the defaultMethod() is cached in
 * the class MemberNameTable without a crash in the VM fastdebug mode.
 * The original issue was that the InstanceKlass _initial_method_idnum was
 * not adjusted properly when the overpass methods are added to the class.
 * The expected/correct behavior: The test does not crash nor throw any exceptions.
 * All the invocations of the defaultMethod() must be completed successfully.
 */

package compiler.runtime.cr8015436;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Test8015436 implements InterfaceWithDefaultMethod {
    public static final String SOME_MTD_INVOKED = "someMethod() invoked";
    public static final String DEFAULT_MTD_INVOKED_DIRECTLY = "defaultMethod() invoked directly";
    public static final String DEFAULT_MTD_INVOKED_MH = "defaultMethod() invoked via a MethodHandle";

    @Override
    public void someMethod() {
        System.out.println(SOME_MTD_INVOKED);
    }

    public static void main(String[] args) throws Throwable {
        Test8015436 testObj = new Test8015436();
        testObj.someMethod();
        testObj.defaultMethod(DEFAULT_MTD_INVOKED_DIRECTLY);

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType   mt = MethodType.methodType(void.class, String.class);
        MethodHandle mh = lookup.findVirtual(Test8015436.class, "defaultMethod", mt);
        mh.invokeExact(testObj, DEFAULT_MTD_INVOKED_MH);
    }
}

interface InterfaceWithDefaultMethod {
    public void someMethod();

    default public void defaultMethod(String str){
        System.out.println(str);
    }
}
/*
 * A successful execution gives the output:
 *   someMethod() invoked
 *   defaultMethod() invoked directly
 *   defaultMethod() invoked via a MethodHandle
 */
