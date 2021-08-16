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
 * @modules java.base/jdk.internal.misc
 *
 * @summary converted from VM Testbase vm/mlvm/anonloader/func/findByName.
 * VM Testbase keywords: [feature_mlvm]
 * VM Testbase readme:
 * DESCRIPTION
 *     Try to find a class loaded as a hidden class through the VM system dictionary
 *     (using Class.forName()). It is an error when the class can be found in this way.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.anonloader.func.findByName.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.anonloader.func.findByName.Test
 */

package vm.mlvm.anonloader.func.findByName;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import vm.mlvm.anonloader.share.AnonkTestee01;
import vm.mlvm.share.MlvmTest;
import vm.share.FileUtils;

public class Test extends MlvmTest {
    private static final Class<?> PARENT = AnonkTestee01.class;

    public boolean run() throws Exception {
        try {
            byte[] classBytes = FileUtils.readClass(PARENT.getName());
            Lookup lookup = MethodHandles.lookup();
            Lookup ank_lookup = MethodHandles.privateLookupIn(PARENT, lookup);
            Class<?> c = ank_lookup.defineHiddenClass(classBytes, true).lookupClass();
            getLog().display("Hidden class name: " + c.getName());
            Class.forName(c.getName()).newInstance();
            return false;
        } catch ( ClassNotFoundException e ) {
            return true;
        }
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
