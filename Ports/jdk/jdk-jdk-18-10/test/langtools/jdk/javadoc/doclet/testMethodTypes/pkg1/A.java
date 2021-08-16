/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

/**
 * This class has a mixture of different types of methods. The methods summary
 * table should appear with "All Methods", "Static Methods", "Instance Methods",
 * "Concrete Methods" and "Deprecated Methods".
 */
public class A {

    /**
     * This is the first concrete instance method.
     */
    public void readObject() {
    }

    /**
     * This is the second concrete instance method.
     */
    public final void setStub() {
    }

    /**
     * This is the third concrete instance method.
     * @return a string
     */
    public String getParameter() {
         return "test";
     }

    /**
     * This is the first concrete instance deprecated method.
     * @deprecated This is a deprecated method that should appear in the tab.
     */
    public void resize() {
    }

    /**
     * This is the fourth concrete instance method.
     */
    public void showStatus() {
    }

    /**
     * This is the first concrete static method.
     */
    public final static void staticMethod() {
    }

    /**
     * This is the second concrete instance deprecated method.
     */
    @Deprecated
    public void init() {
    }
}
