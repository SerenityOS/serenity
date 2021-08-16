/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194251
 * @summary Ensure that messages can be formatted before resources are loaded
 * @library /test/lib
 * @build jdk.test.lib.process.*
 * @run main EarlyResources
 */

import java.io.*;
import java.nio.file.*;
import java.util.*;
import jdk.test.lib.process.*;

public class EarlyResources {

    public static void main(String[] args) throws Exception {

        String testSrc = System.getProperty("test.src");
        String fs = File.separator;
        String policyPath = testSrc + fs + "malformed.policy";

        OutputAnalyzer out = ProcessTools.executeTestJvm(
            "-Djava.security.manager",
            "-Djava.security.policy=" + policyPath,
            "EarlyResources$TestMain");

        out.shouldHaveExitValue(0);
    }

    public static class TestMain {
        public static void main(String[] args) {
            System.out.println(new Date().toString());
        }
    }
}
