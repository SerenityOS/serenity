/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6999891
 * @summary Test valid relative names for Filer.createResource and Filer.getResource
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile TestValidRelativeNames.java
 * @compile/process -processor TestValidRelativeNames -Amode=create java.lang.Object
 * @compile/process -processor TestValidRelativeNames -Amode=get    java.lang.Object
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

@SupportedOptions("mode")
public class TestValidRelativeNames extends JavacTestingAbstractProcessor {
    enum Kind { READER_WRITER, INPUT_OUTPUT_STREAM };

    static final String[] validRelativeNames = {
            "foo", "foo.bar", ".foo", ".foo.bar", "foodir/bar", "foodir/.bar"
    };

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            String mode = options.get("mode");
            for (String relativeBase: validRelativeNames) {
                for (Kind kind: Kind.values()) {
                    if (mode.equals("create"))
                        testCreate(relativeBase, kind);
                    else
                        testGet(relativeBase, kind);
                }
            }
        }

        return true;
    }

    void testCreate(String relativeBase, Kind kind) {
        String relative = getRelative(relativeBase, kind);
        System.out.println("test create relative path: " + relative + ", kind: " + kind);
        try {
            switch (kind) {
                case READER_WRITER:
                    try (Writer writer = filer.createResource(
                            StandardLocation.CLASS_OUTPUT, "", relative).openWriter()) {
                        writer.write(relative);
                    }
                    break;

                case INPUT_OUTPUT_STREAM:
                    try (OutputStream out = filer.createResource(
                            StandardLocation.CLASS_OUTPUT, "", relative).openOutputStream()) {
                        out.write(relative.getBytes());
                    }
                    break;
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "relative path: " + relative + ", kind: " + kind + ", unexpected exception: " + e);
        }
    }

    void testGet(String relativeBase, Kind kind) {
        String relative = getRelative(relativeBase, kind);
        System.out.println("test get relative path: " + relative + ", kind: " + kind);
        try {
            switch (kind) {
                case READER_WRITER:
                    try (Reader reader = new BufferedReader(filer.getResource(
                            StandardLocation.CLASS_OUTPUT, "", relative).openReader(true))) {
                        StringBuilder sb = new StringBuilder();
                        char[] buf = new char[1024];
                        int n;
                        while ((n = reader.read(buf, 0, buf.length)) > 0)
                            sb.append(new String(buf, 0, n));
                        if (!sb.toString().equals(relative)) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "unexpected content: " + sb);
                        }
                    }
                    break;

                case INPUT_OUTPUT_STREAM:
                    try (InputStream in = new DataInputStream(filer.getResource(
                            StandardLocation.CLASS_OUTPUT, "", relative).openInputStream())) {
                        StringBuilder sb = new StringBuilder();
                        byte[] buf = new byte[1024];
                        int n;
                        while ((n = in.read(buf, 0, buf.length)) > 0)
                            sb.append(new String(buf, 0, n));
                        if (!sb.toString().equals(relative)) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "unexpected content: " + sb);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "relative path: " + relative + ", kind: " + kind + ", unexpected exception: " + e);
        }
    }

    String getRelative(String relativeBase, Kind kind) {
        String suffix = (kind == Kind.READER_WRITER ? "RW" : "IOS");
        return relativeBase.replace("foo", "foo" + suffix);
    }
}

