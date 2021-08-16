/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4653179
 * @summary Tests string equality for Statement and Expression
 * @author Mark Davidson
 */

import java.beans.Expression;
import javax.swing.JButton;

public class Test4653179 {
    public static void main(String[] args) throws Exception {
        String [] array = {"first string", "second one"};

        Object valueInt = testInt(array, "get", 0);
        if (!valueInt.equals(array[0]))
            throw new Error("unexpected value: " + valueInt);

        Object valueNew = testNew(array, "get", 1);
        if (!valueNew.equals(array[1]))
            throw new Error("unexpected value: " + valueNew);


        valueInt = testInt(Class.class, "forName", "javax.swing.JButton");
        if (!valueInt.equals(JButton.class))
            throw new Error("unexpected value: " + valueInt);

        valueNew = testNew(Class.class, "forName", "javax.swing.JButton");
        if (!valueNew.equals(JButton.class))
            throw new Error("unexpected value: " + valueNew);


        testInt(JButton.class, "new");
        testNew(JButton.class, "new");
    }

    private static Object testInt(Object target, String name, Object... args) throws Exception {
        return new Expression(target, name, args).getValue();
    }

    private static Object testNew(Object target, String name, Object... args) throws Exception {
        return testInt(target, new String(name), args); // non-intern string
    }
}
