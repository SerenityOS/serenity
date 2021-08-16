/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm -Djava.security.manager=allow ModuleFinderWithSecurityManager allow
 * @run main/othervm -Djava.security.manager=allow ModuleFinderWithSecurityManager deny
 * @summary Basic test for ModuleFinder.ofSystem() with security manager
 */

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class ModuleFinderWithSecurityManager {
    public static void main(String[] args) throws Exception {
        boolean allow = args[0].equals("allow");

        // set security policy to allow access
        if (allow) {
            String testSrc = System.getProperty("test.src");
            if (testSrc == null)
                testSrc = ".";
            Path policyFile = Paths.get(testSrc, "java.policy");
            System.setProperty("java.security.policy", policyFile.toString());
        }

        System.setSecurityManager(new SecurityManager());

        ModuleFinder finder = null;
        try {
            finder = ModuleFinder.ofSystem();
            if (!allow) throw new RuntimeException("SecurityException expected");
        } catch (SecurityException e) {
            if (allow) throw new RuntimeException("SecurityException not expected");
        }

        // no additional permissions should be required to locate modules
        if (finder != null) {
            ModuleReference base = finder.find("java.base").orElse(null);
            if (base == null)
                throw new RuntimeException("java.base not found");
            Set<ModuleReference> allModules = finder.findAll();
            if (!allModules.contains(base))
                throw new RuntimeException("java.base not in all modules");
        }
    }
}
