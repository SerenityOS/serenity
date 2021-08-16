/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.sourceNames;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is debuggee class.
 */
public class sourcenames002t {
    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new sourcenames002t().sourcenames002trunIt(args);
    }

    private int sourcenames002trunIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Thread.currentThread().setName(sourcenames002.DEBUGGEE_THRNAME);

        pipe.println(sourcenames002.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(sourcenames002.COMMAND_QUIT)) {
            System.err.println("Debuggee: exiting due to the command: "
                + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // sourcenames002.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        if (!cmd.equals(sourcenames002.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }

    // fields of primitive types used to fill arrays below
    boolean boolVal = false;
    byte byteVal = 127;
    char charVal = 'a';
    double doubleVal = 6.2D;
    float floatVal = 5.1F;
    int intVal = 2147483647;
    long longVal = 9223372036854775807L;
    short shortVal = -32768;

    // classes representing primitive types used to check
    // AbsentInformationException throwing in the debugger
    Class boolCls = Boolean.TYPE;
    Class byteCls = Byte.TYPE;
    Class charCls = Character.TYPE;
    Class doubleCls = Double.TYPE;
    Class floatCls = Float.TYPE;
    Class intCls = Integer.TYPE;
    Class longCls = Long.TYPE;
    Class shortCls = Short.TYPE;

    // arrays used to check AbsentInformationException throwing
    // in the debugger
    Boolean boolClsArr[] = {Boolean.valueOf(false)};
    Byte byteClsArr[] = {Byte.valueOf((byte) 127)};
    Character charClsArr[] = {Character.valueOf('a')};
    Double doubleClsArr[] = {Double.valueOf(6.2D)};
    Float floatClsArr[] = {Float.valueOf(5.1F)};
    Integer intClsArr[] = {Integer.valueOf(2147483647)};
    Long longClsArr[] = {Long.valueOf(9223372036854775807L)};
    Short shortClsArr[] = {Short.valueOf((short) -32768)};

    boolean boolArr[] = {boolVal};
    byte byteArr[] = {byteVal};
    char charArr[] = {charVal};
    double doubleArr[] = {doubleVal};
    float floatArr[] = {floatVal};
    int intArr[] = {intVal};
    long longArr[] = {longVal};
    short shortArr[] = {shortVal};
}
