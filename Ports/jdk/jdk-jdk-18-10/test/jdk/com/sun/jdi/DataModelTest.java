/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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
 *  @ test:it only runs from SparcToSparcV9Test.sh
 *  @bug 4478312
 *  @summary Test debugging with mixed 32/64bit VMs.
 *
 *  @author Tim Bell
 *
 *  The basic version of this test (32-bit to 32-bit) should pass
 *  on all platforms.  The SparcToSparcv9Test.sh script uses this
 *  test to exercise other combinations on SPARC v9 platforms only.
 *
 *  @run build TestScaffold VMConnection TargetListener TargetAdapter
 *  @run compile -g DataModelTest.java
 *  @run driver DataModelTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class DataModelTarg {
    static String dataModel;
    public DataModelTarg () {
        dataModel = System.getProperty("sun.arch.data.model");
    }
    public void ready () {
        System.out.println("sun.arch.data.model is: " + dataModel);
    }
    public static void main(String[] args){
        System.out.println("Howdy!");
        DataModelTarg my = new DataModelTarg();
        my.ready();
        System.out.println("Goodbye from DataModelTarg!");
    }
}

    /********** test program **********/

public class DataModelTest extends TestScaffold {

    DataModelTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new DataModelTest(args).startTests();
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of ready()
         */
        BreakpointEvent bpe = startTo("DataModelTarg", "ready", "()V");

        ObjectReference targetObject = bpe.thread().frame(0).thisObject();
        ReferenceType rt = targetObject.referenceType();
        Field field = rt.fieldByName("dataModel");
        Value v = targetObject.getValue(field);
        StringReference sv = (StringReference) v;
        String expectedValue = System.getProperty("EXPECTED", "32");
        if (!expectedValue.equals(sv.value())) {
            failure("Expecting sun.arch.data.model = " + expectedValue +
                    " but got " + sv.value() + " instead.");
        }
        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("DataModelTest: passed");
        } else {
            throw new Exception("DataModelTest: failed");
        }
    }
}
