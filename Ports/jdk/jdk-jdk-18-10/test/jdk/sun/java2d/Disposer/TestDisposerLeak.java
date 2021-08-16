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

import java.util.LinkedList;
import java.util.List;

import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

/**
 * @test
 * @bug 8129457
 * @summary Check Disposer disposes all objects without any memory leaks
 * @run main/othervm -mx128m TestDisposerLeak
 * @modules java.desktop/sun.java2d
 */
public final class TestDisposerLeak {

    private static final int COUNT = 30000;

    private static volatile boolean disposerComplete;

    private static volatile boolean readyForDispose;

    static int disposeCount = 0;

    public static void main(String[] args) throws Exception {
        TestDisposerLeak test = new TestDisposerLeak();
        test.testLeak();
        while (!disposerComplete) {}
        if (disposeCount != COUNT) {
            System.err.println("dispose called " + disposeCount);
            throw new RuntimeException("All objects are not disposed");
        }else {
            System.out.println("Test PASSED");
        }
    }

    public void testLeak() throws Exception {
        MyDisposerRec disposerRecord = new MyDisposerRec();
        for (int i = 0; i < 30000; i++) {
            Disposer.addObjectRecord(new Object(), disposerRecord);
        }
        generateOOME();
        readyForDispose = true;
        Disposer.addObjectRecord(new Object(), new EndRec());
        generateOOME();
    }

    class MyDisposerRec implements DisposerRecord, Disposer.PollDisposable {

        public void dispose() {
            while (!readyForDispose) {}
            disposeCount++;
            Disposer.pollRemove();
        }
    }

    class EndRec implements DisposerRecord, Disposer.PollDisposable {

        public void dispose() {
            disposerComplete = true;
        }
    }

    private static void generateOOME() throws Exception {
        List<Object> leak = new LinkedList<>();
        try {
            while (true) {
                leak.add(new byte[1024 * 1024]);
            }
        } catch (OutOfMemoryError e) {
        }
        // Give the GC a chance at that weakref in case of slow systems
        Thread.sleep(2000);
    }
}
