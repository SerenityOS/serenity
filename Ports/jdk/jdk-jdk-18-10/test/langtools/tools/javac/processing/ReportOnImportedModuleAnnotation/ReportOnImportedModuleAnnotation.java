/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8235457
 *      8235458
 * @summary javac shouldn't fail when an annotation processor report a message about an annotation on a module
 *          javac should process annotated module when imports statement are present
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class ReportOnImportedModuleAnnotation {

    public static void main(String[] args) throws Exception {
        final Path testBasePath = Path.of(System.getProperty("test.src"));
        final Path testOutputPath = Path.of(System.getProperty("test.classes"));

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Compile annotation and processor modules
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, List.of(testBasePath.resolve("mods-src1/")));
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(testOutputPath));
        compiler.getTask(new PrintWriter(System.out), fileManager, null, List.of("--module", "annotation,processor"), null, null).call();

        // Compile mod modules
        fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, List.of(testBasePath.resolve("mods-src2/")));
        fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, List.of(testOutputPath.resolve("annotation")));
        fileManager.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, List.of(testOutputPath.resolve("processor")));
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(testOutputPath));

        final StringWriter outputWriter = new StringWriter();
        compiler.getTask(outputWriter, fileManager, null, List.of("-XDrawDiagnostics", "--module", "mod"), null, null).call();

        String actualOutput = outputWriter.toString();
        String expectedOutput = Files.readString(testBasePath.resolve("ReportOnImportedModuleAnnotation.out"));

        String lineSep = System.getProperty("line.separator");
        if(!actualOutput.replace(lineSep, "\n").equals(expectedOutput.replace(lineSep, "\n"))) {
            System.err.println("Expected: [" + expectedOutput + "]");
            System.err.println("Received: [" + actualOutput + "]");
            throw new Exception("Invalid output");
        }
    }
}
