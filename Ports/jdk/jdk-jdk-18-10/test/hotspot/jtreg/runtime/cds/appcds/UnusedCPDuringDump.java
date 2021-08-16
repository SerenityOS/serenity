/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209385
 * @summary non-empty dir in -cp should be fine during dump time if only classes
 *          from the system modules are being loaded even though some are
 *          defined to the PlatformClassLoader and AppClassLoader.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run main/othervm -Dtest.cds.copy.child.stdout=false UnusedCPDuringDump
 */

import java.io.File;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class UnusedCPDuringDump {

    public static void main(String[] args) throws Exception {
        File dir = CDSTestUtils.getOutputDirAsFile();
        File emptydir = new File(dir, "emptydir");
        emptydir.mkdir();
        String appJar = JarBuilder.getOrCreateHelloJar();

        OutputAnalyzer output = TestCommon.dump(dir.getPath(),
            TestCommon.list("sun/util/resources/cldr/provider/CLDRLocaleDataMetaInfo",
                            "com/sun/tools/sjavac/client/ClientMain"),
                            "-Xlog:class+path=info,class+load=debug");
        TestCommon.checkDump(output,
                             "[class,load] sun.util.resources.cldr.provider.CLDRLocaleDataMetaInfo",
                             "for instance a 'jdk/internal/loader/ClassLoaders$PlatformClassLoader'",
                             "[class,load] com.sun.tools.sjavac.client.ClientMain",
                             "for instance a 'jdk/internal/loader/ClassLoaders$AppClassLoader'");

        String jsaOpt = "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName();
        TestCommon.run("-cp", appJar, jsaOpt, "-Xlog:class+path=info,class+load=debug", "Hello")
            .assertNormalExit("Hello World");
  }
}
