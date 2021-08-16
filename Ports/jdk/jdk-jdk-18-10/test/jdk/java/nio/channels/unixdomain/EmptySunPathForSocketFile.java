/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8256461
 * @modules java.base/sun.nio.fs
 * @run testng EmptySunPathForSocketFile
 */

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import sun.nio.fs.AbstractFileSystemProvider;

import static org.testng.Assert.assertEquals;

/**
 * Check that AbstractFileSystemProvider.getSunPathForSocketFile with
 * an empty path returns an empty byte[]
 */
public class EmptySunPathForSocketFile {
    public static void main(String[] args) throws Exception {
        Path path = Path.of("");
        FileSystemProvider provider = FileSystems.getDefault().provider();
        byte[] bb = ((AbstractFileSystemProvider) provider).getSunPathForSocketFile(path);
        assertEquals(bb, new byte[0]);
    }
}
