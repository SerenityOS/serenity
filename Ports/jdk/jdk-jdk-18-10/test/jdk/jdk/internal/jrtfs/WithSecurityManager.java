/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager allow
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager deny
 */

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class WithSecurityManager {
    public static void main(String[] args) throws Exception {
        Path modulesPath = Paths.get(System.getProperty("java.home"),
                "lib", "modules");
        if (Files.notExists(modulesPath)) {
            System.out.printf("%s doesn't exist.", modulesPath.toString());
            System.out.println();
            System.out.println("It is most probably an exploded build."
                    + " Skip the test.");
            return;
        }

        boolean allow = args[0].equals("allow");

        // set security policy to allow access
        if (allow) {

            String testSrc = System.getProperty("test.src");
            if (testSrc == null)
                testSrc = ".";
            Path policyFile = Paths.get(testSrc, "java.policy");
            System.setProperty("java.security.policy", policyFile.toString());
        }

        // make sure that jrt:/ has been created before we have a security manager
        FileSystems.getFileSystem(URI.create("jrt:/"));

        System.setSecurityManager(new SecurityManager());

        // check FileSystems.getFileSystem
        try {
            FileSystems.getFileSystem(URI.create("jrt:/"));
            if (!allow) throw new RuntimeException("access not expected");
        } catch (SecurityException se) {
            if (allow)
                throw se;
        }

        // check FileSystems.newFileSystem
        try {
            FileSystems.newFileSystem(URI.create("jrt:/"), Collections.emptyMap());
            if (!allow) throw new RuntimeException("access not expected");
        } catch (SecurityException se) {
            if (allow)
                throw se;
        }

        // check Paths.get
        try {
            Paths.get(URI.create("jrt:/java.base/java/lang/Object.class"));
            if (!allow) throw new RuntimeException("access not expected");
        } catch (SecurityException se) {
            if (allow)
                throw se;
        }
    }
}
