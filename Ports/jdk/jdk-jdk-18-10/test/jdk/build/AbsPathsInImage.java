/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * @test
 * @bug 8226346
 * @summary Check all output files for absolute path fragments
 * @requires !vm.debug
 * @run main AbsPathsInImage
 */
public class AbsPathsInImage {

    // Set this property on command line to scan an alternate dir or file:
    // JTREG=JAVA_OPTIONS=-Djdk.test.build.AbsPathInImage.dir=/path/to/dir
    public static final String DIR_PROPERTY = "jdk.test.build.AbsPathsInImage.dir";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    private boolean matchFound = false;

    public static void main(String[] args) throws Exception {
        String jdkPathString = System.getProperty("test.jdk");
        Path jdkHome = Paths.get(jdkPathString);

        Path dirToScan = jdkHome;
        String overrideDir = System.getProperty(DIR_PROPERTY);
        if (overrideDir != null) {
            dirToScan = Paths.get(overrideDir);
        }

        String buildWorkspaceRoot = null;
        String buildOutputRoot = null;
        String testImageDirString = System.getenv("TEST_IMAGE_DIR");
        if (testImageDirString != null) {
            Path testImageDir = Paths.get(testImageDirString);
            Path buildInfoPropertiesFile = testImageDir.resolve("build-info.properties");
            System.out.println("Getting patterns from " + buildInfoPropertiesFile.toString());
            Properties buildInfoProperties = new Properties();
            try (InputStream inStream = Files.newInputStream(buildInfoPropertiesFile)) {
                buildInfoProperties.load(inStream);
            }
            buildWorkspaceRoot = buildInfoProperties.getProperty("build.workspace.root");
            buildOutputRoot = buildInfoProperties.getProperty("build.output.root");
        } else {
            System.out.println("Getting patterns from local environment");
            // Try to resolve the workspace root based on the jtreg test root dir
            String testRootDirString = System.getProperty("test.root");
            if (testRootDirString != null) {
                Path testRootDir = Paths.get(testRootDirString);
                // Remove /test/jdk suffix
                buildWorkspaceRoot = testRootDir.getParent().getParent().toString();
            }
            // Remove /jdk
            Path buildOutputRootPath = jdkHome.getParent();
            if (buildOutputRootPath.endsWith("images")) {
                buildOutputRootPath = buildOutputRootPath.getParent();
            }
            buildOutputRoot = buildOutputRootPath.toString();
        }
        if (buildWorkspaceRoot == null) {
            throw new Error("Could not find workspace root, test cannot run");
        }
        if (buildOutputRoot == null) {
            throw new Error("Could not find build output root, test cannot run");
        }

        List<byte[]> searchPatterns = new ArrayList<>();
        expandPatterns(searchPatterns, buildWorkspaceRoot);
        expandPatterns(searchPatterns, buildOutputRoot);

        System.out.println("Looking for:");
        for (byte[] searchPattern : searchPatterns) {
            System.out.println(new String(searchPattern));
        }
        System.out.println();

        AbsPathsInImage absPathsInImage = new AbsPathsInImage();
        absPathsInImage.scanFiles(dirToScan, searchPatterns);

        if (absPathsInImage.matchFound) {
            throw new Exception("Test failed");
        }
    }

    /**
     * Add path pattern to list of patterns to search for. Create all possible
     * variants depending on platform.
     */
    private static void expandPatterns(List<byte[]> searchPatterns, String pattern) {
        if (IS_WINDOWS) {
            String forward = pattern.replace('\\', '/');
            String back = pattern.replace('/', '\\');
            if (pattern.charAt(1) == ':') {
                String forwardUpper = String.valueOf(pattern.charAt(0)).toUpperCase() + forward.substring(1);
                String forwardLower = String.valueOf(pattern.charAt(0)).toLowerCase() + forward.substring(1);
                String backUpper = String.valueOf(pattern.charAt(0)).toUpperCase() + back.substring(1);
                String backLower = String.valueOf(pattern.charAt(0)).toLowerCase() + back.substring(1);
                searchPatterns.add(forwardUpper.getBytes());
                searchPatterns.add(forwardLower.getBytes());
                searchPatterns.add(backUpper.getBytes());
                searchPatterns.add(backLower.getBytes());
            } else {
                searchPatterns.add(forward.getBytes());
                searchPatterns.add(back.getBytes());
            }
        } else {
            searchPatterns.add(pattern.getBytes());
        }
    }

    private void scanFiles(Path root, List<byte[]> searchPatterns) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.toString();
                if (Files.isSymbolicLink(file)) {
                    return super.visitFile(file, attrs);
                } else if (fileName.endsWith(".debuginfo") || fileName.endsWith(".pdb")) {
                    // Do nothing
                } else if (fileName.endsWith(".zip")) {
                    scanZipFile(file, searchPatterns);
                } else {
                    scanFile(file, searchPatterns);
                }
                return super.visitFile(file, attrs);
            }
        });
    }

    private void scanFile(Path file, List<byte[]> searchPatterns) throws IOException {
        List<String> matches = scanBytes(Files.readAllBytes(file), searchPatterns);
        if (matches.size() > 0) {
            matchFound = true;
            System.out.println(file + ":");
            for (String match : matches) {
                System.out.println(match);
            }
            System.out.println();
        }
    }

    private void scanZipFile(Path zipFile, List<byte[]> searchPatterns) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                List<String> matches = scanBytes(zipInputStream.readAllBytes(), searchPatterns);
                if (matches.size() > 0) {
                    matchFound = true;
                    System.out.println(zipFile + ", " + zipEntry.getName() + ":");
                    for (String match : matches) {
                        System.out.println(match);
                    }
                    System.out.println();
                }
            }
        }
    }

    private List<String> scanBytes(byte[] data, List<byte[]> searchPatterns) {
        List<String> matches = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            for (byte[] searchPattern : searchPatterns) {
                boolean found = true;
                for (int j = 0; j < searchPattern.length; j++) {
                    if ((i + j >= data.length || data[i + j] != searchPattern[j])) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    matches.add(new String(data, charsStart(data, i), charsOffset(data, i, searchPattern.length)));
                    // No need to search the same string for multiple patterns
                    break;
                }
            }
        }
        return matches;
    }

    private int charsStart(byte[] data, int startIndex) {
        int index = startIndex;
        while (--index > 0) {
            byte datum = data[index];
            if (datum < 32 || datum > 126) {
                break;
            }
        }
        return index + 1;
    }

    private int charsOffset(byte[] data, int startIndex, int startOffset) {
        int offset = startOffset;
        while (startIndex + ++offset < data.length) {
            byte datum = data[startIndex + offset];
            if (datum < 32 || datum > 126) {
                break;
            }
        }
        return offset;
    }
}
