/*
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8264760
 * @summary JVM crashes when two threads encounter the same resolution error
 *
 * @compile HostNoNestMember.java
 * @compile HostNoNestMember.jcod
 *
 * @run main TestNestHostErrorWithMultiThread
 */

// HostNoNestMember.jcod must be compiled after HostNoNestMember.java
// because the class file from the jcod file must replace the
// HostNoNestMember class file generated from HostNoNestMember.java.

import java.util.concurrent.CountDownLatch;

public class TestNestHostErrorWithMultiThread {

  public static void main(String args[]) {
    CountDownLatch runLatch = new CountDownLatch(1);
    CountDownLatch startLatch = new CountDownLatch(2);

    Runnable test = new Test(runLatch, startLatch);

    Thread t1 = new Thread(test);
    Thread t2 = new Thread(test);

    t1.start();
    t2.start();

    try {
      // waiting thread creation
      startLatch.await();
      runLatch.countDown();

      t1.join();
      t2.join();
    } catch (InterruptedException e) {
      throw new Error("Unexpected interrupt");
    }
  }

  static class Test implements Runnable {
    private CountDownLatch runLatch;
    private CountDownLatch startLatch;

    Test(CountDownLatch runLatch, CountDownLatch startLatch) {
      this.runLatch = runLatch;
      this.startLatch = startLatch;
    }

    @Override
    public void run() {
      try {
        startLatch.countDown();
        // Try to have all threads trigger the nesthost check at the same time
        runLatch.await();
        HostNoNestMember h = new HostNoNestMember();
        h.test();
        throw new Error("IllegalAccessError was not thrown as expected");
      } catch (IllegalAccessError expected) {
        String msg = "current type is not listed as a nest member";
        if (!expected.getMessage().contains(msg)) {
          throw new Error("Wrong " + expected.getClass().getSimpleName() +": \"" +
                          expected.getMessage() + "\" does not contain \"" +
                          msg + "\"", expected);
        }
        System.out.println("OK - got expected exception: " + expected);
      } catch (InterruptedException e) {
        throw new Error("Unexpected interrupt");
      }
    }
  }
}
