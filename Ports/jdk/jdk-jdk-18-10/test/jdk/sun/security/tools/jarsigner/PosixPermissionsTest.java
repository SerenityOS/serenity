/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218021
 * @summary Have jarsigner preserve posix permission attributes
 * @modules jdk.jartool/sun.security.tools.jarsigner
 *          java.base/sun.security.tools.keytool
 * @library /test/lib
 * @run main/othervm PosixPermissionsTest
 */

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.test.lib.SecurityTools;

public class PosixPermissionsTest {
    private static List<String> perms = List.of(
            "---------",
            "r--------",
            "-w-------",
            "--x------",
            "rwx------",
            "---r-----",
            "----w----",
            "-----x---",
            "---rwx---",
            "------r--",
            "-------w-",
            "--------x",
            "------rwx",
            "r--r-----",
            "r--r--r--",
            "rw-rw----",
            "rwxrwx---",
            "rw-rw-r--",
            "r-xr-x---",
            "r-xr-xr-x",
            "rwxrwxrwx");

    private final static String ZIPFILENAME = "8218021-test.zip";
    private final static String JARFILENAME = "8218021-test.jar";
    private final static URI JARURI = URI.create("jar:" + Path.of(JARFILENAME).toUri());
    private final static URI ZIPURI = URI.create("jar:" + Path.of(ZIPFILENAME).toUri());
    private static Path file;
    private static int count;
    private static Set<PosixFilePermission> permsSet;
    private static String expectedJarPerms;
    private static final String WARNING_MSG = "POSIX file permission and/or symlink " +
        "attributes detected. These attributes are ignored when signing and are not " +
        "protected by the signature.";

    public static void main(String[] args) throws Exception {
        createFiles();

        // check permissions before signing
        verifyFilePermissions(ZIPURI, true);
        verifyFilePermissions(JARURI, false);

        // generate key for signing
        SecurityTools.keytool(
                "-genkey",
                "-keyalg", "RSA",
                "-dname", "CN=Coffey, OU=JPG, O=Oracle, L=Santa Clara, ST=California, C=US",
                "-alias", "examplekey",
                "-storepass", "password",
                "-keypass", "password",
                "-keystore", "examplekeystore",
                "-validity", "365")
                .shouldHaveExitValue(0);

        // sign zip file - expect warning
        SecurityTools.jarsigner(
                "-keystore", "examplekeystore",
                "-verbose", ZIPFILENAME,
                "-storepass", "password",
                "-keypass", "password",
                "examplekey")
                .shouldHaveExitValue(0)
                .shouldContain(WARNING_MSG);

        // recheck permissions after signing
        verifyFilePermissions(ZIPURI, true);

        // sign jar file - expect no warning
        SecurityTools.jarsigner(
                "-keystore", "examplekeystore",
                "-verbose", JARFILENAME,
                "-storepass", "password",
                "-keypass", "password",
                "examplekey")
                .shouldHaveExitValue(0)
                .shouldNotContain(WARNING_MSG);

        // recheck permissions after signing
        verifyFilePermissions(JARURI, false);

        // verify zip file - expect warning
        SecurityTools.jarsigner(
                "-keystore", "examplekeystore",
                "-storepass", "password",
                "-keypass", "password",
                "-verbose",
                "-verify", ZIPFILENAME)
                .shouldHaveExitValue(0)
                .shouldContain(WARNING_MSG);

        // verify jar file - expect no warning
        SecurityTools.jarsigner(
                "-keystore", "examplekeystore",
                "-storepass", "password",
                "-keypass", "password",
                "-verbose",
                "-verify", JARFILENAME)
                .shouldHaveExitValue(0)
                .shouldNotContain(WARNING_MSG);
    }

    private static void createFiles() throws Exception {

        String fileList = " ";
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        env.put("enablePosixFileAttributes", "true");

        try (FileSystem zipfs = FileSystems.newFileSystem(ZIPURI, env)) {
            for (String s : perms) {
                file = Path.of("test_" + count++);
                fileList += file + " ";
                permsSet = PosixFilePermissions.fromString(s);
                Files.createFile(file);

                Files.copy(file,
                        zipfs.getPath(file.toString()),
                        StandardCopyOption.COPY_ATTRIBUTES);
                Files.setPosixFilePermissions(zipfs.getPath(file.toString()), permsSet);
            }
        }

        // create jar file for testing also
        SecurityTools.jar("cf " + JARFILENAME + fileList);
        try (FileSystem jarfs = FileSystems.newFileSystem(JARURI, env)) {
            expectedJarPerms = PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(jarfs.getPath("test_1")));
        }
    }

    private static void verifyFilePermissions(URI u, boolean containAttributes) throws Exception {
        count = 0;
        for (String s : perms) {
            file = Path.of("test_" + count++);
            checkEntryAttributes(u, file, s, containAttributes);
        }
    }

    private static void checkEntryAttributes(URI uri, Path file,
                                             String expectedPerms, boolean containAttributes) throws Exception {
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, Map.of("enablePosixFileAttributes", "true"))) {
            Path p = zipfs.getPath(file.getFileName().toString());
            Set<PosixFilePermission> permsSet = Files.getPosixFilePermissions(p);
            String actualPerms = PosixFilePermissions.toString(permsSet);
            if (containAttributes) {
                if (!expectedPerms.equals(actualPerms)) {
                    throw new RuntimeException("Unexpected permissions for: " + file + ". Received: " + actualPerms);
                }
            } else {
                if (!actualPerms.equals(expectedJarPerms)) {
                    throw new RuntimeException("Expected default permissions for " + file);
                }
            }
        }
    }
}
