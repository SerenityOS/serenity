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

package nsk.jvmti.scenarios.allocation.AP05;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap05t001 extends DebugeeClass {
    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap05t001().runThis(argv, out);
    }

    public static native void setTag(Object target, long tag);
    public static native void setReferrer(Object referrer);

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    static long timeout = 0;
    int status = Consts.TEST_PASSED;

    ap05t001Subclass referrer;
    public static final long CLS_TAG = 1l, REFERRER_TAG = 2l, REFERREE_TAG = 10l;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        referrer = new ap05t001Subclass();
        setTag(ap05t001Subclass.class, CLS_TAG);
        setTag(referrer, REFERRER_TAG);

        setTag(ap05t001Interface.referree01, REFERREE_TAG);

        referrer.setFields(ap05t001Interface.referree01);
        referrer.setSubFields(ap05t001Interface.referree01);

        setReferrer(referrer);

        status = checkStatus(status);
        return status;
    }
}

class ap05t001Superclass {
    static private   Object referree11;
    static private   Object referree12;
    static protected Object referree13;
    static protected Object referree14;
    static public    Object referree15;
    static public    Object referree16;
    static           Object referree17;
    static           Object referree18;

           private   Object referree21;
           private   Object referree22;
           protected Object referree23;
           protected Object referree24;
           public    Object referree25;
           public    Object referree26;
                     Object referree27;
                     Object referree28;

    void setFields(Object value) {
        referree11 = value;
        referree12 = value;
        referree13 = value;
        referree14 = value;
        referree15 = value;
        referree16 = value;
        referree17 = value;
        referree18 = value;

        referree21 = value;
        referree22 = value;
        referree23 = value;
        referree24 = value;
        referree25 = value;
        referree26 = value;
        referree27 = value;
        referree28 = value;
    }
}

interface ap05t001Interface {
    static public Object referree01 = new Object();
    static public Object referree02 = referree01;
}

class ap05t001Subclass extends ap05t001Superclass implements ap05t001Interface {

    static private   Object referree31;
    static private   Object referree32;
    static protected Object referree33;
    static protected Object referree34;
    static public    Object referree35;
    static public    Object referree36;
    static           Object referree37;
    static           Object referree38;

           private   Object referree41;
           private   Object referree42;
           protected Object referree43;
           protected Object referree44;
           public    Object referree45;
           public    Object referree46;
                     Object referree47;
                     Object referree48;

    void setSubFields(Object value) {
        referree31 = value;
        referree32 = value;
        referree33 = value;
        referree34 = value;
        referree35 = value;
        referree36 = value;
        referree37 = value;
        referree38 = value;

        referree41 = value;
        referree42 = value;
        referree43 = value;
        referree44 = value;
        referree45 = value;
        referree46 = value;
        referree47 = value;
        referree48 = value;
    }
}
