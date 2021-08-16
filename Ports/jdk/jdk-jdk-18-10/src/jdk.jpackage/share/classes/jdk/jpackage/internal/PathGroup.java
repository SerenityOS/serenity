/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Group of paths.
 * Each path in the group is assigned a unique id.
 */
final class PathGroup {
    PathGroup(Map<Object, Path> paths) {
        entries = new HashMap<>(paths);
    }

    Path getPath(Object id) {
        if (id == null) {
            throw new NullPointerException();
        }
        return entries.get(id);
    }

    void setPath(Object id, Path path) {
        if (path != null) {
            entries.put(id, path);
        } else {
            entries.remove(id);
        }
    }

    /**
     * All configured entries.
     */
    List<Path> paths() {
        return entries.values().stream().toList();
    }

    /**
     * Root entries.
     */
    List<Path> roots() {
        // Sort by the number of path components in ascending order.
        List<Map.Entry<Path, Path>> sorted = normalizedPaths().stream().sorted(
                (a, b) -> a.getKey().getNameCount() - b.getKey().getNameCount()).toList();

        // Returns `true` if `a` is a parent of `b`
        BiFunction<Map.Entry<Path, Path>, Map.Entry<Path, Path>, Boolean> isParentOrSelf = (a, b) -> {
            return a == b || b.getKey().startsWith(a.getKey());
        };

        return sorted.stream().filter(
                v -> v == sorted.stream().sequential().filter(
                        v2 -> isParentOrSelf.apply(v2, v)).findFirst().get()).map(
                        v -> v.getValue()).toList();
    }

    long sizeInBytes() throws IOException {
        long reply = 0;
        for (Path dir : roots().stream().filter(f -> Files.isDirectory(f)).collect(
                Collectors.toList())) {
            try (Stream<Path> stream = Files.walk(dir)) {
                reply += stream.filter(p -> Files.isRegularFile(p)).mapToLong(
                        f -> f.toFile().length()).sum();
            }
        }
        return reply;
    }

    PathGroup resolveAt(Path root) {
        return new PathGroup(entries.entrySet().stream().collect(
                Collectors.toMap(e -> e.getKey(),
                        e -> root.resolve(e.getValue()))));
    }

    void copy(PathGroup dst) throws IOException {
        copy(this, dst, null, false);
    }

    void move(PathGroup dst) throws IOException {
        copy(this, dst, null, true);
    }

    void transform(PathGroup dst, TransformHandler handler) throws IOException {
        copy(this, dst, handler, false);
    }

    static interface Facade<T> {
        PathGroup pathGroup();

        default Collection<Path> paths() {
            return pathGroup().paths();
        }

        default List<Path> roots() {
            return pathGroup().roots();
        }

        default long sizeInBytes() throws IOException {
            return pathGroup().sizeInBytes();
        }

        T resolveAt(Path root);

        default void copy(Facade<T> dst) throws IOException {
            pathGroup().copy(dst.pathGroup());
        }

        default void move(Facade<T> dst) throws IOException {
            pathGroup().move(dst.pathGroup());
        }

        default void transform(Facade<T> dst, TransformHandler handler) throws
                IOException {
            pathGroup().transform(dst.pathGroup(), handler);
        }
    }

    static interface TransformHandler {
        public void copyFile(Path src, Path dst) throws IOException;
        public void createDirectory(Path dir) throws IOException;
    }

    private static void copy(PathGroup src, PathGroup dst,
            TransformHandler handler, boolean move) throws IOException {
        List<Map.Entry<Path, Path>> copyItems = new ArrayList<>();
        List<Path> excludeItems = new ArrayList<>();

        for (var id: src.entries.keySet()) {
            Path srcPath = src.entries.get(id);
            if (dst.entries.containsKey(id)) {
                copyItems.add(Map.entry(srcPath, dst.entries.get(id)));
            } else {
                excludeItems.add(srcPath);
            }
        }

        copy(move, copyItems, excludeItems, handler);
    }

    private static void copy(boolean move, List<Map.Entry<Path, Path>> entries,
            List<Path> excludePaths, TransformHandler handler) throws
            IOException {

        if (handler == null) {
            handler = new TransformHandler() {
                @Override
                public void copyFile(Path src, Path dst) throws IOException {
                    Files.createDirectories(IOUtils.getParent(dst));
                    if (move) {
                        Files.move(src, dst);
                    } else {
                        Files.copy(src, dst);
                    }
                }

                @Override
                public void createDirectory(Path dir) throws IOException {
                    Files.createDirectories(dir);
                }
            };
        }

        // destination -> source file mapping
        Map<Path, Path> actions = new HashMap<>();
        for (var action: entries) {
            Path src = action.getKey();
            Path dst = action.getValue();
            if (Files.isDirectory(src)) {
               try (Stream<Path> stream = Files.walk(src)) {
                   stream.sequential().forEach(path -> actions.put(dst.resolve(
                            src.relativize(path)).normalize(), path));
               }
            } else {
                actions.put(dst.normalize(), src);
            }
        }

        for (var action : actions.entrySet()) {
            Path dst = action.getKey();
            Path src = action.getValue();

            if (excludePaths.stream().anyMatch(src::startsWith)) {
                continue;
            }

            if (src.equals(dst) || !src.toFile().exists()) {
                continue;
            }

            if (Files.isDirectory(src)) {
                handler.createDirectory(dst);
            } else {
                handler.copyFile(src, dst);
            }
        }

        if (move) {
            // Delete source dirs.
            for (var entry: entries) {
                Path srcFile = entry.getKey();
                if (Files.isDirectory(srcFile)) {
                    IOUtils.deleteRecursive(srcFile);
                }
            }
        }
    }

    private static Map.Entry<Path, Path> normalizedPath(Path v) {
        final Path normalized;
        if (!v.isAbsolute()) {
            normalized = Path.of("./").resolve(v.normalize());
        } else {
            normalized = v.normalize();
        }

        return Map.entry(normalized, v);
    }

    private List<Map.Entry<Path, Path>> normalizedPaths() {
        return entries.values().stream().map(PathGroup::normalizedPath).collect(
                Collectors.toList());
    }

    private final Map<Object, Path> entries;
}
