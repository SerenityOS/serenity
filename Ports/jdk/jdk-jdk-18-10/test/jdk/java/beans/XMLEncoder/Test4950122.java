/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4950122
 * @summary Tests DefaultPersistenceDelegate with boolean and integer property
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;

public final class Test4950122 {
    public static void main(String[] args) {
        TestBean bean = new TestBean(true, 11);
        Encoder encoder = new Encoder();
        encoder.setExceptionListener(bean);
        new TestDPD().instantiate(bean, encoder);
    }

    public static class TestDPD extends DefaultPersistenceDelegate {
        public TestDPD() {
            super(new String[] {"boolean", "integer"});
        }

        public Expression instantiate(Object oldInstance, Encoder out) {
            return super.instantiate(oldInstance, out);
        }
    }

    public static class TestBean implements ExceptionListener {
        private boolean b;
        private int i;

        public TestBean(boolean b, int i) {
            this.b = b;
            this.i = i;
        }

        public boolean isBoolean() {
            return this.b;
        }

        public int getInteger() {
            return this.i;
        }

        public void exceptionThrown(Exception exception) {
            throw new Error("unexpected exception", exception);
        }
    }
}
