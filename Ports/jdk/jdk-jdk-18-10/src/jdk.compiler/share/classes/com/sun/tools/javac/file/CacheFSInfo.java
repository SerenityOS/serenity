/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

/**
 * Caching implementation of FSInfo.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class CacheFSInfo extends FSInfo {

    protected final ConcurrentHashMap<Path, Path> canonicalPathCache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Path, Optional<BasicFileAttributes>> attributeCache =
            new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Path, List<Path>> jarClassPathCache =
            new ConcurrentHashMap<>();

    /**
     * Register a Context.Factory to create a CacheFSInfo.
     */
    public static void preRegister(Context context) {
        context.put(FSInfo.class, (Factory<FSInfo>)c -> {
                FSInfo instance = new CacheFSInfo();
                c.put(FSInfo.class, instance);
                return instance;
            });
    }

    public void clearCache() {
        canonicalPathCache.clear();
        attributeCache.clear();
        jarClassPathCache.clear();
    }

    @Override
    public Path getCanonicalFile(Path file) {
        return canonicalPathCache.computeIfAbsent(file, super::getCanonicalFile);
    }

    @Override
    public boolean exists(Path file) {
        return getAttributes(file).isPresent();
    }

    @Override
    public boolean isDirectory(Path file) {
        return getAttributes(file).map(BasicFileAttributes::isDirectory).orElse(false);
    }

    @Override
    public boolean isFile(Path file) {
        return getAttributes(file).map(BasicFileAttributes::isRegularFile).orElse(false);
    }

    @Override
    public List<Path> getJarClassPath(Path file) throws IOException {
        synchronized (jarClassPathCache) {
            List<Path> jarClassPath = jarClassPathCache.get(file);
            if (jarClassPath == null) {
                jarClassPath = super.getJarClassPath(file);
                jarClassPathCache.put(file, jarClassPath);
            }
            return jarClassPath;
        }
    }

    protected Optional<BasicFileAttributes> getAttributes(Path file) {
        return attributeCache.computeIfAbsent(file, this::maybeReadAttributes);
    }

    protected Optional<BasicFileAttributes> maybeReadAttributes(Path file) {
        try {
            return Optional.of(Files.readAttributes(file, BasicFileAttributes.class));
        } catch (IOException e) {
            // Ignore; means file not found
            return Optional.empty();
        }
    }
}
