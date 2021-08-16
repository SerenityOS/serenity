/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8259275
 * @summary VM should not crash during CDS dump and run time when a lambda
 *          expression references an old version of class.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/OldClass.jasm
 * @compile test-classes/LambdaWithOldClassApp.java
 * @run driver LambdaWithOldClass
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class LambdaWithOldClass {

    public static void main(String[] args) throws Exception {
        String mainClass = "LambdaWithOldClassApp";
        String namePrefix = "lambdawitholdclass";
        JarBuilder.build(namePrefix, mainClass, "OldClass", "TestInterface");

        String appJar = TestCommon.getTestJar(namePrefix + ".jar");
        String classList = namePrefix + ".list";
        String archiveName = namePrefix + ".jsa";

        // dump class list
        CDSTestUtils.dumpClassList(classList, "-cp", appJar, mainClass);

        // create archive with the class list
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-XX:ExtraSharedClassListFile=" + classList,
                       "-cp", appJar,
                       "-Xlog:class+load,cds")
            .setArchiveName(archiveName);
        CDSTestUtils.createArchiveAndCheck(opts);

        // run with archive
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", appJar, "-Xlog:class+load,cds=debug")
            .setArchiveName(archiveName)
            .setUseVersion(false)
            .addSuffix(mainClass);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldContain("[class,load] LambdaWithOldClassApp source: shared objects file")
              .shouldMatch(".class.load. LambdaWithOldClassApp[$][$]Lambda[$].*/0x.*source:.*shared objects file")
              .shouldHaveExitValue(0);
    }
}
