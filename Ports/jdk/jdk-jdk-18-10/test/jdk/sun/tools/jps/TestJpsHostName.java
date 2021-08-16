/*
 * Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @bug 8251155
 * @summary Test host names starting with digits
 * @library /test/lib
 * @build JpsHelper
 * @run driver TestJpsHostName
 */
public class TestJpsHostName {

    public static void main(String[] args) throws Throwable {
        testJpsHostName("12345");
        testJpsHostName("12345:37266");
    }

    private static void testJpsHostName(String hostname) throws Exception {
        OutputAnalyzer output = JpsHelper.jps(hostname);
        output.shouldNotContain("Malformed Host Identifier: " + hostname);
    }

}
