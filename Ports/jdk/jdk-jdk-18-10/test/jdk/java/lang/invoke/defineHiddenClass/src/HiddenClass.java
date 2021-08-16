/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  The classfile for this class will be loaded directly and used to define
 *  a hidden class.
 */
public class HiddenClass implements HiddenTest {

    HiddenClass other = null;

    private String realTest() {
        Object o = other;
        HiddenClass local = this;
        local = other;
        local = (HiddenClass) o;
        local = new HiddenClass();

        set_other_maybe(new Object());
        set_other_maybe(this);
        return "HiddenClass";
    }

    private void set_other_maybe(Object o) {
        if (o instanceof HiddenClass) {
        }
    }

    public void test() {
        String result = realTest();
        // Make sure that the Utf8 constant pool entry for "HiddenClass" is okay.
        if (!result.substring(0, 7).equals("HiddenC") ||
            !result.substring(7).equals("lass")) {
            throw new RuntimeException("'HiddenClass string is bad: " + result);
        }

    }
}
