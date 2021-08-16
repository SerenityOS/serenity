/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler.jtreg;

// Stripped down version of jtreg internal class com.sun.javatest.regtest.config.OS
class OS {
    public final String family;

    private static OS current;

    public static OS current() {
        if (current == null) {
            String name = System.getProperty("os.name");
            current = new OS(name);
        }
        return current;
    }

    private OS(String name) {
        if (name.startsWith("AIX")) {
            family = "aix";
        } else if (name.startsWith("Linux")) {
            family = "linux";
        } else if (name.startsWith("Mac") || name.startsWith("Darwin")) {
            family = "mac";
        } else if (name.startsWith("Windows")) {
            family = "windows";
        } else {
            // use first word of name
            family = name.replaceFirst("^([^ ]+).*", "$1");
        }
    }
}


