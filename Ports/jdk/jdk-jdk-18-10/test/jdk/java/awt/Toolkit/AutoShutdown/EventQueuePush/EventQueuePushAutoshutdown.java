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
  test
  @bug        8081485
  @summary    tests that a program terminates automatically after EventQueue.push()
  @author     Anton Nashatyrev : area=toolkit
*/

import java.awt.*;

public class EventQueuePushAutoshutdown implements Runnable {
    private volatile int status = 2;

    public EventQueuePushAutoshutdown() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this));
        Thread thread = new Thread() {
            @Override
            public void run() {
                status = 0;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    status = 1;
                    System.exit(status);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();

        System.setProperty("java.awt.headless", "true");
        final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        systemQueue.push(new EventQueue());
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                System.out.println("Activated EDT");
            }
        });
        System.out.println("After EDT activation");
    }

    public static void main(String[] args) throws Exception  {
        new EventQueuePushAutoshutdown();
    }

    @Override
    public void run() {
        Runtime.getRuntime().halt(status);
    }
}
