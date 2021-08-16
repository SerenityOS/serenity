/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 *  @test
 *  @bug 8075331
 *  @summary Verify that we can call varargs methods
 *  @run build TestScaffold VMConnection TargetAdapter TargetListener
 *  @run compile -g InvokeVarArgs.java
 *  @run driver InvokeVarArgs
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.Arrays;

interface MyInterface {
}

class SomeClass implements MyInterface {
}

class InvokeVarArgsTarg {

    public static void main(String args[]) {
        new InvokeVarArgsTarg().run();
    }

    SomeClass someClass1 = new SomeClass();
    SomeClass someClass2 = new SomeClass();

    MyInterface[] array = new MyInterface[]{someClass1, someClass2};
    SomeClass[] array2 = new SomeClass[]{someClass1, someClass2};

    public void run() {
        System.out.println("size(array) : " + size(array));
        System.out.println("size(array2) : " + size(array2));
    }

    int size(Object... value) {
        return value.length;
    }
}

public class InvokeVarArgs extends TestScaffold {

    public static void main(String args[]) throws Exception {
        new InvokeVarArgs(args).startTests();
    }

    InvokeVarArgs(String args[]) throws Exception {
        super(args);
    }

    protected void runTests() throws Exception {

        BreakpointEvent bpe = startTo("InvokeVarArgsTarg", "run", "()V");
        StackFrame frame = bpe.thread().frame(0);
        ObjectReference targetObj = frame.thisObject();
        ReferenceType targetType = (ReferenceType) targetObj.type();
        Value arrayVal = targetObj.getValue(targetType.fieldByName("array"));
        Value array2Val = targetObj.getValue(targetType.fieldByName("array2"));
        Method sizeMethod = targetType.methodsByName("size", "([Ljava/lang/Object;)I").get(0);

        IntegerValue size = (IntegerValue) targetObj.invokeMethod(bpe.thread(), sizeMethod, Arrays.asList(new Value[]{arrayVal}), 0);
        if (size.value() != 2) {
            throw new Exception("size(array) should be 2, but was " + size.value());
        }

        size = (IntegerValue) targetObj.invokeMethod(bpe.thread(), sizeMethod, Arrays.asList(new Value[]{array2Val}), 0);
        if (size.value() != 2) {
            throw new Exception("size(array2) should be 2, but was " + size.value());
        }

        listenUntilVMDisconnect();
    }
}
