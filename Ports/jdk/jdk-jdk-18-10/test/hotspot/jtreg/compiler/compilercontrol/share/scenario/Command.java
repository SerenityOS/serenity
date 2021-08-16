/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.scenario;

import compiler.compilercontrol.share.processors.LogProcessor;

import java.util.Arrays;

/**
 * Represents a CompileCommand command set
 */
public enum Command {
    COMPILEONLY("compileonly", ".*", "-Xbatch"),
    EXCLUDE("exclude", "", "-Xbatch"),
    INLINE("inline", ".*", "-Xbatch", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:InlineSmallCode=4000"),
    DONTINLINE("dontinline", "", "-Xbatch", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:InlineSmallCode=4000"),
    LOG("log", "", "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+LogCompilation", "-XX:LogFile=" + LogProcessor.LOG_FILE),
    PRINT("print", ""),
    QUIET("quiet", ""),
    INTRINSIC("ControlIntrinsic", ""),
    NONEXISTENT("nonexistent", ""); // wrong command for a negative case

    /**
     * Command name
     */
    public final String name;

    /**
     * Base regular expression
     */
    public final String regex;

    /**
     * Additional VM options for this command
     */
    public final String[] vmOpts;

    private Command(String name, String regex, String... vmOptions) {
        this.name = name;
        this.regex = regex;
        String[] wbOpts = new String[] { "-Xbootclasspath/a:.",
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI" };
        int length = (vmOptions != null) ? vmOptions.length : 0;
        this.vmOpts = Arrays.copyOf(wbOpts, wbOpts.length + length);
        if (vmOptions != null) {
            System.arraycopy(vmOptions, 0, vmOpts, wbOpts.length, length);
        }
    }
}
