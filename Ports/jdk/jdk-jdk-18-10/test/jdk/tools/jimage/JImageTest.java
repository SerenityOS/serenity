/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import tests.Helper;
import tests.JImageGenerator;
import tests.JImageValidator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/*
 * jimage testing.
 * @test
 * @summary Test jimage tool
 * @bug 8222100
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.compiler
 * @run build JImageTest
 * @run build tests.*
 * @run main/othervm/timeout=360 -verbose:gc -Xmx1g JImageTest
*/
public class JImageTest {

    public static void main(String[] args) throws Exception {
        List<String> bootClasses = new ArrayList<>();

        FileSystem fs;
        try {
            fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        } catch (ProviderNotFoundException | FileSystemNotFoundException e) {
            System.out.println("Not an image build, test skipped.");
            return;
        }

        // Build the set of locations expected in the Image
        Consumer<Path> c = (p) -> {
               // take only the .class resources.
               if (Files.isRegularFile(p) && p.toString().endsWith(".class")
                       && !p.toString().endsWith("module-info.class")) {
                   String loc = p.toString().substring("/modules".length());
                   bootClasses.add(loc);
               }
           };

        Path javabase = fs.getPath("/modules/java.base");
        Path mgtbase = fs.getPath("/modules/java.management");
        try (Stream<Path> stream = Files.walk(javabase)) {
            stream.forEach(c);
        }
        try (Stream<Path> stream = Files.walk(mgtbase)) {
            stream.forEach(c);
        }

        if (bootClasses.isEmpty()) {
            throw new RuntimeException("No boot class to check against");
        }

        File jdkHome = new File(System.getProperty("test.jdk"));
        Helper helper = Helper.newHelper();
        if (helper == null) {
            // Skip test if the jmods directory is missing (e.g. exploded image)
            System.err.println("Test not run, NO jmods directory");
            return;
        }

        // Generate the sample image
        String module = "mod1";
        String[] classes = {module + ".Main"};
        helper.generateDefaultJModule(module, Arrays.asList(classes), "java.management");

        Path image = helper.generateDefaultImage(module).assertSuccess();
        Path extractedDir = JImageGenerator.getJImageTask()
                .dir(helper.createNewExtractedDir("modules"))
                .image(image.resolve("lib").resolve("modules"))
                .extract().assertSuccess();
    }
}
