/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.awt.Rectangle;
import java.util.logging.*;

public class TesterClient {
    private static final Logger log = Logger.getLogger("test.xembed.TesterClient");
    private static Method test;
    private static boolean passed = false;
    public static void main(String[] args) throws Throwable {
        // First parameter is the name of the test, second is the window, the rest are rectangles
        Class cl = Class.forName("sun.awt.X11.XEmbedServerTester");

        test = cl.getMethod(args[0], new Class[0]);
        long window = Long.parseLong(args[1]);
        Rectangle r[] = new Rectangle[(args.length-2)/4];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Rectangle(Integer.parseInt(args[2+i*4]), Integer.parseInt(args[2+i*4+1]),
                                 Integer.parseInt(args[2+i*4+2]), Integer.parseInt(args[2+i*4+3]));
        }
        startClient(r, window);
    }

    public static void startClient(Rectangle bounds[], long window) throws Throwable {
        Method m_getTester = Class.forName("sun.awt.X11.XEmbedServerTester").
            getMethod("getTester", new Class[] {bounds.getClass(), Long.TYPE});
        final Object tester = m_getTester.invoke(null, new Object[] {bounds, window});
        try {
            log.info("Starting test " + test.getName());
            test.invoke(tester, (Object[])null);
            log.info("Test " + test.getName() + " PASSED.");
            passed = true;
        } catch (Exception e) {
            log.log(Level.WARNING, "Test " + test.getName() + " FAILED.", e);
        }
        System.exit(passed?0:1);
    }
}
