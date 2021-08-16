/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.spi;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * An interface for command-line tools to provide a way to
 * be invoked without necessarily starting a new VM.
 *
 * <p>Tool providers are normally located using the service-provider
 * loading facility defined by {@link ServiceLoader}.
 * Each provider must provide a name, and a method to run
 * an instance of the corresponding tool. When a tool is run,
 * it will be provided with an array of string arguments, and a
 * pair of streams: one for normal (or expected) output and the other
 * for reporting any errors that may occur.
 * The interpretation of the string arguments will normally be defined by
 * each individual tool provider, but will generally correspond to the
 * arguments that could be provided to the tool when invoking the tool
 * from the command line.
 *
 * @since 9
 */
public interface ToolProvider {
    /**
     * Returns the name of this tool provider.
     *
     * @apiNote It is recommended that the name be the same as would be
     * used on the command line: for example, "javac", "jar", "jlink".
     *
     * @return the name of this tool provider
     */
    String name();

    /**
     * Runs an instance of the tool, returning zero for a successful run.
     * Any non-zero return value indicates a tool-specific error during the
     * execution.
     *
     * Two streams should be provided, for "expected" output, and for any
     * error messages. If it is not necessary to distinguish the output,
     * the same stream may be used for both.
     *
     * @apiNote The interpretation of the arguments will be specific to
     * each tool.
     *
     * @param out a stream to which "expected" output should be written
     *
     * @param err a stream to which any error messages should be written
     *
     * @param args the command-line arguments for the tool
     *
     * @return the result of executing the tool.
     *         A return value of 0 means the tool did not encounter any errors;
     *         any other value indicates that at least one error occurred
     *         during execution.
     *
     * @throws NullPointerException if any of the arguments are {@code null},
     *         or if there are any {@code null} values in the {@code args}
     *         array
     */
    int run(PrintWriter out, PrintWriter err, String... args);

    /**
     * Runs an instance of the tool, returning zero for a successful run.
     * Any non-zero return value indicates a tool-specific error during the
     * execution.
     *
     * Two streams should be provided, for "expected" output, and for any
     * error messages. If it is not necessary to distinguish the output,
     * the same stream may be used for both.
     *
     * @apiNote The interpretation of the arguments will be specific to
     * each tool.
     *
     * @implNote This implementation wraps the {@code out} and {@code err}
     * streams within {@link PrintWriter}s, and then calls
     * {@link #run(PrintWriter, PrintWriter, String[])}.
     *
     * @param out a stream to which "expected" output should be written
     *
     * @param err a stream to which any error messages should be written
     *
     * @param args the command-line arguments for the tool
     *
     * @return the result of executing the tool.
     *         A return value of 0 means the tool did not encounter any errors;
     *         any other value indicates that at least one error occurred
     *         during execution.
     *
     * @throws NullPointerException if any of the arguments are {@code null},
     *         or if there are any {@code null} values in the {@code args}
     *         array
     */
    default int run(PrintStream out, PrintStream err, String... args) {
        Objects.requireNonNull(out);
        Objects.requireNonNull(err);
        Objects.requireNonNull(args);
        for (String arg : args) {
            Objects.requireNonNull(arg);
        }

        PrintWriter outWriter = new PrintWriter(out);
        PrintWriter errWriter = new PrintWriter(err);
        try {
            try {
                return run(outWriter, errWriter, args);
            } finally {
                outWriter.flush();
            }
        } finally {
            errWriter.flush();
        }
    }

    /**
     * Returns the first instance of a {@code ToolProvider} with the given name,
     * as loaded by {@link ServiceLoader} using the system class loader.
     *
     * @param name the name of the desired tool provider
     *
     * @return an {@code Optional<ToolProvider>} of the first instance found
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @SuppressWarnings("removal")
    static Optional<ToolProvider> findFirst(String name) {
        Objects.requireNonNull(name);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        return AccessController.doPrivileged(
            (PrivilegedAction<Optional<ToolProvider>>) () -> {
                ServiceLoader<ToolProvider> sl =
                    ServiceLoader.load(ToolProvider.class, systemClassLoader);
                return StreamSupport.stream(sl.spliterator(), false)
                    .filter(p -> p.name().equals(name))
                    .findFirst();
            });
    }
}

