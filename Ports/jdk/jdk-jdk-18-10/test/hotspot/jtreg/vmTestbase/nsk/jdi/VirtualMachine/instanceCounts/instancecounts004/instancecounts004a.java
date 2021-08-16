/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.VirtualMachine.instanceCounts.instancecounts004;

import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jdi.*;

import java.io.*;
import java.util.*;

/*
 *  This debuggee class handle requests for start/stop thread which provokes GC
 */
public class instancecounts004a extends HeapwalkingDebuggee {
    static public final String COMMAND_START_GC_PROVOCATEUR = "startGCProvokateur";

    static public final String COMMAND_STOP_GC_PROVOCATEUR = "stopGCProvokateur";

    static public final String COMMAND_CONSUME_MEMORY = "consumeMemory";

    protected GCProvokateur provokateur;

    class GCProvokateur extends Thread {
        public volatile boolean stop;

        Collection<byte[]> garbage;

        public void run() {
            while (!stop) {
                try {
                    while (!stop) {
                        long memory = Runtime.getRuntime().freeMemory() / 100;

                        int arraySize;

                        if (memory > Integer.MAX_VALUE)
                            arraySize = Integer.MAX_VALUE;
                        else
                            arraySize = (int) memory;

                        garbage = new ArrayList<byte[]>();

                        for (int i = 0; i < 50; i++)
                            garbage.add(new byte[arraySize]);

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                        }
                    }
                } catch (OutOfMemoryError ignoreError) {
                }
            }
        }
    }

    // create large list to increase time needed for VirtualMachine.instanceCount
    ArrayList<Object> garbageList = new ArrayList<Object>();

    protected String[] doInit(String args[]) {
        args = super.doInit(args);

        for (int i = 0; i < 250000; i++) {
            TestClass1 t1 = new TestClass1();
            TestClass2 t2 = new TestClass2();
            garbageList.add(t1);
            garbageList.add(t2);
        }
        return args;
    }

    protected void consumeMemory(double consumedPart) {
        Collection<Object> garbage = new ArrayList<Object>();

        log.display("consumeMemory: " + consumedPart);

        if ((consumedPart > 1.0) || (consumedPart < 0)) {
            throw new TestBug("Invalid value 'consumedPart'=" + consumedPart + " in consumeMemory, sholud be in [0..1]");
        }

        garbage = new ArrayList<Object>();

        long freeSize = (long) (Runtime.getRuntime().totalMemory() * (1 - consumedPart));

        int arraySize = (int) (freeSize / 100F);
        if (arraySize < 1000)
            arraySize = 1000;

        while (Runtime.getRuntime().freeMemory() > freeSize) {
            garbage.add(new byte[arraySize]);
        }
    }

    protected void stopGCProvokateur() {
        if (provokateur != null) {
            provokateur.stop = true;

            try {
                provokateur.join();
            } catch (InterruptedException e) {
                log.complain("Main thread was unexpected interrupted when waiting for GCProvokateur termination");
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
            }
        }
    }

    protected void startGCProvokateur() {
        stopGCProvokateur();

        provokateur = new GCProvokateur();
        provokateur.start();
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
            tokenizer.whitespaceChars(':', ':');

            if (command.equals(COMMAND_START_GC_PROVOCATEUR)) {
                startGCProvokateur();

                return true;
            } else if (command.equals(COMMAND_STOP_GC_PROVOCATEUR)) {
                stopGCProvokateur();

                return true;
            } else if (command.startsWith(COMMAND_CONSUME_MEMORY)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format");

                consumeMemory(tokenizer.nval);

                return true;
            }

        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    public static void main(String args[]) {
        new instancecounts004a().doTest(args);
    }
}
