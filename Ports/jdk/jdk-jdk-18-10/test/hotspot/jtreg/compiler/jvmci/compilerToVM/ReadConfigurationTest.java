/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library / /test/lib
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @build compiler.jvmci.compilerToVM.ReadConfigurationTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.ReadConfigurationTest
 */

package compiler.jvmci.compilerToVM;

import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.VMField;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;
import jdk.vm.ci.hotspot.VMIntrinsicMethod;

public class ReadConfigurationTest {
    public static void main(String args[]) {
        new ReadConfigurationTest().runTest();
    }

    private void runTest() {
        HotSpotVMConfigStore store = HotSpotJVMCIRuntime.runtime().getConfigStore();
        TestHotSpotVMConfig config = new TestHotSpotVMConfig(store);
        Asserts.assertNE(config.codeCacheHighBound, 0L, "Got null address");
        Asserts.assertNE(config.stubRoutineJintArrayCopy, 0L, "Got null address");

        for (VMField field : store.getFields().values()) {
                Object value = field.value;
                if (value != null) {
                    Asserts.assertTrue(value instanceof Long || value instanceof Boolean,
                        "Got unexpected value type for VM field " + field.name + ": " + value.getClass());
                }
        }

        for (VMIntrinsicMethod m : config.getStore().getIntrinsics()) {
            Asserts.assertNotNull(m);
            Asserts.assertNotNull(m.declaringClass);
            Asserts.assertFalse(m.declaringClass.contains("."),
                "declaringClass should be in class file format: " + m.declaringClass);
            Asserts.assertNotNull(m.name);
            Asserts.assertNotNull(m.descriptor);
            Asserts.assertTrue(m.id > 0);
        }
    }

    private static class TestHotSpotVMConfig extends HotSpotVMConfigAccess {

        private TestHotSpotVMConfig(HotSpotVMConfigStore store) {
            super(store);
        }

        final long codeCacheHighBound = getFieldValue("CodeCache::_high_bound", Long.class);
        final long stubRoutineJintArrayCopy = getFieldValue("StubRoutines::_jint_arraycopy", Long.class);
    }
}
