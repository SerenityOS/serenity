/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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


// see Deadlock.sh

import java.security.*;

public class Deadlock implements Runnable {

    private volatile Exception exc;

    public void run() {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            System.out.println("getInstance() ok: " + random);
        } catch (Exception e) {
            System.out.println("Exception during getInstance() call: " + e);
            this.exc = e;
        }
    }

    public static void main(String[] args) throws Exception {
        Deadlock d = new Deadlock();
        Thread t = new Thread(d);
        t.start();
        String className = (args.length == 0) ? "com.abc.Tst1" : args[0];
        System.out.println("Loading class: " + className);
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        System.out.println("SystemClassLoader: " + cl);
        Class clazz = cl.loadClass(className);
        System.out.println("OK: " + clazz);
        t.join();
        if (d.exc != null) {
            throw d.exc;
        }
    }

}
