/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4144543
 * @summary Tests that introspection handles multiple setters in any order
 * @author Janet Koenig
 */

import java.beans.Beans;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class Test4144543 {
    public static void main(String[] args) throws Exception {
        Class type = Beans.instantiate(null, "Test4144543").getClass();

        // try all the various places that this would break before

        Introspector.getBeanInfo(type);
        new PropertyDescriptor("value", type);
        new PropertyDescriptor("value", type, "getValue", "setValue");
    }

    private int value;

    public int getValue() {
        return this.value;
    }

    /*
     * The Introspector expects the return type of the getter method to
     * match the parameter type of the setter method.  So list the setter
     * method which has a different type (but compatible) first so that
     * the Introspector will find it first and recognize that it is not
     * the correct setter method.
     */

    public void setValue(byte value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
