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

package compiler.codecache.cli.codeheapsize;

import compiler.codecache.cli.common.CodeCacheCLITestCase;
import compiler.codecache.cli.common.CodeCacheOptions;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

/**
 * Test case runner aimed to verify that all four options related to code cache
 * sizing have correct values.
 */
public class GenericCodeHeapSizeRunner implements CodeCacheCLITestCase.Runner {
    @Override
    public void run(CodeCacheCLITestCase.Description testCaseDescription,
            CodeCacheOptions options) throws Throwable {
        CodeCacheOptions expectedValues
                = options.mapOptions(testCaseDescription.involvedCodeHeaps);

        CommandLineOptionTest.verifyOptionValueForSameVM(
                BlobType.All.sizeOptionName,
                Long.toString(expectedValues.reserved),
                String.format("%s should have value %d.",
                        BlobType.All.sizeOptionName, expectedValues.reserved),
                testCaseDescription.getTestOptions(options));

        CommandLineOptionTest.verifyOptionValueForSameVM(
                BlobType.NonNMethod.sizeOptionName,
                Long.toString(expectedValues.nonNmethods),
                String.format("%s should have value %d.",
                        BlobType.NonNMethod.sizeOptionName,
                        expectedValues.nonNmethods),
                testCaseDescription.getTestOptions(options));

        CommandLineOptionTest.verifyOptionValueForSameVM(
                BlobType.MethodNonProfiled.sizeOptionName,
                Long.toString(expectedValues.nonProfiled),
                String.format("%s should have value %d.",
                        BlobType.MethodNonProfiled.sizeOptionName,
                        expectedValues.nonProfiled),
                testCaseDescription.getTestOptions(options));

        CommandLineOptionTest.verifyOptionValueForSameVM(
                BlobType.MethodProfiled.sizeOptionName,
                Long.toString(expectedValues.profiled),
                String.format("%s should have value %d.",
                        BlobType.MethodProfiled.sizeOptionName,
                        expectedValues.profiled),
                testCaseDescription.getTestOptions(options));
    }
}
