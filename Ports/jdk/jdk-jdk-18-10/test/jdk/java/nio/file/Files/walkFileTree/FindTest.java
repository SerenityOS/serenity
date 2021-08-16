/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6907737
 * @summary Tests that walkFileTree is consistent with the native find program
 * @requires (os.family != "windows")
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        CreateFileTree
 * @run testng/othervm -Djava.io.tmpdir=. FindTest
 */

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FindTest {

    private static final Random rand = new Random();
    private static final boolean isAIX = System.getProperty("os.name").equals("AIX");
    private static Path top;
    private static String TOP;

    @BeforeClass
    public static void createFileTree() throws Exception {
        top = CreateFileTree.create();
        TOP = top.toAbsolutePath().toString();
    }

    @Test
    public void printTreeTest() throws Throwable {
        // print the file tree and compare output with find(1)
        assertOutputEquals(printFileTree(top), runFind("find", TOP));
    }

    @Test
    public void printTreeFollowLinkTest() throws Throwable {
        // print the file tree, following links, and compare output with find(1).

        // On AIX "find -follow" may core dump on recursive links without '-L'
        // see: http://www-01.ibm.com/support/docview.wss?uid=isg1IV28143
        String[] cmds = isAIX
                      ? new String[]{"find", "-L", TOP, "-follow"}
                      : new String[]{"find", TOP, "-follow"};
        OutputAnalyzer expected = runFind(cmds);

        // Some versions of find(1) output cycles (sym links to ancestor
        // directories), other versions do not. For that reason we run
        // PrintFileTree with the -printCycles option when the output without
        // this option differs to find(1).
        try {
            assertOutputEquals(printFileTree(top, "-follow"), expected);
        } catch (AssertionError x) {
            assertOutputEquals(printFileTree(top, "-follow", "-printCycles"), expected);
        }
    }

    private void assertOutputEquals(List<String> actual, OutputAnalyzer expected)
            throws IOException {
        List<String> expectedList = Arrays.asList(expected.getStdout()
                                          .split(System.lineSeparator()));
        assertEquals(actual.size(), expectedList.size());
        assertTrue(actual.removeAll(expectedList));
    }

    private OutputAnalyzer runFind(String... cmds) throws Throwable {
        return ProcessTools.executeCommand(cmds);
    }

    /**
     * Invokes Files.walkFileTree to traverse a file tree and prints
     * each of the directories and files. The -follow option causes symbolic
     * links to be followed and the -printCycles option will print links
     * where the target of the link is an ancestor directory.
     */
    private static List<String> printFileTree(Path dir, String... opts) throws Exception {
        List<Path> fileTreeList = new ArrayList<>();

        List<String> optsList = Arrays.asList(opts);
        boolean followLinks = optsList.contains("-follow");
        boolean reportCycles = optsList.contains("-printCycles");

        Set<FileVisitOption> options = new HashSet<>();
        if (followLinks)
            options.add(FileVisitOption.FOLLOW_LINKS);

        Files.walkFileTree(dir, options, Integer.MAX_VALUE, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                fileTreeList.add(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                fileTreeList.add(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException
            {
                if (exc != null)
                    throw exc;
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                throws IOException
            {
                if (followLinks && (exc instanceof FileSystemLoopException)) {
                    if (reportCycles)
                        fileTreeList.add(file);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });

        return fileTreeList.stream()
                           .map(f -> f.toAbsolutePath().toString())
                           .collect(Collectors.toCollection(ArrayList::new));
    }
}
