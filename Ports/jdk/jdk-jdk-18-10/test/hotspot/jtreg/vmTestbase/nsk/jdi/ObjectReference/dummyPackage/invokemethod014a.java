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

// dummy package
package nsk.jdi.ObjectReference.dummyPackage;

/**
 * This is an auxiliary class outside a main debuggee package. It's
 * used to provoke IllegalArgumentException in the debugger:
 * non-public methods cannot be accessed from outside the package.
 */
public class invokemethod014a {
    // methods with default access are below
    byte byteMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"byteMeth\" was invoked from outside package!");
        return 127;
    }

    short shortMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"shortMeth\" was invoked from outside not public!");
        return -32768;
    }

    int intMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"intMeth\" was invoked from outside not public!");
        return 2147483647;
    }

    long longMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"longMeth\" was invoked from outside not public!");
        return 9223372036854775807L;
    }

    float floatMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"floatMeth\" was invoked from outside not public!");
        return 5.1F;
    }

    double doubleMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"doubleMeth\" was invoked from outside not public!");
        return 6.2D;
    }

    char charMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"charMeth\" was invoked from outside not public!");
        return 'a';
    }

    boolean booleanMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"booleanMeth\" was invoked from outside not public!");
        return false;
    }

    String strMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"strMeth\" was invoked from outside not public!");
        return "string method";
    }

    void voidMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod14a: non-public method \"voidMeth\" was invoked from outside not public!");
    }

    // protected methods are below
    protected byte protByteMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protByteMeth\" was invoked!");
        return 127;
    }

    protected short protShortMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protShortMeth\" was invoked!");
        return -32768;
    }

    protected int protIntMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protIntMeth\" was invoked!");
        return 2147483647;
    }

    protected long protLongMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protLongMeth\" was invoked!");
        return 9223372036854775807L;
    }

    protected float protFloatMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protFloatMeth\" was invoked!");
        return 5.1F;
    }

    protected double protDoubleMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protDoubleMeth\" was invoked!");
        return 6.2D;
    }

    protected char protCharMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protCharMeth\" was invoked!");
        return 'a';
    }

    protected boolean protBooleanMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protBooleanMeth\" was invoked!");
        return false;
    }

    protected String protStrMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protStrMeth\" was invoked!");
        return "string method";
    }

    protected void protVoidMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: protected method \"protVoidMeth\" was invoked!");
    }

    // private methods are below
    private byte privByteMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privByteMeth\" was invoked!");
        return 127;
    }

    private short privShortMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privShortMeth\" was invoked!");
        return -32768;
    }

    private int privIntMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privIntMeth\" was invoked!");
        return 2147483647;
    }

    private long privLongMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privLongMeth\" was invoked!");
        return 9223372036854775807L;
    }

    private float privFloatMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privFloatMeth\" was invoked!");
        return 5.1F;
    }

    private double privDoubleMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privDoubleMeth\" was invoked!");
        return 6.2D;
    }

    private char privCharMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privCharMeth\" was invoked!");
        return 'a';
    }

    private boolean privBooleanMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privBooleanMeth\" was invoked!");
        return false;
    }

    private String privStrMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privStrMeth\" was invoked!");
        return "string method";
    }

    private void privVoidMeth() {
        nsk.jdi.ObjectReference.invokeMethod.invokemethod014t.log.complain("invokemethod014a: private method \"privVoidMeth\" was invoked!");
    }
}
