/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6487638 7041595
 * @summary Calling LogManager.addLogger() and Logger.getLogger() cause deadlock
 * @author  Serguei Spitsyn
 * @build LoggingDeadlock3
 * @run main/timeout=80 LoggingDeadlock3
 */

import java.io.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingDeadlock3 {
  static final int         ITER_CNT   = 50000;
  static final String      MSG_PASSED = "LoggingDeadlock3: passed";
  static final LogManager  logMgr     = LogManager.getLogManager();
  static final PrintStream out        = System.out;

  public static void main(String args[]) throws Exception {
    String tstSrc = System.getProperty("test.src");
    File   fname  = new File(tstSrc, "LoggingDeadlock3.props");
    String prop   = fname.getCanonicalPath();
    System.setProperty("java.util.logging.config.file", prop);
    logMgr.readConfiguration();

    Thread t1 = new Thread(new AddLogger());
    Thread t2 = new Thread(new GetLogger());
    t1.start(); t2.start();
    t1.join();  t2.join();
    out.println("\n" + MSG_PASSED);
  }

  public static class MyLogger extends Logger {
    protected MyLogger(String name) { super(name, null); }
  }

  public static class GetLogger implements Runnable {
    public void run() {
      for (int cnt = 0; cnt < ITER_CNT * 8; cnt++) {
        Logger.getLogger("com.sun.Hello"+cnt/10);
        if (cnt % 1000  == 0) out.print("1");
        if (cnt % 10000 == 0) out.println();
      }
    }
  }

  public static class AddLogger implements Runnable {
    public void run() {
      for (int cnt = 0; cnt < ITER_CNT; cnt++) {
        Logger addLogger = new MyLogger("com.sun.Hello"+cnt);
        logMgr.addLogger(addLogger);
        if (cnt % 100  == 0) out.print("2");
        if (cnt % 1000 == 0) out.println();
      }
    }
  }
}
