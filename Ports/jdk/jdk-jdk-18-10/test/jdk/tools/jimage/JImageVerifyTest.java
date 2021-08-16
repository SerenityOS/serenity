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
 * @summary Tests to verify jimage 'verify' action
 * @library /test/lib
 * @modules jdk.jlink/jdk.tools.jimage
 * @build jdk.test.lib.Asserts
 * @run main JImageVerifyTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static jdk.test.lib.Asserts.assertTrue;

public class JImageVerifyTest extends JImageCliTest {

    public void testVerify() {
        jimage("verify", getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.startsWith("jimage: " + getImagePath()),
                            "Contains verified image's path");
                });
    }

    public void testVerifyHelp() {
        for (String opt : Arrays.asList("-h", "--help")) {
            jimage("verify", opt)
                    .assertSuccess()
                    .resultChecker(r -> {
                        // verify  -  descriptive text
                        assertMatches("\\s+verify\\s+-\\s+.*", r.output);
                    });
        }
    }

    public void testVerifyImageNotSpecified() {
        jimage("verify", "")
                .assertFailure()
                .assertShowsError();
    }

    public void testVerifyNotAnImage() throws IOException {
        Path tmp = Files.createTempFile(Paths.get("."), getClass().getName(), "not_an_image");
        jimage("verify", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testVerifyNotExistingImage() throws IOException {
        Path tmp = Paths.get(".", "not_existing_image");
        Files.deleteIfExists(tmp);
        jimage("verify", "")
                .assertFailure()
                .assertShowsError();
    }

    public void testVerifyWithUnknownOption() {
        jimage("verify", "--unknown")
                .assertFailure()
                .assertShowsError();
    }

    public static void main(String[] args) throws Throwable {
        new JImageVerifyTest().runTests();
    }

}
