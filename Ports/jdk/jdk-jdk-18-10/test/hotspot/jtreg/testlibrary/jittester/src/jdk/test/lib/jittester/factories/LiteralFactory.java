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

package jdk.test.lib.jittester.factories;

import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.utils.PseudoRandom;

import java.util.Locale;

class LiteralFactory extends Factory<Literal> {
    protected final Type resultType;

    LiteralFactory(Type resultType) {
        this.resultType = resultType;
    }

    @Override
    public Literal produce() throws ProductionFailedException {
        Literal literal;
        if (resultType.equals(TypeList.BOOLEAN)) {
            literal = new Literal(PseudoRandom.randomBoolean(), TypeList.BOOLEAN);
        } else if (resultType.equals(TypeList.CHAR)) {
            literal = new Literal((char) ((char) (PseudoRandom.random() * ('z' - 'A')) + 'A'), TypeList.CHAR);
        } else if (resultType.equals(TypeList.INT)) {
            literal = new Literal((int) (PseudoRandom.random() * Integer.MAX_VALUE), TypeList.INT);
        } else if (resultType.equals(TypeList.LONG)) {
            literal = new Literal((long) (PseudoRandom.random() * Long.MAX_VALUE), TypeList.LONG);
        } else if (resultType.equals(TypeList.FLOAT)) {
            literal = new Literal(Float.valueOf(String.format(
                    (Locale) null,
                    "%." + ProductionParams.floatingPointPrecision.value() + "EF",
                    (float) PseudoRandom.random() * Float.MAX_VALUE)),
                    TypeList.FLOAT);
        } else if (resultType.equals(TypeList.DOUBLE)) {
            literal = new Literal(Double.valueOf(String.format(
                    (Locale) null,
                    "%." + 2 * ProductionParams.floatingPointPrecision.value() + "E",
                    PseudoRandom.random() * Double.MAX_VALUE)),
                    TypeList.DOUBLE);
        } else if (resultType.equals(TypeList.BYTE)) {
            literal = new Literal((byte)(PseudoRandom.random() * Byte.MAX_VALUE), TypeList.BYTE);
        } else if (resultType.equals(TypeList.SHORT)) {
            literal = new Literal((short)(PseudoRandom.random() * Short.MAX_VALUE), TypeList.SHORT);
        } else if (resultType.equals(TypeList.STRING)) {
            int size = (int) (PseudoRandom.random() * ProductionParams.stringLiteralSizeLimit.value());
            byte[] str = new byte[size];
            for (int i = 0; i < size; i++) {
                str[i] = (byte) ((int) (('z' - 'a') * PseudoRandom.random()) + 'a');
            }
            literal = new Literal(new String(str), TypeList.STRING);
        } else {
            throw new ProductionFailedException();
        }
        return literal;
    }
}
