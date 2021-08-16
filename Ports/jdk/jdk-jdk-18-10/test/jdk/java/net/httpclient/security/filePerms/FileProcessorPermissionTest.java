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

/*
 * @test
 * @summary Basic checks for SecurityException from body processors APIs
 * @run testng/othervm/java.security.policy=allpermissions.policy FileProcessorPermissionTest
 */

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.List;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.testng.annotations.Test;
import static java.nio.file.StandardOpenOption.*;
import static org.testng.Assert.*;

public class FileProcessorPermissionTest {

    static final String testSrc = System.getProperty("test.src", ".");
    static final Path fromFilePath = Paths.get(testSrc, "FileProcessorPermissionTest.java");
    static final Path asFilePath = Paths.get(testSrc, "asFile.txt");
    static final Path CWD = Paths.get(".");
    static final Class<SecurityException> SE = SecurityException.class;

    static AccessControlContext withPermissions(Permission... perms) {
        Permissions p = new Permissions();
        for (Permission perm : perms) {
            p.add(perm);
        }
        ProtectionDomain pd = new ProtectionDomain(null, p);
        return new AccessControlContext(new ProtectionDomain[]{ pd });
    }

    static AccessControlContext noPermissions() {
        return withPermissions(/*empty*/);
    }

    @Test
    public void test() throws Exception {
        List<PrivilegedExceptionAction<?>> list = List.of(
                () -> HttpRequest.BodyPublishers.ofFile(fromFilePath),

                () -> BodyHandlers.ofFile(asFilePath),
                () -> BodyHandlers.ofFile(asFilePath, CREATE),
                () -> BodyHandlers.ofFile(asFilePath, CREATE, WRITE),

                () -> BodyHandlers.ofFileDownload(CWD),
                () -> BodyHandlers.ofFileDownload(CWD, CREATE),
                () -> BodyHandlers.ofFileDownload(CWD, CREATE, WRITE)
        );

        // TEST 1 - sanity, just run ( no security manager )
        System.setSecurityManager(null);
        try {
            for (PrivilegedExceptionAction pa : list) {
                AccessController.doPrivileged(pa);
            }
        } finally {
            System.setSecurityManager(new SecurityManager());
        }

        // Run with all permissions, i.e. no further restrictions than test's AllPermission
        for (PrivilegedExceptionAction pa : list) {
            try {
                assert System.getSecurityManager() != null;
                AccessController.doPrivileged(pa, null, new Permission[] { });
            } catch (PrivilegedActionException pae) {
                fail("UNEXPECTED Exception:" + pae);
                pae.printStackTrace();
            }
        }

        // TEST 2 - with all file permissions
        AccessControlContext allFilesACC = withPermissions(
                new FilePermission("<<ALL FILES>>" , "read,write")
        );
        for (PrivilegedExceptionAction pa : list) {
            try {
                assert System.getSecurityManager() != null;
                AccessController.doPrivileged(pa, allFilesACC);
            } catch (PrivilegedActionException pae) {
                fail("UNEXPECTED Exception:" + pae);
                pae.printStackTrace();
            }
        }

        // TEST 3 - with limited permissions, i.e. just what is required
        AccessControlContext minimalACC = withPermissions(
                new FilePermission(fromFilePath.toString() , "read"),
                new FilePermission(asFilePath.toString(), "write"),
                // ofFileDownload requires read and write to the dir
                new FilePermission(CWD.toString(), "read,write"),
                new FilePermission(CWD.toString() + File.separator + "*", "read,write")
        );
        for (PrivilegedExceptionAction pa : list) {
            try {
                assert System.getSecurityManager() != null;
                AccessController.doPrivileged(pa, minimalACC);
            } catch (PrivilegedActionException pae) {
                fail("UNEXPECTED Exception:" + pae);
                pae.printStackTrace();
            }
        }

        // TEST 4 - with NO permissions, i.e. expect SecurityException
        for (PrivilegedExceptionAction pa : list) {
            try {
                assert System.getSecurityManager() != null;
                AccessController.doPrivileged(pa, noPermissions());
                fail("EXPECTED SecurityException");
            } catch (SecurityException expected) {
                System.out.println("Caught expected SE:" + expected);
            }
        }
    }
}
