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

package jdk.jfr.internal.consumer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

// Protected by modular boundaries.
public abstract class FileAccess {
    public static final FileAccess UNPRIVILEGED = new UnPrivileged();

    public abstract RandomAccessFile openRAF(File f, String mode) throws IOException;

    public abstract DirectoryStream<Path> newDirectoryStream(Path repository) throws IOException;

    public abstract String getAbsolutePath(File f) throws IOException;

    public abstract long length(File f) throws IOException;

    public abstract long fileSize(Path p) throws IOException;

    public abstract boolean exists(Path s) throws IOException;

    public abstract boolean isDirectory(Path p);

    public abstract FileTime getLastModified(Path p) throws IOException;

    private static class UnPrivileged extends FileAccess {
        @Override
        public RandomAccessFile openRAF(File f, String mode) throws IOException {
            return new RandomAccessFile(f, mode);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
            return Files.newDirectoryStream(dir);
        }

        @Override
        public String getAbsolutePath(File f) throws IOException {
            return f.getAbsolutePath();
        }

        @Override
        public long length(File f) throws IOException {
            return f.length();
        }

        @Override
        public long fileSize(Path p) throws IOException {
            return Files.size(p);
        }

        @Override
        public boolean exists(Path p) {
            return Files.exists(p);
        }

        @Override
        public boolean isDirectory(Path p) {
            return Files.isDirectory(p);
        }

        @Override
        public FileTime getLastModified(Path p) throws IOException {
            return Files.getLastModifiedTime(p);
        }
    }
}
