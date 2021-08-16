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
 * @bug 4936682
 * @summary Tests encoding of reference to target
 * @run main/othervm -Djava.security.manager=allow Test4936682
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.XMLEncoder;

public final class Test4936682 extends AbstractTest<Object[]> {
    public static void main(String[] args) {
        new Test4936682().test(true);
    }

    protected Object[] getObject() {
        OuterClass outer = new OuterClass();
        return new Object [] {outer.getInner(), outer};
    }

    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(
                OuterClass.InnerClass.class,
                new DefaultPersistenceDelegate() {
                    protected Expression instantiate(Object oldInstance, Encoder out) {
                        OuterClass.InnerClass inner = (OuterClass.InnerClass) oldInstance;
                        OuterClass outer = inner.getOuter();
                        return new Expression(inner, outer, "getInner", new Object[0]);
                    }
                }
        );
    }

    protected void validate(Object[] before, Object[] after) {
        validate(before);
        validate(after);
    }

    private static void validate(Object[] array) {
        if (2 != array.length) {
            throw new Error("unexpected array length: " + array.length);
        }
        OuterClass outer = (OuterClass) array[1];
        if (!outer.getInner().equals(array[0])) {
            throw new Error("unexpected array content");
        }
    }

    public static final class OuterClass {
        private final InnerClass inner = new InnerClass();

        public InnerClass getInner() {
            return this.inner;
        }

        public class InnerClass {
            private InnerClass() {
            }

            public OuterClass getOuter() {
                return OuterClass.this;
            }
        }
    }
}
