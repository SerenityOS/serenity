/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8129116
  @summary Deadlock with multimonitor fullscreen windows.
  @run main/timeout=20 MultimonDeadlockTest
 */
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

public class MultimonDeadlockTest {

    public static void main(String argv[]) {
        final GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();
        if (devices.length < 2) {
            System.out.println("It's a multiscreen test... skipping!");
            return;
        }

        Frame frames[] = new Frame[devices.length];
        try {
            EventQueue.invokeAndWait(() -> {
                for (int i = 0; i < devices.length; i++) {
                    frames[i] = new Frame();
                    frames[i].setSize(100, 100);
                    frames[i].setBackground(Color.BLUE);
                    devices[i].setFullScreenWindow(frames[i]);
                }
            });
            Thread.sleep(5000);
        } catch (InterruptedException | InvocationTargetException ex) {
        } finally {
            for (int i = 0; i < devices.length; i++) {
                devices[i].setFullScreenWindow(null);
                frames[i].dispose();
            }
        }

    }
}
