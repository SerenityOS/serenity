/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.JDKToolFinder;
import static jdk.test.lib.process.ProcessTools.executeCommand;

/**
 * @test
 * @bug 8142968
 * @summary Access a jrt:/ resource in an observable module that is not in
 *          the boot layer and hence not known to the built-in class loaders.
 *          This test is intended to run with --limit-modules.
 * @library /test/lib
 * @build OtherResources OtherResourcesTest
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.process.*
 * @run main OtherResourcesTest
 */
public class OtherResourcesTest {
    public static void main(String[] args) throws Throwable {
        String classes = System.getProperty("test.classes");
        executeCommand(JDKToolFinder.getTestJDKTool("java"),
                       "--limit-modules", "java.base",
                       "-cp", classes, "OtherResources")
                      .outputTo(System.out)
                      .errorTo(System.out)
                      .shouldHaveExitValue(0);
    }
}

