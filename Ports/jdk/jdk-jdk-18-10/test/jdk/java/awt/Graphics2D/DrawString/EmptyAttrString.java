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

/**
 * @test
 * @bug 6699843
 * @summary IllegalArgumentException when using Graphics.drawString( "", 0, 0 )
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;

public class EmptyAttrString {

    public static void main(String[] args) {
        BufferedImage bi =
           new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        Font f = new Font( "Dialog", Font.PLAIN, 12 );
        Map map = new HashMap();
        map.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        f = f.deriveFont(map);
        g.setFont(f);
        g.drawString("", 50, 50);
        g.drawString("", 50f, 50f);
        char[] chs = { } ;
        g.drawChars(chs, 0, 0, 50, 50);
        byte[] bytes = { } ;
        g.drawBytes(bytes, 0, 0, 50, 50);
        AttributedString astr = new AttributedString("");
        g.drawString(astr.getIterator(), 50, 50);
        g.drawString(astr.getIterator(), 50f, 50f);
        return;
    }
}
