/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4990596
 * @summary Make sure that any subclass of Number can be formatted using DecimalFormat.format().
 */

import java.text.DecimalFormat;

public class Bug4990596 {

    public static void main(String[] args) {
        new DecimalFormat().format(new MutableInteger(0));
    }

    @SuppressWarnings("serial")
    public static class MutableInteger extends Number {
        public int value;

        public MutableInteger() {
        }
        public MutableInteger(int value) {
            this.value = value;
        }
        public double doubleValue() {
            return this.value;
        }
        public float floatValue() {
            return this.value;
        }
        public int intValue() {
            return this.value;
        }
        public long longValue() {
            return this.value;
        }
    }
}
