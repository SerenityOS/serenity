/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;

import jdk.tools.jlink.internal.Archive.Entry.EntryType;

/**
 * An Archive backed by a jar file.
 */
public class ModularJarArchive extends JarArchive {

    private static final String JAR_EXT = ".jar";

    public ModularJarArchive(String mn, Path jmod, Runtime.Version version) {
        super(mn, jmod, version);
        String filename = Objects.requireNonNull(jmod.getFileName()).toString();
        if (!filename.endsWith(JAR_EXT)) {
            throw new UnsupportedOperationException("Unsupported format: " + filename);
        }
    }

    @Override
    EntryType toEntryType(String section) {
        return EntryType.CLASS_OR_RESOURCE;
    }

    @Override
    Entry toEntry(ZipEntry ze) {
        String name = ze.getName();
        EntryType type = toEntryType(name);
        return new JarEntry(ze.getName(), getFileName(name), type, getJarFile(), ze);
    }

    @Override
    String getFileName(String entryName) {
        return entryName;
    }
}
