/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.IllegalComponentStateException;

public class Flag {

    public static final int ATTEMPTS = 50;

    private volatile boolean flag;
    private final Object monitor = new Object();
    private final long delay;

    public Flag() {
        this.delay = 500;
    }

    public void reset() {
        flag = false;
    }

    public void flagTriggered() {
        synchronized (monitor) {
            flag = true;
            monitor.notifyAll();
        }
    }

    public boolean flag() {
        return flag;
    }

    public void waitForFlagTriggered() throws InterruptedException {
        waitForFlagTriggered(delay, ATTEMPTS);
    }

    public void waitForFlagTriggered(int attempts) throws InterruptedException {
        waitForFlagTriggered(delay, attempts);
    }

    public void waitForFlagTriggered(long delay) throws InterruptedException {
        waitForFlagTriggered(delay, ATTEMPTS);
    }

    private void waitForFlagTriggered(long delay, int attempts) throws InterruptedException {
        int a = 0;
        synchronized (monitor) {
            while (!flag && (a++ < attempts)) {
                monitor.wait(delay);
            }
        }
    }

    public static void waitTillShown(final Component comp) throws InterruptedException {
        while (true) {
            try {
                Thread.sleep(100);
                comp.getLocationOnScreen();
                break;
            } catch (IllegalComponentStateException e) {}
        }
    }
}
