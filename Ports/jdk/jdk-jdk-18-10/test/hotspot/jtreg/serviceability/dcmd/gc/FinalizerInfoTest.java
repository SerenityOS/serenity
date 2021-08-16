/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

/*
 * @test
 * @summary
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.xml
 *          java.management
 * @run testng FinalizerInfoTest
 */
public class FinalizerInfoTest {
    static ReentrantLock lock = new ReentrantLock();
    static volatile int wasInitialized = 0;
    static volatile int wasTrapped = 0;
    static final String cmd = "GC.finalizer_info";
    static final int objectsCount = 1000;

    class MyObject {
        public MyObject() {
            // Make sure object allocation/deallocation is not optimized out
            wasInitialized += 1;
        }

        protected void finalize() {
            // Trap the object in a finalization queue
            wasTrapped += 1;
            lock.lock();
        }
    }

    public void run(CommandExecutor executor) {
        try {
            lock.lock();
            for(int i = 0; i < objectsCount; ++i) {
                new MyObject();
            }
            System.out.println("Objects initialized: " + objectsCount);
            System.gc();

            while(wasTrapped < 1) {
                // Waiting for gc thread.
            }

            OutputAnalyzer output = executor.execute(cmd);
            output.shouldContain("MyObject");
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void pid() {
        run(new PidJcmdExecutor());
    }
}
