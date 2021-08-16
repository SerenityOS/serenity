/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6799551
  @summary Extended key codes for small letters undefined
  @author Andrei Dmitriev: area=awt.keyboard
  @modules java.desktop/sun.awt
  @run main EqualKeyCode
*/


import java.awt.*;
import java.awt.event.KeyEvent;

public class EqualKeyCode {

    final static String LETTERS = "abcdefghijklmnopqrstuvwxyz";

    public static void main(String []s) {
        for (int i = 0; i < LETTERS.length(); i++){
            char cSmall = LETTERS.charAt(i);
            char cLarge = Character.toUpperCase(cSmall);

            int iSmall = KeyEvent.getExtendedKeyCodeForChar(cSmall);
            int iLarge = KeyEvent.getExtendedKeyCodeForChar(cLarge);

            System.out.print(" " + cSmall + ":" + iSmall + " ---- ");
            System.out.println(" " + cLarge + " : " + iLarge);
            if (KeyEvent.getExtendedKeyCodeForChar(cSmall) !=
                KeyEvent.getExtendedKeyCodeForChar(cLarge))
            {
                throw new RuntimeException("ExtendedKeyCode doesn't exist or doesn't match between capital and small letters.");
            }
        }
    }
}
