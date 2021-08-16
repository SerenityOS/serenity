/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import jdk.internal.access.SharedSecrets;

/*
 * @test
 * @bug 8064331
 * @summary Make sure that JavaSecurityAccess.doIntersectionPrivilege()
 *          is not dropping the information about the domain combiner of
 *          the stack ACC
 * @modules java.base/jdk.internal.access
 */

public class PreserveCombinerTest {
    public static void main(String[]args) throws Exception {
        final DomainCombiner dc = new DomainCombiner() {
            @Override
            public ProtectionDomain[] combine(ProtectionDomain[] currentDomains, ProtectionDomain[] assignedDomains) {
                return currentDomains; // basically a no-op
            }
        };

        // Get an instance of the saved ACC
        AccessControlContext saved = AccessController.getContext();
        // Simulate the stack ACC with a DomainCombiner attached
        AccessControlContext stack = new AccessControlContext(AccessController.getContext(), dc);

        // Now try to run JavaSecurityAccess.doIntersectionPrivilege() and assert
        // whether the DomainCombiner from the stack ACC is preserved
        boolean ret = SharedSecrets.getJavaSecurityAccess().doIntersectionPrivilege(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return dc == AccessController.getContext().getDomainCombiner();
            }
        }, stack, saved);

        if (!ret) {
            System.exit(1);
        }
    }
}

