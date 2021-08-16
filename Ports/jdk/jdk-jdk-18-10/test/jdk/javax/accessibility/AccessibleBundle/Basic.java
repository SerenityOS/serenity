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

import java.util.Locale;

import javax.accessibility.AccessibleBundle;

import static javax.accessibility.AccessibleRole.ALERT;
import static javax.accessibility.AccessibleRole.LABEL;
import static javax.accessibility.AccessibleRole.PANEL;
import static javax.accessibility.AccessibleState.MANAGES_DESCENDANTS;

/**
 * @test
 * @bug 8213516
 * @summary Checks basic functionality of AccessibleBundle class
 */
public final class Basic extends AccessibleBundle {

    private Basic(final String key) {
        this.key = key;
    }

    public static void main(final String[] args) {
        testStandardResource();
        testCustomResource();
    }

    private static void testCustomResource() {
        final Basic bundle = new Basic("managesDescendants");
        test(bundle.toDisplayString(Locale.ENGLISH), "manages descendants");
        test(bundle.toDisplayString("NonExistedBundle", Locale.ENGLISH),
             "managesDescendants");
    }

    private static void testStandardResource() {
        test(ALERT.toDisplayString(Locale.ENGLISH), "alert");
        test(ALERT.toDisplayString(Locale.JAPAN), "\u30a2\u30e9\u30fc\u30c8");
        test(LABEL.toDisplayString(Locale.ENGLISH), "label");
        test(LABEL.toDisplayString(Locale.JAPAN), "\u30e9\u30d9\u30eb");
        test(PANEL.toDisplayString(Locale.ENGLISH), "panel");
        test(PANEL.toDisplayString(Locale.JAPAN), "\u30D1\u30CD\u30EB");
        test(MANAGES_DESCENDANTS.toDisplayString(Locale.ENGLISH),
             "manages descendants");
        test(MANAGES_DESCENDANTS.toDisplayString(Locale.JAPAN),
             "\u5B50\u5B6B\u3092\u7BA1\u7406");
    }

    private static void test(final String actual, final String expected) {
        if (!actual.equals(expected)) {
            System.err.println("Expected: " + expected);
            System.err.println("Actual: " + actual);
            throw new RuntimeException("Wrong text");
        }
    }
}
