/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class ParallelLambdaLoad {
    public static int NUM_THREADS = 10;
    public static int MAX_CLASSES = 10;
    public static AtomicInteger num_ready[];
    public static boolean isRuntime;

    public static void main(String args[]) throws Throwable {
        isRuntime = (args.length == 1 && args[0].equals("run")) ? true : false;
        num_ready = new AtomicInteger[MAX_CLASSES];
        for (int i = 0; i < MAX_CLASSES; i++) {
            num_ready[i] = new AtomicInteger();
        }

        ArrayList<Thread> list = new ArrayList<>();

        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(0);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
          }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(1);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(2);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(3);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(4);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(5);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(6);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(7);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(8);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));
        list.add(new Thread(() -> {
               DoSomething ds = new DoSomething(9);
               {ThreadUtil.WaitForLock(0); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(1); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(2); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(3); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(4); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(5); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(6); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(7); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(8); Runnable runner = ds::run; runner.run();}
               {ThreadUtil.WaitForLock(9); Runnable runner = ds::run; runner.run();}
           }));


        for (Thread t : list) {
            t.start();
        }

        for (Thread t : list) {
            t.join();
        }

        synchronized(map) {
            System.out.println("map size " + map.size());
            int expectedSize = NUM_THREADS * MAX_CLASSES;
            if (map.size() != expectedSize) {
                throw new RuntimeException("Expected number of lamdba classes is " +
                    expectedSize + " but got only " + map.size());
            }
        }
    }

    static HashMap<Class<?>, Class<?>> map = new HashMap<>();
}

class DoSomething {
    DoSomething(int dummy) {}

    public void run() {
        Class<?> c = LambdaVerification.getCallerClass(1);
        synchronized(ParallelLambdaLoad.map) {
            ParallelLambdaLoad.map.put(c, c);
        }
        LambdaVerification.verifyCallerIsArchivedLambda(ParallelLambdaLoad.isRuntime);
    }
}

class ThreadUtil {
    static void WaitForLock(int i) {
        // Spin until every thread is ready to proceed
        ParallelLambdaLoad.num_ready[i].incrementAndGet();
        while (ParallelLambdaLoad.num_ready[i].intValue() < ParallelLambdaLoad.NUM_THREADS) {
            ;
        }
    }
}
