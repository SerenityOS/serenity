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

import com.sun.jdi.ObjectReference;
import nsk.share.Consts;
import nsk.share.TestBug;

import java.io.PrintStream;
import java.util.List;

/*
 * Class for testing instance filter, this filter is added by request's
 * method addInstanceFilter(ObjectReference instance). Class tests 3 following
 * cases:
 * - add filter for single object
 * - add filter for the same object 2 times, expect behavior such as in previous case
 * - add filter for 2 different objects, so events shouldn't be received
 */
public class InstanceFilterTest extends EventFilterTest {
    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new InstanceFilterTest().runIt(argv, out);
    }

    protected int getTestFiltersNumber() {
        return 3;
    }

    protected EventFilters.DebugEventFilter[] createTestFilters(int testedFilterIndex) {
        List<ObjectReference> objects = getEventObjects();

        if (objects.size() < 2) {
            throw new TestBug("Debuggee didn't create event generating objects");
        }

        switch (testedFilterIndex) {
            case 0:
                // filter for 1 object
                return new EventFilters.DebugEventFilter[]{new EventFilters.ObjectReferenceFilter(objects.get(0))};
            case 1:
                // add 2 filters for the same object
                return new EventFilters.DebugEventFilter[]{new EventFilters.ObjectReferenceFilter(objects.get(0)),
                        new EventFilters.ObjectReferenceFilter(objects.get(0))};
            case 2:
                // add 2 filters for 2 different objects, so don't expect any event
                return new EventFilters.DebugEventFilter[]{new EventFilters.ObjectReferenceFilter(objects.get(0)),
                        new EventFilters.ObjectReferenceFilter(objects.get(1))};
            default:
                throw new TestBug("Invalid testedFilterIndex: " + testedFilterIndex);
        }
    }
}
