/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.tools;

import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;
import javax.lang.model.SourceVersion;

/**
 * Common interface for tools that can be invoked from a program.
 * A tool is traditionally a command line program such as a compiler.
 * The set of tools available with a platform is defined by the
 * vendor.
 *
 * <p>Tools can be located using {@link
 * java.util.ServiceLoader#load(Class)}.
 *
 * @author Neal M Gafter
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface Tool {
    /**
     * Returns the name of this tool, or an empty string if no name is provided.
     *
     * @apiNote It is recommended that the name be the same as would be
     * used on the command line: for example, "javac", "jar", "jlink".
     * @implNote This implementation returns an empty string.
     *
     * @return the name of this tool
     * @since 9
     */
    default String name() {
        return "";
    }

    /**
     * Run the tool with the given I/O channels and arguments. By
     * convention a tool returns 0 for success and nonzero for errors.
     * Any diagnostics generated will be written to either {@code out}
     * or {@code err} in some unspecified format.
     *
     * @param in "standard" input; use System.in if null
     * @param out "standard" output; use System.out if null
     * @param err "standard" error; use System.err if null
     * @param arguments arguments to pass to the tool
     * @return 0 for success; nonzero otherwise
     * @throws NullPointerException if the array of arguments contains
     * any {@code null} elements.
     */
    int run(InputStream in, OutputStream out, OutputStream err, String... arguments);

    /**
     * Returns the source versions of the Java programming language
     * supported by this tool.
     * @return a set of supported source versions
     */
    Set<SourceVersion> getSourceVersions();

}
