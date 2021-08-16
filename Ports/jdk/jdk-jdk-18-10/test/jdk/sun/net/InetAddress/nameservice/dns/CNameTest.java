/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @test
 * @bug 4763315
 * @modules java.naming
 * @library /test/lib
 * @build CanonicalName Lookup jdk.test.lib.process.*
 * @run main/othervm/timeout=120 CNameTest
 * @summary Test DNS provider's handling of CNAME records
 */
public class CNameTest {
    private static final String HOST = "www.w3c.org";
    private static final String POLICY = "grant {" + System.lineSeparator() +
            " permission java.net .SocketPermission \"${HOST}\", \"resolve\";" +
            System.lineSeparator() + "};";

    public static void main(String[] args) throws Exception {
        // Prerequisite check
        int rc = ProcessTools.executeTestJava("CanonicalName", HOST)
                             .outputTo(System.out)
                             .errorTo(System.out)
                             .getExitValue();
        if (rc != 0) {
            System.out.println("DNS not configured or host doesn't" +
                    " resolve to CNAME record");
            return;
        }

        // Tests - with & without security manager
        Path policy = Paths.get(".", "java.policy");
        Files.write(policy, POLICY.getBytes(), StandardOpenOption.CREATE_NEW);
        String[] opts = new String[]{
                "-Dsun.net.spi.nameservice.provider.1=dns,sun",
                "-Djava.security.manager -Djava.security.policy=" + policy
        };
        for (String opt : opts) {
            ProcessTools.executeTestJava(opt, "Lookup", HOST)
                        .outputTo(System.out)
                        .errorTo(System.err)
                        .shouldHaveExitValue(0);
        }
    }
}

