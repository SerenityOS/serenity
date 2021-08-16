/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassUnloadRequest.addClassFilter;

import jdk.test.lib.Utils;
import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.*;
import java.util.*;

/**
 * Debugger requests <code>ClassUnloadEvent</code> and sets class filter by calling
 * <code>addClassFilter</code> with pattern that begins with '*' and ends with '*'.
 *
 * <p>
 * To excite a class loading/unloading onto debugee, debugeer uses
 * <code>nsk.share.ClassUnloader</code> and performs it twice : <br>
 *      1. for pattern that ends with '*'   <br>
 *      2. for pattern that begins with '*' <br>
 * It is expected:  <br>
 *  - in case no events occured, test is considered as passed     <br>
 *  - in case classunload event occurs, debugger checks class name to match to
 * pattern.
 */
public class filter001 {

    final static String prefix = "nsk.jdi.ClassUnloadRequest.addClassFilter.";
    private final static String className = "filter001";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";

    // signals to control working of debugee
    public final static String SGNL_LOAD = "load";
    public final static String SGNL_UNLOAD = "unload";
    public final static String SGNL_BREAK = "break";
    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    String[] patterns = {
                    prefix + "Sub*",
                    "*.Superfilter002b"
    };

    private static void display(String msg) {
        log.display("debugger> " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        filter001 tstObj = new filter001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        waitTime = argHandler.getWaitTime();

        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);
        tstObj.execTest();

        display("execTest finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        ClassUnloadRequest request;

        // when there are no class unload events, test should be considered as PASSED
        exitStatus = Consts.TEST_PASSED;

        for (int i = 0; i < patterns.length; i++) {
            // loading of tested classes
            display("sending " + SGNL_LOAD);
            debugee.sendSignal(SGNL_LOAD);
            debugee.receiveExpectedSignal(SGNL_READY);

            // unloading of tested classes
            display("------------------------------------------------");
            request = requestClassUnload(patterns[i]);
            display("================================================");

            display("sending " + SGNL_UNLOAD);
            debugee.sendSignal(SGNL_UNLOAD);
            debugee.receiveExpectedSignal(SGNL_READY);

            receiveEvents(patterns[i]);

            display("");
            debugee.getEventRequestManager().deleteEventRequest(request);
        }

        display("sending " + SGNL_BREAK);
        debugee.sendSignal(SGNL_BREAK);
        debugee.quit();

    }

    private ClassUnloadRequest requestClassUnload(String filter) {
        display(">>>creating ClassUnloadRequest");
        ClassUnloadRequest request =
                debugee.getEventRequestManager().createClassUnloadRequest();
        display("adding filter :");
        display("  <" + filter + ">");

        request.addClassFilter(filter);
        request.enable();

        return request;
    }

    private void receiveEvents(String pattern) {
        EventSet eventSet = null;
        Event event;
        long totalWaitTime = Utils.adjustTimeout(waitTime * 10000);
        long waitTimeout = Utils.adjustTimeout(waitTime * 1000);
        long begin, delta;
        int count = 0;
        boolean exit = false;

        try {
            begin = System.currentTimeMillis();
            eventSet = debugee.VM().eventQueue().remove(waitTimeout);
            delta = System.currentTimeMillis() - begin;
            totalWaitTime -= delta;
            while (eventSet != null) {
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext()) {
                    event = eventIterator.nextEvent();
                    if (event instanceof ClassUnloadEvent) {
                        if (!analyzeEvents((ClassUnloadEvent )event, pattern)) {
                            display("exiting...");
                            exit = true;
                            break;
                        }
                        count++;
                    } else if (event instanceof VMDisconnectEvent) {
                        throw new Failure("Unexpected VMDisconnectEvent received");
                    }
                }
                if (totalWaitTime <= 0 || exit) {
                    break;
                }
                debugee.resume();
                begin = System.currentTimeMillis();
                eventSet = debugee.VM().eventQueue().remove(waitTimeout);
                delta = System.currentTimeMillis() - begin;
                totalWaitTime -= delta;
            }
        } catch(InterruptedException e) {
            throw new Failure(e);
        } catch(VMDisconnectedException e) {
            throw new Failure(e);
        }

        if (count==0) {
            display("");
            display("WARNING: No events were arrived");
            display("");
        }

        debugee.resume();
    }

    private boolean analyzeEvents(ClassUnloadEvent event, String pattern) {
        String className;

        className = event.className();

        if (!checkMatching(className, pattern)) {
            complain("***no matching***\n " + className);
            display("");
            exitStatus = Consts.TEST_FAILED;
            return false;
        }

        display("OK--> " + className);

        return true;
    }

    private boolean checkMatching(String className, String pattern) {

        String tmp;
        int length = pattern.length();

        // pattern ends with '*'
        if (pattern.charAt(length - 1) == '*') {
            tmp = pattern.substring(0, length -1);
            return className.indexOf(tmp) == 0;

        // pattern begins with '*'
        } else if (pattern.charAt(0) == '*') {
            tmp = pattern.substring(1);
            return className.endsWith(tmp);

        // unsupported pattern
        } else new TestBug("Wrong pattern <" + pattern + ">");

        return false;
    }

}
