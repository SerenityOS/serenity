/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

/**
 * This class is generated/overridden at link time to return the names of the
 * SystemModules classes generated at link time.
 *
 * @see SystemModuleFinders
 * @see jdk.tools.jlink.internal.plugins.SystemModulesPlugin
 */

class SystemModulesMap {

    /**
     * Returns the SystemModules object to reconstitute all modules or null
     * if this is an exploded build.
     */
    static SystemModules allSystemModules() {
        return null;
    }

    /**
     * Returns the SystemModules object to reconstitute default modules or null
     * if this is an exploded build.
     */
    static SystemModules defaultSystemModules() {
        return null;
    }

    /**
     * Returns the array of initial module names identified at link time.
     */
    static String[] moduleNames() {
        return new String[0];
    }

    /**
     * Returns the array of SystemModules class names. The elements
     * correspond to the elements in the array returned by moduleNames().
     */
    static String[] classNames() {
        return new String[0];
    }
}