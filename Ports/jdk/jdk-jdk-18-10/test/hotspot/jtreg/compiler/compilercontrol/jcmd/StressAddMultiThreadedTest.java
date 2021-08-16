/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8137167
 * @summary Tests jcmd to be able to add a lot of huge directive files with
 *          parallel executed jcmds until timeout has reached
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver compiler.compilercontrol.jcmd.StressAddMultiThreadedTest
 */

package compiler.compilercontrol.jcmd;

import jdk.test.lib.dcmd.PidJcmdExecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StressAddMultiThreadedTest extends StressAddJcmdBase {
    private static final int THREADS = Integer.getInteger(
            "compiler.compilercontrol.jcmd.StressAddMultiThreadedTest.threads",
            5);
    private volatile int commands = Integer.getInteger(
            "compiler.compilercontrol.jcmd.StressAddMultiThreadedTest.commands",
            20);
    private final BlockingQueue<Runnable> queue;
    private final ExecutorService executor;

    public StressAddMultiThreadedTest() {
        queue = new ArrayBlockingQueue<>(THREADS);
        executor = new ThreadPoolExecutor(THREADS, THREADS, 100,
                TimeUnit.MILLISECONDS, queue,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static void main(String[] args) {
        new StressAddMultiThreadedTest().test();
    }

    @Override
    protected boolean makeConnection(int pid) {
        String nextCommand = nextCommand();
        executor.submit(() -> new PidJcmdExecutor(String.valueOf(pid))
                .execute(nextCommand));
        return (--commands != 0);
    }

    @Override
    protected void finish() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new Error("Interrupted while awaiting for termination: " + e,
                    e);
        }
        executor.shutdownNow();
    }
}
