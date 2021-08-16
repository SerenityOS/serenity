/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6625450
 * @summary Tests the baseline of the titled border
 * @author Sergey Malenkov
 */

import java.awt.Component;
import javax.swing.border.TitledBorder;

public class Test6625450 {

    public static void main(String[] args) {
        // test height
        test(false, 0, Integer.MAX_VALUE);
        test(false, 0, 1);
        test(true, 0, -1);
        test(true, 0, Integer.MIN_VALUE);
        // test width
        test(false, Integer.MAX_VALUE, 0);
        test(false, 1, 0);
        test(true, -1, 0);
        test(true, Integer.MIN_VALUE, 0);
    }

    private static final TitledBorder BORDER = new TitledBorder("123");
    private static final Component COMPONENT = new Component() {
    };

    private static void test(boolean expected, int width, int height) {
        try {
            BORDER.getBaseline(COMPONENT, width, height);

            if (expected) {
                throw new Error("expected IllegalArgumentException");
            }
        }
        catch (IllegalArgumentException exception) {
            if (!expected) {
                throw new Error("unexpected exception", exception);
            }
        }
    }
}
