/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test NonBootLoaderClasses
 * @summary Test to ensure platform and app classes are archived when specified in classlist
 * @requires vm.cds
 * @library /test/lib
 * @run driver NonBootLoaderClasses
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class NonBootLoaderClasses {
    public static void main(String[] args) throws Exception {
        final String PLATFORM_CLASS = "jdk/dynalink/DynamicLinker";
        final String APP_CLASS = "com/sun/tools/javac/Main";
        String[] classes = {PLATFORM_CLASS, APP_CLASS};
        String classList =
            CDSTestUtils.makeClassList(classes).getPath();
        String archiveName = "NonBootLoaderClasses.jsa";
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-XX:ExtraSharedClassListFile=" + classList, "-Djava.class.path=")
            .setArchiveName(archiveName);
        CDSTestUtils.createArchiveAndCheck(opts);

        // Print the shared dictionary and inspect the output
        opts = (new CDSOptions())
            .setUseVersion(false)
            .addSuffix("-Djava.class.path=",
                "-XX:+UnlockDiagnosticVMOptions", "-XX:SharedArchiveFile=./" + archiveName,
                "-XX:+PrintSharedArchiveAndExit", "-XX:+PrintSharedDictionary");
        CDSTestUtils.run(opts)
            .assertNormalExit(output -> {
                output.shouldContain("archive is valid");
                output.shouldContain(PLATFORM_CLASS.replace('/', '.'));
                output.shouldContain(APP_CLASS.replace('/', '.'));
            });
   }
}
