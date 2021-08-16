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

package nsk.jdi.ArrayReference.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class getvalues003a {

    static getvalues003aClassToCheck testedObj = new getvalues003aClassToCheck();

    public static void main (String argv[]) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println("ready");

        String instruction = pipe.readln();

        if ( instruction.equals("quit") ) {
            log.display("DEBUGEE> \"quit\" signal recieved.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(getvalues003.TEST_FAILED + getvalues003.JCK_STATUS_BASE);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                         + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(getvalues003.TEST_FAILED + getvalues003.JCK_STATUS_BASE);
    }
}

class getvalues003aClassToCheck {
    static      int[] staticIntArr      = {};
    static      Object[] staticObjArr   = {};

    public      int[] publicIntArrC;
    protected   int[] protecIntArrC;
    private     int[] privatIntArrC;

    public      Object[] publicObjArrC;
    protected   Object[] protecObjArrC;
    private     Object[] privatObjArrC;

    public getvalues003aClassToCheck() {
        publicIntArrC = createIntArray();
        protecIntArrC = createIntArray();
        privatIntArrC = createIntArray();

        publicObjArrC = createObjArray();
        protecObjArrC = createObjArray();
        privatObjArrC = createObjArray();
    }

    static private int[] createIntArray() {
        return new int[0];
    }

    static private Object[] createObjArray() {
        return new Object[0];
    }

}
