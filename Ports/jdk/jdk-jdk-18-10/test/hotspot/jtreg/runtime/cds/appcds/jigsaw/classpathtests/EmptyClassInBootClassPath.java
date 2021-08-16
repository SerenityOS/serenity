/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test a few scenarios if an empty class, which has the same name as the one in the jimage, is specified in the -Xbootclasspath/a
 *     1) boot loader will always load the class from the bootclasspath
 *     2) app loader will load the class from the jimage by default;
 *        app loader will load the class from the bootclasspath if the
 *        "--limit-modules java.base" option is specified
 * @requires vm.cds & !vm.graal.enabled
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @modules java.base/jdk.internal.access
 * @compile ../../test-classes/EmptyClassHelper.java
 * @compile ../../test-classes/com/sun/tools/javac/Main.jasm
 * @run driver EmptyClassInBootClassPath
 */

import java.io.File;
import java.lang.*;
import java.lang.reflect.*;
import java.util.List;
import java.util.ArrayList;
import jdk.test.lib.process.OutputAnalyzer;

public class EmptyClassInBootClassPath {
    static final String EXPECTED_EXCEPTION =
        "java.lang.NoSuchMethodException: com.sun.tools.javac.Main.main([Ljava.lang.String;)";
    public static void main(String[] args) throws Exception {
        String[] className = {"com/sun/tools/javac/Main"};
        JarBuilder.build("emptyClass", className);
        String appJar = TestCommon.getTestJar("emptyClass.jar");
        JarBuilder.build("EmptyClassHelper", "EmptyClassHelper");
        String helperJar = TestCommon.getTestJar("EmptyClassHelper.jar");
        OutputAnalyzer dumpOutput = TestCommon.dump(
            appJar, className, "-Xbootclasspath/a:" + appJar);
        TestCommon.checkDump(dumpOutput);
        dumpOutput.shouldNotContain("Preload Warning: skipping class from -Xbootclasspath/a " + className[0]);

        String bootclasspath = "-Xbootclasspath/a:" + appJar;
        String classPath = "-Djava.class.path=" + appJar + File.pathSeparator + helperJar;
        List<String> argsList = new ArrayList<String>();
        argsList.add(classPath);
        argsList.add(bootclasspath);
        argsList.add("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED");
        argsList.add("EmptyClassHelper");

        // case 1: load class in bootclasspath using app loader
        argsList.add("useAppLoader");
        String[] opts = new String[argsList.size()];
        opts = argsList.toArray(opts);
        TestCommon.run(opts).assertNormalExit("appLoader found method main");

        // case 2: load class in bootclasspath using boot loader
        argsList.remove(argsList.size() - 1);
        argsList.add("useBootLoader");
        opts = new String[argsList.size()];
        opts = argsList.toArray(opts);
        TestCommon.run(opts).assertNormalExit(EXPECTED_EXCEPTION);

        // case 3: load class in bootclasspath using app loader with '--limit-modules java.base'
        argsList.add(0, "--limit-modules");
        argsList.add(1, "java.base");
        argsList.remove(argsList.size() - 1);
        argsList.add("useAppLoader");
        opts = new String[argsList.size()];
        opts = argsList.toArray(opts);
        TestCommon.run(opts)
            .assertSilentlyDisabledCDS(0, EXPECTED_EXCEPTION);

        // case 4: load class in bootclasspath using boot loader with '--limit-modules java.base'
        argsList.remove(argsList.size() - 1);
        argsList.add("useBootLoader");
        opts = new String[argsList.size()];
        opts = argsList.toArray(opts);
        TestCommon.run(opts)
            .assertSilentlyDisabledCDS(0, EXPECTED_EXCEPTION);
    }
}
