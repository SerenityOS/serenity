/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.allocation.AP09;

import java.io.*;
import java.security.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap09t001 extends DebugeeClass implements Cloneable {
    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap09t001().runThis(argv, out);
    }

    public static native void setTag(Object target, long tag);
    public static native void setReferrer(Object referrer);

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    static long timeout = 0;
    int status = Consts.TEST_PASSED;

    static Object[] staticField = {new Object()};
    Object instanceField = new Object();

    public static final long OBJECT_TAG         = 1l;
    public static final long CLASS_TAG          = 2l;
    public static final long LOADER_TAG         = 3l;
    public static final long DOMAIN_TAG         = 4l;
    public static final long INSTANCE_FIELD_TAG = 5l;
    public static final long STATIC_FIELD_TAG   = 6l;
    public static final long ARRAY_TAG          = 7l;
    public static final long INTERFACE_TAG      = 8l;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        setTag(this, OBJECT_TAG);
        setTag(this.getClass(), CLASS_TAG);
        setTag(this.getClass().getInterfaces()[0], INTERFACE_TAG);
        setTag(instanceField, INSTANCE_FIELD_TAG);
        setTag(staticField, ARRAY_TAG);
        setTag(staticField[0], STATIC_FIELD_TAG);

        try {
            setTag(this.getClass().getClassLoader(), LOADER_TAG);
        } catch (SecurityException e) {
            throw new Failure("SecurityException was thrown when trying to get class loader.");
        }
        try {
            setTag(this.getClass().getProtectionDomain(), DOMAIN_TAG);
        } catch (SecurityException e) {
            throw new Failure("SecurityException was thrown when trying to get protection domain.");
        }

        setReferrer(this);
        status = checkStatus(status);

        return status;
    }
}
