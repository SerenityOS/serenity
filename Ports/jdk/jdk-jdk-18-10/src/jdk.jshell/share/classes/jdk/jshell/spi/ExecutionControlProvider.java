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

package jdk.jshell.spi;

import java.util.Collections;
import java.util.Map;

/**
 * The provider used by JShell to generate the execution engine needed to
 * evaluate Snippets.  Alternate execution engines can be created by
 * implementing this interface, then configuring JShell with the provider or
 * the providers name and parameter specifier.
 *
 * @author Robert Field
 * @since 9
 */
public interface ExecutionControlProvider {

    /**
     * The unique name of this {@code ExecutionControlProvider}.  The name must
     * be a sequence of characters from the Basic Multilingual Plane which are
     * {@link Character#isJavaIdentifierPart(char) }.
     *
     * @return the ExecutionControlProvider's name
     */
    String name();

    /**
     * Create and return the default parameter map for this
     * {@code ExecutionControlProvider}. The map can optionally be modified;
     * Modified or unmodified it can be passed to
     * {@link #generate(jdk.jshell.spi.ExecutionEnv, java.util.Map) }.
     *
     * @return the default parameter map
     */
    default Map<String,String> defaultParameters() {
        return Collections.emptyMap();
    }

    /**
     * Create and return the {@code ExecutionControl} instance.
     *
     * @param env the execution environment, provided by JShell
     * @param parameters the {@linkplain #defaultParameters() default} or
     * modified parameter map.
     * @return the execution engine
     * @throws Throwable an exception that occurred attempting to create the
     * execution engine.
     */
    ExecutionControl generate(ExecutionEnv env, Map<String,String> parameters)
            throws Throwable;
}
