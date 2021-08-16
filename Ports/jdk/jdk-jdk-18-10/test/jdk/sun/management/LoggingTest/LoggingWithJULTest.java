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

import java.nio.file.Paths;

/**
 * @test
 * @bug 8172971
 * @summary Smoke test to check that logging in java.management works as expected.
 * @author danielfuchs
 *
 * @modules java.management
 *          java.logging
 *
 * @build LoggingTest LoggingWithJULTest
 * @run main/othervm LoggingWithJULTest
 */
public class LoggingWithJULTest {

    public static void main(String[] args) {
        // Replace System.err
        LoggingTest.TestStream ts = new LoggingTest.TestStream(System.err);
        System.setErr(ts);

        // activate the javax.management traces
        String properties = Paths.get(System.getProperty("test.src", "src"),
                                      "logging.properties").toString();
        System.setProperty("java.util.logging.config.file", properties);

        // run the test
        new LoggingTest().run(ts);
    }

}
