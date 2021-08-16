/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test ArchiveDoesNotExist
 * @summary Test how VM handles "file does not exist" situation while
 *          attempting to use CDS archive. JVM should exit gracefully
 *          when sharing mode is ON, and continue w/o sharing if sharing
 *          mode is AUTO.
 * @requires vm.cds
 * @library /test/lib
 * @run driver ArchiveDoesNotExist
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import java.io.File;

public class ArchiveDoesNotExist {
    public static void main(String[] args) throws Exception {
        String fileName = "ArchiveDoesNotExist.jsa";

        File cdsFile = new File(fileName);
        if (cdsFile.exists())
            throw new RuntimeException("Test error: cds file already exists");

        CDSOptions opts = (new CDSOptions()).setArchiveName(fileName);

        // -Xshare=on
        OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
        CDSTestUtils.checkMappingFailure(out);
        out.shouldContain("Specified shared archive not found")
            .shouldHaveExitValue(1);

        // -Xshare=auto
        opts.setXShareMode("auto");
        out = CDSTestUtils.runWithArchive(opts);
        CDSTestUtils.checkMappingFailure(out);
        out.shouldMatch("(java|openjdk) version")
            .shouldNotContain("sharing")
            .shouldHaveExitValue(0);
    }
}
