/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4470017
 * @summary Ensure that illegal arguments cause appropriate exceptions
 *          to be thrown
 */

import java.io.*;
import java.nio.channels.*;


public class Args {

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    static interface Thunk {
        public void run() throws Exception;
    }

    private static void tryCatch(Class ex, Thunk thunk) {
        boolean caught = false;
        try {
            thunk.run();
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass())) {
                caught = true;
                System.err.println("Thrown as expected: " + x);
            }
        }
        if (!caught)
            fail(ex.getName() + " not thrown");
    }

    public static void main(String[] args) throws Exception {

        File f = File.createTempFile("foo", null);
        f.deleteOnExit();
        final FileChannel fc = new RandomAccessFile(f, "rw").getChannel();

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.transferFrom(fc, -1, 1);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.transferFrom(fc, 0, -1);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.transferTo(-1, 1, fc);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.transferTo(0, -1, fc);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.map(FileChannel.MapMode.READ_ONLY, -1, 0);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.map(FileChannel.MapMode.READ_ONLY, 0, -1);
                }});

        tryCatch(IllegalArgumentException.class, new Thunk() {
                public void run() throws Exception {
                    fc.map(FileChannel.MapMode.READ_ONLY, 0,
                           (long)Integer.MAX_VALUE << 3);
                }});

        fc.close();
        f.delete();
    }

}
