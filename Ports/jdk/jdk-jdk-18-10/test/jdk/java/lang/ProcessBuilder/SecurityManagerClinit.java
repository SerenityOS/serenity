/*
 * Copyright 2010 Google Inc.  All Rights Reserved.
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
 * @bug 6980747
 * @summary Check that Process-related classes have the proper
 *     doPrivileged blocks, and can be initialized with an adversarial
 *     security manager.
 * @run main/othervm -Djava.security.manager=allow SecurityManagerClinit
 * @author Martin Buchholz
 */

import java.io.*;
import java.security.*;

public class SecurityManagerClinit {
    private static class SimplePolicy extends Policy {
        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        private Permissions perms;

        public SimplePolicy(Permission... permissions) {
            perms = new Permissions();
            for (Permission permission : permissions)
                perms.add(permission);
        }

        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p) || DEFAULT_POLICY.implies(pd, p);
        }
    }

    public static void main(String[] args) throws Throwable {
        String javaExe =
            System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";

        final SimplePolicy policy =
            new SimplePolicy
            (new FilePermission("<<ALL FILES>>", "execute"),
             new RuntimePermission("setSecurityManager"));
        Policy.setPolicy(policy);

        System.setSecurityManager(new SecurityManager());

        try {
            String[] cmd = { javaExe, "-version" };
            Process p = Runtime.getRuntime().exec(cmd);
            p.getOutputStream().close();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor();
        } finally {
            System.setSecurityManager(null);
        }
    }
}
