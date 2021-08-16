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

/*
 * @test
 * @bug 4274624
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        GetContentType GetContentTypeTest
 * @run main/othervm GetContentTypeTest
 * @summary Test JarURLConnection.getContentType would
 *          would return default "content/unknown"
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GetContentTypeTest {
    public static void main(String[] args) throws Throwable {
        Path resJar = Paths.get(System.getProperty("test.src"),
                "resource.jar");
        Path classes = Paths.get(System.getProperty("test.classes"));
        ProcessTools.executeCommand(
                JDKToolFinder.getTestJDKTool("java"),
                "-cp", resJar + File.pathSeparator + classes, "GetContentType")
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldHaveExitValue(0);
    }
}
