/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @modules java.base/jdk.internal.misc
 *
 * @summary converted from VM Testbase vm/mlvm/anonloader/stress/oome/heap.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.anonloader.stress.oome.heap.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm -XX:-UseGCOverheadLimit -Xmx128m vm.mlvm.anonloader.stress.oome.heap.Test
 */

package vm.mlvm.anonloader.stress.oome.heap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;
import java.io.IOException;

import vm.mlvm.anonloader.share.AnonkTestee01;
import vm.mlvm.share.MlvmOOMTest;
import vm.mlvm.share.MlvmTestExecutor;
import vm.mlvm.share.Env;
import vm.share.FileUtils;

/**
 * This test loads a class using defineHiddenClass, creates instances
 * of that class and stores them, expecting Heap OOME.
 *
 */

public class Test extends MlvmOOMTest {
    @Override
    protected void checkOOME(OutOfMemoryError oome) {
        String message = oome.getMessage();
        if (!"Java heap space".equals(message)) {
            throw new RuntimeException("TEST FAIL : wrong OOME", oome);
        }
    }
    @Override
    protected void eatMemory(List<Object> list) {
        byte[] classBytes = null;
        try {
            classBytes = FileUtils.readClass(AnonkTestee01.class.getName());
        } catch (IOException e) {
            Env.throwAsUncheckedException(e);
        }
        try {
            while (true) {
                Lookup lookup = MethodHandles.lookup();
                Lookup ank_lookup = MethodHandles.privateLookupIn(AnonkTestee01.class, lookup);
                Class<?> c = ank_lookup.defineHiddenClass(classBytes, true).lookupClass();
                list.add(c.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            Env.throwAsUncheckedException(e);
        }
    }

    public static void main(String[] args) {
        MlvmTestExecutor.launch(args);
    }
}
