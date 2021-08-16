/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test config, cmd and lib directories of jmod.
 * @author Andrei Eremeev
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main NativeTest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import tests.Helper;
import tests.JImageGenerator;

public class NativeTest {

    private final Helper helper;

    public NativeTest(Helper helper) {
        this.helper = helper;
    }

    public static void main(String[] args) throws IOException {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Not run");
            return;
        }
        new NativeTest(helper).test();
    }

    public void test() throws IOException {
        String moduleName = "native_library";
        Path classesDir = helper.generateModuleCompiledClasses(
                helper.getJmodSrcDir(), helper.getJmodClassesDir(), moduleName);
        Path libsDir = helper.getJmodDir().resolve("lib").resolve(moduleName);
        Path configDir = helper.getJmodDir().resolve("config").resolve(moduleName);
        Path cmdDir = helper.getJmodDir().resolve("cmd").resolve(moduleName);

        Path config = writeFile(configDir.resolve("config.txt"), "AAAA\nBBBB");
        Path cmd = writeFile(cmdDir.resolve("ls.sh"), "ls");
        Path lib = writeFile(libsDir.resolve("native.so"), "native library");

        JImageGenerator.getJModTask()
                .addClassPath(classesDir)
                .addNativeLibraries(libsDir)
                .addCmds(cmdDir)
                .addConfig(configDir)
                .jmod(helper.createNewJmodFile(moduleName))
                .create()
                .assertSuccess();

        String[] expectedFiles = new String[] {
                "bin" + File.separator + cmd.getFileName(),
                "conf" + File.separator + config.getFileName(),
                "lib" + File.separator + lib.getFileName()
        };
        Path image = JImageGenerator.getJLinkTask()
                .modulePath(helper.defaultModulePath())
                .addMods(moduleName)
                .output(helper.createNewImageDir(moduleName))
                .call().assertSuccess();
        helper.checkImage(image, moduleName, null, null, expectedFiles);
    }

    private Path writeFile(Path path, String content) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, Arrays.asList(content.split("\n")));
        return path;
    }
}
