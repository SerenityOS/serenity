/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8037776 8167288
  @summary tests that the WarningWindow is properly disposed
  @library ../../regtesthelpers/process
  @build ProcessResults ProcessCommunicator
  @run main WarningWindowDisposeTest
*/

import java.awt.*;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.Permission;
import java.io.File;

import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;

public class WarningWindowDisposeTest {

    public static void main(String[] args) {
        final AtomicBoolean passed = new AtomicBoolean(false);
        new Thread(() -> {
            for (int trial = 0; trial < 5; ++trial) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Test FAILED!", e);
                }
                if (passed.get()) {
                    break;
                } else if (trial == 4) {
                    throw new RuntimeException("Child process never exits");
                }
            }
        }, "TimeoutThread").start();

        String classpath = System.getProperty("java.class.path");
        String policyPath = System.getProperty("test.src")+File.separatorChar+"policy";
        System.out.println("policyPath in main: "+policyPath);
        ProcessResults pres = ProcessCommunicator.executeChildProcess(TestApplication.class, classpath+" -Djava.security.manager -Djava.security.policy="+policyPath, new String[0]);
        passed.set(true);
        if (pres.getStdErr() != null && pres.getStdErr().length() > 0) {
            System.err.println("========= Child VM System.err ========");
            System.err.print(pres.getStdErr());
            System.err.println("======================================");
        }

        if (pres.getStdOut() != null && pres.getStdOut().length() > 0) {
            System.err.println("========= Child VM System.out ========");
            System.err.print(pres.getStdOut());
            System.err.println("======================================");
        }
    }

    public static class TestApplication {
        public static void main(String[] args) throws Exception {
            Robot robot;
            try{
                robot = new Robot();
            }catch(Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Cannot create Robot");
            }
            Frame f = new Frame("Test frame");
            f.setVisible(true);
            robot.waitForIdle();
            Thread.sleep(500);
            f.setVisible(false);
            f.dispose();
        }
    }
}
