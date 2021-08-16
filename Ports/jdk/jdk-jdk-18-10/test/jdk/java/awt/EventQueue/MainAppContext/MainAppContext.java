/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004584
 * @summary Tests 8004584
 * @author anthony.petrov@oracle.com, petr.pchelko@oracle.com
 * @modules java.desktop/sun.awt
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import sun.awt.*;

public class MainAppContext {

    public static void main(String[] args) {
        ThreadGroup secondGroup = new ThreadGroup("test");
        new Thread(secondGroup, () -> {
            SunToolkit.createNewAppContext();
            test(true);
        }).start();

        // Sleep on the main thread so that the AWT Toolkit is initialized
        // in a user AppContext first
        try { Thread.sleep(2000); } catch (Exception e) {}

        test(false);
    }

    private static void test(boolean expectAppContext) {
        boolean appContextIsCreated = AppContext.getAppContext() != null;
        if (expectAppContext != appContextIsCreated) {
            throw new RuntimeException("AppContext is created: " + appContextIsCreated
                                                 + " expected: " + expectAppContext);
        }
    }
}
