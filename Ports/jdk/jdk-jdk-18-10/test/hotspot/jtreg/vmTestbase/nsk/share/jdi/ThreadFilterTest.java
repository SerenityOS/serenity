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
package nsk.share.jdi;

import com.sun.jdi.ThreadReference;
import nsk.share.Consts;
import nsk.share.TestBug;

import java.io.PrintStream;
import java.util.List;

/*
 * Class for testing thread filter, this filter is added by request's method
 * addThreadFilter(ThreadReference thread). Class test 3 follows cases:
 * - add filter for single thread
 * - add filter for the same thread 2 times, expect behavior such as in previous case
 * - add filter for 2 different threads, so events shouldn't be received
 */
public class ThreadFilterTest extends EventFilterTest {
    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new ThreadFilterTest().runIt(argv, out);
    }

    protected int getTestFiltersNumber() {
        return 3;
    }

    protected EventFilters.DebugEventFilter[] createTestFilters(int testedFilterIndex) {
        List<ThreadReference> threads = getEventThreads();

        if (threads.size() < 2) {
            throw new TestBug("Debuggee didn't create event generating threads");
        }

        switch (testedFilterIndex) {
            case 0:
                // filter for 1 thread
                return new EventFilters.DebugEventFilter[]{new EventFilters.ThreadFilter(threads.get(0))};
            case 1:
                // add 2 filters for the same thread
                return new EventFilters.DebugEventFilter[]{new EventFilters.ThreadFilter(threads.get(0)),
                        new EventFilters.ThreadFilter(threads.get(0))};
            case 2:
                // add 2 filters for 2 different threads, so don't expect any event
                return new EventFilters.DebugEventFilter[]{new EventFilters.ThreadFilter(threads.get(0)),
                        new EventFilters.ThreadFilter(threads.get(1))};
            default:
                throw new TestBug("Invalid testedFilterIndex: " + testedFilterIndex);
        }
    }
}
