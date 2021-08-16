/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4518797
 * @summary Make sure that hashCode() and read/writeObject() are thread-safe.
 * @run main Bug4518797 10
 */

import java.util.*;
import java.io.*;

// Usage: java Bug4518797 [duration]
public class Bug4518797 {
    static volatile boolean runrun = true;
    static volatile String message = null;

    public static void main(String[] args) {
        int duration = 180;
        if (args.length == 1) {
            duration = Math.max(5, Integer.parseInt(args[0]));
        }
        final Locale loc = new Locale("ja", "US");
        final int hashcode = loc.hashCode();

        System.out.println("correct hash code: " + hashcode);
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                while (runrun) {
                    int hc = loc.hashCode();
                    if (hc != hashcode) {
                        runrun = false;
                        message = "t1: wrong hashcode: " + hc;
                    }
                }
            }
          });

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                // Repeat serialization and deserialization. And get the
                // hash code from a deserialized Locale object.
                while (runrun) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(loc);
                        byte[] b = baos.toByteArray();
                        oos.close();
                        ByteArrayInputStream bais = new ByteArrayInputStream(b);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        Locale loc2 = (Locale) ois.readObject();
                        int hc = loc2.hashCode();
                        if (hc != hashcode) {
                            runrun = false;
                            message = "t2: wrong hashcode: " + hc;
                        }
                    } catch (IOException ioe) {
                        runrun = false;
                        throw new RuntimeException("t2: can't perform test", ioe);
                    } catch (ClassNotFoundException cnfe) {
                        runrun = false;
                        throw new RuntimeException("t2: can't perform test", cnfe);
                    }
                }
            }
          });

        t1.start();
        t2.start();
        try {
            for (int i = 0; runrun && i < duration; i++) {
                Thread.sleep(1000);
            }
            runrun = false;
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
        }
        if (message != null) {
            throw new RuntimeException(message);
        }
    }
}
