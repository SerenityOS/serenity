/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.apps.LingeredApp;


public class LingeredAppWithLock extends LingeredApp {

    public static void lockMethod(Object lock) {
        synchronized (lock) {
            try {
                Thread.sleep(300000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String args[]) {
        Thread classLock1 = new Thread(() -> lockMethod(LingeredAppWithLock.class));
        Thread classLock2 = new Thread(() -> lockMethod(LingeredAppWithLock.class));
        Thread objectLock = new Thread(() -> lockMethod(classLock1));
        Thread primitiveLock = new Thread(() -> lockMethod(int.class));

        classLock1.start();
        classLock2.start();
        objectLock.start();
        primitiveLock.start();

        LingeredApp.main(args);
    }
 }
