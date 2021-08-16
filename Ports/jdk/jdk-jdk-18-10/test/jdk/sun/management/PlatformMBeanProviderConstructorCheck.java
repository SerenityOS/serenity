/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlException;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.List;

/*
 * @test
 * @bug     8042901
 * @summary Check permission for PlatformMBeanProvider Constructor
 * @modules java.management/sun.management.spi
 * @author  Shanliang Jiang
 * @run main/othervm -Djava.security.manager=allow PlatformMBeanProviderConstructorCheck
 */
public class PlatformMBeanProviderConstructorCheck {
    public static void main(String[] args) throws Exception {
        Policy origPolicy = Policy.getPolicy();
        SecurityManager origSM = System.getSecurityManager();
        try {
            System.out.println("---PlatformMBeanProviderConstructorCheck starting...");

            Policy.setPolicy(new MyPolicy());
            System.setSecurityManager(new SecurityManager());

            System.out.println("---PlatformMBeanProviderConstructorCheck Testing without permission...");
            try {
                new MyProvider();
                throw new RuntimeException("Does not get expected AccessControlException!");
            } catch (AccessControlException ace) {
                System.out.println("---PlatformMBeanProviderConstructorCheck got the expected exception: "
                        + ace);
            }

            System.out.println("---PlatformMBeanProviderConstructorCheck Testing with permission...");
            MyPolicy.allowed = true;
            new MyProvider();

            System.out.println("---PlatformMBeanProviderConstructorCheck PASSED!");
        } finally {
            System.setSecurityManager(origSM);
            Policy.setPolicy(origPolicy);
        }
    }

    private static class MyPolicy extends Policy {
        private static String permName = "sun.management.spi.PlatformMBeanProvider.subclass";
        private static boolean allowed = false;

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (permName.equals(permission.getName())) {
                System.out.println("---MyPolicy-implies checks permission for "
                        +permName+" and returns "+allowed);

                return allowed;
            } else {
                return true;
            }
        }
    }

    private static class MyProvider extends sun.management.spi.PlatformMBeanProvider {
        @Override
        public List<PlatformComponent<?>> getPlatformComponentList() {
            return null;
        }
    }
}
