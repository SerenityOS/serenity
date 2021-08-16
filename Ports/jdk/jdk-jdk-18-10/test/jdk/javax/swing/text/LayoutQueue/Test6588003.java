/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6588003
   @summary LayoutQueue should not share its DefaultQueue across AppContexts
   @author Peter Zhelezniakov
   @modules java.desktop/sun.awt
   @run main Test6588003
*/

import javax.swing.text.LayoutQueue;
import sun.awt.SunToolkit;

public class Test6588003 implements Runnable {
    private static final LayoutQueue DEFAULT = new LayoutQueue();

    public static void main(String[] args) throws InterruptedException {
        LayoutQueue.setDefaultQueue(DEFAULT);

        ThreadGroup group = new ThreadGroup("Test6588003");
        Thread thread = new Thread(group, new Test6588003());
        thread.start();
        thread.join();

        if (LayoutQueue.getDefaultQueue() != DEFAULT) {
            throw new RuntimeException("Sharing detected");
        }
    }

    public void run() {
        SunToolkit.createNewAppContext();

        if (LayoutQueue.getDefaultQueue() == DEFAULT) {
            throw new RuntimeException("Sharing detected");
        }

        LayoutQueue.setDefaultQueue(new LayoutQueue());
    }
}
