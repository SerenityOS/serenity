/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4146853
 * @summary Make sure we can construct an AttributedString from
 * an AttributedCharacterIterator covering only a subrange
 * @modules java.desktop
 */

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Hashtable;

public class TestAttributedStringCtor {

    public static void main(String[] args) {

        // Create a new AttributedString with one attribute.
        Hashtable attributes = new Hashtable();
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        AttributedString origString = new AttributedString("Hello world.", attributes);

        // Create an iterator over part of the AttributedString.
        AttributedCharacterIterator iter = origString.getIterator(null, 4, 6);

        // Attempt to create a new AttributedString from the iterator.
        // This will throw IllegalArgumentException.
        AttributedString newString = new AttributedString(iter);

        // Without the exception this would get executed.
        System.out.println("DONE");
    }
}
