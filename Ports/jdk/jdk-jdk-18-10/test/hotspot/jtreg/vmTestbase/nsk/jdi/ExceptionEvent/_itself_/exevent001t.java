/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ExceptionEvent._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class exevent001t {
    static ArgumentHandler argHandler;

    public static void main(String args[]) {
        exevent001t _exevent001t = new exevent001t();
        System.exit(exevent001.JCK_STATUS_BASE
            + _exevent001t.communication(args));
    }

    private static void raiseEx() throws exevent001tException {
        throw new exevent001tException();
    }

    static void raiseException(int testCase) {
        switch(testCase) {
            case 1: // raise own Exception then catch it
                try {
                    raiseEx();
                } catch (exevent001tException e) {}
                break;
            case 2: // raise IllegalMonitorStateException then catch it
                Object obj = new Object();
                try {
                    obj.notify();
                } catch (IllegalMonitorStateException e) {}
                break;
            case 3: // raise NumberFormatException in another class
                new exevent001tNFException("oops!");
                break;
        }
    }

    int communication(String args[]) {
        String command;
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        do {
            pipe.println(exevent001.COMMAND_READY);
            command = pipe.readln();
            if (command.equals(exevent001.COMMAND_TEST1)) {
                raiseException(1);
            } else if (command.equals(exevent001.COMMAND_TEST2)) {
                raiseException(2);
            } else if (command.equals(exevent001.COMMAND_TEST3)) {
                raiseException(3);
            } else if (command.equals(exevent001.COMMAND_QUIT)) {
                break;
            } else {
                System.err.println("TEST BUG: Debuggee: unknown command: " +
                                   command);
                return exevent001.FAILED;
            }
        } while(true);
        return exevent001.PASSED;
    }
}

class exevent001tException extends Exception {}

class exevent001tNFException {
    exevent001tNFException(String arg) {
        try {
            int i = Integer.parseInt(arg);
        } catch (NumberFormatException e) {}
    }
}
