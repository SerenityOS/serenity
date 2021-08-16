/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5023557
 * @summary Tests complex references
 * @run main/othervm -Djava.security.manager=allow Test5023557
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.XMLEncoder;

public class Test5023557 extends AbstractTest {
    public static void main(String[] args) {
        new Test5023557().test(true);
    }

    @Override
    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(B.class, new BDelegate());
        encoder.setPersistenceDelegate(C.class, new CDelegate());
    }

    protected Object getObject() {
        A a = new A();
        return a.newC(a.newB());
    }

    public static class A {
        public B newB() {
            return new B(this);
        }

        public C newC(B b) {
            return new C(b);
        }
    }

    public static class B {
        private final A a;

        private B(A a) {
            this.a = a;
        }

        public A getA() {
            return this.a;
        }
    }

    public static class C {
        private final B b;

        private C(B b) {
            this.b = b;
        }

        public B getB() {
            return this.b;
        }
    }

    public static class BDelegate extends DefaultPersistenceDelegate {
        protected Expression instantiate(Object old, Encoder out) {
            B b = (B) old;
            return new Expression(b, b.getA(), "newB", new Object[0]);
        }
    }

    public static class CDelegate extends DefaultPersistenceDelegate {
        protected Expression instantiate(Object old, Encoder out) {
            C c = (C) old;
            return new Expression(c, c.getB().getA(), "newC", new Object[] { c.getB() });
        }
    }
}
