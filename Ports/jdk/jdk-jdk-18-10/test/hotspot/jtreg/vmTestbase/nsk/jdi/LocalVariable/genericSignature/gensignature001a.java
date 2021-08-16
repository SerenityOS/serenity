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

package nsk.jdi.LocalVariable.genericSignature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.util.*;
import java.io.*;

/**
 * This class is used as debugee part of the test.
 */

public class gensignature001a {

    private static ArgumentHandler  argsHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debugger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    public static void main(String args[]) {
        gensignature001a _gensignature001a = new gensignature001a();
        System.exit(Consts.JCK_STATUS_BASE + _gensignature001a.runThis(args, System.err));
    }

    public int runThis(String args[], PrintStream out) {

        argsHandler = new ArgumentHandler(args);
        log = new Log(out, argsHandler);
        pipe = argsHandler.createDebugeeIOPipe(log);

        display("Debugee started!");

        // ensure tested class loaded
        display("Creating object of tested class");
        TestedClass<String, Long> foo = new TestedClass<String, Long>();

        // send debugger signal READY
        display("Sending signal to debugger: " + gensignature001.READY);
        pipe.println(gensignature001.READY);

        // wait for signal QUIT from debugeer
        display("Wait for '" + gensignature001.QUIT + "' signal from debugger...");
        receiveSignal(gensignature001.QUIT);

        display("Debugee PASSED");
        return Consts.TEST_PASSED;
    }

    // tested class
    public static class TestedClass<T, N extends Number> {
        int foo = 0;

        // tested method
        public void testedMethod(
                        // not generic argumments
                        boolean arg11PrimBoolean,
                        int     arg12PrimInt,
                        Object  arg13Object,
                        String  arg14String,
                        short[] arg15PrimArrShort,
                        Object[] arg16ObjArrObject,

                        // generic arguments
                        T       arg21GenObject,
                        N       arg22GenNumber,
                        T[]     arg23GenObjectArr,
                        N[]     arg24GenNumberArr,
                        List<T> arg25GenObjectList,
                        List<N> arg26GenNumberList,
                        List<? extends T> arg27GenObjectDerivedList,
                        List<? extends N> arg28GenNumberDerivedList
                    ) {

            // not generic variables
            boolean var11PrimBoolean    = arg11PrimBoolean;
            int     var12PrimInt        = arg12PrimInt;
            Object  var13Object         = arg13Object;
            String  var14String         = arg14String;
            short[] var15PrimArrShort   = arg15PrimArrShort;
            Object[] var16ObjArrObject  = arg16ObjArrObject;

            // generic variables
            T       var21GenObject      = arg21GenObject;
            N       var22GenNumber      = arg22GenNumber;
            T[]     var23GenObjectArr   = arg23GenObjectArr;
            N[]     var24GenNumberArr   = arg24GenNumberArr;
            List<T> var25GenObjectList  = arg25GenObjectList;
            List<N> var26GenNumberList  = arg26GenNumberList;
            List<? extends T> var27GenObjectDerivedList = arg27GenObjectDerivedList;
            List<? extends N> var28GenNumberDerivedList = arg28GenNumberDerivedList;
        }
    }
} // end of gensignature001a class
