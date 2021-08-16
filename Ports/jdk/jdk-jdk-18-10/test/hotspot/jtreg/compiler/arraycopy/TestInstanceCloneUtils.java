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

package compiler.arraycopy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

abstract class TestInstanceCloneUtils {
    static class Base implements Cloneable {
        void initialize(Class c, int i) {
            for (Field f : c.getDeclaredFields()) {
                setVal(f, i);
                i++;
            }
            if (c != Base.class) {
                initialize(c.getSuperclass(), i);
            }
        }

        Base(boolean initialize) {
            if (initialize) {
                initialize(getClass(), 0);
            }
        }

        void setVal(Field f, int i) {
            try {
                if (f.getType() == int.class) {
                    f.setInt(this, i);
                    return;
                } else if (f.getType() == short.class) {
                    f.setShort(this, (short)i);
                    return;
                } else if (f.getType() == byte.class) {
                    f.setByte(this, (byte)i);
                    return;
                } else if (f.getType() == long.class) {
                    f.setLong(this, i);
                    return;
                }
            } catch(IllegalAccessException iae) {
                throw new RuntimeException("Getting fields failed");
            }
            throw new RuntimeException("unexpected field type");
        }

        int getVal(Field f) {
            try {
                if (f.getType() == int.class) {
                    return f.getInt(this);
                } else if (f.getType() == short.class) {
                    return (int)f.getShort(this);
                } else if (f.getType() == byte.class) {
                    return (int)f.getByte(this);
                } else if (f.getType() == long.class) {
                    return (int)f.getLong(this);
                }
            } catch(IllegalAccessException iae) {
                throw new RuntimeException("Setting fields failed");
            }
            throw new RuntimeException("unexpected field type");
        }

        boolean fields_equal(Class c, Base o) {
            for (Field f : c.getDeclaredFields()) {
                if (getVal(f) != o.getVal(f)) {
                    return false;
                }
            }
            if (c != Base.class) {
                return fields_equal(c.getSuperclass(), o);
            }
            return true;
        }

        public boolean equals(Object obj) {
            return fields_equal(getClass(), (Base)obj);
        }

        String print_fields(Class c, String s) {
            for (Field f : c.getDeclaredFields()) {
                if (s != "") {
                    s += "\n";
                }
                s = s + f + " = " + getVal(f);
            }
            if (c != Base.class) {
                return print_fields(c.getSuperclass(), s);
            }
            return s;
        }

        public String toString() {
            return print_fields(getClass(), "");
        }

        int fields_sum(Class c, int s) {
            for (Field f : c.getDeclaredFields()) {
                s += getVal(f);
            }
            if (c != Base.class) {
                return fields_sum(c.getSuperclass(), s);
            }
            return s;
        }

        public int sum() {
            return fields_sum(getClass(), 0);
        }

    }

    static class A extends Base {
        int i1;
        int i2;
        int i3;
        int i4;
        int i5;

        A(boolean initialize) {
            super(initialize);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static class B extends A {
        int i6;

        B(boolean initialize) {
            super(initialize);
        }
    }

    static final class D extends Base {
        byte  i1;
        short i2;
        long  i3;
        int   i4;
        int   i5;

        D(boolean initialize) {
            super(initialize);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static final class E extends Base {
        int i1;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;

        E(boolean initialize) {
            super(initialize);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static final class F extends Base {
        F(boolean initialize) {
            super(initialize);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static class G extends Base {
        int i1;
        int i2;
        int i3;

        G(boolean initialize) {
            super(initialize);
        }

        public Object myclone() throws CloneNotSupportedException {
            return clone();
        }
    }

    static class H extends G {
        int i4;
        int i5;

        H(boolean initialize) {
            super(initialize);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static class J extends Base  {
        int i1;
        int i2;
        int i3;

        J(boolean initialize) {
            super(initialize);
        }

        public Object myclone() throws CloneNotSupportedException {
            return clone();
        }
    }

    static class K extends J {
        int i4;
        int i5;

        K(boolean initialize) {
            super(initialize);
        }

    }

    static final A a = new A(true);
    static final B b = new B(true);
    static final D d = new D(true);
    static final E e = new E(true);
    static final F f = new F(true);
    static final G g = new G(true);
    static final H h = new H(true);
    static final J j = new J(true);
    static final K k = new K(true);

    final HashMap<String,Method> tests = new HashMap<>();
    {
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.getName().matches("m[0-9]+")) {
                assert(Modifier.isStatic(m.getModifiers())) : m;
                tests.put(m.getName(), m);
            }
        }
    }

    boolean success = true;

    void doTest(Base src, String name) throws Exception {
        Method m = tests.get(name);

        for (int i = 0; i < 20000; i++) {
            boolean failure = false;
            Base res = null;
            int s = 0;
            Class retType = m.getReturnType();
            if (retType.isPrimitive()) {
                if (!retType.equals(Void.TYPE)) {
                    s = (int)m.invoke(null, src);
                    failure = (s != src.sum());
                } else {
                    m.invoke(null, src);
                }
            } else {
                res = (Base)m.invoke(null, src);
                failure = !res.equals(src);
            }
            if (failure) {
                System.out.println("Test " + name + " failed");
                System.out.println("source: ");
                System.out.println(src);
                System.out.println("result: ");
                if (m.getReturnType().isPrimitive()) {
                    System.out.println(s);
                } else {
                    System.out.println(res);
                }
                success = false;
                break;
            }
        }
    }

}
