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
 * @summary Verify RTMTotalCountIncrRate option processing on CPU and OS with
 *          rtm support and on VM with rtm locking support.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestRTMTotalCountIncrRateOptionOnSupportedConfig
 */

package compiler.rtm.cli;

public class TestRTMTotalCountIncrRateOptionOnSupportedConfig
        extends RTMLockingAwareTest {
    private static final String DEFAULT_VALUE = "64";

    private TestRTMTotalCountIncrRateOptionOnSupportedConfig() {
        super("RTMTotalCountIncrRate", false, true,
                TestRTMTotalCountIncrRateOptionOnSupportedConfig.DEFAULT_VALUE,
                /* correct values */
                new String[] { "1", "2", "128", "1024" },
                /* incorrect values */
                new String[] { "3", "5", "7", "42" },
                RTMGenericCommandLineOptionTest.RTM_COUNT_INCR_WARNING);
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMTotalCountIncrRateOptionOnSupportedConfig().runTestCases();
    }
}
