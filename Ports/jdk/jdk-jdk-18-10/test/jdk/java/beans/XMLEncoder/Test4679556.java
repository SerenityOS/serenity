/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4679556
 * @summary Tests for duplication of some kind instances
 * @run main/othervm -Djava.security.manager=allow Test4679556
 * @author Sergey Malenkov, Mark Davidson, Philip Milne
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.XMLEncoder;

/**
 * Demonstrates the archiver bug, where the XMLEncoder duplicates
 * the instance of class A because it is required as the target of
 * a factory method (to produce an instance of class C).
 * See the output in the file Test.xml for the results and note
 * the (invalid) forward reference to the instance of class C.
 *
 * TO FIX
 *
 * Move the first line of the XMLEncoder::mark(Statement method)
 * to the end of the method.
 * I.e. replace the mark() method in XMLEncoder with this:
 * <pre>private void mark(Statement stm) {
 *     Object[] args = stm.getArguments();
 *     for (int i = 0; i < args.length; i++) {
 *         Object arg = args[i];
 *         mark(arg, true);
 *     }
 *     mark(stm.getTarget(), false);
 * }</pre>
 *
 * VALID ARCHIVE (WITH FIX):
 * <pre>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;java version="1.4.0" class="java.beans.XMLDecoder"&gt;
 *  &lt;object class="TestDuplicates$A"&gt;
 *   &lt;void id="TestDuplicates$C0" method="createC"/&gt;
 *   &lt;void property="x"&gt;
 *    &lt;void property="x"&gt;
 *     &lt;object idref="TestDuplicates$C0"/&gt;
 *    &lt;/void&gt;
 *   &lt;/void&gt;
 *  &lt;/object&gt;
 * &lt;/java&gt;</pre>
 *
 * INVALID ARCHIVE (WITHOUT FIX):
 *  &lt;object class="TestDuplicates$A"&gt;
 *   &lt;void property="x"&gt;
 *    &lt;void property="x"&gt;
 *     &lt;void class="TestDuplicates$A"&gt;
 *      &lt;void property="x"&gt;
 *       &lt;void property="x"&gt;
 *        &lt;object idref="TestDuplicates$C0"/&gt;
 *       &lt;/void&gt;
 *      &lt;/void&gt;
 *      &lt;void id="TestDuplicates$C0" method="createC"/&gt;
 *     &lt;/void&gt;
 *     &lt;object idref="TestDuplicates$C0"/&gt;
 *    &lt;/void&gt;
 *   &lt;/void&gt;
 *   &lt;void id="TestDuplicates$C0" method="createC"/&gt;
 *  &lt;/object&gt;
 * &lt;/java&gt;</pre>
 */
public class Test4679556 extends AbstractTest {
    public static void main(String[] args) {
        new Test4679556().test(true);
    }

    protected Object getObject() {
        A a = new A();
        B b = (B) a.getX();
        b.setX(a.createC());
        return a;
    }

    protected Object getAnotherObject() {
        return new A();
    }

    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(C.class, new DefaultPersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                C c = (C) oldInstance;
                return new Expression(c, c.getX(), "createC", new Object[] {});
            }
        });
    }

    public static class Base {
        private Object x;

        public Object getX() {
            return this.x;
        }

        public void setX(Object x) {
            this.x = x;
        }
    }

    public static class A extends Base {
        public A() {
            setX(new B());
        }

        public C createC() {
            return new C(this);
        }
    }

    public static class B extends Base {
    }

    public static class C extends Base {
        private C(Object x) {
            setX(x);
        }
    }
}
