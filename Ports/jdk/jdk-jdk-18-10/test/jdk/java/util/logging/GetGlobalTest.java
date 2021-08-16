/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     6476146
 *
 * @summary Added convenience method for deprecated Logger.global
 * @author  Serguei Spitsyn
 *
 * @build GetGlobalTest
 * @run main GetGlobalTest
 */

/**
 * RFE ID:
 * 6476146: Add convenience method for deprecated Logger.global
 * This is simple unit test for the RFE.
 *
 * With the deprecation of Logger.global, there is no convenient way of
 * doing entry-level logging. Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
 * is not an adequate substitute for Logger.global.
 * Solution: A convenience method Logger.getGlobal() should be provided.
 */

import java.util.logging.*;

public class GetGlobalTest {
    static final java.io.PrintStream out = System.out;
    public static void main(String arg[]) {
        Logger glogger1 = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        Logger glogger2 = Logger.getGlobal();
        if (glogger1.equals(glogger2)) {
            out.println("Test passed");
        } else {
            out.println("Test FAILED");
        }
    }
}
