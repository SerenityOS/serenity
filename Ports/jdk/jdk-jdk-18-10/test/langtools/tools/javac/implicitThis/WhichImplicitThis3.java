/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4696701
 * @summary wrong enclosing instance for local class creation
 *
 * @compile WhichImplicitThis3.java
 * @run main WhichImplicitThis3
 */

public class WhichImplicitThis3 {
    boolean isCorrect() { return true; }
    void check() {
        class I2 {
            public void check() {
                if (!isCorrect()) throw new Error();
            }
        }
        class I3 extends WhichImplicitThis3 {
            boolean isCorrect() { return false; }
            public void check() {
                new I2().check(); // which outer does I2 get?
            }
        }
        new I3().check();
    }
    public static void main(String[] args) {
        new WhichImplicitThis3().check();
    }
}
