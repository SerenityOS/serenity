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

package jdk.jpackage.tests;

import java.io.IOException;
import java.nio.file.Path;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;


/*
 * @test
 * @summary jpackage with --module-path testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ModulePathTest2.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.ModulePathTest2
 */

public final class ModulePathTest2 {

    /**
     * Test case for JDK-8233265.
     * Adding modules in .jmod files for non-modular app results in unexpected
     * jpackage failure.
     * @param mainAppDesc
     */
    @Test
    @Parameter("Hello!")
    @Parameter("com.foo/com.foo.ModuleApp")
    public void test8233265(String mainAppDesc) throws IOException {
        JPackageCommand cmd = JPackageCommand.helloAppImage(mainAppDesc);

        // The test should make jpackage invoke jlink.
        cmd.ignoreDefaultRuntime(true);

        Path modulePath = cmd.getArgumentValue("--module-path", () -> null, Path::of);
        if (modulePath == null) {
            modulePath = TKit.createTempDirectory("input-modules");
            cmd.addArguments("--module-path", modulePath);
        }

        JavaAppDesc extraModule = JavaAppDesc.parse("x.jmod:com.x/com.x.Y");
        HelloApp.createBundle(extraModule, modulePath);
        cmd.addArguments("--add-modules", extraModule.moduleName());

        cmd.executeAndAssertHelloAppImageCreated();
    }
}
