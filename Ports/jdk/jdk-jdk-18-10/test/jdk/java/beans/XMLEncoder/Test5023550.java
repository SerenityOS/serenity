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
 * @bug 5023550
 * @summary Tests complex references to owner
 * @run main/othervm -Djava.security.manager=allow Test5023550
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

public class Test5023550 extends AbstractTest {
    public static void main(String[] args) {
        new Test5023550().test(true);
    }

    private final Owner owner = new Owner();

    @Override
    protected void initialize(XMLEncoder encoder) {
        encoder.setOwner(this.owner);
        encoder.setPersistenceDelegate(A.class, new ADelegate());
        encoder.setPersistenceDelegate(B.class, new BDelegate());
        encoder.setPersistenceDelegate(C.class, new CDelegate());
    }

    @Override
    protected void initialize(XMLDecoder decoder) {
        decoder.setOwner(this.owner);
    }

    protected Object getObject() {
        return this.owner.newA(this.owner.newB().newC());
    }

    public static class Owner {
        public A newA(C c) {
            return new A(c);
        }

        public B newB() {
            return new B();
        }
    }

    public static class A {
        private final C c;

        private A(C c) {
            this.c = c;
        }

        public C getC() {
            return this.c;
        }
    }

    public static class B {
        public C newC() {
            return new C(this);
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

    public static class ADelegate extends DefaultPersistenceDelegate {
        protected Expression instantiate(Object old, Encoder out) {
            XMLEncoder encoder = (XMLEncoder) out;
            A a = (A) old;
            return new Expression(old, encoder.getOwner(), "newA", new Object[] { a.getC() });
        }
    }

    public static class BDelegate extends DefaultPersistenceDelegate {
        protected Expression instantiate(Object old, Encoder out) {
            XMLEncoder encoder = (XMLEncoder) out;
            return new Expression(old, encoder.getOwner(), "newB", new Object[0]);
        }
    }

    public static class CDelegate extends DefaultPersistenceDelegate {
        protected Expression instantiate(Object old, Encoder out) {
            C c = (C) old;
            return new Expression(c, c.getB(), "newC", new Object[0]);
        }
    }
}
