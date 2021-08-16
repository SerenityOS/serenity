/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.util;

import java.util.ServiceLoader;
import javax.tools.StandardLocation;

/**
 * The interface for a javac plug-in.
 *
 * <p>The javac plug-in mechanism allows a user to specify one or more plug-ins
 * on the javac command line, to be started soon after the compilation
 * has begun. Plug-ins are identified by a user-friendly name. Each plug-in that
 * is started will be passed an array of strings, which may be used to
 * provide the plug-in with values for any desired options or other arguments.
 *
 * <p>Plug-ins are located via a {@link ServiceLoader},
 * using the same class path as annotation processors (i.e.
 * {@link StandardLocation#ANNOTATION_PROCESSOR_PATH ANNOTATION_PROCESSOR_PATH} or
 * {@code -processorpath}).
 *
 * <p>It is expected that a typical plug-in will simply register a
 * {@link TaskListener} to be informed of events during the execution
 * of the compilation, and that the rest of the work will be done
 * by the task listener.
 *
 * @since 1.8
 */
public interface Plugin {
    /**
     * Returns the user-friendly name of this plug-in.
     * @return the user-friendly name of the plug-in
     */
    String getName();

    /**
     * Initializes the plug-in for a given compilation task.
     * @param task The compilation task that has just been started
     * @param args Arguments, if any, for the plug-in
     */
    void init(JavacTask task, String... args);

    /**
     * Returns whether or not this plugin should be automatically started,
     * even if not explicitly specified in the command-line options.
     *
     * <p>This method will be called by javac for all plugins located by the
     * service loader. If the method returns {@code true}, the plugin will be
     * {@link #init(JavacTask,String[]) initialized} with an empty array of
     * string arguments if it is not otherwise initialized due to an explicit
     * command-line option.
     *
     * @return whether or not this plugin should be automatically started
     */
    default boolean autoStart() {
        return false;
    }
}
