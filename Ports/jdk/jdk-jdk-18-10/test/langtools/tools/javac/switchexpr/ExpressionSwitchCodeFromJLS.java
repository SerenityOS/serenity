/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8206986
 * @summary Check switch expressions
 * @compile ExpressionSwitchCodeFromJLS.java
 * @run main ExpressionSwitchCodeFromJLS
 */

public class ExpressionSwitchCodeFromJLS {
    static void howMany(int k) {
        switch (k) {
            case 1: System.out.print("one ");
            case 2: System.out.print("too ");
            case 3: System.out.println("many");
        }
    }
    static void howManyRule(int k) {
        switch (k) {
            case 1 -> System.out.println("one");
            case 2 -> System.out.println("two");
            case 3 -> System.out.println("many");
        }
    }
    static void howManyGroup(int k) {
        switch (k) {
            case 1: System.out.println("one");
                break;  // exit the switch
            case 2: System.out.println("two");
                break;  // exit the switch
            case 3: System.out.println("many");
                break;  // not needed, but good style
        }
    }
    public static void main(String[] args) {
        howMany(3);
        howMany(2);
        howMany(1);
        howManyRule(1);
        howManyRule(2);
        howManyRule(3);
        howManyGroup(1);
        howManyGroup(2);
        howManyGroup(3);
    }
}
