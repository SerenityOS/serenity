/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary "+=" applied to String operands can provoke side effects
 * @bug 8204322
 *
 * @compile ImplicitStringConcatAssignLHS.java
 * @run main/othervm -Xverify:all ImplicitStringConcatAssignLHS
 *
 * @compile -XDstringConcat=inline ImplicitStringConcatAssignLHS.java
 * @run main/othervm -Xverify:all ImplicitStringConcatAssignLHS
 *
 * @compile -XDstringConcat=indy ImplicitStringConcatAssignLHS.java
 * @run main/othervm -Xverify:all ImplicitStringConcatAssignLHS
 *
 * @compile -XDstringConcat=indyWithConstants ImplicitStringConcatAssignLHS.java
 * @run main/othervm -Xverify:all ImplicitStringConcatAssignLHS
*/
import java.lang.StringBuilder;

public class ImplicitStringConcatAssignLHS {

    static final int ARR_SIZE = 10; // enough padding to capture ill offsets

    static int x;

    public static void main(String[] args) throws Exception {
        {
          x = 0;
            Object[] arr = new Object[ARR_SIZE];
            arr[x++] += "foo";
            check(1, "plain-plain Object[]");
        }

        {
          x = 0;
            getObjArray()[x++] += "foo";
            check(2, "method-plain Object[]");
        }

        {
          x = 0;
            getObjArray()[getIndex()] += "foo";
            check(2, "method-method Object[]");
        }

        {
            x = 0;
            String[] arr = new String[ARR_SIZE];
            arr[x++] += "foo";
            check(1, "plain-plain String[]");
    }

        {
            x = 0;
            getStringArray()[x++] += "foo";
            check(2, "method-plain String[]");
        }

        {
            x = 0;
            getStringArray()[getIndex()] += "foo";
            check(2, "method-method String[]");
        }

        {
            x = 0;
            CharSequence[] arr = new CharSequence[ARR_SIZE];
            arr[x++] += "foo";
            check(1, "plain-plain CharSequence[]");
        }

        {
            x = 0;
            getCharSequenceArray()[x++] += "foo";
            check(2, "method-plain CharSequence[]");
        }

        {
            x = 0;
            getCharSequenceArray()[getIndex()] += "foo";
            check(2, "method-method CharSequence[]");
        }

        {
            x = 0;
            new MyClass().s += "foo";
            check(1, "MyClass::new (String)");
        }

        {
            x = 0;
            getMyClass().s += "foo";
            check(1, "method MyClass::new (String)");
        }

        {
            x = 0;
            new MyClass().o += "foo";
            check(1, "MyClass::new (object)");
        }

        {
            x = 0;
            getMyClass().o += "foo";
            check(1, "method MyClass::new (object)");
        }
    }

    public static void check(int expected, String label) {
        if (x != expected) {
           StringBuilder sb = new StringBuilder();
           sb.append(label);
           sb.append(": ");
           sb.append("Expected = ");
           sb.append(expected);
           sb.append("actual = ");
           sb.append(x);
           throw new IllegalStateException(sb.toString());
        }
    }

    public static int getIndex() {
       return x++;
    }

    public static class MyClass {
        Object o;
        String s;

        public MyClass() {
       x++;
        }
    }

    public static MyClass getMyClass() {
        return new MyClass();
}

    public static Object[] getObjArray() {
        x++;
        return new Object[ARR_SIZE];
    }

    public static String[] getStringArray() {
        x++;
        return new String[ARR_SIZE];
    }

    public static CharSequence[] getCharSequenceArray() {
        x++;
        return new String[ARR_SIZE];
    }

}

