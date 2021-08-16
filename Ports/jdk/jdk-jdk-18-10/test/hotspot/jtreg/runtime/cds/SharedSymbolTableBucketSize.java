/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8059510
 * @summary Test SharedSymbolTableBucketSize option
 * @requires vm.cds
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class SharedSymbolTableBucketSize {
    public static void main(String[] args) throws Exception {
        int bucket_size = 8;

        OutputAnalyzer output =
            CDSTestUtils.createArchiveAndCheck("-XX:SharedSymbolTableBucketSize="
                                               + Integer.valueOf(bucket_size));
        CDSTestUtils.checkMappingFailure(output);

        String s = output.firstMatch("Average bucket size     : .*");
        Float f = Float.parseFloat(s.substring(25));
        int size = Math.round(f);
        if (size != bucket_size) {
            throw new Exception("FAILED: incorrect bucket size " + size +
                                ", expect " + bucket_size);
        }

        // Invalid SharedSymbolTableBucketSize input
        String input[] = {"-XX:SharedSymbolTableBucketSize=-1",
                          "-XX:SharedSymbolTableBucketSize=2.5"};
        for (int i = 0; i < input.length; i++) {
            CDSTestUtils.createArchive(input[i])
                .shouldContain("Improperly specified VM option")
                .shouldHaveExitValue(1);
        }
    }
}
