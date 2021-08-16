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
 * @bug 6502392
 * @summary Invalid relative names for Filer.createResource and Filer.getResource
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile TestInvalidRelativeNames.java
 * @compile/process -processor TestInvalidRelativeNames java.lang.Object
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

public class TestInvalidRelativeNames extends JavacTestingAbstractProcessor {
    enum Kind { CREATE_WRITER, GET_READER, CREATE_OUTPUT_STREAM, GET_INPUT_STREAM };

    static final String[] invalidRelativeNames = {
            "/boo", "goo/../hoo", "./ioo", ""
    };

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            for (String relative: invalidRelativeNames) {
                for (Kind kind: Kind.values()) {
                    test(relative, kind);
                }
            }
        }
        return true;
    }

    void test(String relative, Kind kind) {
        System.out.println("test relative path: " + relative + ", kind: " + kind);
        try {
            switch (kind) {
                case CREATE_WRITER:
                    Writer writer = filer.createResource(
                            StandardLocation.SOURCE_OUTPUT, "", relative).openWriter();
                    writer.close();
                    break;

                case GET_READER:
                    Reader reader = filer.getResource(
                            StandardLocation.SOURCE_OUTPUT, "", relative).openReader(true);
                    reader.close();
                    break;

                case CREATE_OUTPUT_STREAM:
                    OutputStream out = filer.createResource(
                            StandardLocation.SOURCE_OUTPUT, "", relative).openOutputStream();
                    out.close();
                    break;

                case GET_INPUT_STREAM:
                    InputStream in = filer.createResource(
                            StandardLocation.SOURCE_OUTPUT, "", relative).openInputStream();
                    in.close();
                    break;
            }
        } catch (IllegalArgumentException expected) {
            System.out.println("expected exception thrown: " + expected);
            return;
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "relative path: " + relative + ", kind: " + kind + ", unexpected exception: " + e);
            return;
        }
        messager.printMessage(Diagnostic.Kind.ERROR,
                "relative path: " + relative + ", kind: " + kind + ", no exception thrown");
    }
}

