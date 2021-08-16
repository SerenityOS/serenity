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
 * @bug 8229785
 * @library /test/lib
 * @build jdk.unsupported/*
 * @summary MethodTypeDesc::resolveConstantDesc with security manager
 * @run main/othervm/policy=test.policy ResolveConstantDesc
 */

import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.security.AccessControlException;;
import java.security.Permission;;

import static jdk.test.lib.Asserts.*;

/*
 * MethodTypeDesc::resolveConstantDec may get security exception depending
 * on the access of Lookup object
 */
public class ResolveConstantDesc {
    private static final String DESCRIPTOR = "()Ljdk/internal/misc/VM;";

    public static void main(String... args) throws Exception {
        // private Lookup object has access to classes exported from another module
        Lookup lookup = sun.misc.Test.LOOKUP;
        Module m = lookup.lookupClass().getModule();

        MethodType mtype = MethodType.fromMethodDescriptorString(DESCRIPTOR, ClassLoader.getPlatformClassLoader());
        Class<?> target = mtype.returnType();
        Module javaBase = target.getModule();
        assertTrue(javaBase.isExported(target.getPackageName(), m));

        // MethodType that references java.base internal class
        MethodTypeDesc mtd = MethodTypeDesc.ofDescriptor(DESCRIPTOR);
        testInaccessibleClass(mtd);

        // Lookup has no access to JDK internal API; IAE
        throwIAE(MethodHandles.lookup(), mtd);

        // resolve successfully if Lookup has access to sun.misc and security permission
        MethodTypeDesc.ofDescriptor("()Lsun/misc/Unsafe;")
                      .resolveConstantDesc(MethodHandles.lookup());
    }

    /*
     * Test Lookup with different access
     */
    private static void testInaccessibleClass(MethodTypeDesc mtd) throws Exception {
        Lookup lookup = sun.misc.Test.LOOKUP;
        // full power lookup can resolve MethodTypeDesc of java.base internal types
        mtd.resolveConstantDesc(lookup);

        // drop PRIVATE access; fail package access check
        throwACC(lookup.dropLookupMode(Lookup.PRIVATE), mtd);

        // jdk.internal.access is not accessible by jdk.unsupported
        MethodTypeDesc mtd1 = MethodTypeDesc.ofDescriptor("()Ljdk/internal/access/SharedSecrets;");
        throwIAE(lookup, mtd1);
    }

    // IAE thrown when resolving MethodType using the given Lookup object
    private static void throwIAE(Lookup lookup, MethodTypeDesc mtd) throws Exception {
        try {
            MethodType mtype = (MethodType)mtd.resolveConstantDesc(lookup);
            throw new RuntimeException("unexpected IAE not thrown");
        } catch (IllegalAccessException e) { }
    }

    private static void throwACC(Lookup lookup, MethodTypeDesc mtd) throws Exception {
        try {
            MethodType mtype = (MethodType)mtd.resolveConstantDesc(lookup);
            throw new RuntimeException("unexpected IAE not thrown");
        } catch (AccessControlException e) {
            Permission perm = e.getPermission();
            if (!(perm instanceof RuntimePermission &&
                  "accessClassInPackage.jdk.internal.misc".equals(perm.getName()))) {
                throw e;
            }
        }
    }
}
