/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.io.PrintStream;
import sun.tools.jconsole.Messages;

public class Version {
    private static final String jconsole_version =
        System.getProperty("java.runtime.version");

    public static void print(PrintStream ps) {
        printFullVersion(ps);

        ps.println(Resources.format(Messages.NAME_AND_BUILD,
                                    System.getProperty("java.runtime.name"),
                                    System.getProperty("java.runtime.version")));

        ps.println(Resources.format(Messages.NAME_AND_BUILD,
                                    System.getProperty("java.vm.name"),
                                    System.getProperty("java.vm.version"),
                                    System.getProperty("java.vm.info")));

    }

    public static void printFullVersion(PrintStream ps) {
        ps.println(Resources.format(Messages.JCONSOLE_VERSION, jconsole_version));
    }

    static String getVersion() {
        return jconsole_version;
    }
}
