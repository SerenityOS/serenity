/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.Expression;
import java.beans.XMLEncoder;

final class TestEncoder extends XMLEncoder {
    private Expression expression;

    private TestEncoder() {
        super(System.out);
    }

    @Override
    public void writeExpression(Expression expression) {
        if (this.expression == null) {
            this.expression = expression;
        }
        super.writeExpression(expression);
    }

    public static void test(Object provider, Object object, Object value) {
        System.setSecurityManager(new SecurityManager());

        TestEncoder encoder = new TestEncoder();
        encoder.setPersistenceDelegate(
                object.getClass(),
                encoder.getPersistenceDelegate(provider.getClass()));
        encoder.writeObject(object);
        encoder.close();

        if (encoder.expression != null) {
            for (Object argument : encoder.expression.getArguments()) {
                if (value.equals(argument)) {
                    throw new Error("Found private value!");
                }
            }
        }
    }
}
