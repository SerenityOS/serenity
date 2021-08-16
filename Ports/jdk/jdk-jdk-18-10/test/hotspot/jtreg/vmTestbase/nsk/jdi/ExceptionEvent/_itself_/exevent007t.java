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


public class exevent007t {
    public static void main(String args[]) throws ClassNotFoundException,
                                                  NoSuchMethodException,
                                                  InstantiationException,
                                                  IllegalAccessException,
                                                  InvocationTargetException {
        exevent007t _exevent007t = new exevent007t();
        System.exit(exevent007.JCK_STATUS_BASE + _exevent007t.communication(args));
    }

    int communication(String args[]) throws ClassNotFoundException,
                               NoSuchMethodException,
                               InstantiationException,
                               IllegalAccessException,
                               InvocationTargetException {
        String command;
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        pipe.println(exevent007.COMMAND_READY);
        command = pipe.readln();
        if (command.equals(exevent007.COMMAND_RUN)) {
            Class<?> testClass = Class.forName(exevent007.DEBUGGEE_CLASS+"Exception");
            Class<?> methodArgs[] = { String.class };
            Method testMeth = testClass.getMethod("exevent007traiseEx", methodArgs);
            Object testInstance = testClass.newInstance();
            Object parameters[] = { "oops!" };
            Object result = testMeth.invoke(testInstance, parameters);

            return exevent007.PASSED;
        } else
            return exevent007.FAILED;
    }
}

class exevent007tException {
    public void exevent007traiseEx(String arg) {
        int i = Integer.parseInt(arg);
    }
}
