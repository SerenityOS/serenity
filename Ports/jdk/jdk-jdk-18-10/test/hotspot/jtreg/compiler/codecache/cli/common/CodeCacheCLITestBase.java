/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codecache.cli.common;

/**
 * Base for code cache related command line options tests.
 */
public class CodeCacheCLITestBase {
    public static final CodeCacheOptions[] OPTIONS_SET
            = new CodeCacheOptions[] {
            new CodeCacheOptions(CodeCacheOptions.mB(60),
                    CodeCacheOptions.mB(20), CodeCacheOptions.mB(20),
                    CodeCacheOptions.mB(20)),
            new CodeCacheOptions(CodeCacheOptions.mB(200),
                    CodeCacheOptions.mB(75), CodeCacheOptions.mB(75),
                    CodeCacheOptions.mB(50)),
            new CodeCacheOptions(CodeCacheOptions.mB(300),
                    CodeCacheOptions.mB(100), CodeCacheOptions.mB(100),
                    CodeCacheOptions.mB(100)),
            new CodeCacheOptions(CodeCacheOptions.mB(60)),
            new CodeCacheOptions(CodeCacheOptions.mB(200)),
            new CodeCacheOptions(CodeCacheOptions.mB(300))
    };

    private final CodeCacheCLITestCase[] testCases;
    private final CodeCacheOptions[] options;

    public CodeCacheCLITestBase(CodeCacheOptions[] options,
            CodeCacheCLITestCase... testCases) {
        this.testCases = testCases;
        this.options = options;
    }

    protected void runTestCases() throws Throwable {
        for (CodeCacheCLITestCase testCase : testCases) {
            for (CodeCacheOptions opts : options) {
                testCase.run(opts);
            }
        }
    }
}
