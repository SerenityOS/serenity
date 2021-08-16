/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot.test;

import org.testng.annotations.DataProvider;

public class ForObjectDataProvider {
    @DataProvider(name = "forObjectDataProvider")
    public static Object[][] forObjectDataProvider() {
        return new Object[][]{
                        {TestHelper.DUMMY_CLASS_INSTANCE.objectField,
                                        "Object[Object@" + TestHelper.DUMMY_CLASS_INSTANCE.objectField.hashCode() + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.stringField,
                                        "Object[String:\"" + TestHelper.DUMMY_CLASS_INSTANCE.stringField + "\"]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.booleanField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.booleanField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.byteField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.byteField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.charField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.charField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.shortField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.shortField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.intField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.intField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.longField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.longField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.floatField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.floatField + "]"},
                        {TestHelper.DUMMY_CLASS_INSTANCE.doubleField,
                                        "Object[" + TestHelper.DUMMY_CLASS_INSTANCE.doubleField + "]"},
                        {new Object[0], "Object[Object[" + 0 + "]{}]"}, {new Object[1], "Object[Object[" + 1 + "]{null}]"},
                        {null, "Object[null]"}};
    }
}
