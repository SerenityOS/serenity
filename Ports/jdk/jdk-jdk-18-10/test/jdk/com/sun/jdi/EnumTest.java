/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4728816
 * @summary JPDA: Add support for enums
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g EnumTest.java
 * @run driver EnumTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/


enum Coin {
    penny(1), nickel(5), dime(10), quarter(25);

    Coin(int value) { this.value = value; }

    private final int value;

    public int value() { return value; }
}

class EnumTarg {
    static Coin myCoin = Coin.penny;
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from EnumTarg!");
    }
}

    /********** test program **********/

public class EnumTest extends TestScaffold {
    ReferenceType targetClass;

    EnumTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new EnumTest(args).startTests();
    }

    void fail(String reason) throws Exception {
        failure(reason);
    }

    /********** test core **********/


    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass
         */
        BreakpointEvent bpe = startToMain("EnumTarg");
        targetClass = bpe.location().declaringType();

        ReferenceType rt = findReferenceType("EnumTarg");
        Field myField = rt.fieldByName("myCoin");
        ObjectReference enumObject = (ObjectReference)rt.getValue(myField);
        ClassType enumClass =(ClassType) enumObject.referenceType();
        ClassType superClass = enumClass.superclass();
        if (!superClass.name().equals("java.lang.Enum")) {
            fail("failure: Superclass of enum class is not java.lang.Enum: " + superClass.name());
        }
        if (!enumClass.isEnum()) {
            fail("failure: isEnum() is false but should be true");
        }
        if (((ClassType)rt).isEnum()) {
            fail("failure: isEnum() is true for EnumTarg but should be false");
        }
        Field enumConstant = enumClass.fieldByName("penny");
        if (!enumConstant.isEnumConstant()) {
            fail("failure: The 'penny' field is not marked " +
                 "as an enum constant.");
        }

        /*
         * This isn't really part of the test, it just
         * shows how to look at all the enum constants,
         * but not necessarily in the correct order
         */
        List allFields = enumClass.fields();
        List enumConstantFields = new ArrayList();
        StringBuffer enumDecl = new StringBuffer("enum " + enumClass.name() + " {");
        char delim = ' ';

        for (Iterator iter = allFields.iterator(); iter.hasNext(); ) {
            Field aField = (Field)iter.next();
            if (aField.isEnumConstant()) {
                enumDecl.append(' ');
                enumDecl.append(aField.name());
                enumDecl.append(delim);
                delim = ',';
            }
        }
        enumDecl.append("; };");
        System.out.println("Enum decl is: " + enumDecl);

        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("EnumTest: passed");
        } else {
            throw new Exception("EnumTest: failed");
        }
    }
}
