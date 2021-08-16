/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4836939 6646613
 * @summary JDI add addSourceNameFilter to ClassPrepareRequest
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g SourceNameFilterTest.java
 * @run driver SourceNameFilterTest
 * @run compile -g:none SourceNameFilterTest.java
 * @run driver SourceNameFilterTest
 */
// The compile -g:none suppresses the lineNumber table to trigger bug 6646613.

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class SourceNameFilterTarg {
    static  void bkpt() {
    }

    public static void main(String[] args){
        System.out.println("Howdy!");

        LoadedLater1.doit();
        bkpt();

        LoadedLater2.doit();
        bkpt();

        LoadedLater3.doit();
        bkpt();
        System.out.println("Goodbye from SourceNameFilterTarg!");
    }
}
class LoadedLater1 {
    public static void doit() {
        System.out.println("didit1");
    }
}

class LoadedLater2 {
    public static void doit() {
        System.out.println("didit2");
    }
}

class LoadedLater3 {
    public static void doit() {
        System.out.println("didit3");
    }
}

    /********** test program **********/

public class SourceNameFilterTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    boolean gotEvent1 = false;
    boolean gotEvent2 = false;
    boolean gotEvent3 = false;
    ClassPrepareRequest cpReq;
    boolean shouldResume = false;
    SourceNameFilterTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new SourceNameFilterTest(args).startTests();
    }
    public void eventSetComplete(EventSet set) {
        //System.out.println("jj: resuming, set = " + set);
        if (shouldResume) {
            set.resume();
            shouldResume = false;
        }
    }

    public void classPrepared(ClassPrepareEvent event) {
        shouldResume = true;

        ReferenceType rt = event.referenceType();
        String rtname = rt.name();

        if (rtname.equals("LoadedLater1")) {
            gotEvent1 = true;
        }

        if (rtname.equals("LoadedLater2")) {
            gotEvent2 = true;
        }

        if (rtname.equals("LoadedLater3")) {
            gotEvent3 = true;
        }

        // debug code
        if (false) {
            println("Got ClassPrepareEvent for : " + rtname);
            try {
                println("    sourceName = " + rt.sourceName());
            } catch (AbsentInformationException ee) {
                failure("failure: absent info on sourceName(): " + ee);
            }

            String stratum = rt.defaultStratum();
            println("    defaultStratum = " + stratum);

            try {
                println("    sourceNames = " + rt.sourceNames(stratum));
            } catch (AbsentInformationException ee) {
                failure("failure: absent info on sourceNames(): " + ee);
            }
            println("\nAvailable strata:  " + rt.availableStrata());
        }
    }


    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("SourceNameFilterTarg");
        targetClass = bpe.location().declaringType();
        boolean noSourceName = false;
        try {
            targetClass.sourceName();
        } catch (AbsentInformationException ee) {
            noSourceName = true;
        }
        if (noSourceName) {
            println("-- Running with no source names");
        } else {
            println("-- Running with source names");
        }

        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();
        addListener(this);

        /*
         * Resume the target listening for events
         * This should cause a class prepare event for LoadedLater1
         */
        cpReq = erm.createClassPrepareRequest();
        cpReq.enable();
        resumeTo("SourceNameFilterTarg", "bkpt", "()V");

        /*
         * This should cause us to not get a class prepared for
         * LoadedLater2 since it doesn't come from "jj"
         */
        cpReq.disable();
        cpReq.addSourceNameFilter("jj");
        cpReq.enable();
        resumeTo("SourceNameFilterTarg", "bkpt", "()V");
        cpReq.disable();

        /*
         * This should cause us to get a class prepare event for
         * LoadedLater3 except in the case where -g:none
         * was used to compile so that there is no LineNumberTable
         * and therefore, no source name for the class.
         */
        cpReq = erm.createClassPrepareRequest();
        cpReq.addSourceNameFilter("SourceNameFilterTest.java");
        cpReq.enable();
        resumeTo("SourceNameFilterTarg", "bkpt", "()V");

        listenUntilVMDisconnect();

        if (!gotEvent1) {
            failure("failure: Did not get a class prepare request " +
                    "for LoadedLater1");
        }

        if (gotEvent2) {
            failure("failure: Did get a class prepare request " +
                    "for LoadedLater2");
        }

        if (gotEvent3 && noSourceName) {
            failure("failure: Did get a class prepare request " +
                    "for LoadedLater3");
        }
        else if (!gotEvent3 && !noSourceName) {
            failure("failure: Did not get a class prepare request " +
                    "for LoadedLater3");
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("SourceNameFilterTest: passed");
        } else {
            throw new Exception("SourceNameFilterTest: failed");
        }
    }
}
