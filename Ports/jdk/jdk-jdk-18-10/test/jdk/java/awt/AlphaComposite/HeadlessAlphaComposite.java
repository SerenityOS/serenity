/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.ColorModel;

/*
 * @test
 * @summary Check no exception occurrence when running AlphaComposite getInstance(),
 *          createContext(), getAlpha(), getRule(), hashCode() methods in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessAlphaComposite
 */

public class HeadlessAlphaComposite {

    public static void main(String args[]) {
        AlphaComposite ac;
        ac = AlphaComposite.getInstance(AlphaComposite.CLEAR);
        ac = AlphaComposite.getInstance(AlphaComposite.DST_IN);
        ac = AlphaComposite.getInstance(AlphaComposite.DST_OUT);
        ac = AlphaComposite.getInstance(AlphaComposite.DST_OVER);
        ac = AlphaComposite.getInstance(AlphaComposite.SRC);
        ac = AlphaComposite.getInstance(AlphaComposite.SRC_IN);
        ac = AlphaComposite.getInstance(AlphaComposite.SRC_OUT);
        ac = AlphaComposite.getInstance(AlphaComposite.DST_IN);
        ac = AlphaComposite.getInstance(AlphaComposite.SRC, (float) 0.5);

        ac = AlphaComposite.getInstance(AlphaComposite.SRC, (float) 0.5);
        CompositeContext cc = ac.createContext(ColorModel.getRGBdefault(),
                ColorModel.getRGBdefault(),
                new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON));

        ac = AlphaComposite.getInstance(AlphaComposite.SRC, (float) 0.5);
        float alpha = ac.getAlpha();

        ac = AlphaComposite.getInstance(AlphaComposite.SRC, (float) 0.5);
        int rule = ac.getRule();

        ac = AlphaComposite.getInstance(AlphaComposite.SRC, (float) 0.5);
        int hc = ac.hashCode();
    }
}
