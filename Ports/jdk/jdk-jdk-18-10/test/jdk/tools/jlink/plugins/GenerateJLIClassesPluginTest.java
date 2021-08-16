/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import tests.Helper;
import tests.JImageGenerator;
import tests.JImageValidator;
import tests.Result;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

 /*
 * @test
 * @bug 8252919
 * @library ../../lib
 * @summary Test --generate-jli-classes plugin
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 * @build tests.*
 * @run testng/othervm GenerateJLIClassesPluginTest
 */
public class GenerateJLIClassesPluginTest {

    private static Helper helper;

    @BeforeTest
    public static void setup() throws Exception {
        helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();
    }

    @Test
    public static void testSpecies()  throws IOException {
        // Check that --generate-jli-classes=@file works as intended
        Path baseFile = Files.createTempFile("base", "trace");
        String species = "LLLLLLLLLLLLLLLLLLL";
        String fileString = "[SPECIES_RESOLVE] java.lang.invoke.BoundMethodHandle$Species_" + species + " (salvaged)\n";
        Files.write(baseFile, fileString.getBytes(Charset.defaultCharset()));
        Result result = JImageGenerator.getJLinkTask()
                .modulePath(helper.defaultModulePath())
                .output(helper.createNewImageDir("generate-jli-file"))
                .option("--generate-jli-classes=@" + baseFile.toString())
                .addMods("java.base")
                .call();

        Path image = result.assertSuccess();
        validateHolderClasses(image);
        JImageValidator.validate(image.resolve("lib").resolve("modules"),
                classFilesForSpecies(List.of(species)), // species should be in the image
                classFilesForSpecies(List.of(species.substring(1)))); // but not it's immediate parent
    }

    @Test
    public static void testInvalidSignatures() throws IOException {
        // Check that --generate-jli-classes=@file fails as intended on shapes that can't be generated
        String[] args = new String[] {
                "[LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeVirtual L_L (success)\n",
                "[LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeInterface L_L (success)\n",
                "[LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic I_L (success)\n"
        };
        for (String fileString : args) {
            Path failFile = Files.createTempFile("fail", "trace");
            fileString = "[LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeVirtual L_L (success)\n";
            Files.write(failFile, fileString.getBytes(Charset.defaultCharset()));
            Result result = JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir("invalid-signature"))
                    .option("--generate-jli-classes=@" + failFile.toString())
                    .addMods("java.base")
                    .call();

            result.assertFailure();
        }
    }

    @Test
    public static void nonExistentTraceFile() throws IOException {
        Result result = JImageGenerator.getJLinkTask()
                .modulePath(helper.defaultModulePath())
                .output(helper.createNewImageDir("non-existent-tracefile"))
                .option("--generate-jli-classes=@NON_EXISTENT_FILE")
                .addMods("java.base")
                .call();

        Path image = result.assertSuccess();
        validateHolderClasses(image);
    }

    private static void validateHolderClasses(Path image) throws IOException {
        JImageValidator.validate(image.resolve("lib").resolve("modules"),
                List.of("/java.base/java/lang/invoke/DirectMethodHandle$Holder.class",
                        "/java.base/java/lang/invoke/DelegatingMethodHandle$Holder.class",
                        "/java.base/java/lang/invoke/LambdaForm$Holder.class",
                        "/java.base/java/lang/invoke/Invokers$Holder.class"),
                List.of());
    }

    private static List<String> classFilesForSpecies(Collection<String> species) {
        return species.stream()
                .map(s -> "/java.base/java/lang/invoke/BoundMethodHandle$Species_" + s + ".class")
                .collect(Collectors.toList());
    }
}
