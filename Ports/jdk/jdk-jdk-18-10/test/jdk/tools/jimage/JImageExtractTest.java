/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests to verify jimage 'extract' action
 * @library /test/lib
 * @modules jdk.jlink/jdk.tools.jimage
 * @build jdk.test.lib.Asserts
 * @run main/othervm/timeout=300 JImageExtractTest
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.spi.ToolProvider;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;

public class JImageExtractTest extends JImageCliTest {
    private static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );


    private String smallBootImagePath;

    public JImageExtractTest() {
        try {
            Path tmp = Files.createTempDirectory(Paths.get("."), getClass().getName());
            tmp = tmp.toAbsolutePath();
            tmp.toFile().deleteOnExit();
            Path smalljre = tmp.resolve("smalljdk");
            if (JLINK_TOOL.run(System.out, System.err,
                    "--add-modules", "java.base",
                    "--add-modules", "jdk.zipfs",
                    "--output", smalljre.toString()) != 0) {
                throw new RuntimeException("failed to create small boot image");
            }
            this.smallBootImagePath = smalljre.resolve("lib").resolve("modules").toString();
        } catch (IOException ioExp) {
            throw new UncheckedIOException(ioExp);
        }
    }

    @Override
    public String getImagePath() {
        return smallBootImagePath;
    }

    public void testExtract() throws IOException {
        Set<Path> notJImageModules = Files.walk(Paths.get("."),1).collect(Collectors.toSet());
        jimage("extract", getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.isEmpty(), "Output is not expected");
                });
        verifyExplodedImage(Paths.get("."), notJImageModules);
    }

    public void testExtractHelp() {
        for (String opt : Arrays.asList("-h", "--help")) {
            jimage("extract", "--help")
                    .assertSuccess()
                    .resultChecker(r -> {
                        // extract  -  descriptive text
                        assertMatches("\\s+extract\\s+-\\s+.*", r.output);
                    });
        }
    }

    public void testExtractToDir() throws IOException {
        Path tmp = Files.createTempDirectory(Paths.get("."), getClass().getName());
        Set<Path> notJImageModules = Files.walk(tmp,1).collect(Collectors.toSet());
        jimage("extract", "--dir", tmp.toString(), getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.isEmpty(), "Output is not expected");
                });
        verifyExplodedImage(tmp, notJImageModules);
    }

    public void testExtractNoImageSpecified() {
        jimage("extract", "")
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractNotAnImage() throws IOException {
        Path tmp = Files.createTempFile(Paths.get("."), getClass().getName(), "not_an_image");
        jimage("extract", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractNotExistingImage() throws IOException {
        Path tmp = Paths.get(".", "not_existing_image");
        Files.deleteIfExists(tmp);
        jimage("extract", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractToUnspecifiedDir() {
        jimage("extract", "--dir", "--", getImagePath())
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractToNotExistingDir() throws IOException {
        Path tmp = Files.createTempDirectory(Paths.get("."), getClass().getName());
        Set<Path> notJImageModules = Files.walk(tmp,1).collect(Collectors.toSet());
        Files.delete(tmp);
        jimage("extract", "--dir", tmp.toString(), getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.isEmpty(), "Output is not expected");
                });
        verifyExplodedImage(tmp, notJImageModules);
    }

    public void testExtractFromDir() {
        Path imagePath = Paths.get(getImagePath());
        Path imageDirPath = imagePath.subpath(0, imagePath.getNameCount() - 1);
        jimage("extract", imageDirPath.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractToDirBySymlink() throws IOException {
        Path tmp = Files.createTempDirectory(Paths.get("."), getClass().getName());
        Path symlink;
        try {
            symlink = Files.createSymbolicLink(Paths.get(".", "symlink"), tmp);
        } catch (IOException|UnsupportedOperationException e) {
            // symlinks are not supported
            // nothing to test
            return;
        }
        Set<Path> notJImageModules = Files.walk(tmp,1).collect(Collectors.toSet());
        jimage("extract", "--dir", symlink.toString(), getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.isEmpty(), "Output is not expected");
                });
        verifyExplodedImage(tmp, notJImageModules);
    }

    public void testExtractToReadOnlyDir() throws IOException {
        Path filePath = Files.createTempDirectory(Paths.get("."), getClass().getName());
        Set<String> supportedAttr = filePath.getFileSystem().supportedFileAttributeViews();
        if (supportedAttr.contains("posix")) {
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("r-xr--r--"));
        } else if (supportedAttr.contains("acl")) {
            System.out.println("Entered into acl block");
            UserPrincipal fileOwner = Files.getOwner(filePath);
            AclFileAttributeView view = Files.getFileAttributeView(filePath, AclFileAttributeView.class);
            AclEntry entry = AclEntry.newBuilder()
                                     .setType(AclEntryType.DENY)
                                     .setPrincipal(fileOwner)
                                     .setPermissions(AclEntryPermission.WRITE_DATA)
                                     .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                                     .build();
            List<AclEntry> acl = view.getAcl();
            acl.add(0, entry);
            view.setAcl(acl);
        }
        jimage("extract", "--dir", filePath.toString(), getImagePath())
                .assertFailure()
                .assertShowsError();
    }

    public void testExtractToNotEmptyDir() throws IOException {
        Path tmp = Files.createTempDirectory(Paths.get("."), getClass().getName());
        Files.createFile(Paths.get(tmp.toString(), ".not_empty"));
        jimage("extract", "--dir", tmp.toString(), getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.isEmpty(), "Output is not expected");
                });
    }

    public void testExtractToFile() throws IOException {
        Path tmp = Files.createTempFile(Paths.get("."), getClass().getName(), "not_a_dir");
        jimage("extract", "--dir", tmp.toString(), getImagePath())
                .assertFailure()
                .assertShowsError();
    }

    private void verifyExplodedImage(Path imagePath, Set<Path> notJImageModules) throws IOException {
        Set<Path> allModules = Files.walk(imagePath, 1).collect(Collectors.toSet());
        assertTrue(allModules.stream().anyMatch(p -> "java.base".equals(p.getFileName().toString())),
                "Exploded image contains java.base module.");
        Set<Path> badModules = allModules.stream()
                .filter(p -> !Files.exists(p.resolve("module-info.class")))
                .collect(Collectors.toSet());
        // filter bad modules which are not part of jimage
        badModules.removeAll(notJImageModules);
        assertEquals(badModules, new HashSet<Path>() {{}},
                "There are no exploded modules with missing 'module-info.class'");
    }

    public static void main(String[] args) throws Throwable {
        new JImageExtractTest().runTests();
    }
}

