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

package com.foo;

import java.text.*;

/**
 * FooNumberFormat provides DecimalFormat methods required for the SPI testing.
 */
public class FooNumberFormat extends NumberFormat {
    private DecimalFormat df;

    public FooNumberFormat(String pattern, DecimalFormatSymbols dfs) {
        df = new DecimalFormat(pattern, dfs);
    }

    @Override
    public StringBuffer format(double number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        return df.format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(long number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        return df.format(number, toAppendTo, pos);
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        return df.parse(source, parsePosition);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FooNumberFormat
            && df.equals(((FooNumberFormat)other).df);
    }

    @Override
    public int hashCode() {
        return df.hashCode();
    }

    // DecimalFormat specific methods required for testing

    public String toPattern() {
        return df.toPattern();
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return df.getDecimalFormatSymbols();
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        df.setDecimalSeparatorAlwaysShown(newValue);
    }
}
