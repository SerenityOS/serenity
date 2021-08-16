/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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


//import java.beans.ConstructorProperties;
import javax.management.ConstructorParameters;

/**
 * This class defines a simple standard MBean.
 */
public class Simple implements SimpleMBean {

    private String attribute = "initial_value";
    private boolean operationInvoked = false;
    private boolean operation2Invoked = false;

    @SqeDescriptorKey("NO PARAMETER CONSTRUCTOR Simple")
    public Simple() {
    }

    @SqeDescriptorKey("TWO PARAMETERS CONSTRUCTOR Simple")
    @ConstructorParameters({"unused1", "unused2"})
    public Simple(@SqeDescriptorKey("CONSTRUCTOR PARAMETER unused1")int unused1,
            @SqeDescriptorKey("CONSTRUCTOR PARAMETER unused2")int unused2) {
    }

    public String getAttribute() {
        return attribute;
    }
    public void setAttribute(String s) {
        attribute = s;
    }
    public boolean getOperationInvoked() {
        return operationInvoked;
    }
    public boolean getOperation2Invoked() {
        return operation2Invoked;
    }

    public void operation() {
        operationInvoked = true;
        return;
    }

    public String operation2(int i) {
        operation2Invoked = true;
        return String.valueOf(i);
    }

    public void reset() {
        attribute = "initial_value";
        operationInvoked = false;
        operation2Invoked = false;
    }
}
