/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Handler for files containing a list of classes to compile.
 */
public class ClassesListInFile extends PathHandler.PathEntry {
    private final BufferedReader reader;

    public ClassesListInFile(Path root) {
        super(root);
        if (!Files.exists(root)) {
            throw new Error(root + " file does not exist");
        }
        try {
           reader = Files.newBufferedReader(root);
        } catch (IOException e) {
            throw new Error("can not open " + root + " : " + e.getMessage(), e);
        }
    }

    @Override
    protected byte[] findByteCode(String name) {
        return null;
    }

    @Override
    protected Stream<String> classes() {
        return reader.lines();
    }

    @Override
    protected String description() {
        return "# list: " + root;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new Error("error on closing reader for " + root
                    + " : "  + e.getMessage(), e);
        } finally {
            super.close();
        }
    }
}
