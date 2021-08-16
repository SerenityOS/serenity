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
 */

/**
 * @test
 * @summary Stress test that reaches the process limit for thread count, or time limit.
 * @key stress
 * @run main ThreadCountLimit
 */

import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;

public class ThreadCountLimit {

  static final int TIME_LIMIT_MS = 5000; // Create as many threads as possible in 5 sec

  static class Worker extends Thread {
    private final int index;
    private final CountDownLatch startSignal;

    Worker(int index, CountDownLatch startSignal) {
      this.index = index;
      this.startSignal = startSignal;
    }

    @Override
    public void run() {
      if ((index % 250) == 0) {
        System.out.println("INFO: thread " + index + " waiting to start");
      }

      try {
        startSignal.await();
      } catch (InterruptedException e) {
        throw new Error("Unexpected: " + e);
      }

      setName(String.valueOf(index));

      Thread.yield();

      if (index != Integer.parseInt(getName())) {
        throw new Error("setName/getName failed!");
      }

      if ((index % 250) == 0) {
        System.out.println("INFO: thread " + getName() + " working");
      }
    }
  }

  public static void main(String[] args) {
    CountDownLatch startSignal = new CountDownLatch(1);
    ArrayList<Worker> workers = new ArrayList<Worker>();

    int count = 1;
    long start = System.currentTimeMillis();
    try {
      while (true) {
        Worker w = new Worker(count, startSignal);
        w.start();
        workers.add(w);
        count++;

        long end = System.currentTimeMillis();
        if ((end - start) > TIME_LIMIT_MS) {
          // Windows path or a system with very large ulimit
          System.out.println("INFO: reached the time limit " + TIME_LIMIT_MS + " ms, with " + count + " threads created");
          break;
        }
      }
    } catch (OutOfMemoryError e) {
      if (e.getMessage().contains("unable to create native thread")) {
        // Linux, macOS path
        long end = System.currentTimeMillis();
        System.out.println("INFO: reached this process thread count limit at " + count + " [" + (end - start) + " ms]");
      } else {
        throw e;
      }
    }

    startSignal.countDown();

    try {
      for (Worker w : workers) {
        w.join();
      }
    } catch (InterruptedException e) {
      throw new Error("Unexpected: " + e);
    }
  }
}
