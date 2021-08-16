/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.c1;

import java.io.IOException;
import java.io.InterruptedIOException;

/*
 * @test
 * @author Chris Cole
 * @bug 8267042
 * @summary missing displaced_header initialization causes hangup
 * @run main/othervm -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                   -XX:-BackgroundCompilation -XX:CompileThreshold=1
 *                   -XX:CompileOnly=compiler.c1.Test8267042::write
 *                   compiler.c1.Test8267042
 */
public class Test8267042 {

    private static int DATA_SIZE = 4;

    private char buffer;
    private boolean empty = true;

    public static void main(String[] args) {
        Test8267042 test = new Test8267042();
        test.run();
    }

    private void run() {
        System.out.println("Starting test");

        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                char data[] = new char[DATA_SIZE];
                try {
                    write(data, 0, data.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        writeThread.setDaemon(true);
        writeThread.start();

        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < DATA_SIZE; i++) {
                        read();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();

        try {
            writeThread.join(5000);
            if (writeThread.isAlive()) {
                throw new InternalError("write thread deadlocked");
            }
            readThread.join(5000);
            if (readThread.isAlive()) {
                throw new InternalError("read thread deadlocked");
            }
        } catch (InterruptedException e) {
            throw new InternalError("unexpected InterrruptedException while waiting to join threads", e);
        }
        System.out.println("Test passed");
    }

    synchronized void write(char data[], int offset, int length) throws IOException {
        while (--length >= 0) {
            getZeroOnStack(offset);
            write(data[offset++]);
        }
    }

    synchronized void write(int c) throws IOException {
        while (!empty) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }
        buffer = (char) c;
        empty = false;
        notifyAll();
    }

    public synchronized int read() throws IOException {
        while (empty) {
            try {
                System.out.println("read() before wait");
                wait(1000);
                System.out.println("read() after wait");
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }
        int value = buffer;
        empty = true;
        notifyAll();
        return value;
    }

    private void getZeroOnStack(int offset) {
        int l1;
        int l2;
        int l3;
        int l4;
        int l5;
        int l6;
        int l7;
        int l8;
        int l9;
        int l10;
        int l11;
        int l12;
        int l13;
        int l14;
        int l15;
        int l16;

        l1 = 0;
        l2 = 0;
        l3 = 0;
        l4 = 0;
        l5 = 0;
        l6 = 0;
        l7 = 0;
        l8 = 0;
        l9 = 0;
        l10 = 0;
        l11 = 0;
        l12 = 0;
        l13 = 0;
        l14 = 0;
        l15 = 0;
        l16 = 0;
    }
}

