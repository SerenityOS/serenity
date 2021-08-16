/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.hotspot.tools.ctw;

import jdk.internal.jimage.ImageLocation;
import jdk.internal.jimage.ImageReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Handler for jimage-files containing classes to compile.
 */
public class ClassPathJimageEntry extends PathHandler.PathEntry {

    @Override
    protected Stream<String> classes() {
        return Arrays.stream(reader.getEntryNames())
                     .filter(name -> name.endsWith(".class"))
                     .filter(name -> !name.endsWith("module-info.class"))
                     .map(ClassPathJimageEntry::toFileName)
                     .map(Utils::fileNameToClassName);
    }

    private static String toFileName(String name) {
        final char nameSeparator = '/';
        assert name.charAt(0) == nameSeparator : name;
        return name.substring(name.indexOf(nameSeparator, 1) + 1);
    }

    @Override
    protected String description() {
        return "# jimage: " + root;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new Error("error on closing reader for " + root + " : "
                    + e.getMessage(), e);
        } finally {
            super.close();
        }
    }

    private final ImageReader reader;

    public ClassPathJimageEntry(Path root) {
        super(root);
        if (!Files.exists(root)) {
            throw new Error(root + " image file not found");
        }
        try {
            reader = ImageReader.open(root);
        } catch (IOException e) {
            throw new Error("can not open " + root + " : " + e.getMessage(), e);
        }
    }

    @Override
    protected byte[] findByteCode(String name) {
        String resource = Utils.classNameToFileName(name);
        for (String m : reader.getModuleNames()) {
            ImageLocation location = reader.findLocation(m, resource);
            if (location != null) {
                return reader.getResource(location);
            }
        }
        return null;
    }

}
