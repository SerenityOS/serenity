/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.System.Logger;
import java.util.ResourceBundle;

/*
 * BootUsage is put to Xbootclasspath, it will be used by
 * BootClient to test when logger client is in boot classpath
 */
public final class BootUsage {

    public static Logger getLogger(String name) {
        check();
        return System.getLogger(name);
    }

    public static Logger getLogger(String name, ResourceBundle rb) {
        check();
        return System.getLogger(name, rb);
    }

    private static void check() {
        final Module m = BootUsage.class.getModule();
        final ClassLoader moduleCL = m.getClassLoader();
        assertTrue(!m.isNamed());
        assertTrue(moduleCL == null);
    }

    private static void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("expected true, but get false.");
        }
    }
}
