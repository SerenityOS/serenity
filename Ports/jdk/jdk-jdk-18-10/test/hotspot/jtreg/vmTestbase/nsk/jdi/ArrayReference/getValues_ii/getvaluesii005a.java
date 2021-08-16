/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ArrayReference.getValues_ii;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class getvaluesii005a {

    static getvaluesii005aClassToCheck testedObj = new getvaluesii005aClassToCheck();


    public static void main (String argv[]) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        pipe.println("ready");

        String instruction = pipe.readln();

        if ( instruction.equals("quit") ) {
            log.display("DEBUGEE> \"quit\" signal received.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(getvaluesii005.TEST_PASSED + getvaluesii005.JCK_STATUS_BASE);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                         + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(getvaluesii005.TEST_FAILED + getvaluesii005.JCK_STATUS_BASE);
    }
}

class getvaluesii005aClassToCheck {
    static      int[] staticIntArr      = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    static      Object[] staticObjArr   = {null, null, null, null, null, null, null, null, null, null};

    static      int[][] staticIntArr2  = {{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                                            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}};

    static      int[][] staticIntArr0  = {{},{},{},{},{},{},{}};

    public      int[] publicIntArr0;
    protected   int[] protecIntArr0;
    private     int[] privatIntArr0;

    public      int[] publicIntArrC;
    protected   int[] protecIntArrC;
    private     int[] privatIntArrC;

    public      Object[] publicObjArr0;
    protected   Object[] protecObjArr0;
    private     Object[] privatObjArr0;

    public      Object[] publicObjArrC;
    protected   Object[] protecObjArrC;
    private     Object[] privatObjArrC;

    public getvaluesii005aClassToCheck() {
        publicIntArr0 = createIntArray(0);
        protecIntArr0 = createIntArray(0);
        privatIntArr0 = createIntArray(0);

        publicIntArrC = createIntArray(10);
        protecIntArrC = createIntArray(10);
        privatIntArrC = createIntArray(10);

        publicObjArr0 = createObjArray(0);
        protecObjArr0 = createObjArray(0);
        privatObjArr0 = createObjArray(0);

        publicObjArrC = createObjArray(10);
        protecObjArrC = createObjArray(10);
        privatObjArrC = createObjArray(10);
    }

    static private int[] createIntArray(int length) {
        int[] array = new int[length];
        for ( int i = 0; i < length; i++ ) array[i] = i;
        return array;
    }

    static private Object[] createObjArray(int length) {
        Object[] array = new Object[length];
        return array;
    }
}
