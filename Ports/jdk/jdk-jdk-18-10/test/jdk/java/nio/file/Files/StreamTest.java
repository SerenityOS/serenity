/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8006884 8019526 8132539
 * @library ..
 * @build PassThroughFileSystem FaultyFileSystem
 * @run testng StreamTest
 * @summary Unit test for java.nio.file.Files methods that return a Stream
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test(groups = "unit")
public class StreamTest {
    /**
     * Default test folder
     * testFolder - empty
     *            - file
     *            - dir - d1
     *                  - f1
     *                  - lnDir2 (../dir2)
     *            - dir2
     *            - linkDir (./dir)
     *            - linkFile(./file)
     */
    static Path testFolder;
    static boolean supportsLinks;
    static Path[] level1;
    static Path[] all;
    static Path[] all_folowLinks;

    @BeforeClass
    void setupTestFolder() throws IOException {
        testFolder = TestUtil.createTemporaryDirectory();
        supportsLinks = TestUtil.supportsLinks(testFolder);
        TreeSet<Path> set = new TreeSet<>();

        // Level 1
        Path empty = testFolder.resolve("empty");
        Path file = testFolder.resolve("file");
        Path dir = testFolder.resolve("dir");
        Path dir2 = testFolder.resolve("dir2");
        Files.createDirectory(empty);
        Files.createFile(file);
        Files.createDirectory(dir);
        Files.createDirectory(dir2);
        set.add(empty);
        set.add(file);
        set.add(dir);
        set.add(dir2);
        if (supportsLinks) {
            Path tmp = testFolder.resolve("linkDir");
            Files.createSymbolicLink(tmp, dir);
            set.add(tmp);
            tmp = testFolder.resolve("linkFile");
            Files.createSymbolicLink(tmp, file);
            set.add(tmp);
        }
        level1 = set.toArray(new Path[0]);

        // Level 2
        Path tmp = dir.resolve("d1");
        Files.createDirectory(tmp);
        set.add(tmp);
        tmp = dir.resolve("f1");
        Files.createFile(tmp);
        set.add(tmp);
        if (supportsLinks) {
            tmp = dir.resolve("lnDir2");
            Files.createSymbolicLink(tmp, dir2);
            set.add(tmp);
        }
        // walk include starting folder
        set.add(testFolder);
        all = set.toArray(new Path[0]);

        // Follow links
        if (supportsLinks) {
            tmp = testFolder.resolve("linkDir");
            set.add(tmp.resolve("d1"));
            set.add(tmp.resolve("f1"));
            tmp = tmp.resolve("lnDir2");
            set.add(tmp);
        }
        all_folowLinks = set.toArray(new Path[0]);
    }

    @AfterClass
    void cleanupTestFolder() throws IOException {
        TestUtil.removeAll(testFolder);
    }

    public void testBasic() {
        try (Stream<Path> s = Files.list(testFolder)) {
            Object[] actual = s.sorted().toArray();
            assertEquals(actual, level1);
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }

        try (Stream<Path> s = Files.list(testFolder.resolve("empty"))) {
            int count = s.mapToInt(p -> 1).reduce(0, Integer::sum);
            assertEquals(count, 0, "Expect empty stream.");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    public void testWalk() {
        try (Stream<Path> s = Files.walk(testFolder)) {
            Object[] actual = s.sorted().toArray();
            assertEquals(actual, all);
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    public void testWalkOneLevel() {
        try (Stream<Path> s = Files.walk(testFolder, 1)) {
            Object[] actual = s.filter(path -> ! path.equals(testFolder))
                               .sorted()
                               .toArray();
            assertEquals(actual, level1);
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    public void testWalkFollowLink() {
        // If link is not supported, the directory structure won't have link.
        // We still want to test the behavior with FOLLOW_LINKS option.
        try (Stream<Path> s = Files.walk(testFolder, FileVisitOption.FOLLOW_LINKS)) {
            Object[] actual = s.sorted().toArray();
            assertEquals(actual, all_folowLinks);
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    private void validateFileSystemLoopException(Path start, Path... causes) {
        try (Stream<Path> s = Files.walk(start, FileVisitOption.FOLLOW_LINKS)) {
            try {
                int count = s.mapToInt(p -> 1).reduce(0, Integer::sum);
                fail("Should got FileSystemLoopException, but got " + count + "elements.");
            } catch (UncheckedIOException uioe) {
                IOException ioe = uioe.getCause();
                if (ioe instanceof FileSystemLoopException) {
                    FileSystemLoopException fsle = (FileSystemLoopException) ioe;
                    boolean match = false;
                    for (Path cause: causes) {
                        if (fsle.getFile().equals(cause.toString())) {
                            match = true;
                            break;
                        }
                    }
                    assertTrue(match);
                } else {
                    fail("Unexpected UncheckedIOException cause " + ioe.toString());
                }
            }
        } catch(IOException ex) {
            fail("Unexpected IOException " + ex);
        }
    }

    public void testWalkFollowLinkLoop() {
        if (!supportsLinks) {
            return;
        }

        // Loops.
        try {
            Path dir = testFolder.resolve("dir");
            Path linkdir = testFolder.resolve("linkDir");
            Path d1 = dir.resolve("d1");
            Path cause = d1.resolve("lnSelf");
            Files.createSymbolicLink(cause, d1);

            // loop in descendant.
            validateFileSystemLoopException(dir, cause);
            // loop in self
            validateFileSystemLoopException(d1, cause);
            // start from other place via link
            validateFileSystemLoopException(linkdir,
                    linkdir.resolve(Paths.get("d1", "lnSelf")));
            Files.delete(cause);

            // loop to parent.
            cause = d1.resolve("lnParent");
            Files.createSymbolicLink(cause, dir);

            // loop should be detected at test/dir/d1/lnParent/d1
            validateFileSystemLoopException(d1, cause.resolve("d1"));
            // loop should be detected at link
            validateFileSystemLoopException(dir, cause);
            // loop should be detected at test/linkdir/d1/lnParent
            // which is test/dir we have visited via test/linkdir
            validateFileSystemLoopException(linkdir,
                    linkdir.resolve(Paths.get("d1", "lnParent")));
            Files.delete(cause);

            // cross loop
            Path dir2 = testFolder.resolve("dir2");
            cause = dir2.resolve("lnDir");
            Files.createSymbolicLink(cause, dir);
            validateFileSystemLoopException(dir,
                    dir.resolve(Paths.get("lnDir2", "lnDir")));
            validateFileSystemLoopException(dir2,
                    dir2.resolve(Paths.get("lnDir", "lnDir2")));
            validateFileSystemLoopException(linkdir,
                    linkdir.resolve(Paths.get("lnDir2", "lnDir")));
        } catch(IOException ioe) {
            fail("Unexpected IOException " + ioe);
        }
    }

    private static class PathBiPredicate implements BiPredicate<Path, BasicFileAttributes> {
        private final BiPredicate<Path, BasicFileAttributes> pred;
        private final Set<Path> visited = new TreeSet<Path>();

        PathBiPredicate(BiPredicate<Path, BasicFileAttributes> pred) {
            this.pred = Objects.requireNonNull(pred);
        }

        public boolean test(Path path, BasicFileAttributes attrs) {
            visited.add(path);
            return pred.test(path, attrs);
        }

        public Path[] visited() {
            return visited.toArray(new Path[0]);
        }
    }

    public void testFind() throws IOException {
        PathBiPredicate pred = new PathBiPredicate((path, attrs) -> true);

        try (Stream<Path> s = Files.find(testFolder, Integer.MAX_VALUE, pred)) {
            Set<Path> result = s.collect(Collectors.toCollection(TreeSet::new));
            assertEquals(pred.visited(), all);
            assertEquals(result.toArray(new Path[0]), pred.visited());
        }

        pred = new PathBiPredicate((path, attrs) -> attrs.isSymbolicLink());
        try (Stream<Path> s = Files.find(testFolder, Integer.MAX_VALUE, pred)) {
            s.forEach(path -> assertTrue(Files.isSymbolicLink(path)));
            assertEquals(pred.visited(), all);
        }

        pred = new PathBiPredicate((path, attrs) ->
            path.getFileName().toString().startsWith("e"));
        try (Stream<Path> s = Files.find(testFolder, Integer.MAX_VALUE, pred)) {
            s.forEach(path -> assertEquals(path.getFileName().toString(), "empty"));
            assertEquals(pred.visited(), all);
        }

        pred = new PathBiPredicate((path, attrs) ->
            path.getFileName().toString().startsWith("l") && attrs.isRegularFile());
        try (Stream<Path> s = Files.find(testFolder, Integer.MAX_VALUE, pred)) {
            s.forEach(path -> fail("Expect empty stream"));
            assertEquals(pred.visited(), all);
        }
    }

    // Test borrowed from BytesAndLines
    public void testLines() throws IOException {
        final Charset US_ASCII = Charset.forName("US-ASCII");
        Path tmpfile = Files.createTempFile("blah", "txt");

        try {
            // zero lines
            assertTrue(Files.size(tmpfile) == 0, "File should be empty");
            try (Stream<String> s = Files.lines(tmpfile)) {
                checkLines(s, Collections.emptyList());
            }
            try (Stream<String> s = Files.lines(tmpfile, US_ASCII)) {
                checkLines(s, Collections.emptyList());
            }

            // one line
            List<String> oneLine = Arrays.asList("hi");
            Files.write(tmpfile, oneLine, US_ASCII);
            try (Stream<String> s = Files.lines(tmpfile)) {
                checkLines(s, oneLine);
            }
            try (Stream<String> s = Files.lines(tmpfile, US_ASCII)) {
                checkLines(s, oneLine);
            }

            // two lines using platform's line separator
            List<String> twoLines = Arrays.asList("hi", "there");
            Files.write(tmpfile, twoLines, US_ASCII);
            try (Stream<String> s = Files.lines(tmpfile)) {
                checkLines(s, twoLines);
            }
            try (Stream<String> s = Files.lines(tmpfile, US_ASCII)) {
                checkLines(s, twoLines);
            }

            // MalformedInputException
            byte[] bad = { (byte)0xff, (byte)0xff };
            Files.write(tmpfile, bad);
            try (Stream<String> s = Files.lines(tmpfile)) {
                checkMalformedInputException(s);
            }
            try (Stream<String> s = Files.lines(tmpfile, US_ASCII)) {
                checkMalformedInputException(s);
            }

            // NullPointerException
            checkNullPointerException(() -> Files.lines(null));
            checkNullPointerException(() -> Files.lines(null, US_ASCII));
            checkNullPointerException(() -> Files.lines(tmpfile, null));

        } finally {
            Files.delete(tmpfile);
        }
    }

    private void checkLines(Stream<String> s, List<String> expected) {
        List<String> lines = s.collect(Collectors.toList());
        assertTrue(lines.size() == expected.size(), "Unexpected number of lines");
        assertTrue(lines.equals(expected), "Unexpected content");
    }

    private void checkMalformedInputException(Stream<String> s) {
        try {
            List<String> lines = s.collect(Collectors.toList());
            fail("UncheckedIOException expected");
        } catch (UncheckedIOException ex) {
            IOException cause = ex.getCause();
            assertTrue(cause instanceof MalformedInputException,
                "MalformedInputException expected");
        }
    }

    private void checkNullPointerException(Callable<?> c) {
        try {
            c.call();
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        } catch (Exception e) {
            fail(e + " not expected");
        }
    }

    public void testDirectoryIteratorException() throws IOException {
        Path dir = testFolder.resolve("dir2");
        Path trigger = dir.resolve("DirectoryIteratorException");
        Files.createFile(trigger);
        FaultyFileSystem.FaultyFSProvider fsp = FaultyFileSystem.FaultyFSProvider.getInstance();
        FaultyFileSystem fs = (FaultyFileSystem) fsp.newFileSystem(dir, null);

        try {
            fsp.setFaultyMode(false);
            Path fakeRoot = fs.getRoot();
            try {
                try (Stream<Path> s = Files.list(fakeRoot)) {
                    s.forEach(path -> assertEquals(path.getFileName().toString(), "DirectoryIteratorException"));
                }
            } catch (UncheckedIOException uioe) {
                fail("Unexpected exception.");
            }

            fsp.setFaultyMode(true);
            try {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(fakeRoot)) {
                    Iterator<Path> itor = ds.iterator();
                    while (itor.hasNext()) {
                        itor.next();
                    }
                }
                fail("Shoule throw DirectoryIteratorException");
            } catch (DirectoryIteratorException die) {
            }

            try {
                try (Stream<Path> s = Files.list(fakeRoot)) {
                    s.forEach(path -> fail("should not get here"));
                }
            } catch (UncheckedIOException uioe) {
                assertTrue(uioe.getCause() instanceof FaultyFileSystem.FaultyException);
            } catch (DirectoryIteratorException die) {
                fail("Should have been converted into UncheckedIOException.");
            }
        } finally {
            // Cleanup
            if (fs != null) {
                fs.close();
            }
            Files.delete(trigger);
        }
    }

    public void testUncheckedIOException() throws IOException {
        Path triggerFile = testFolder.resolve(Paths.get("dir2", "IOException"));
        Files.createFile(triggerFile);
        Path triggerDir = testFolder.resolve(Paths.get("empty", "IOException"));
        Files.createDirectories(triggerDir);
        Files.createFile(triggerDir.resolve("file"));
        FaultyFileSystem.FaultyFSProvider fsp = FaultyFileSystem.FaultyFSProvider.getInstance();
        FaultyFileSystem fs = (FaultyFileSystem) fsp.newFileSystem(testFolder, null);

        try {
            fsp.setFaultyMode(false);
            Path fakeRoot = fs.getRoot();
            try (Stream<Path> s = Files.list(fakeRoot.resolve("dir2"))) {
                // only one file
                s.forEach(path -> assertEquals(path.getFileName().toString(), "IOException"));
            }

            try (Stream<Path> s = Files.walk(fakeRoot.resolve("empty"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                // ordered as depth-first
                assertEquals(result, new String[] { "empty", "IOException", "file"});
            }

            fsp.setFaultyMode(true);
            try (Stream<Path> s = Files.list(fakeRoot.resolve("dir2"))) {
                s.forEach(path -> fail("should have caused exception"));
            } catch (UncheckedIOException uioe) {
                assertTrue(uioe.getCause() instanceof FaultyFileSystem.FaultyException);
            }

            try (Stream<Path> s = Files.walk(fakeRoot.resolve("empty"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to IOException");
            } catch (UncheckedIOException uioe) {
                assertTrue(uioe.getCause() instanceof FaultyFileSystem.FaultyException);
            }

            try (Stream<Path> s = Files.walk(
                fakeRoot.resolve("empty").resolve("IOException")))
            {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to IOException");
            } catch (IOException ioe) {
                assertTrue(ioe instanceof FaultyFileSystem.FaultyException);
            } catch (UncheckedIOException ex) {
                fail("Top level should be repored as is");
            }
         } finally {
            // Cleanup
            if (fs != null) {
                fs.close();
            }
            Files.delete(triggerFile);
            TestUtil.removeAll(triggerDir);
        }
    }

    public void testSecurityException() throws IOException {
        Path empty = testFolder.resolve("empty");
        Path triggerFile = Files.createFile(empty.resolve("SecurityException"));
        Path sampleFile = Files.createDirectories(empty.resolve("sample"));

        Path dir2 = testFolder.resolve("dir2");
        Path triggerDir = Files.createDirectories(dir2.resolve("SecurityException"));
        Files.createFile(triggerDir.resolve("fileInSE"));
        Path sample = Files.createFile(dir2.resolve("file"));

        Path triggerLink = null;
        Path linkTriggerDir = null;
        Path linkTriggerFile = null;
        if (supportsLinks) {
            Path dir = testFolder.resolve("dir");
            triggerLink = Files.createSymbolicLink(dir.resolve("SecurityException"), empty);
            linkTriggerDir = Files.createSymbolicLink(dir.resolve("lnDirSE"), triggerDir);
            linkTriggerFile = Files.createSymbolicLink(dir.resolve("lnFileSE"), triggerFile);
        }

        FaultyFileSystem.FaultyFSProvider fsp = FaultyFileSystem.FaultyFSProvider.getInstance();
        FaultyFileSystem fs = (FaultyFileSystem) fsp.newFileSystem(testFolder, null);

        try {
            fsp.setFaultyMode(false);
            Path fakeRoot = fs.getRoot();
            // validate setting
            try (Stream<Path> s = Files.list(fakeRoot.resolve("empty"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "SecurityException", "sample" });
            }

            try (Stream<Path> s = Files.walk(fakeRoot.resolve("dir2"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "dir2", "SecurityException", "fileInSE", "file" });
            }

            if (supportsLinks) {
                try (Stream<Path> s = Files.list(fakeRoot.resolve("dir"))) {
                    String[] result = s.map(path -> path.getFileName().toString())
                                       .toArray(String[]::new);
                    assertEqualsNoOrder(result, new String[] { "d1", "f1", "lnDir2", "SecurityException", "lnDirSE", "lnFileSE" });
                }
            }

            // execute test
            fsp.setFaultyMode(true);
            // ignore file cause SecurityException
            try (Stream<Path> s = Files.walk(fakeRoot.resolve("empty"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "empty", "sample" });
            }
            // skip folder cause SecurityException
            try (Stream<Path> s = Files.walk(fakeRoot.resolve("dir2"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "dir2", "file" });
            }

            if (supportsLinks) {
                // not following links
                try (Stream<Path> s = Files.walk(fakeRoot.resolve("dir"))) {
                    String[] result = s.map(path -> path.getFileName().toString())
                                       .toArray(String[]::new);
                    assertEqualsNoOrder(result, new String[] { "dir", "d1", "f1", "lnDir2", "lnDirSE", "lnFileSE" });
                }

                // following links
                try (Stream<Path> s = Files.walk(fakeRoot.resolve("dir"), FileVisitOption.FOLLOW_LINKS)) {
                    String[] result = s.map(path -> path.getFileName().toString())
                                       .toArray(String[]::new);
                    // ?? Should fileInSE show up?
                    // With FaultyFS, it does as no exception thrown for link to "SecurityException" with read on "lnXxxSE"
                    assertEqualsNoOrder(result, new String[] { "dir", "d1", "f1", "lnDir2", "file", "lnDirSE", "lnFileSE", "fileInSE" });
                }
            }

            // list instead of walk
            try (Stream<Path> s = Files.list(fakeRoot.resolve("empty"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "sample" });
            }
            try (Stream<Path> s = Files.list(fakeRoot.resolve("dir2"))) {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                assertEqualsNoOrder(result, new String[] { "file" });
            }

            // root cause SecurityException should be reported
            try (Stream<Path> s = Files.walk(
                fakeRoot.resolve("dir2").resolve("SecurityException")))
            {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to SecurityException");
            } catch (SecurityException se) {
                assertTrue(se.getCause() instanceof FaultyFileSystem.FaultyException);
            }

            // Walk a file cause SecurityException, we should get SE
            try (Stream<Path> s = Files.walk(
                fakeRoot.resolve("dir").resolve("SecurityException")))
            {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to SecurityException");
            } catch (SecurityException se) {
                assertTrue(se.getCause() instanceof FaultyFileSystem.FaultyException);
            }

            // List a file cause SecurityException, we should get SE as cannot read attribute
            try (Stream<Path> s = Files.list(
                fakeRoot.resolve("dir2").resolve("SecurityException")))
            {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to SecurityException");
            } catch (SecurityException se) {
                assertTrue(se.getCause() instanceof FaultyFileSystem.FaultyException);
            }

            try (Stream<Path> s = Files.list(
                fakeRoot.resolve("dir").resolve("SecurityException")))
            {
                String[] result = s.map(path -> path.getFileName().toString())
                                   .toArray(String[]::new);
                fail("should not reach here due to SecurityException");
            } catch (SecurityException se) {
                assertTrue(se.getCause() instanceof FaultyFileSystem.FaultyException);
            }
         } finally {
            // Cleanup
            if (fs != null) {
                fs.close();
            }
            if (supportsLinks) {
                Files.delete(triggerLink);
                Files.delete(linkTriggerDir);
                Files.delete(linkTriggerFile);
            }
            Files.delete(triggerFile);
            Files.delete(sampleFile);
            Files.delete(sample);
            TestUtil.removeAll(triggerDir);
        }
    }

    public void testConstructException() {
        try (Stream<String> s = Files.lines(testFolder.resolve("notExist"), Charset.forName("UTF-8"))) {
            s.forEach(l -> fail("File is not even exist!"));
        } catch (IOException ioe) {
            assertTrue(ioe instanceof NoSuchFileException);
        }
    }

    public void testClosedStream() throws IOException {
        try (Stream<Path> s = Files.list(testFolder)) {
            s.close();
            Object[] actual = s.sorted().toArray();
            fail("Operate on closed stream should throw IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }

        try (Stream<Path> s = Files.walk(testFolder)) {
            s.close();
            Object[] actual = s.sorted().toArray();
            fail("Operate on closed stream should throw IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }

        try (Stream<Path> s = Files.find(testFolder, Integer.MAX_VALUE,
                    (p, attr) -> true)) {
            s.close();
            Object[] actual = s.sorted().toArray();
            fail("Operate on closed stream should throw IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testProcFile() throws IOException {
        if (System.getProperty("os.name").equals("Linux")) {
            Path path = Paths.get("/proc/cpuinfo");
            if (Files.exists(path)) {
                String NEW_LINE = System.getProperty("line.separator");
                String s =
                    Files.lines(path).collect(Collectors.joining(NEW_LINE));
                if (s.length() == 0) {
                    fail("Files.lines(\"" + path + "\") returns no data");
                }
            }
        }
    }
}
