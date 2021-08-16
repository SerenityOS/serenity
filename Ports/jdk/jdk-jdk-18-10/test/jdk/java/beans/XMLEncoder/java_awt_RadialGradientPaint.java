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
 * @test
 * @bug 4358979
 * @summary Tests RadialGradientPaint encoding
 * @run main/othervm -Djava.security.manager=allow java_awt_RadialGradientPaint
 * @author Sergey Malenkov
 */

import java.awt.Color;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static java.awt.MultipleGradientPaint.ColorSpaceType.LINEAR_RGB;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;

public final class java_awt_RadialGradientPaint extends AbstractTest<RadialGradientPaint> {
    public static void main(String[] args) {
        new java_awt_RadialGradientPaint().test(true);
    }

    protected RadialGradientPaint getObject() {
        float[] f = { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f };
        Color[] c = { Color.BLUE, Color.GREEN, Color.RED, Color.BLUE, Color.GREEN, Color.RED };
        return new RadialGradientPaint(f[0], f[1], f[2], f, c);
    }

    protected RadialGradientPaint getAnotherObject() {
        return null; /* TODO: could not update property
        float[] f = { 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f };
        Color[] c = { Color.RED, Color.GREEN, Color.BLUE, Color.RED, Color.GREEN, Color.BLUE };
        return new RadialGradientPaint(
                new Point2D.Float(f[0], f[1]), 100.0f,
                new Point2D.Float(f[2], f[3]),
                f, c, REFLECT, LINEAR_RGB,
                new AffineTransform(f));*/
    }
}
