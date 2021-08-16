/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jvmti;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * Base class for debuggee class in JVMTI tests.
 *
 * <p>This class provides method checkStatus(int) which is used for
 * synchronization between agent and debuggee class.</p>
 *
 * @see #checkStatus(int)
 */
public class DebugeeClass {
    /**
     * This method is used for synchronization status between agent and debuggee class.
     */
    public synchronized static native int checkStatus(int status);

    /**
     * Reset agent data to prepare for another run.
     */
    public synchronized static native void resetAgentData();

    /**
     * This method is used to load library with native methods implementation, if needed.
     */
    public static void loadLibrary(String name) {
        try {
            System.loadLibrary(name);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("# ERROR: Could not load native library: " + name);
            System.err.println("# java.library.path: "
                                + System.getProperty("java.library.path"));
            throw e;
        }
    }

    public static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new Failure(e.getMessage());
        }
    }

}
