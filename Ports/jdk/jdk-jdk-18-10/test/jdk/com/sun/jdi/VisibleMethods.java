/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Test ReferenceType.visibleMethods
 * @bug 8028430
 * @author Staffan Larsen
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g VisibleMethods.java
 * @run driver VisibleMethods
 */
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/********** target program **********/

interface Super {
    public void m(Object o); // This method should not be visible in AC
    public void m(String s); // This method should not be visible in AC
}

interface One extends Super {
    public void m(Object o);
    public void m1(); // Either this method or Two.m1 should be visible in AC
}

interface Two extends Super {
    public void m(String s);
    public void m1(); // Either this method or One.m1 should be visible in AC
}

abstract class AC implements One, Two {
}

class CC extends AC {
    public void m(Object o) {
    }
    public void m(String s) {
    }
    public void m1() {
    }
    public static void main(String[] args) {
        System.out.println("Goodbye from VisibleMethods!");
    }
}

/********** test program **********/

public class VisibleMethods extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    VisibleMethods(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new VisibleMethods(args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {
        /*
         * Run to String.<init>
         */
        startToMain("CC");

        ReferenceType ac = findReferenceType("AC");
        List<String> visible = ac.visibleMethods().
                stream().
                map(Method::toString).
                collect(Collectors.toList());

        System.out.println("visibleMethods(): " + visible);

        verifyContains(visible, 1, "Two.m(java.lang.String)");
        verifyContains(visible, 1, "One.m(java.lang.Object)");
        verifyContains(visible, 0, "Super.m(java.lang.Object)");
        verifyContains(visible, 0, "Super.m(java.lang.String)");
        verifyContains(visible, 1, "Two.m1()", "One.m1()");

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();
    }

    private void verifyContains(List<String> methods, int matches,
            String... sigs) throws Exception {
        if (countMatches(methods, sigs) != matches) {
            throw new Exception("visibleMethods() should have contained "
                    + matches + " entry/entries from " + Arrays.toString(sigs));
        }
    }

    private int countMatches(List<String> list1, String[] list2) {
        int count = 0;
        for (String s1 : list1) {
            for (String s2 : list2) {
                if (s1.equals(s2)) {
                    count++;
                }
            }
        }
        return count;
    }
}
