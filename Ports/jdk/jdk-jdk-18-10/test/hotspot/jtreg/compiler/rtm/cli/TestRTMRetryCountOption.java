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
 * @bug 8031320
 * @summary Verify processing of RTMRetryCount option.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestRTMRetryCountOption
 */

package compiler.rtm.cli;

public class TestRTMRetryCountOption extends RTMGenericCommandLineOptionTest {
    private static final String DEFAULT_VALUE = "5";

    private TestRTMRetryCountOption() {
        super("RTMRetryCount", false, false,
                TestRTMRetryCountOption.DEFAULT_VALUE,
                "0", "10", "100", "1000");
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMRetryCountOption().runTestCases();
    }
}
