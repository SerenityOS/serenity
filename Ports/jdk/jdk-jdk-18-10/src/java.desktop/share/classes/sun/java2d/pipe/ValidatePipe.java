/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.pipe;

import java.awt.Color;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import sun.java2d.SunGraphics2D;
import java.awt.font.GlyphVector;

/**
 * This class is used to force a revalidation of the pipelines of
 * the indicated SunGraphics2D object before a draw command.
 * After calling SunGraphics2D.validatePipe() to force the pipeline
 * to be revalidated, this object redispatches the draw command to
 * the new valid pipe object.
 */
public class ValidatePipe
    implements PixelDrawPipe, PixelFillPipe, ShapeDrawPipe, TextPipe,
    DrawImagePipe
{
    /*
     * Different subclasses may override this to do various
     * other forms of validation and return a return code
     * indicating whether or not the validation was successful.
     */
    public boolean validate(SunGraphics2D sg) {
        sg.validatePipe();
        return true;
    }

    public void drawLine(SunGraphics2D sg,
                         int x1, int y1, int x2, int y2) {
        if (validate(sg)) {
            sg.drawpipe.drawLine(sg, x1, y1, x2, y2);
        }
    }

    public void drawRect(SunGraphics2D sg,
                         int x, int y, int width, int height) {
        if (validate(sg)) {
            sg.drawpipe.drawRect(sg, x, y, width, height);
        }
    }

    public void fillRect(SunGraphics2D sg,
                         int x, int y, int width, int height) {
        if (validate(sg)) {
            sg.fillpipe.fillRect(sg, x, y, width, height);
        }
    }

    public void drawRoundRect(SunGraphics2D sg,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        if (validate(sg)) {
            sg.drawpipe.drawRoundRect(sg, x, y, width, height,
                                      arcWidth, arcHeight);
        }
    }

    public void fillRoundRect(SunGraphics2D sg,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        if (validate(sg)) {
            sg.fillpipe.fillRoundRect(sg, x, y, width, height,
                                      arcWidth, arcHeight);
        }
    }

    public void drawOval(SunGraphics2D sg,
                         int x, int y, int width, int height) {
        if (validate(sg)) {
            sg.drawpipe.drawOval(sg, x, y, width, height);
        }
    }

    public void fillOval(SunGraphics2D sg,
                         int x, int y, int width, int height) {
        if (validate(sg)) {
            sg.fillpipe.fillOval(sg, x, y, width, height);
        }
    }

    public void drawArc(SunGraphics2D sg,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        if (validate(sg)) {
            sg.drawpipe.drawArc(sg, x, y, width, height, startAngle, arcAngle);
        }
    }

    public void fillArc(SunGraphics2D sg,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        if (validate(sg)) {
            sg.fillpipe.fillArc(sg, x, y, width, height, startAngle, arcAngle);
        }
    }

    public void drawPolyline(SunGraphics2D sg,
                             int[] xPoints, int[] yPoints,
                             int nPoints) {
        if (validate(sg)) {
            sg.drawpipe.drawPolyline(sg, xPoints, yPoints, nPoints);
        }
    }

    public void drawPolygon(SunGraphics2D sg,
                            int[] xPoints, int[] yPoints,
                            int nPoints) {
        if (validate(sg)) {
            sg.drawpipe.drawPolygon(sg, xPoints, yPoints, nPoints);
        }
    }

    public void fillPolygon(SunGraphics2D sg,
                            int[] xPoints, int[] yPoints,
                            int nPoints) {
        if (validate(sg)) {
            sg.fillpipe.fillPolygon(sg, xPoints, yPoints, nPoints);
        }
    }

    public void draw(SunGraphics2D sg, Shape s) {
        if (validate(sg)) {
            sg.shapepipe.draw(sg, s);
        }
    }

    public void fill(SunGraphics2D sg, Shape s) {
        if (validate(sg)) {
            sg.shapepipe.fill(sg, s);
        }
    }
    public void drawString(SunGraphics2D sg, String s, double x, double y) {
        if (validate(sg)) {
            sg.textpipe.drawString(sg, s, x, y);
        }
    }
    public void drawGlyphVector(SunGraphics2D sg, GlyphVector g,
                                float x, float y) {
        if (validate(sg)) {
            sg.textpipe.drawGlyphVector(sg, g, x, y);
        }
    }
    public void drawChars(SunGraphics2D sg,
                                char[] data, int offset, int length,
                                int x, int y) {
        if (validate(sg)) {
            sg.textpipe.drawChars(sg, data, offset, length, x, y);
        }
    }
    public boolean copyImage(SunGraphics2D sg, Image img,
                             int x, int y,
                             Color bgColor,
                             ImageObserver observer) {
        if (validate(sg)) {
            return sg.imagepipe.copyImage(sg, img, x, y, bgColor, observer);
        } else {
            return false;
        }
    }
    public boolean copyImage(SunGraphics2D sg, Image img,
                             int dx, int dy, int sx, int sy, int w, int h,
                             Color bgColor,
                             ImageObserver observer) {
        if (validate(sg)) {
            return sg.imagepipe.copyImage(sg, img, dx, dy, sx, sy, w, h,
                                          bgColor, observer);
        } else {
            return false;
        }
    }
    public boolean scaleImage(SunGraphics2D sg, Image img, int x, int y,
                              int w, int h,
                              Color bgColor,
                              ImageObserver observer) {
        if (validate(sg)) {
            return sg.imagepipe.scaleImage(sg, img, x, y, w, h, bgColor,
                                           observer);
        } else {
            return false;
        }
    }
    public boolean scaleImage(SunGraphics2D sg, Image img,
                              int dx1, int dy1, int dx2, int dy2,
                              int sx1, int sy1, int sx2, int sy2,
                              Color bgColor,
                              ImageObserver observer) {
        if (validate(sg)) {
            return sg.imagepipe.scaleImage(sg, img, dx1, dy1, dx2, dy2,
                                           sx1, sy1, sx2, sy2, bgColor,
                                           observer);
        } else {
            return false;
        }
    }
    public boolean transformImage(SunGraphics2D sg, Image img,
                                  AffineTransform atfm,
                                  ImageObserver observer) {
        if (validate(sg)) {
            return sg.imagepipe.transformImage(sg, img, atfm, observer);
        } else {
            return false;
        }
    }
    public void transformImage(SunGraphics2D sg, BufferedImage img,
                               BufferedImageOp op, int x, int y) {
        if (validate(sg)) {
            sg.imagepipe.transformImage(sg, img, op, x, y);
        }
    }
}
