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

package nsk.jdi.ClassUnloadEvent.className;

import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugged application in the test

class classname001a {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE  = 95;

    static final String PREFIX = "nsk.jdi.ClassUnloadEvent.className";
    static final String CHECKED_CLASS = PREFIX + ".classname001b";

    public static void main(String args[]) {
        classname001a _classname001a = new classname001a();
        System.exit(JCK_STATUS_BASE + _classname001a.run(args));
    }

    int run (String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // define directory to class files
        String loadClassDir = (argHandler.getArguments())[0] + File.separator + "loadclass";

        // notify debugger that debugee is ready
        pipe.println(classname001.COMMAND_READY);

        // wait for a command to load checked class
        String command = pipe.readln();
        if (!command.equals(classname001.COMMAND_LOAD)) {
             System.err.println("TEST BUG: unexpected command: " + command);
             return FAILED;
        }

        // load class for further unloading
        ClassUnloader classUnloader = new ClassUnloader();
        try {
            classUnloader.loadClass(CHECKED_CLASS, loadClassDir);
        } catch (Exception ex) {
            System.err.println("Unexpected exception while loading classname001b:");
            System.err.println(ex);
            return FAILED;
        }

        // notify debugger that checked class is loaded
        pipe.println(classname001.COMMAND_LOADED);

        // turn off pipe pinging
        pipe.setPingTimeout(0);

        // wait for a command to unload checked class
        command = pipe.readln();
        if (!command.equals(classname001.COMMAND_UNLOAD)) {
             System.err.println("TEST BUG: unexpected command: " + command);
             return FAILED;
        }

        // try to unload checked class
        boolean unloaded = classUnloader.unloadClass();

        if (!unloaded) {
             pipe.println(classname001.COMMAND_LOADED);
        } else {
             pipe.println(classname001.COMMAND_UNLOADED);
        }

        // wait for a command to exit
        command = pipe.readln();
        if (!command.equals(classname001.COMMAND_QUIT)) {
             System.err.println("TEST BUG: unknown command: " + command);
             return FAILED;
        }
        return PASSED;
    }
}
