/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.io.*;

/**
 * This class can be used to set timeout for test execution.
 */
public class TimeoutHandler {

    /**
     * Test execution timeout in minutes.
     */
    private int waitTime;

    /**
     * Make new <code>TimeoutHandler</code> object for timeout value
     * specified in command line arguments.
     */
    public TimeoutHandler(ArgumentParser argumentHandler) {
        this.waitTime = argumentHandler.getWaitTime();
    }

    /**
     * Perform test execution in separate thread and wait for
     * thread finishes or timeout exceeds.
     */
    public void runTest(Thread testThread) {
        long millisec = waitTime * 60 * 1000;
        testThread.start();
        try {
            testThread.join(millisec);
        } catch (InterruptedException ex) {
            throw new Failure(ex);
        }
    }

}
