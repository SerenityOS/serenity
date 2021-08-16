/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.*;

/*
 * @test
 * @bug 8010727
 * @summary  LogManager.addLogger should succeed to add a logger named ""
 *           if LogManager.getLogger("") returns null.
 *
 * @run main LogManagerInstanceTest
 */

public class LogManagerInstanceTest {
    public static void main(String[] argv) {
        LogManager mgr = LogManager.getLogManager();
        if (getRootLogger(mgr) == null) {
            throw new RuntimeException("Root logger not exist");
        }

        SecondLogManager mgr2 = new SecondLogManager();
        Logger root = getRootLogger(mgr2);
        if (mgr2.base != root) {
            throw new RuntimeException(mgr2.base + " is not the root logger");
        }
    }

    private static Logger getRootLogger(LogManager mgr) {
        Logger l = mgr.getLogger("");
        if (l != null && !l.getName().isEmpty()) {
            throw new RuntimeException(l.getName() + " is not an invalid root logger");
        }
        return l;
    }

    static class SecondLogManager extends LogManager {
        final Logger base;
        private SecondLogManager() {
            Logger root = getLogger("");
            if (root == null) {
                root = new BaseLogger("", null);
                if (!super.addLogger(root))
                    throw new RuntimeException("Fail to addLogger " + root);
            } else {
                throw new RuntimeException("Root logger already exists");
            }
            this.base = root;
        }
    }
    static class BaseLogger extends Logger {
        BaseLogger(String name, String rbname) {
            super(name, rbname);
        }
    }
}
