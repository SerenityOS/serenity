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
 * @summary Unused metadata created during dump time should be freed from the CDS archive.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/MethodNoReturn.jasm test-classes/Hello.java
 * @run driver FreeUnusedMetadata
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import jdk.test.lib.process.OutputAnalyzer;

public class FreeUnusedMetadata {
    static byte iconst_1 =  4;
    static byte pop      = 87;
    static byte[] pattern = { // This has the same sequence as in test-classes/MethodNoReturn.jasm
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        pop,
        iconst_1,
        iconst_1,
        iconst_1,
        iconst_1,
        iconst_1,
        iconst_1,
        iconst_1,
        iconst_1,
        pop,
        pop,
        pop,
        pop,
        pop,
        pop,
        pop,
        pop
    };

    public static void main(String[] args) throws Exception {
        String[] ARCHIVE_CLASSES = {"Hello", "MethodNoReturn"};
        String appJar = JarBuilder.build("FreeUnusedMetadata", ARCHIVE_CLASSES);

        OutputAnalyzer dumpOutput = TestCommon.dump(
                appJar, ARCHIVE_CLASSES);
        TestCommon.checkDump(dumpOutput, "Loading classes to share");

        OutputAnalyzer execOutput = TestCommon.exec(appJar, "Hello");
        TestCommon.checkExec(execOutput, "Hello World");


        String archive = TestCommon.getCurrentArchiveName();
        System.out.println("Checking for pattern inside " + archive + "...");

        byte[] data = Files.readAllBytes(Paths.get(archive));
        int max = data.length - pattern.length;
        for (int i=0; i<max; i++) {
            if (data[i+0] == iconst_1 && data[i+1] == pop &&
                data[i+2] == iconst_1 && data[i+3] == pop) {
                boolean match = true;
                for (int x=4; x<pattern.length; x++) {
                    if (data[i+x] != pattern[x]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    throw new RuntimeException("method of unverifiable class should have been " +
                        "removed from the archive " + archive +
                        " , but was found at offset " + i);
                }
            }
        }
        System.out.println("Not found: method from unverifiable class has been removed");
    }
}
