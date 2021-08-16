/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8196322
 * @summary [macosx] When the screen menu bar is used, clearing the default menu bar should permit AWT shutdown
 * @author Alan Snyder
 * @run main/othervm DefaultMenuBarDispose
 * @requires (os.family == "mac")
 */

import java.awt.Desktop;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

public class DefaultMenuBarDispose
{
    public DefaultMenuBarDispose()
    {
        Thread watcher = new Thread(() -> {
            try {
                synchronized (this) {
                    wait(5000);
                }
                throw new RuntimeException("Test failed: failed to exit");
            } catch (InterruptedException ex) {
            }
        });
        watcher.setDaemon(true);
        watcher.start();

        runSwing(() ->
            {
                JMenuBar mb = new JMenuBar();
                Desktop.getDesktop().setDefaultMenuBar(mb);
                Desktop.getDesktop().setDefaultMenuBar(null);
            }
        );
    }

    private static void runSwing(Runnable r)
    {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");

        new DefaultMenuBarDispose();
    }
}
