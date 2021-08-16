/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8249276
 * @summary Make sure that archived module graph is not loaded if critical classes have been redefined.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @requires vm.cds
 * @requires vm.flavor != "minimal"
 * @modules java.instrument
 * @run driver RedefineClassesInModuleGraph
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefineClassesInModuleGraph {
    public static String appClasses[] = {
        RedefineClassesInModuleGraphApp.class.getName(),
    };
    public static String agentClasses[] = {
        RedefineClassesInModuleGraphAgent.class.getName(),
        RedefineClassesInModuleGraphTransformer.class.getName(),
    };

    private static final String MANIFEST =
        "Manifest-Version: 1.0\n" +
        "Premain-Class: RedefineClassesInModuleGraphAgent\n" +
        "Can-Retransform-Classes: true\n" +
        "Can-Redefine-Classes: true\n";

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("RedefineClassesInModuleGraphAgent.jar",
                                        ClassFileInstaller.Manifest.fromString(MANIFEST),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("RedefineClassesInModuleGraphApp.jar", appClasses);

        TestCommon.testDump(appJar, agentClasses);

        TestCommon.run(
            "-cp", appJar,
            "-javaagent:" + agentJar,
            RedefineClassesInModuleGraphApp.class.getName())
          .assertNormalExit();
    }
}

