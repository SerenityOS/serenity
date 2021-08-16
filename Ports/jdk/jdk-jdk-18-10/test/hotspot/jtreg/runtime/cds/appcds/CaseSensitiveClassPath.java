/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */


/*
 * @test
 * @summary Test case sensitive aspect of comparing class paths
 *     between dump time and archive use time
 * @requires vm.cds
 * @library /test/lib
 * @requires os.family != "mac"
 * @compile test-classes/Hello.java
 * @run driver CaseSensitiveClassPath
 */

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;


// Excluded from running on MAC: a more comprehensive case sensitivity detection
// and fix mechanism is needed, which is planned to be implemented in the future.
public class CaseSensitiveClassPath {
    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appJarUpper = appJar.replace("hello", "Hello");

        OutputAnalyzer out = TestCommon.dump(appJar, TestCommon.list("Hello"));
        TestCommon.checkDump(out);

        Path jarPath = Paths.get(appJar);
        Path jarPathUpper = null;

        boolean fileExists = false;
        try {
            jarPathUpper = Files.createFile(Paths.get(appJarUpper));
        } catch (FileAlreadyExistsException faee) {
            fileExists = true;
        }

        if (!fileExists) {
            try {
                Files.copy(jarPath, jarPathUpper, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new java.lang.RuntimeException(
                    "Failed copying file from " + appJar + " to " + appJarUpper + ".", e);
            }
        } else {
            jarPathUpper = Paths.get(appJarUpper);
        }
        boolean isSameFile = Files.isSameFile(jarPath, jarPathUpper);

        TestCommon.run("-Xlog:class+path=info,cds", "-cp", appJarUpper, "Hello")
            .ifNoMappingFailure(output -> {
                    if (isSameFile) {
                        output.shouldContain("Hello World");
                    } else {
                        output.shouldContain("shared class paths mismatch");
                        output.shouldHaveExitValue(1);
                    }
                });
    }
}
