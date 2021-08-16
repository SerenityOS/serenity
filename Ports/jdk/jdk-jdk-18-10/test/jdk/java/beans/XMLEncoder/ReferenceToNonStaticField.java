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

import java.awt.font.TextAttribute;

/**
 * @test
 * @bug 8060027
 * @run main/othervm -Djava.security.manager=allow ReferenceToNonStaticField
 */
public final class ReferenceToNonStaticField
        extends AbstractTest<ReferenceToNonStaticField.TestValue> {

    public static final class TestValue {

        // reference to static field
        public TextAttribute font_default = TextAttribute.FONT;
        public TextAttribute family_default = TextAttribute.FAMILY;
        public TextAttribute family_set1; // will be set to the same as default
        public TextAttribute family_set2; // will be set to the same as default
        public TextAttribute family_set3; // will be set to the same as default

        // primitive small
        public int int_1_default = 1;
        public int int_10_default = 10;
        public int int_10_set1; // will be set to the same as default
        public int int_10_set2; // will be set to the same as default
        public int int_10_set3; // will be set to the same as default

        // primitive big
        public int int_1000_default = 1000;
        public int int_2000_default = 2000;
        public int int_2000_set1; // will be set to the same as default
        public int int_2000_set2; // will be set to the same as default
        public int int_2000_set3; // will be set to the same as default

        // wrappers
        public Integer integer_1_default = new Integer(1);
        public Integer integer_10_default = new Integer(10);
        public Integer integer_10_set1; // will be set to the same as default
        public Integer integer_10_set2; // will be set to the same as default
        public Integer integer_10_set3; // will be set to the same as default

        public TestValue() {
        }

        public TestValue(final Object ignored) {
            // set some fields to non-default values, so they will be saved
            family_set1 = family_default;
            family_set3 = family_default;
            family_set2 = family_default;
            int_10_set1 = int_10_default;
            int_10_set2 = int_10_default;
            int_10_set3 = int_10_default;
            int_2000_set1 = int_2000_default;
            int_2000_set2 = int_2000_default;
            int_2000_set3 = int_2000_default;
            integer_10_set1 = integer_10_default;
            integer_10_set2 = integer_10_default;
            integer_10_set3 = integer_10_default;
        }
    }

    public static void main(final String[] args) {
        new ReferenceToNonStaticField().test(true);
    }

    protected TestValue getObject() {
        return new TestValue(new Object());
    }

    @Override
    protected void validate(final TestValue before,final TestValue after) {
        super.validate(before, after);
        validate(before);
        validate(after);
    }

    private static void validate(final TestValue object) {
        // reference to static field
        if (object.font_default != TextAttribute.FONT) {
            throw new Error("Wrong font_default: " + object.font_default);
        }
        if (object.family_default != TextAttribute.FAMILY) {
            throw new Error("Wrong family_default: " + object.family_default);
        }
        if (object.family_set1 != object.family_default) {
            throw new Error("Wrong family_set1: " + object.family_set1);
        }
        if (object.family_set2 != object.family_default) {
            throw new Error("Wrong family_set2: " + object.family_set2);
        }
        if (object.family_set3 != object.family_default) {
            throw new Error("Wrong family_set3: " + object.family_set3);
        }
        // primitive small
        if (object.int_1_default != 1) {
            throw new Error("Wrong int_1_default: " + object.int_1_default);
        }
        if (object.int_10_default != 10) {
            throw new Error("Wrong int_10_default: " + object.int_10_default);
        }
        if (object.int_10_set1 != object.int_10_default) {
            throw new Error("Wrong int_10_set1: " + object.int_10_set1);
        }
        if (object.int_10_set2 != object.int_10_default) {
            throw new Error("Wrong int_10_set2: " + object.int_10_set2);
        }
        if (object.int_10_set3 != object.int_10_default) {
            throw new Error("Wrong int_10_set3: " + object.int_10_set3);
        }
        // primitive big
        if (object.int_1000_default != 1000) {
            throw new Error("Wrong int_1000_default: " + object.int_1000_default);
        }
        if (object.int_2000_default != 2000) {
            throw new Error("Wrong int_2000_default: " + object.int_2000_default);
        }
        if (object.int_2000_set1 != object.int_2000_default) {
            throw new Error("Wrong int_2000_set1: " + object.int_2000_set1);
        }
        if (object.int_2000_set2 != object.int_2000_default) {
            throw new Error("Wrong int_2000_set2: " + object.int_2000_set2);
        }
        if (object.int_2000_set3 != object.int_2000_default) {
            throw new Error("Wrong int_2000_set3: " + object.int_2000_set3);
        }
        // wrappers
        if (!object.integer_1_default.equals(new Integer(1))) {
            throw new Error("Wrong integer_1_default: " + object.integer_1_default);
        }
        if (!object.integer_10_default.equals(new Integer(10))) {
            throw new Error("Wrong integer_10_default: " + object.integer_10_default);
        }
        if (object.integer_10_set1 != object.integer_10_default) {
            throw new Error("Wrong integer_10_set1: " + object.integer_10_set1);
        }
        if (object.integer_10_set2 != object.integer_10_default) {
            throw new Error("Wrong integer_10_set2: " + object.integer_10_set2);
        }
        if (object.integer_10_set3 != object.integer_10_default) {
            throw new Error("Wrong integer_10_set3: " + object.integer_10_set3);
        }
    }
}
