/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186334
 * @summary Make sure scanning manifest doesn't throw AIOOBE on certain strings containing backticks.
 * @library /test/lib/
 * @build jdk.test.lib.util.JarBuilder
 * @run testng JarBacktickManifest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.jar.JarFile;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jdk.test.lib.util.JarBuilder;

public class JarBacktickManifest {

    public static final String VERIFY_MANIFEST_JAR = "verifyManifest.jar";

    @BeforeClass
    public void initialize() throws Exception {
        JarBuilder jb = new JarBuilder(VERIFY_MANIFEST_JAR);
        jb.addAttribute("Test", " Class-`Path` ");
        jb.addAttribute("Test2", " Multi-`Release ");
        jb.build();
    }

    @Test
    public void test() throws Exception {
        try (JarFile jf = new JarFile(VERIFY_MANIFEST_JAR)) {  // do not set runtime versioning
            Assert.assertFalse(jf.isMultiRelease(), "Shouldn't be multi-release");
        }
    }

    @AfterClass
    public void close() throws IOException {
        Files.delete(new File(VERIFY_MANIFEST_JAR).toPath());
    }
}
