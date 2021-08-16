/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8039951
 * @summary com.sun.security.auth.module missing classes on some platforms
 * @run main/othervm AllPlatforms
 */

import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AllPlatforms {
    public static void main(String[] args) throws Exception {
        login("cross-platform",
                "UnixLoginModule", "optional",
                "NTLoginModule", "optional",
                "SolarisLoginModule", "optional");
        try {
            login("windows", "NTLoginModule", "required");
            login("unix", "UnixLoginModule", "required");
            login("solaris", "SolarisLoginModule", "required");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e.toString().contains("UnsatisfiedLinkError")) {
                throw new Exception("This is ugly");
            }
        }
    }

    static void login(String test, String... conf) throws Exception {
        System.out.println("Testing " + test + "...");

        StringBuilder sb = new StringBuilder();
        sb.append("hello {\n");
        for (int i=0; i<conf.length; i+=2) {
            sb.append("    com.sun.security.auth.module." + conf[i]
                    + " " + conf[i+1] + ";\n");
        }
        sb.append("};\n");
        Files.write(Paths.get(test), sb.toString().getBytes());

        // Must be called. Configuration has an internal static field.
        Configuration.setConfiguration(null);
        System.setProperty("java.security.auth.login.config", test);

        LoginContext lc = new LoginContext("hello");
        lc.login();
        System.out.println(lc.getSubject());
    }
}
