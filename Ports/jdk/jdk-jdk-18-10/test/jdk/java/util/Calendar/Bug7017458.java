/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7017458
 * @summary Test of multithreaded serialization/deserialization of Calendar.
 */

import java.io.*;
import java.util.Calendar;

public class Bug7017458 {

    static volatile boolean err = false;

    public static void main(String[] args) {
        try {
            new Bug7017458().perform();
        }
        catch (Exception e) {
            e.printStackTrace();
            err = true;
        }

        if (err) {
            throw new RuntimeException("Multithreaded serialization/deserialization test failed.");
        } else {
            System.out.println("Multithreaded serialization/deserialization test passed.");
        }
    }

    public void perform() throws Exception {
        int nbThreads = 8;
        Calendar cal = Calendar.getInstance();
        SerializationThread[] threads = new SerializationThread[nbThreads];
        for (int i = 0; i < nbThreads; i++) {
            threads[i] = new SerializationThread(cal);
        }
        for (int i = 0; i < nbThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < nbThreads; i++) {
            threads[i].join();
        }

        DeserializationThread[] threads2 = new DeserializationThread[nbThreads];
        for (int i = 0; i < nbThreads; i++) {
            threads2[i] = new DeserializationThread(threads[i].data);
        }
        for (int i = 0; i < nbThreads; i++) {
            threads2[i].start();
        }
        for (int i = 0; i < nbThreads; i++) {
            threads2[i].join();
        }
    }

    public class SerializationThread extends Thread {
        private Calendar cal;
        public byte[] data;

        public SerializationThread(Calendar cal) {
            this.cal = cal;
        }

        public void run() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(cal);
                oos.flush();
                oos.close();
                data = baos.toByteArray();
                }
            catch (Exception e) {
                e.printStackTrace();
                err = true;
            }
        }
    }

    public class DeserializationThread extends Thread {
        public Calendar cal;
        public byte[] data;

        public DeserializationThread(byte[] data) {
            this.data = data;
        }

        public void run() {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                cal = (Calendar) ois.readObject();
                ois.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                err = true;
            }
        }
    }
}

