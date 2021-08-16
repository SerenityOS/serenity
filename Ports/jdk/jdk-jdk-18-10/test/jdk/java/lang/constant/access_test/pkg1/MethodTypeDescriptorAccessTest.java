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
 * @bug 8226709
 * @summary MethodTypeDesc::resolveConstantDesc needs access check per the specification
 * @compile ../pkg2/PublicClass.java ../pkg2/NonPublicClass.java
 * @run main pkg1.MethodTypeDescriptorAccessTest
 */

package pkg1;

import java.lang.invoke.*;
import java.lang.invoke.MethodType;
import java.lang.constant.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodHandles.Lookup.*;
import static java.lang.invoke.MethodType.*;

public class MethodTypeDescriptorAccessTest {
    public static void main(String... args) throws Throwable {
        new MethodTypeDescriptorAccessTest().test();
    }

    void test() {
        Lookup selfLookup = MethodHandles.lookup();
        //first test PublicClass
        String descriptorpub = "(Lpkg2/PublicClass;)Lpkg2/PublicClass;";
        MethodTypeDesc mtdpub = MethodTypeDesc.ofDescriptor(descriptorpub);
        checkValidAccess(mtdpub, selfLookup);

        // test NonPublicClass in the return type
        String descriptornp = "()Lpkg2/NonPublicClass;";
        MethodTypeDesc mtdnp = MethodTypeDesc.ofDescriptor(descriptornp);
        checkInvalidAccess(mtdnp, selfLookup);

        // test NonPublicClass in the parameters
        descriptornp = "(Lpkg2/NonPublicClass;)I";
        mtdnp = MethodTypeDesc.ofDescriptor(descriptornp);
        checkInvalidAccess(mtdnp, selfLookup);

        MethodType mt = MethodType.fromMethodDescriptorString("(Lpkg2/NonPublicClass;)I", selfLookup.lookupClass().getClassLoader());
    }

    private void checkValidAccess(MethodTypeDesc mtd, Lookup lookup) {
        try {
            MethodType mt = (MethodType)mtd.resolveConstantDesc(lookup);
        } catch (ReflectiveOperationException unexpected) {
            throw new Error("resolveConstantDesc() threw ReflectiveOperationException unexpectedly with cause " +
                    unexpected.getCause() + " for " + mtd);
        }
    }

    private void checkInvalidAccess(MethodTypeDesc mtd, Lookup lookup) {
        try {
            MethodType mt = (MethodType)mtd.resolveConstantDesc(lookup);
            throw new Error("resolveConstantDesc() succeeded unexpectedly " + mtd);
        } catch (ReflectiveOperationException expected) {
            if (expected.getClass() != IllegalAccessException.class) {
                throw new Error("resolveConstantDesc() threw unexpected ReflectiveOperationException with cause " +
                        expected.getCause() + " for " + mtd);
            }
        }
    }

}
