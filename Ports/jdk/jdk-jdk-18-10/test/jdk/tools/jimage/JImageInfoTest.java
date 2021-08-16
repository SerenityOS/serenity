/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary Tests to verify jimage 'info' action
 * @library /test/lib
 * @modules jdk.jlink/jdk.tools.jimage
 * @build jdk.test.lib.Asserts
 * @run main JImageInfoTest
 */

import java.util.Arrays;

public class JImageInfoTest extends JImageCliTest {
    public void testInfo() {
        jimage("info", getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertMatches("(?m)^\\s+Major Version: +[1-9]\\d*$.*", r.output);
                    assertMatches("(?m)^\\s+Minor Version: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Flags: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Resource Count: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Table Length: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Offsets Size: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Redirects Size: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Locations Size: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Strings Size: +\\d+$.*", r.output);
                    assertMatches("(?m)^\\s+Index Size: +\\d+$.*", r.output);
                });
    }

    public void testInfoHelp() {
        for (String opt : Arrays.asList("-h", "--help")) {
            jimage("info", opt)
                    .assertSuccess()
                    .resultChecker(r -> {
                        // info  -  descriptive text
                        assertMatches("\\s+info\\s+-\\s+.*", r.output);
                    });
        }
    }

    public void testInfoUnknownOption() {
        jimage("info", "--unknown")
                .assertFailure()
                .assertShowsError();
    }

    public static void main(String[] args) throws Throwable {
        new JImageInfoTest().runTests();
    }
}

