/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codecache.cli.printcodecache;

import compiler.codecache.cli.common.CodeCacheCLITestCase;
import compiler.codecache.cli.common.CodeCacheInfoFormatter;
import compiler.codecache.cli.common.CodeCacheOptions;
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Runner implementation aimed to verify PrintCodeCache output.
 */
public class PrintCodeCacheRunner implements CodeCacheCLITestCase.Runner {
    private final boolean printCodeCache;

    public PrintCodeCacheRunner(boolean printCodeCache) {
        this.printCodeCache = printCodeCache;
    }

    public PrintCodeCacheRunner() {
        this(true);
    }

    @Override
    public void run(CodeCacheCLITestCase.Description testCaseDescription,
            CodeCacheOptions options) throws Throwable {
        CodeCacheOptions expectedValues
                = testCaseDescription.expectedValues(options);

        String[] expectedMessages
                = testCaseDescription.involvedCodeHeaps.stream()
                        .map(heap -> CodeCacheInfoFormatter.forHeap(heap)
                                .withSize(expectedValues.sizeForHeap(heap)))
                        .map(CodeCacheInfoFormatter::getInfoString)
                        .toArray(String[]::new);

        EnumSet<BlobType> unexpectedHeapsSet
                = EnumSet.complementOf(testCaseDescription.involvedCodeHeaps);

        String[] unexpectedMessages = CodeCacheInfoFormatter.forHeaps(
                unexpectedHeapsSet.toArray(
                        new BlobType[unexpectedHeapsSet.size()]));

        String description = String.format("JVM output should contain entries "
                + "for following code heaps: [%s] and should not contain "
                + "entries for following code heaps: [%s].",
                testCaseDescription.involvedCodeHeaps.stream()
                        .map(BlobType::name)
                        .collect(Collectors.joining(", ")),
                unexpectedHeapsSet.stream()
                        .map(BlobType::name)
                        .collect(Collectors.joining(", ")));

        CommandLineOptionTest.verifySameJVMStartup(expectedMessages,
                unexpectedMessages, "JVM startup failure is not expected, "
                + "since all options have allowed values", description,
                ExitCode.OK,
                testCaseDescription.getTestOptions(options,
                        CommandLineOptionTest.prepareBooleanFlag(
                                "PrintCodeCache", printCodeCache),
                        // Do not use large pages to avoid large page
                        // alignment of code heaps affecting their size.
                        "-XX:-UseLargePages"));
    }
}
