/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.tools.compiler;

import java.util.Arrays;
import java.util.Collection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class TestCompare {

    String logFile;

    static final String setupArgsTieredVersion[] = {
        "java",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+LogCompilation",
        "-XX:LogFile=target/tiered_version.log",
        "-version"
    };

    static final String setupArgsTieredVersion2[] = {
        "java",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+LogCompilation",
        "-XX:LogFile=target/tiered_version.log.2",
        "-version"
    };

        static final String setupArgsNoTiered[] = {
        "java",
        "-XX:-TieredCompilation",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+LogCompilation",
        "-XX:LogFile=target/no_tiered_short.log"
    };

    static final String setupArgsNoTiered2[] = {
        "java",
        "-XX:-TieredCompilation",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+LogCompilation",
        "-XX:LogFile=target/no_tiered_short.log.2"
    };

    static final String allSetupArgs[][] = {
        setupArgsTieredVersion,
        setupArgsTieredVersion2,
        setupArgsNoTiered,
        setupArgsNoTiered2
    };

    @Parameters
    public static Collection data() {
        Object[][] data = new Object[][]{
            // Take care these match whats created in the setup method
            {"./target/tiered_version.log"},
            {"./target/no_tiered_short.log"}
        };
        assert data.length == (allSetupArgs.length/2) : "Files dont match args. Need 2 inputs per test case.";
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void setup() {
        try {
            for (String[] setupArgs : allSetupArgs) {
                Process p = Runtime.getRuntime().exec(setupArgs);
                p.waitFor();
            }
        } catch (Exception e) {
            System.out.println(e + ": exec failed:" + setupArgsNoTiered[0]);
        }
    }

    public TestCompare(String logFile) {
        this.logFile = logFile;
    }

    @Test
    public void testDashC() throws Exception {
        String[] args = {"-C",
            logFile,
            logFile + ".2"
        };

        LogCompilation.main(args);
    }

}
