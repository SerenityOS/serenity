/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8241041
 * @summary C2: "assert((Value(phase) == t) || (t != TypeInt::CC_GT && t != TypeInt::CC_EQ)) failed: missing Value() optimization" still happens after fix for 8239335
 *
 * @run main/othervm -XX:-BackgroundCompilation TestSubTypeCheckNewObjectNotConstant
 *
 */

public class TestSubTypeCheckNewObjectNotConstant {
    public static void main(String[] args) throws CloneNotSupportedException {
        for (int i = 0; i < 20_000; i++) {
            test();
            test_helper1(test_helper2(0));
        }
    }

    private static boolean test() throws CloneNotSupportedException {
        int i = 0;
        for (; i < 10; i++);
        AbstractClass o = test_helper2(i);
        return test_helper1(o);
    }

    private static AbstractClass test_helper2(int i) {
        AbstractClass o;
        if (i == 10) {
            o = new ConcreteSubClass1();
        } else {
            o = new ConcreteSubClass2();
        }
        return o;
    }

    private static boolean test_helper1(AbstractClass o) throws CloneNotSupportedException {
        final Object c = o.clone();
        return c instanceof ConcreteSubClass1;
    }

    static abstract class AbstractClass implements Cloneable{
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static class ConcreteSubClass1 extends AbstractClass {
    }

    static class ConcreteSubClass2 extends AbstractClass {
    }
}
