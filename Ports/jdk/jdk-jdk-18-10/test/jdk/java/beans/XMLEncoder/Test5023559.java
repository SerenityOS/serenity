/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5023559
 * @summary Tests encoding of the object with nested target
 * @run main/othervm -Djava.security.manager=allow Test5023559
 * @author Sergey Malenkov
 */

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;

public final class Test5023559 extends AbstractTest {
    public static void main(String[] args) {
        new Test5023559().test(true);
    }

    protected Object getObject() {
        return new GrandParent().create().create();
    }

    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(Parent.class, new PersistenceDelegate() {
            protected Expression instantiate(Object old, Encoder out) {
                Parent parent = (Parent) old;
                return new Expression(old, parent.getParent(), "create", new Object[] {});
            }
        });
        encoder.setPersistenceDelegate(Child.class, new PersistenceDelegate() {
            protected Expression instantiate(Object old, Encoder out) {
                Child child = (Child) old;
                return new Expression(old, child.getParent(), "create", new Object[] {});
            }
        });
    }

    public static final class GrandParent {
        public Parent create() {
            return new Parent(this);
        }
    }

    public static final class Parent {
        private final GrandParent parent;

        private Parent(GrandParent parent) {
            this.parent = parent;
        }

        public GrandParent getParent() {
            return this.parent;
        }

        public Child create() {
            return new Child(this);
        }
    }

    public static final class Child {
        private final Parent parent;

        private Child(Parent parent) {
            this.parent = parent;
        }

        public Parent getParent() {
            return this.parent;
        }
    }
}
