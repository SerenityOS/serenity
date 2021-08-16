/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.annotations.Test;

import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @summary Test that various diagnostic commands which can show core reflection
 *          invocation targets do so correctly (See: JDK-8203343).
 * @library /test/lib
 * @run testng/othervm -Dsun.reflect.noInflation=true ShowReflectionTargetTest
 * @author stuefe
 */

public class ShowReflectionTargetTest {

    @SuppressWarnings("unused")
    private static class Dummy {
        int _i;
        public Dummy(int i) { _i = i; }
        public int get_i() { return _i; }
    }

    public void run(CommandExecutor executor) throws Exception {
        // Do some reflection; since we set -Dsun.reflect.noInflation=true, this should
        // immediately generate Generated{Method|Constructor}Accessor objects.
        Class<?> c = Class.forName("ShowReflectionTargetTest$Dummy");
        Constructor<?> ctor = c.getConstructor(int.class);
        Method m = c.getMethod("get_i");

        Object o = ctor.newInstance(17);
        int j = ((Integer)m.invoke(o)).intValue();
        Assert.assertEquals(j, 17);

        // Now invoke VM.class_hierarchy and check its output.
        // Should show reflection targets, e.g.:
        // ....
        //        |--jdk.internal.reflect.MagicAccessorImpl/null
        //        |  |--jdk.internal.reflect.FieldAccessorImpl/null
        //        |  |  |--jdk.internal.reflect.UnsafeFieldAccessorImpl/null
        //        |  |  |  |--jdk.internal.reflect.UnsafeStaticFieldAccessorImpl/null
        //        |  |  |  |  |--jdk.internal.reflect.UnsafeQualifiedStaticFieldAccessorImpl/null
        //        |  |  |  |  |  |--jdk.internal.reflect.UnsafeQualifiedStaticObjectFieldAccessorImpl/null
        //        |  |--jdk.internal.reflect.ConstructorAccessorImpl/null
        //        |  |  |--jdk.internal.reflect.DelegatingConstructorAccessorImpl/null
        //        |  |  |--jdk.internal.reflect.NativeConstructorAccessorImpl/null
        // >       |  |  |--jdk.internal.reflect.GeneratedConstructorAccessor1/0x00007f75f04889b0 (invokes: java/lang/management/ManagementPermission::<init> (Ljava/lang/String;)V)
        // >       |  |  |--jdk.internal.reflect.GeneratedConstructorAccessor2/0x00007f75f0494990 (invokes: ShowReflectionTargetTest$Dummy::<init> (I)V)
        //        |  |  |--jdk.internal.reflect.BootstrapConstructorAccessorImpl/null
        //        |  |--jdk.internal.reflect.MethodAccessorImpl/null
        // >       |  |  |--jdk.internal.reflect.GeneratedMethodAccessor1/0x00007f75f0494450 (invokes: ShowReflectionTargetTest$Dummy::get_i ()I)
        //        |  |  |--jdk.internal.reflect.DelegatingMethodAccessorImpl/null
        // ...

        OutputAnalyzer output = executor.execute("VM.class_hierarchy");

        output.shouldMatch(".*jdk.internal.reflect.GeneratedConstructorAccessor.*invokes.*ShowReflectionTargetTest\\$Dummy::<init>.*");
        output.shouldMatch(".*jdk.internal.reflect.GeneratedMethodAccessor.*invokes.*ShowReflectionTargetTest\\$Dummy::get_i.*");

    }

    @Test
    public void jmx() throws Exception {
        run(new JMXExecutor());
    }

}



