/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;

/**
 * @test
 * @bug 4629190
 * @summary CompoundControl: getMemberControls() and toString() throw
 *          NullPointerException
 */
public class ToString {
    public static void main(String args[]) throws Exception {
        System.out.println();
        System.out.println();
        System.out.println("4629190: CompoundControl: getMemberControls() and toString() throw NullPointerException");

        String firstControlTypeName = "first_Control_Type_Name";
        String secondControlTypeName = "second_Control_Type_Name";
        String thirdControlTypeName = "third_Control_Type_Name";

        Control.Type firstControlType = new TestControlType(firstControlTypeName);
        Control.Type secondControlType = new TestControlType(secondControlTypeName);
        Control.Type thirdControlType = new TestControlType(thirdControlTypeName);

        Control firstControl = new TestControl(firstControlType);
        Control secondControl = new TestControl(secondControlType);
        Control thirdControl = new TestControl(thirdControlType);

        String testCompoundControlTypeName = "CompoundControl_Type_Name";
        CompoundControl.Type testCompoundControlType
            = new TestCompoundControlType(testCompoundControlTypeName);

        Control[] setControls = { firstControl, secondControl, thirdControl };
        CompoundControl testedCompoundControl
            = new TestCompoundControl(testCompoundControlType, setControls);

        // this may throw exception if bug applies
        Control[] producedControls = testedCompoundControl.getMemberControls();
        System.out.println("Got "+producedControls.length+" member controls.");

        // this may throw exception if bug applies
        String producedString = testedCompoundControl.toString();
        System.out.println("toString() returned: "+producedString);

        System.out.println("Test passed.");
    }

}

class TestControl extends Control {

    TestControl(Control.Type type) {
        super(type);
    }
}

class TestControlType extends Control.Type {

    TestControlType(String name) {
        super(name);
    }
}

class TestCompoundControl extends CompoundControl {

    TestCompoundControl(CompoundControl.Type type, Control[] memberControls) {
        super(type, memberControls);
    }
}

class TestCompoundControlType extends CompoundControl.Type {

    TestCompoundControlType(String name) {
        super(name);
    }
}
