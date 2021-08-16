/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.compiler
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 *        SelfReferenceDescriptor
 * @run main/othervm -Xverify:remote SelfReferenceDescriptor
 * @summary Test that a hidden class cannot be referenced in descriptor
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import static java.lang.invoke.MethodHandles.lookup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;

import jdk.test.lib.Utils;

/* package-private */ interface Test {
  void test();
}

public class SelfReferenceDescriptor {

    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC);
    private static final Path CLASSES_DIR = Paths.get("classes");

    static void compileSources(Path sourceFile, String... options) throws IOException {
        Stream<String> ops = Stream.of("-cp", Utils.TEST_CLASSES + File.pathSeparator + CLASSES_DIR);
        if (options != null && options.length > 0) {
            ops = Stream.concat(ops, Arrays.stream(options));
        }
        if (!CompilerUtils.compile(sourceFile, CLASSES_DIR, ops.toArray(String[]::new))) {
            throw new RuntimeException("Compilation of the test failed: " + sourceFile);
        }
    }

    // Test that a hidden class cannot use its own name in a field
    // signature.
    public static void hiddenClassInFieldDescriptor() throws Exception {
        compileSources(SRC_DIR.resolve("SelfRefField.java"));
        Path path = CLASSES_DIR.resolve("SelfRefField.class");
        byte[] bytes = Files.readAllBytes(path);
        try {
            lookup().defineHiddenClass(bytes, false, NESTMATE);
            throw new RuntimeException("expected NCDFE in defining SelfRefField hidden class");
        } catch (NoClassDefFoundError e) {
            if (!e.getMessage().contains("SelfRefField")) throw e;
        }
    }

    // Test that a hidden class cannot use its own name in a method
    // signature.
    public static void hiddenClassInMethodDescriptor() throws Exception {
        compileSources(SRC_DIR.resolve("SelfRefMethod.java"));
        Path path = CLASSES_DIR.resolve("SelfRefMethod.class");
        byte[] bytes = Files.readAllBytes(path);
        try {
            lookup().defineHiddenClass(bytes, false, NESTMATE);
            throw new RuntimeException("expected NCDFE in defining SelfRefMethod hidden class");
        } catch (NoClassDefFoundError e) {
            if (!e.getMessage().contains("SelfRefMethod")) throw e;
        }
    }

    public static void main(String... args) throws Exception {
        hiddenClassInMethodDescriptor();
        hiddenClassInFieldDescriptor();
    }
}
