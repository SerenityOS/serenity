/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import tests.Helper;

/*
 * @test
 * @summary Test image creation
 * @author Jean-Francois Denise
 * @requires (vm.compMode != "Xcomp" & os.maxMemory >= 2g)
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm -verbose:gc -Xmx1g JLinkPluginsTest
 */
public class JLinkPluginsTest {

    private static String createProperties(String fileName, String content) throws IOException {
        Path p = Paths.get(fileName);
        Files.write(p, Collections.singletonList(content));
        return p.toAbsolutePath().toString();
    }

    public static void main(String[] args) throws Exception {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();
        {
            // Skip debug
            String[] userOptions = {"--strip-debug"};
            String moduleName = "skipdebugcomposite";
            helper.generateDefaultJModule(moduleName, "composite2");
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, null, null);
        }
        {
            // Filter out files
            String[] userOptions = {"--exclude-resources", "*.jcov, */META-INF/*"};
            String moduleName = "excludecomposite";
            helper.generateDefaultJModule(moduleName, "composite2");
            String[] res = {".jcov", "/META-INF/"};
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, res, null);
        }
        {
            // disable generate jli classes - JDK-8160063
            String[] userOptions = {"--disable-plugin", "generate-jli-classes"};
            String moduleName = "jlidisabled";
            helper.generateDefaultJModule(moduleName, "composite2");
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, null, null);
        }
        {
            // disable invalid plugin - JDK-8160063
            String[] userOptions = {"--disable-plugin", "non-existent-plugin"};
            String moduleName = "invaliddisabled";
            helper.generateDefaultJModule(moduleName, "composite2");
            helper.generateDefaultImage(userOptions, moduleName).
                assertFailure("Error: No such plugin: non-existent-plugin");
        }
    }
}
