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
 *
 */

/*
 * @test
 * @summary Dump time should not crash if any class with shared strings fails verification due to missing dependencies.
 * @bug 8186789
 * @requires vm.cds.archived.java.heap
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile MyOuter.java MyException.java
 * @run driver DumpTimeVerifyFailure
 */

import jdk.test.lib.process.OutputAnalyzer;

public class DumpTimeVerifyFailure {
    public static void main(String[] args) throws Exception {
        // App classes (see MyOuter.java):
        //   MyOuter
        //   MyInnder$MyOuter extends MyOuter
        //   MyException
        //
        // MyOuter$MyInner.test() throws MyException.
        // The missingMyException.jar file only includes MyOuter and
        // MyOuter$MyInner classes, but not the MyException class.
        // At dump time, MyOuter and MyOuter$MyInner classes fail
        // verification due to missing MyException class.
        String[] ARCHIVE_CLASSES = {"MyOuter", "MyOuter$MyInner"};
        String appJar = JarBuilder.build("missingMyException", ARCHIVE_CLASSES);

        OutputAnalyzer dumpOutput = TestCommon.dump(
                appJar, ARCHIVE_CLASSES,
                "-Xlog:verification",
                "-XX:SharedArchiveConfigFile=" + TestCommon.getSourceFile("DumpTimeVerifyFailure.config.txt"));
        TestCommon.checkDump(dumpOutput, "Loading classes to share");
    }
}
