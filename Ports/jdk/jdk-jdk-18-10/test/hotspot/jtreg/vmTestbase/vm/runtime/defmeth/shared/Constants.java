/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import static java.lang.String.format;

/**
 * Set of constants which are used in the test suite.
 * The actual values can be changed through corresponding property.
 */
public class Constants {
    static public final boolean PRINT_ASSEMBLY    = getValue("vm.runtime.defmeth.printAssembly",          false);
    static public final boolean PRINT_TESTS       = getValue("vm.runtime.defmeth.printTests",             false);
    static public final boolean ASMIFY            = getValue("vm.runtime.defmeth.printASMify",            false);
    static public final boolean DUMP_CLASSES      = getValue("vm.runtime.defmeth.dumpClasses",            false);
    static public final boolean PRINT_STACK_TRACE = getValue("vm.runtime.defmeth.printStackTrace",        false);
    static public final boolean TRACE_CLASS_REDEF = getValue("vm.runtime.defmeth.traceClassRedefinition", false);

    /**
     * Get value of the test suite's property
     * Supported values:
     *   ""        == true
     *   "true"    == true
     *   "false"   == false
     *   undefined == default value
     */
    static boolean getValue(String property, boolean defaultValue) {
        String value = System.getProperty(property);

        if ("false".equals(value))  return false;
        if ("true".equals(value))   return true;
        if ("".equals(value))       return true;
        if (value == null )         return defaultValue;

        throw new IllegalArgumentException(format("Unknown value: -D%s=%s", property, value));
    }
}
