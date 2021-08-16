/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8151664
 * @requires vm.jvmci
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.vm.annotation
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *
 * @compile StableFieldTest.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller compiler.jvmci.meta.StableFieldTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler -Xbootclasspath/a:. compiler.jvmci.meta.StableFieldTest
 */

package compiler.jvmci.meta;

import jdk.internal.vm.annotation.Stable;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.runtime.JVMCI;

public class StableFieldTest {

    @Stable static int myStaticField = 5;
    @Stable int myInstanceField = 10;

    public static void main(String[] args) throws Throwable {
        MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        for (String name : new String[] {"myStaticField", "myInstanceField"}) {
            java.lang.reflect.Field javaField = StableFieldTest.class.getDeclaredField(name);
            HotSpotResolvedJavaField field = (HotSpotResolvedJavaField) metaAccess.lookupJavaField(javaField);
            if (!field.isStable()) {
                throw new AssertionError("Expected HotSpotResolvedJavaField.isStable() to return true for " + javaField);
            }
        }
    }
}
