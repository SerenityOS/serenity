/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.jshell.tool;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.Tool;
import jdk.jshell.tool.JavaShellToolBuilder;

/**
 * Provider for launching the jshell tool.
 */
public class JShellToolProvider implements Tool {

    /**
     * Returns the name of this Java shell tool provider.
     *
     * @return the name of this tool provider
     */
    @Override
    public String name() {
        return "jshell";
    }

    /**
     * Run the jshell tool.  The streams {@code out} and {@code err} are
     * converted to {@code PrintStream} if they are not already.
     * Any {@code Exception} is caught, printed and results in a non-zero return.
     *
     * @param in command line input (snippets and commands), and execution
     * "standard" input; use System.in if null
     * @param out command line output, feedback including errors, and execution
     * "standard" output; use System.out if null
     * @param err start-up errors and execution "standard" error; use System.err
     * if null
     * @param arguments arguments to pass to the tool
     * @return the exit status with which the tool explicitly exited (if any),
     * otherwise 0 for success or 1 for failure
     * @throws NullPointerException if the array of arguments contains
     * any {@code null} elements.
     */
    @Override
    public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
        InputStream xin =
                (in == null)
                        ? System.in
                        : in;
        PrintStream xout =
                (out == null)
                        ? System.out
                        : (out instanceof PrintStream)
                                ? (PrintStream) out
                                : new PrintStream(out);
        PrintStream xerr =
                (err == null)
                        ? System.err
                        : (err instanceof PrintStream)
                                ? (PrintStream) err
                                : new PrintStream(err);
        try {
            return JavaShellToolBuilder
                    .builder()
                    .in(xin, null)
                    .out(xout)
                    .err(xerr)
                    .start(arguments);
        } catch (Throwable ex) {
            xerr.println(ex.getMessage());
            return 1;
        }
    }

    /**
     * Returns the source versions of the jshell tool.
     * @return a set of supported source versions
     */
    @Override
    public Set<SourceVersion> getSourceVersions() {
        return Collections.unmodifiableSet(
                EnumSet.range(SourceVersion.RELEASE_9, SourceVersion.latest()));
    }

    /**
     * Launch the tool and exit.
     * @param arguments the command-line arguments (including options), if any
     * @throws Exception an unexpected fatal exception
     */
    public static void main(String[] arguments) throws Exception {
        System.exit(
                JavaShellToolBuilder
                        .builder()
                        .start(arguments));
    }
}
