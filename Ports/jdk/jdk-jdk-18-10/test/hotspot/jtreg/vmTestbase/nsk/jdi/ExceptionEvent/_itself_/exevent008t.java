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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class exevent008t {
    public static void main(String args[]) throws ClassNotFoundException,
                                                  NoSuchMethodException,
                                                  InstantiationException,
                                                  IllegalAccessException,
                                                  InvocationTargetException {
        exevent008t _exevent008t = new exevent008t();
        System.exit(exevent008.JCK_STATUS_BASE + _exevent008t.communication(args));
    }

    private static void raiseEx() throws exevent008tException {
        throw new exevent008tException();
    }

    static void raiseException(int testCase) throws ClassNotFoundException,
                                                    NoSuchMethodException,
                                                    InstantiationException,
                                                    IllegalAccessException,
                                                    InvocationTargetException {
        switch(testCase) {
            case 1: // raise own Exception then catch it
                try {
                    raiseEx();
                } catch (exevent008tException e) {}
                break;
            case 2: // raise uncaught InvokationTargerexception with enveloped
                    // NumberFormatException in another class
                Class<?> testClass = Class.forName(exevent008.DEBUGGEE_CLASS+"NFException");
                Class<?> args[] = { String.class };
                Method testMeth = testClass.getMethod("exevent008traiseEx", args);
                Object testInstance = testClass.newInstance();
                Object parameters[] = { "oops!" };
                Object result = testMeth.invoke(testInstance, parameters);
                break;
        }
    }

    int communication(String args[]) throws ClassNotFoundException,
                               NoSuchMethodException,
                               InstantiationException,
                               IllegalAccessException,
                               InvocationTargetException {
        String command;
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        do {
            pipe.println(exevent008.COMMAND_READY);
            command = pipe.readln();
            if (command.equals(exevent008.COMMAND_TEST1)) {
                raiseException(1);
            } else if (command.equals(exevent008.COMMAND_TEST2)) {
                raiseException(2);
            } else if (command.equals(exevent008.COMMAND_QUIT)) {
                break;
            } else {
                System.err.println("TEST BUG: Debugee: unknown command: " +
                                   command);
                return exevent008.FAILED;
            }
        } while(true);
        return exevent008.PASSED;
    }
}

class exevent008tException extends Exception {}

class exevent008tNFException {
    public void exevent008traiseEx(String arg) {
        int i = Integer.parseInt(arg);
    }
}
