/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class PathGroupTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void testNullId() {
         new PathGroup(Map.of()).getPath(null);
    }

    @Test
    public void testEmptyPathGroup() {
        PathGroup pg = new PathGroup(Map.of());

        assertNull(pg.getPath("foo"));

        assertEquals(0, pg.paths().size());
        assertEquals(0, pg.roots().size());
    }

    @Test
    public void testRootsSinglePath() {
        final PathGroup pg = new PathGroup(Map.of("main", PATH_FOO));

        List<Path> paths = pg.paths();
        assertEquals(1, paths.size());
        assertEquals(PATH_FOO, paths.iterator().next());

        List<Path> roots = pg.roots();
        assertEquals(1, roots.size());
        assertEquals(PATH_FOO, roots.iterator().next());
    }

    @Test
    public void testDuplicatedRoots() {
        final PathGroup pg = new PathGroup(Map.of("main", PATH_FOO, "another",
                PATH_FOO, "root", PATH_EMPTY));

        List<Path> paths = pg.paths();
        paths = paths.stream().sorted().toList();

        assertEquals(3, paths.size());
        assertEquals(PATH_EMPTY, paths.get(0));
        assertEquals(PATH_FOO, paths.get(1));
        assertEquals(PATH_FOO, paths.get(2));

        List<Path> roots = pg.roots();
        assertEquals(1, roots.size());
        assertEquals(PATH_EMPTY, roots.get(0));
    }

    @Test
    public void testRoots() {
        final PathGroup pg = new PathGroup(Map.of(1, Path.of("foo"), 2, Path.of(
                "foo", "bar"), 3, Path.of("foo", "bar", "buz")));

        List<Path> paths = pg.paths();
        assertEquals(3, paths.size());
        assertTrue(paths.contains(Path.of("foo")));
        assertTrue(paths.contains(Path.of("foo", "bar")));
        assertTrue(paths.contains(Path.of("foo", "bar", "buz")));

        List<Path> roots = pg.roots();
        assertEquals(1, roots.size());
        assertEquals(Path.of("foo"), roots.get(0));
    }

    @Test
    public void testResolveAt() {
        final PathGroup pg = new PathGroup(Map.of(0, PATH_FOO, 1, PATH_BAR, 2,
                PATH_EMPTY));

        final Path aPath = Path.of("a");

        final PathGroup pg2 = pg.resolveAt(aPath);
        assertThat(pg, not(equalTo(pg2)));

        List<Path> paths = pg.paths();
        assertEquals(3, paths.size());
        assertTrue(paths.contains(PATH_EMPTY));
        assertTrue(paths.contains(PATH_FOO));
        assertTrue(paths.contains(PATH_BAR));
        assertEquals(PATH_EMPTY, pg.roots().get(0));

        paths = pg2.paths();
        assertEquals(3, paths.size());
        assertTrue(paths.contains(aPath.resolve(PATH_EMPTY)));
        assertTrue(paths.contains(aPath.resolve(PATH_FOO)));
        assertTrue(paths.contains(aPath.resolve(PATH_BAR)));
        assertEquals(aPath, pg2.roots().get(0));
    }

    @Test
    public void testTransform() throws IOException {
        for (var transform : TransformType.values()) {
            testTransform(false, transform);
        }
    }

    @Test
    public void testTransformWithExcludes() throws IOException {
        for (var transform : TransformType.values()) {
            testTransform(true, transform);
        }
    }

    enum TransformType { Copy, Move, Handler };

    private void testTransform(boolean withExcludes, TransformType transform)
            throws IOException {
        final PathGroup pg = new PathGroup(Map.of(0, PATH_FOO, 1, PATH_BAR, 2,
                PATH_EMPTY, 3, PATH_BAZ));

        final Path srcDir = tempFolder.newFolder().toPath();
        final Path dstDir = tempFolder.newFolder().toPath();

        Files.createDirectories(srcDir.resolve(PATH_FOO).resolve("a/b/c/d"));
        Files.createFile(srcDir.resolve(PATH_FOO).resolve("a/b/c/file1"));
        Files.createFile(srcDir.resolve(PATH_FOO).resolve("a/b/file2"));
        Files.createFile(srcDir.resolve(PATH_FOO).resolve("a/b/file3"));
        Files.createFile(srcDir.resolve(PATH_BAR));
        Files.createFile(srcDir.resolve(PATH_EMPTY).resolve("file4"));
        Files.createDirectories(srcDir.resolve(PATH_BAZ).resolve("1/2/3"));

        var dst = pg.resolveAt(dstDir);
        var src = pg.resolveAt(srcDir);
        if (withExcludes) {
            // Exclude from transformation.
            src.setPath(new Object(), srcDir.resolve(PATH_FOO).resolve("a/b/c"));
            src.setPath(new Object(), srcDir.resolve(PATH_EMPTY).resolve("file4"));
        }

        var srcFilesBeforeTransform = walkFiles(srcDir);

        if (transform == TransformType.Handler) {
            List<Map.Entry<Path, Path>> copyFile = new ArrayList<>();
            List<Path> createDirectory = new ArrayList<>();
            src.transform(dst, new PathGroup.TransformHandler() {
                @Override
                public void copyFile(Path src, Path dst) throws IOException {
                    copyFile.add(Map.entry(src, dst));
                }

                @Override
                public void createDirectory(Path dir) throws IOException {
                    createDirectory.add(dir);
                }
            });

            Consumer<Path> assertFile = path -> {
                var entry = Map.entry(srcDir.resolve(path), dstDir.resolve(path));
                assertTrue(copyFile.contains(entry));
            };

            Consumer<Path> assertDir = path -> {
                assertTrue(createDirectory.contains(dstDir.resolve(path)));
            };

            assertEquals(withExcludes ? 3 : 5, copyFile.size());
            assertEquals(withExcludes ? 8 : 10, createDirectory.size());

            assertFile.accept(PATH_FOO.resolve("a/b/file2"));
            assertFile.accept(PATH_FOO.resolve("a/b/file3"));
            assertFile.accept(PATH_BAR);
            assertDir.accept(PATH_FOO.resolve("a/b"));
            assertDir.accept(PATH_FOO.resolve("a"));
            assertDir.accept(PATH_FOO);
            assertDir.accept(PATH_BAZ);
            assertDir.accept(PATH_BAZ.resolve("1"));
            assertDir.accept(PATH_BAZ.resolve("1/2"));
            assertDir.accept(PATH_BAZ.resolve("1/2/3"));
            assertDir.accept(PATH_EMPTY);

            if (!withExcludes) {
                assertFile.accept(PATH_FOO.resolve("a/b/c/file1"));
                assertFile.accept(PATH_EMPTY.resolve("file4"));
                assertDir.accept(PATH_FOO.resolve("a/b/c/d"));
                assertDir.accept(PATH_FOO.resolve("a/b/c"));
            }

            assertArrayEquals(new Path[] { Path.of("") }, walkFiles(dstDir));
            return;
        }

        if (transform == TransformType.Copy) {
            src.copy(dst);
        } else if (transform == TransformType.Move) {
            src.move(dst);
        }

        final List<Path> excludedPaths;
        if (withExcludes) {
            excludedPaths = List.of(
                PATH_EMPTY.resolve("file4"),
                PATH_FOO.resolve("a/b/c")
            );
        } else {
            excludedPaths = Collections.emptyList();
        }
        UnaryOperator<Path[]> removeExcludes = paths -> {
            return Stream.of(paths)
                    .filter(path -> !excludedPaths.stream().anyMatch(
                            path::startsWith))
                    .collect(Collectors.toList()).toArray(Path[]::new);
        };

        var dstFiles = walkFiles(dstDir);
        assertArrayEquals(removeExcludes.apply(srcFilesBeforeTransform), dstFiles);

        if (transform == TransformType.Copy) {
            assertArrayEquals(dstFiles, removeExcludes.apply(walkFiles(srcDir)));
        } else if (transform == TransformType.Move) {
            assertFalse(Files.exists(srcDir));
        }
    }

    private static Path[] walkFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.map(root::relativize).sorted().collect(
                    Collectors.toList()).toArray(Path[]::new);
        }
    }

    private final static Path PATH_FOO = Path.of("foo");
    private final static Path PATH_BAR = Path.of("bar");
    private final static Path PATH_BAZ = Path.of("baz");
    private final static Path PATH_EMPTY = Path.of("");
}
