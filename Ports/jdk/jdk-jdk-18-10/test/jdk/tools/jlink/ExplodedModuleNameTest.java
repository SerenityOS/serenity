/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.spi.ToolProvider;

import tests.Helper;
import tests.JImageGenerator;
import tests.Result;

/*
 * @test
 * @bug 8192986
 * @summary Inconsistent handling of exploded modules in jlink
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main ExplodedModuleNameTest
 */
public class ExplodedModuleNameTest {
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    public static void main(String[] args) throws Exception {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }

        // generate a new exploded module
        String modName = "mod8192986";
        Path modDir = helper.generateDefaultExplodedModule(modName).getFile();
        // rename the module containing directory
        Path renamedModDir = modDir.resolveSibling("modified_mod8192986");
        // copy the content from original directory to modified name directory
        copyDir(modDir, renamedModDir);

        Path outputDir = helper.createNewImageDir("image8192986");
        JImageGenerator.getJLinkTask()
                .modulePath(renamedModDir.toAbsolutePath().toString())
                .output(outputDir)
                .addMods(modName)
                .launcher(modName + "=" + modName + "/" + modName +".Main")
                .call().assertSuccess();
    }

    private static void copyDir(Path srcDir, Path destDir) throws IOException {
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = destDir.resolve(srcDir.relativize(dir));
                Files.createDirectory(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destDir.resolve(srcDir.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
