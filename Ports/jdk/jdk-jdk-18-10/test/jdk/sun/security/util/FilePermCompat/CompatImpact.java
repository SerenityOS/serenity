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
 * @bug 8164705 8168410
 * @summary check compatibility after FilePermission change
 * @library /test/lib
 * @run main CompatImpact prepare
 * @run main CompatImpact builtin
 * @run main/othervm -Djdk.security.filePermCompat=true CompatImpact mine
 * @run main/fail CompatImpact mine
 * @run main CompatImpact dopriv
 */

import jdk.test.lib.process.Proc;

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;

public class CompatImpact {

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            // copy class files to future classpath
            case "prepare":
                // cp in .
                String cp = System.getProperty("test.classes");
                Files.copy(Paths.get(cp, "CompatImpact.class"),
                        Paths.get("CompatImpact.class"));
                Files.copy(Paths.get(cp, "CompatImpact$MP.class"),
                        Paths.get("CompatImpact$MP.class"));
                Files.write(Paths.get("f"), new byte[10]);
                // cp in ./sub
                Files.createDirectory(Paths.get("sub"));
                Files.copy(Paths.get(cp, "CompatImpact.class"),
                        Paths.get("sub", "CompatImpact.class"));
                Files.copy(Paths.get(cp, "CompatImpact$MP.class"),
                        Paths.get("sub", "CompatImpact$MP.class"));
                Files.write(Paths.get("sub", "f"), new byte[10]);
                // cp in ./inner
                Files.createDirectory(Paths.get("inner"));
                Files.copy(Paths.get(cp, "CompatImpact$DoPrivInner.class"),
                        Paths.get("inner", "CompatImpact$DoPrivInner.class"));
                break;
            // default policy always covered, user-defined depends on
            // system property jdk.security.filePermCompact.
            case "builtin":
            case "mine":
                cp = System.getProperty("test.classes");
                Proc p;
                String failed = "";
                String testcase = "";
                String cwd = System.getProperty("user.dir");

                // Granting a FilePermission on an absolute path
                testcase = "PonA";
                p = p(args[0], cwd + "/f")
                        .args("f", cwd + "/f")
                        .debug(testcase)
                        .start();
                if (p.waitFor() != 0) {
                    Files.copy(Paths.get(testcase + ".stderr"), System.out);
                    failed += testcase + " ";
                }

                // Granting a FilePermission on a relative path
                testcase = "PonR";
                p = p(args[0], "f")
                        .args("f", cwd + "/f")
                        .debug(testcase)
                        .start();
                if (p.waitFor() != 0) {
                    Files.copy(Paths.get(testcase + ".stderr"), System.out);
                    failed += testcase + " ";
                }

                // Reading file on classpath, not cwd
                testcase = "cp";
                String cprel = Paths.get(cwd).relativize(Paths.get(cp))
                        .normalize().toString();
                p = p(args[0], "x")
                        .args(cp + "/f", cprel + "/f")
                        .debug(testcase)
                        .start();
                if (p.waitFor() != 0) {
                    Files.copy(Paths.get(testcase + ".stderr"), System.out);
                    failed += testcase + " ";
                }

                // Reading file on classpath, cwd
                testcase = "cpHere";
                p = p(args[0], "x")
                        .args(cwd + "/f", "f", "RES")
                        .cp(".")   // Must! cancel the old CLASSPATH.
                        .debug(testcase)
                        .start();
                if (p.waitFor() != 0) {
                    Files.copy(Paths.get(testcase + ".stderr"), System.out);
                    failed += testcase + " ";
                }

                // Reading file on classpath, cwd
                testcase = "cpSub";
                p = p(args[0], "x")
                        .args(cwd + "/sub/f", "sub/f", "RES")
                        .cp("sub")   // Must! There's CLASSPATH.
                        .debug(testcase)
                        .start();
                if (p.waitFor() != 0) {
                    Files.copy(Paths.get(testcase + ".stderr"), System.out);
                    failed += testcase + " ";
                }

                if (!failed.isEmpty()) {
                    throw new Exception(failed + "failed");
                }
                break;
            // test <policy_type> <grant> <read...>
            case "test":
                if (args[1].equals("mine")) {
                    Policy.setPolicy(new MP(args[2]));
                }
                Exception e = null;
                for (int i = 3; i < args.length; i++) {
                    try {
                        System.out.println(args[i]);
                        if (args[i].equals("RES")) {
                            CompatImpact.class.getResourceAsStream("f")
                                    .close();
                        } else {
                            new File(args[i]).exists();
                        }
                    } catch (Exception e2) {
                        e = e2;
                        e2.printStackTrace(System.out);
                    }
                }
                if (e != null) {
                    System.err.println("====================");
                    throw e;
                }
                break;
            // doPrivWithPerm test launcher
            case "dopriv":
                cwd = System.getProperty("user.dir");
                // caller (CompatImpact doprivouter, no permission) in sub,
                // executor (DoPrivInner, AllPermission) in inner.
                p = Proc.create("CompatImpact")
                        .args("doprivouter")
                        .prop("java.security.manager", "")
                        .grant(new File("inner"))
                        .perm(new AllPermission())
                        .cp("sub", "inner")
                        .debug("doPriv")
                        .args(cwd)
                        .start();
                if (p.waitFor() != 0) {
                    throw new Exception("dopriv test fails");
                }
                break;
            // doprivouter <cwd>
            case "doprivouter":
                DoPrivInner.main(args);
                break;
            default:
                throw new Exception("unknown " + args[0]);
        }
    }

    // Call by CompatImpact doprivouter, with AllPermission
    public static class DoPrivInner {
        public static void main(String[] args) throws Exception {
            AccessController.doPrivileged((PrivilegedAction<Boolean>)
                            () -> new File("x").exists(),
                    null,
                    new FilePermission(args[1] + "/x", "read"));
            AccessController.doPrivileged((PrivilegedAction<Boolean>)
                            () -> new File(args[1] + "/x").exists(),
                    null,
                    new FilePermission("x", "read"));
            try {
                AccessController.doPrivileged((PrivilegedAction<Boolean>)
                                () -> new File("x").exists(),
                        null,
                        new FilePermission("y", "read"));
                throw new Exception("Should not read");
            } catch (SecurityException se) {
                // Expected
            }
        }
    }

    // Return a Proc object for different policy types
    private static Proc p(String type, String f) throws Exception {
        Proc p = Proc.create("CompatImpact")
                .prop("java.security.manager", "")
                .inheritProp("jdk.security.filePermCompat");
        p.args("test", type);
        switch (type) {
            case "builtin":
                // For builtin policy, reading access to f can be
                // granted as a permission
                p.perm(new FilePermission(f, "read"));
                p.args("-");
                break;
            case "mine":
                // For my policy, f is passed into test and new MP(f)
                // will be set as new policy
                p.perm(new SecurityPermission("setPolicy"));
                p.perm(new SecurityPermission("getPolicy"));
                p.args(f);
                break;
            default:
                throw new Exception("unknown " + type);
        }
        return p;
    }

    // My own Policy impl, with only one granted permission, also not smart
    // enough to know whether ProtectionDomain grants any permission
    static class MP extends Policy {
        static final Policy DEFAULT_POLICY = Policy.getPolicy();
        final PermissionCollection pc;

        MP(String f) {
            FilePermission p = new FilePermission(f, "read");
            pc = p.newPermissionCollection();
            pc.add(p);
        }
        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return pc;
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return pc;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return pc.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }
    }
}
