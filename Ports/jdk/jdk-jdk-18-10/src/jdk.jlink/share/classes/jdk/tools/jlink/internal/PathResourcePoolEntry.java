/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A ResourcePoolEntry backed by a given nio Path.
 */
public class PathResourcePoolEntry extends AbstractResourcePoolEntry {
    private final Path file;

    /**
     * Create a new PathResourcePoolEntry.
     *
     * @param module The module name.
     * @param path The path for the resource content.
     * @param type The data type.
     * @param file The data file identifier.
     */
    public PathResourcePoolEntry(String module, String path, Type type, Path file) {
        super(module, path, type);
        this.file = Objects.requireNonNull(file);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(file + " not a file");
        }
    }

    @Override
    public final InputStream content() {
        try {
            return Files.newInputStream(file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public final long contentLength() {
        try {
            return Files.size(file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
