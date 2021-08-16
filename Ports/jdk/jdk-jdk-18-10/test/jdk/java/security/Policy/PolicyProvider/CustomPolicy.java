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

import java.security.AccessController;
import java.security.Permission;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class CustomPolicy extends Policy {

    // the ProtectionDomain of CustomPolicy
    private final ProtectionDomain policyPd;

    public CustomPolicy() {
        policyPd = AccessController.doPrivileged(
            (PrivilegedAction<ProtectionDomain>)
                () -> this.getClass().getProtectionDomain());
    }

    @Override
    public boolean implies(ProtectionDomain pd, Permission perm) {
        System.out.println("CustomPolicy.implies");

        // If the protection domain is the same as CustomPolicy, then
        // we return true. This is to prevent recursive permission checks
        // that lead to StackOverflow errors when the policy implementation
        // performs a sensitive operation that triggers a permission check,
        // for example, as below.
        if (pd == policyPd) {
            return true;
        }

        // Do something that triggers a permission check to make sure that
        // we don't cause a StackOverflow error.
        String home = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("user.home"));

        return true;
    }
}
