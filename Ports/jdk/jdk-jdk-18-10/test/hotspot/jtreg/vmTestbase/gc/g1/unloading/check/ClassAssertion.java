/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.check;

import sun.hotspot.WhiteBox;

/**
 * This assertion checks that class is alive using WhiteBox isClassAlive method.
 */
public class ClassAssertion extends Assertion {

    private String className;

    private boolean shouldBeAlive;

    private static long counterOfCheckedUnloaded = 0;

    private static long counterOfCheckedAlive = 0;

    public static long getCounterOfCheckedUnloaded() {
        return counterOfCheckedUnloaded;
    }

    public static long getCounterOfCheckedAlive() {
        return counterOfCheckedAlive;
    }

    public ClassAssertion(String className, boolean shouldBeAlive) {
        this.shouldBeAlive = shouldBeAlive;
        this.className = className;
    }

    @Override
    public void check() {
        boolean isAlive = WhiteBox.getWhiteBox().isClassAlive(className);
        if (isAlive != shouldBeAlive) {
            if (isAlive) {
                throw new RuntimeException("Class " + className + " was not unloaded! Failing test.");
            } else {
                throw new RuntimeException("Class " + className + " must live! Failing test.");
            }
        } else {
            System.out.println(" Check OK, class " + className + ", isAlive = " + isAlive + ", shouldBeAlive = " + shouldBeAlive);
            if (isAlive) {
                counterOfCheckedAlive++;
            } else {
                counterOfCheckedUnloaded++;
            }
        }
    }

    private static long numberOfChecksLimit = -1;

    static {
        String s;
        if ((s = System.getProperty("NumberOfChecksLimit")) != null) {
            numberOfChecksLimit = Long.valueOf(s);
        }
    }

}
