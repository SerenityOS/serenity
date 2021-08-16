/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package tools.javac.combo;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.tools.Diagnostic;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static java.util.stream.Collectors.toList;

/**
 * Base class for negative and positive compilation tests.
 */
@Test
public class CompilationTestCase extends JavacTemplateTestBase {
    private String[] compileOptions = new String[] { };
    private String defaultFileName = "Source.java";
    private String programShell = "#";

    @AfterMethod
    public void dumpTemplateIfError(ITestResult result) {
        // Make sure offending template ends up in log file on failure
        if (!result.isSuccess()) {
            System.err.printf("Diagnostics: %s%nTemplate: %s%n", diags.errorKeys(),
                              sourceFiles.stream().map(p -> p.snd).collect(toList()));
        }
    }

    protected void setProgramShell(String shell) {
        programShell = shell;
    }

    protected void setCompileOptions(String... options) {
        compileOptions = options.clone();
    }

    protected String[] getCompileOptions() {
        return compileOptions.clone();
    }

    protected void appendCompileOptions(String... additionalOptions) {
        String[] moreOptions = additionalOptions.clone();
        String[] newCompileOptions = Arrays.copyOf(compileOptions, compileOptions.length + additionalOptions.length);
        IntStream.range(0, additionalOptions.length).forEach(i -> {
            newCompileOptions[newCompileOptions.length - additionalOptions.length + i] = additionalOptions[i];
        });
        compileOptions = newCompileOptions;
    }

    protected void removeLastCompileOptions(int i) {
        if (i < 0) {
            throw new AssertionError("unexpected negative value " + i);
        }
        if (i >= compileOptions.length) {
            compileOptions = new String[] {};
        } else {
            compileOptions = Arrays.copyOf(compileOptions, compileOptions.length - i);
        }
    }

    protected void setDefaultFilename(String name) {
        defaultFileName = name;
    }

    protected String expandMarkers(String... constructs) {
        String s = programShell;
        for (String c : constructs)
            s = s.replaceFirst("#", c);
        return s;
    }

    private File assertCompile(String program, Runnable postTest, boolean generate) {
        reset();
        addCompileOptions(compileOptions);
        addSourceFile(defaultFileName, program);
        File dir = null;
        try {
            dir = compile(generate);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        postTest.run();
        return dir;
    }

    protected void assertOK(String... constructs) {
        assertCompile(expandMarkers(constructs), this::assertCompileSucceeded, false);
    }

    protected File assertOK(boolean generate, String... constructs) {
        return assertCompile(expandMarkers(constructs), this::assertCompileSucceeded, generate);
    }

    protected File assertOK(Consumer<Diagnostic<?>> diagConsumer, String... constructs) {
        return assertCompile(expandMarkers(constructs), () -> assertCompileSucceeded(diagConsumer), false);
    }

    protected void assertOKWithWarning(String warning, String... constructs) {
        assertCompile(expandMarkers(constructs), () -> assertCompileSucceededWithWarning(warning), false);
    }

    protected void assertFail(String expectedDiag, String... constructs) {
        assertCompile(expandMarkers(constructs), () -> assertCompileFailed(expectedDiag), false);
    }

    protected void assertFail(String expectedDiag, Consumer<Diagnostic<?>> diagConsumer, String... constructs) {
        assertCompile(expandMarkers(constructs), () -> assertCompileFailed(expectedDiag, diagConsumer), false);
    }
}
