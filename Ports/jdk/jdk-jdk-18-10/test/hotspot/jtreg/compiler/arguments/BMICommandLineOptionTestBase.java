/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.arguments;

import jdk.test.lib.cli.CPUSpecificCommandLineOptionTest;
import jdk.test.lib.cli.CommandLineOptionTest;

/**
 * Base class for all X86 bit manipulation related command line options.
 *
 * Note that this test intended to verify that VM could be launched with
 * specific options and that values of these options processed correctly.
 * In order to do that test launch a new VM with tested options, the same
 * flavor-specific flag as one that was used for parent VM (-client, -server,
 * -minimal, -graal) and '-version'.
 */
public abstract class BMICommandLineOptionTestBase
              extends CPUSpecificCommandLineOptionTest {

    public static final String LZCNT_WARNING =
        "lzcnt instruction is not available on this CPU";
    public static final String TZCNT_WARNING =
        "tzcnt instruction is not available on this CPU";
    public static final String BMI1_WARNING =
        "BMI1 instructions are not available on this CPU";

    protected final String optionName;
    protected final String warningMessage;
    protected final String errorMessage;

    /**
     * Construct new test on {@code optionName} option.
     *
     * @param optionName Name of the option to be tested
     *                   without -XX:[+-] prefix.
     * @param warningMessage Message that can occur in VM output
     *                       if CPU on test box does not support
     *                       features required by the option.
     * @param supportedCPUFeatures CPU features requires by the option,
     *                             that should be supported on test box.
     * @param unsupportedCPUFeatures CPU features requires by the option,
     *                               that should not be supported on test box.
     */
    public BMICommandLineOptionTestBase(String optionName,
                                        String warningMessage,
                                        String supportedCPUFeatures[],
                                        String unsupportedCPUFeatures[]) {
        super(".*", supportedCPUFeatures, unsupportedCPUFeatures);
        this.optionName = optionName;
        this.warningMessage = warningMessage;
        this.errorMessage = String.format(
                CommandLineOptionTest.UNRECOGNIZED_OPTION_ERROR_FORMAT,
                optionName);
    }

}

