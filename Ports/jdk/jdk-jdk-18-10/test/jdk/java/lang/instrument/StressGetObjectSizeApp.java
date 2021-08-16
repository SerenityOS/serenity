/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.Instrumentation;

public class StressGetObjectSizeApp
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for StressGetObjectSizeApp.
     * @param name
     */
    public StressGetObjectSizeApp(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new StressGetObjectSizeApp(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        stressGetObjectSize();
    }

    public void stressGetObjectSize() {
        System.out.println("main: an object size=" +
            fInst.getObjectSize(new Object()));

        RoundAndRound[] threads = new RoundAndRound[10];
        for (int i = 0; i < threads.length; ++i) {
            threads[i] = new RoundAndRound(fInst);
            threads[i].start();
        }
        try {
            Thread.sleep(500); // let all threads get going in their loops
        } catch (InterruptedException ie) {
        }
        System.out.println("stressGetObjectSize: returning");
        return;
    }

    private static class RoundAndRound extends Thread {
        private final Instrumentation inst;
        private final Object anObject;

        public RoundAndRound(Instrumentation inst) {
            this.inst = inst;
            this.anObject = new Object();
            setDaemon(true);
        }

        public void run() {
            long sum = 0;
            while (true) {
              sum += inst.getObjectSize(anObject);
            }
        }
    }
}
