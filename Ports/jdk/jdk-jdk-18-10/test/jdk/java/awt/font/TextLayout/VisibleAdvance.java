/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;

/* @test
 * @summary verify TextLine advance
 * @bug 6582460 8164818
 */

/*
 Sample correct output:
Left-to-right (One style): Advance = 127.30078, Visible advance = 118.30078
Right-to-left (One style): Advance = 127.30078, Visible advance = 118.30078
Left-to-right (Multiple styles): Advance = 127.30078, Visible advance = 118.30078
Right-to-left (Multiple styles): Advance = 127.30078, Visible advance = 118.30078
*/

public class VisibleAdvance
{
    public static void main (String [] args)
    {
        System.out.println ("java.version = " + System.getProperty ("java.version"));

        float advances[] = null;
        advances = showAndCalculateAdvance ("Left-to-right (One style): ", getString (TextAttribute.RUN_DIRECTION_LTR, false), advances);
        advances = showAndCalculateAdvance ("Right-to-left (One style): ", getString (TextAttribute.RUN_DIRECTION_RTL, false), advances);

        advances = showAndCalculateAdvance ("Left-to-right (Multiple styles): ", getString (TextAttribute.RUN_DIRECTION_LTR, true), advances);
        advances = showAndCalculateAdvance ("Right-to-left (Multiple styles): ", getString (TextAttribute.RUN_DIRECTION_RTL, true), advances);
    }

    private static final String textA = "Text with trailing ";
    private static final String textB = "spaces";
    private static final String textC = "   ";
    private static final String text = textA + textB + textC;

    private static final int startOfTextB = textA.length ();
    private static final int endOfTextB = startOfTextB + textB.length ();

    private static final Font font = new Font ("Serif", Font.PLAIN, 12);

    private static AttributedString getString (Boolean direction,
                                               boolean multipleStyles)
    {
        AttributedString as = new AttributedString (text);
        as.addAttribute (TextAttribute.FONT, font);
        as.addAttribute (TextAttribute.RUN_DIRECTION, direction);

        if (multipleStyles)
            as.addAttribute (TextAttribute.FOREGROUND, Color.RED, startOfTextB, endOfTextB);

        return as;
    }

    private static FontRenderContext fontRenderContext =
        new FontRenderContext (new AffineTransform (), true, true);

    /*
     * @param advances on input,  null or float[2]. On output:  { advance, visibleAdvance }
     * @param return new float array
     */
    private static float[] showAndCalculateAdvance (String what,
                                     AttributedString as,
                                     float advances[])
    {
        TextLayout layout = new TextLayout (as.getIterator (), fontRenderContext);

        System.out.println (what + "Advance = " + layout.getAdvance () +
                            ", Visible advance = " + layout.getVisibleAdvance ());
        float advance = layout.getAdvance();
        float visAdvance = layout.getVisibleAdvance();
        if(advances == null) {
           advances = new float[2];
        } else if( Float.compare(advances[0],advance)!=0 || Float.compare(advances[1],visAdvance)!=0) {
                throw new RuntimeException("MISMATCH in advance.. " + what + "Advance = " + layout.getAdvance () +
                            ", Visible advance = " + layout.getVisibleAdvance () + ", previous values were: ["+advances[0]+","+advances[1]+"]");
        }
        advances[0] = advance;
        advances[1] = visAdvance;
        return advances;
    }
}
