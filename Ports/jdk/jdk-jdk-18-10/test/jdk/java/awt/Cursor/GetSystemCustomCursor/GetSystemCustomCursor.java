/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Cursor;

/**
 * @test
 * @key headful
 * @bug 8039269
 * @author Sergey Bylokhov
 */
public final class GetSystemCustomCursor {

    public static void main(final String[] args) throws AWTException {
        // This list is copied from cursors.properties
        String[] names = {"CopyDrop.32x32", "MoveDrop.32x32", "LinkDrop.32x32",
                          "CopyNoDrop.32x32", "MoveNoDrop.32x32",
                          "LinkNoDrop.32x32", "Invalid.32x32"};
        for (final String name : names) {
            if (Cursor.getSystemCustomCursor(name) == null) {
                throw new RuntimeException("Cursor is null: " + name);
            }
        }
        System.out.println("Test passed");
    }
}
