/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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

package MyPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** API for handling heap allocating threads. */
class ThreadInformation {
  private Thread thread;
  private Allocator allocator;

  public ThreadInformation(Thread thread, Allocator allocator) {
    this.thread = thread;
    this.allocator = allocator;
  }

  public void waitForJobDone() {
    allocator.waitForJobDone();
  }

  public void stop() {
    try {
      allocator.stopRun();
      thread.join();

      if (!allocator.endedNormally()) {
        throw new RuntimeException("Thread did not end normally...");
      }

    } catch(InterruptedException e) {
      throw new RuntimeException("Thread got interrupted...");
    }
  }

  private void start() {
    allocator.start();
  }

  public static void startThreads(List<ThreadInformation> threadList) {
    for (ThreadInformation info : threadList) {
      info.start();
    }
  }

  public static void stopThreads(List<ThreadInformation> threadList) {
    for (ThreadInformation info : threadList) {
      info.stop();
    }
  }

  public Thread getThread() {
    return thread;
  }

  public static void waitForThreads(List<ThreadInformation> threadList) {
    System.err.println("Waiting for threads to be done");
    // Wait until all threads have put an object in the queue.
    for (ThreadInformation info : threadList) {
      info.waitForJobDone();
    }
  }

  public static List<ThreadInformation> createThreadList(int numThreads) {
    List<ThreadInformation> threadList = new ArrayList<>();
    for (int i = 0 ; i < numThreads; i++) {
      Allocator allocator = new Allocator(i);
      Thread thread = new Thread(allocator, "Allocator" + i);

      ThreadInformation info = new ThreadInformation(thread, allocator);
      threadList.add(info);
      thread.start();
    }
    return threadList;
  }
}

class Allocator implements Runnable {
  private int depth;
  private List<int[]> currentList;
  private BlockingQueue<Object> jobCanStart;
  private BlockingQueue<Object> jobDone;
  private BlockingQueue<Object> jobCanStop;
  private boolean failed;

  public Allocator(int depth) {
    this.jobCanStart = new LinkedBlockingQueue<>();
    this.jobDone = new LinkedBlockingQueue<>();
    this.jobCanStop = new LinkedBlockingQueue<>();
    this.depth = depth;
  }

  public boolean endedNormally() {
    return !failed;
  }

  private void helper() {
    List<int[]> newList = new ArrayList<>();
    // Let us assume that the array is 40 bytes of memory, keep in
    // memory at least 1.7MB without counting the link-list itself, which adds to this.
    int iterations = (1 << 20) / 24;
    for (int i = 0; i < iterations; i++) {
      int newTmp[] = new int[5];
      // Force it to be kept.
      newList.add(newTmp);
    }

    // Replace old list with new list, which provokes two things:
    //  Old list will get GC'd at some point.
    //  New list forces that this thread has some allocations still sampled.
    currentList = newList;
  }

  private void recursiveWrapper(int depth) {
    if (depth > 0) {
      recursiveWrapper(depth - 1);
      return;
    }
    helper();
  }

  public void stopRun() throws InterruptedException {
    jobCanStop.put(new Object());
  }

  public void run() {
    String name = Thread.currentThread().getName();
    System.err.println("Going to run: " + name);
    // Wait till we are told to really start.
    waitForStart();

    System.err.println("Running: " + name);
    for (int j = 0; j < 100; j++) {
      recursiveWrapper(depth);
    }

    try {
      // Tell the main thread we are done.
      jobDone.put(new Object());

      System.err.println("Waiting for main: " + name);
      // Wait until the main thread says we can stop.
      jobCanStop.take();
      System.err.println("Waited for main: " + name);
    } catch (InterruptedException e) {
      failed = true;
    }
  }

  public void waitForJobDone() {
    try {
      jobDone.take();
    } catch(InterruptedException e) {
      throw new RuntimeException("Thread got interrupted...");
    }
  }

  public void waitForStart() {
    try {
      jobCanStart.take();
    } catch(InterruptedException e) {
      throw new RuntimeException("Thread got interrupted...");
    }
  }

  public void start() {
    try {
      jobCanStart.put(new Object());
    } catch(InterruptedException e) {
      throw new RuntimeException("Thread got interrupted...");
    }
  }
}
