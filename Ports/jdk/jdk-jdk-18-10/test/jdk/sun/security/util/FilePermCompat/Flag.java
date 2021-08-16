/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164705 8209901
 * @library /test/lib
 * @summary check jdk.filepermission.canonicalize
 */

import jdk.test.lib.process.Proc;
import java.io.File;
import java.io.FilePermission;
import java.lang.*;
import java.nio.file.Path;

public class Flag {
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            String policy = Path.of(
                    System.getProperty("test.src"), "flag.policy").toString();

            // effectively true
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .prop("jdk.io.permissionsUseCanonicalPath", "true")
                    .args("run", "true", "true")
                    .start()
                    .waitFor(0);
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .secprop("jdk.io.permissionsUseCanonicalPath", "true")
                    .args("run", "true", "true")
                    .start()
                    .waitFor(0);
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .secprop("jdk.io.permissionsUseCanonicalPath", "false")
                    .prop("jdk.io.permissionsUseCanonicalPath", "true")
                    .args("run", "true", "true")
                    .start()
                    .waitFor(0);

            // effectively false
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .prop("jdk.io.permissionsUseCanonicalPath", "false")
                    .args("run", "false", "true")
                    .start()
                    .waitFor(0);
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .secprop("jdk.io.permissionsUseCanonicalPath", "false")
                    .args("run", "false", "true")
                    .start()
                    .waitFor(0);
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .secprop("jdk.io.permissionsUseCanonicalPath", "true")
                    .prop("jdk.io.permissionsUseCanonicalPath", "false")
                    .args("run", "false", "true")
                    .start()
                    .waitFor(0);
            Proc.create("Flag")
                    .prop("java.security.manager", "")
                    .prop("java.security.policy", policy)
                    .args("run", "false", "true")
                    .start()
                    .waitFor(0);
        } else {
            run(args);
        }
    }

    static void run(String[] args) throws Exception {

        boolean test1;
        boolean test2;

        String here = System.getProperty("user.dir");
        File abs = new File(here, "x");
        FilePermission fp1 = new FilePermission("x", "read");
        FilePermission fp2 = new FilePermission(abs.toString(), "read");
        test1 = fp1.equals(fp2);

        try {
            System.getSecurityManager().checkPermission(fp2);
            test2 = true;
        } catch (SecurityException se) {
            test2 = false;
        }

        if (test1 != Boolean.parseBoolean(args[1]) ||
                test2 != Boolean.parseBoolean(args[2])) {
            throw new Exception("Test failed: " + test1 + " " + test2);
        }
    }
}
