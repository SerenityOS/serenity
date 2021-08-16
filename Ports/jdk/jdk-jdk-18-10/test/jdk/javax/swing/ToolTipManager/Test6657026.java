/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6657026
 * @summary Tests shared ToolTipManager in different application contexts
 * @author Sergey Malenkov
 * @modules java.desktop/sun.awt
 */

import sun.awt.SunToolkit;
import javax.swing.ToolTipManager;

public class Test6657026 implements Runnable {

    private static final int DISMISS = 4000;
    private static final int INITIAL = 750;
    private static final int RESHOW = 500;

    public static void main(String[] args) throws InterruptedException {
        ToolTipManager manager = ToolTipManager.sharedInstance();
        if (DISMISS != manager.getDismissDelay()) {
            throw new Error("unexpected dismiss delay");
        }
        if (INITIAL != manager.getInitialDelay()) {
            throw new Error("unexpected initial delay");
        }
        if (RESHOW != manager.getReshowDelay()) {
            throw new Error("unexpected reshow delay");
        }
        manager.setDismissDelay(DISMISS + 1);
        manager.setInitialDelay(INITIAL + 1);
        manager.setReshowDelay(RESHOW + 1);

        ThreadGroup group = new ThreadGroup("$$$");
        Thread thread = new Thread(group, new Test6657026());
        thread.start();
        thread.join();
    }

    public void run() {
        SunToolkit.createNewAppContext();
        ToolTipManager manager = ToolTipManager.sharedInstance();
        if (DISMISS != manager.getDismissDelay()) {
            throw new Error("shared dismiss delay");
        }
        if (INITIAL != manager.getInitialDelay()) {
            throw new Error("shared initial delay");
        }
        if (RESHOW != manager.getReshowDelay()) {
            throw new Error("shared reshow delay");
        }
    }
}
