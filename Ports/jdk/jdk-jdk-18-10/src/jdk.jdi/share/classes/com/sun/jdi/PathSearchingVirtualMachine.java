/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi;

import java.util.List;

/**
 * A virtual machine which searches for classes through paths
 *
 * @author Gordon Hirsch
 * @since  1.3
 */
public interface PathSearchingVirtualMachine extends VirtualMachine {

    /**
     * Get the class path for this virtual machine.
     *
     * @return {@link List} of components of the classpath,
     * each represented by a {@link String}.
     */
    List<String> classPath();

    /**
     * Get the boot class path for this virtual machine.
     *
     * @return {@link List} of components of the boot class path,
     * each represented by a {@link String}.
     */
    List<String> bootClassPath();

    /**
     * Get the base directory used for path searching. Relative directories
     * in the class path and boot class path can be resolved through
     * this directory name.
     *
     * @return the base directory.
     */
    String baseDirectory();
}
