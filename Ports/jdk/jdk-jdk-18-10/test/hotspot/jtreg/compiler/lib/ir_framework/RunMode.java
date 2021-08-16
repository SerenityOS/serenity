/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework;

/**
 * The run mode for a <b>custom run test</b> specified in {@link Run#mode}.
 *
 * @see Run
 */
public enum RunMode {
    /**
     * Default mode: First warm up the run method (if a warm-up is done), then compile the associated {@link Test}
     * method and finally invoke the run method once more.
     */
    NORMAL,
    /**
     * Standalone mode: There is no warm-up and no compilation done by the framework. The run method is responsible to
     * trigger the compilation(s), especially in regard of possible {@link IR} annotations at the associated {@link Test}
     * method.
     */
    STANDALONE,
}
