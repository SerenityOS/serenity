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

/*
  @test
  @bug 6783910
  @summary java.awt.Color.brighter()/darker() methods make color opaque
  @author Andrei Dmitriev: area=awt-color
  @run main OpacityChange
*/

import java.awt.*;

public class OpacityChange {
    private final static int INITIAL_ALPHA = 125;

    public static void main(String argv[]) {
        Color color = new Color(20, 20, 20, INITIAL_ALPHA);
        System.out.println("Initial alpha: " + color.getAlpha());
        Color colorBrighter = color.brighter();
        System.out.println("New alpha (after brighter): " + colorBrighter.getAlpha());

        Color colorDarker = color.darker();
        System.out.println("New alpha (after darker): " + colorDarker.getAlpha());


        if (INITIAL_ALPHA != colorBrighter.getAlpha()) {
            throw new RuntimeException("Brighter color alpha has changed from : " +INITIAL_ALPHA + " to " + colorBrighter.getAlpha());
        }
        if (INITIAL_ALPHA != colorDarker.getAlpha()) {
            throw new RuntimeException("Darker color alpha has changed from : " +INITIAL_ALPHA + " to " + colorDarker.getAlpha());
        }
    }
}
