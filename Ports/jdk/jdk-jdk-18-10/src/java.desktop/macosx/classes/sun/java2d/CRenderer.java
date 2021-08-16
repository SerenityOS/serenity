/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.nio.*;

import sun.awt.image.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;
import sun.lwawt.macosx.*;

public class CRenderer implements PixelDrawPipe, PixelFillPipe, ShapeDrawPipe, DrawImagePipe {
    static native void init();

    // cache of the runtime options
    static {
        init(); // initialize coordinate tables for shapes
    }

    native void doLine(SurfaceData sData, float x1, float y1, float x2, float y2);

    public void drawLine(SunGraphics2D sg2d, int x1, int y1, int x2, int y2) {
        drawLine(sg2d, (float) x1, (float) y1, (float) x2, (float) y2);
    }

    Line2D lineToShape;

    public void drawLine(SunGraphics2D sg2d, float x1, float y1, float x2, float y2) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doLine(this, sg2d, x1, y1, x2, y2);
        } else {
            if (lineToShape == null) {
                synchronized (this) {
                    if (lineToShape == null) {
                        lineToShape = new Line2D.Float();
                    }
                }
            }
            synchronized (lineToShape) {
                lineToShape.setLine(x1, y1, x2, y2);
                drawfillShape(sg2d, sg2d.stroke.createStrokedShape(lineToShape), true, true);
            }
        }
    }

    native void doRect(SurfaceData sData, float x, float y, float width, float height, boolean isfill);

    public void drawRect(SunGraphics2D sg2d, int x, int y, int width, int height) {
        drawRect(sg2d, (float) x, (float) y, (float) width, (float) height);
    }

    Rectangle2D rectToShape;

    public void drawRect(SunGraphics2D sg2d, float x, float y, float width, float height) {
        if ((width < 0) || (height < 0)) return;

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doRect(this, sg2d, x, y, width, height, false);
        } else {
            if (rectToShape == null) {
                synchronized (this) {
                    if (rectToShape == null) {
                        rectToShape = new Rectangle2D.Float();
                    }
                }
            }
            synchronized (rectToShape) {
                rectToShape.setRect(x, y, width, height);
                drawfillShape(sg2d, sg2d.stroke.createStrokedShape(rectToShape), true, true);
            }
        }
    }

    public void fillRect(SunGraphics2D sg2d, int x, int y, int width, int height) {
        fillRect(sg2d, (float) x, (float) y, (float) width, (float) height);
    }

    public void fillRect(SunGraphics2D sg2d, float x, float y, float width, float height) {
        if ((width >= 0) && (height >= 0)) {
            OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
            surfaceData.doRect(this, sg2d, x, y, width, height, true);
        }
    }

    native void doRoundRect(SurfaceData sData, float x, float y, float width, float height, float arcW, float arcH, boolean isfill);

    public void drawRoundRect(SunGraphics2D sg2d, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        drawRoundRect(sg2d, (float) x, (float) y, (float) width, (float) height, (float) arcWidth, (float) arcHeight);
    }

    RoundRectangle2D roundrectToShape;

    public void drawRoundRect(SunGraphics2D sg2d, float x, float y, float width, float height, float arcWidth, float arcHeight) {
        if ((width < 0) || (height < 0)) return;

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doRoundRect(this, sg2d, x, y, width, height, arcWidth, arcHeight, false);
        } else {
            if (roundrectToShape == null) {
                synchronized (this) {
                    if (roundrectToShape == null) {
                        roundrectToShape = new RoundRectangle2D.Float();
                    }
                }
            }
            synchronized (roundrectToShape) {
                roundrectToShape.setRoundRect(x, y, width, height, arcWidth, arcHeight);
                drawfillShape(sg2d, sg2d.stroke.createStrokedShape(roundrectToShape), true, true);
            }
        }
    }

    public void fillRoundRect(SunGraphics2D sg2d, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        fillRoundRect(sg2d, (float) x, (float) y, (float) width, (float) height, (float) arcWidth, (float) arcHeight);
    }

    public void fillRoundRect(SunGraphics2D sg2d, float x, float y, float width, float height, float arcWidth, float arcHeight) {
        if ((width < 0) || (height < 0)) return;
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        surfaceData.doRoundRect(this, sg2d, x, y, width, height, arcWidth, arcHeight, true);
    }

    native void doOval(SurfaceData sData, float x, float y, float width, float height, boolean isfill);

    public void drawOval(SunGraphics2D sg2d, int x, int y, int width, int height) {
        drawOval(sg2d, (float) x, (float) y, (float) width, (float) height);
    }

    Ellipse2D ovalToShape;

    public void drawOval(SunGraphics2D sg2d, float x, float y, float width, float height) {
        if ((width < 0) || (height < 0)) return;

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doOval(this, sg2d, x, y, width, height, false);
        } else {
            if (ovalToShape == null) {
                synchronized (this) {
                    if (ovalToShape == null) {
                        ovalToShape = new Ellipse2D.Float();
                    }
                }
            }
            synchronized (ovalToShape) {
                ovalToShape.setFrame(x, y, width, height);
                drawfillShape(sg2d, sg2d.stroke.createStrokedShape(ovalToShape), true, true);
            }
        }
    }

    public void fillOval(SunGraphics2D sg2d, int x, int y, int width, int height) {
        fillOval(sg2d, (float) x, (float) y, (float) width, (float) height);
    }

    public void fillOval(SunGraphics2D sg2d, float x, float y, float width, float height) {
        if ((width < 0) || (height < 0)) return;
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        surfaceData.doOval(this, sg2d, x, y, width, height, true);
    }

    native void doArc(SurfaceData sData, float x, float y, float width, float height, float angleStart, float angleExtent, int type, boolean isfill);

    public void drawArc(SunGraphics2D sg2d, int x, int y, int width, int height, int startAngle, int arcAngle) {
        drawArc(sg2d, x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
    }

    Arc2D arcToShape;

    public void drawArc(SunGraphics2D sg2d, float x, float y, float width, float height, float startAngle, float arcAngle, int type) {
        if ((width < 0) || (height < 0)) return;

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doArc(this, sg2d, x, y, width, height, startAngle, arcAngle, type, false);
        } else {
            if (arcToShape == null) {
                synchronized (this) {
                    if (arcToShape == null) {
                        arcToShape = new Arc2D.Float();
                    }
                }
            }
            synchronized (arcToShape) {
                arcToShape.setArc(x, y, width, height, startAngle, arcAngle, type);
                drawfillShape(sg2d, sg2d.stroke.createStrokedShape(arcToShape), true, true);
            }
        }
    }

    public void fillArc(SunGraphics2D sg2d, int x, int y, int width, int height, int startAngle, int arcAngle) {
        fillArc(sg2d, x, y, width, height, startAngle, arcAngle, Arc2D.PIE);
    }

    public void fillArc(SunGraphics2D sg2d, float x, float y, float width, float height, float startAngle, float arcAngle, int type) {
        if ((width < 0) || (height < 0)) return;

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        surfaceData.doArc(this, sg2d, x, y, width, height, startAngle, arcAngle, type, true);
    }

    native void doPoly(SurfaceData sData, int[] xpoints, int[] ypoints, int npoints, boolean ispolygon, boolean isfill);

    public void drawPolyline(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doPolygon(this, sg2d, xpoints, ypoints, npoints, false, false);
        } else {
            GeneralPath polyToShape = new GeneralPath();
            polyToShape.moveTo(xpoints[0], ypoints[0]);
            for (int i = 1; i < npoints; i++) {
                polyToShape.lineTo(xpoints[i], ypoints[i]);
            }
            drawfillShape(sg2d, sg2d.stroke.createStrokedShape(polyToShape), true, true);
        }
    }

    public void drawPolygon(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            surfaceData.doPolygon(this, sg2d, xpoints, ypoints, npoints, true, false);
        } else {
            GeneralPath polyToShape = new GeneralPath();
            polyToShape.moveTo(xpoints[0], ypoints[0]);
            for (int i = 1; i < npoints; i++) {
                polyToShape.lineTo(xpoints[i], ypoints[i]);
            }
            polyToShape.lineTo(xpoints[0], ypoints[0]);
            drawfillShape(sg2d, sg2d.stroke.createStrokedShape(polyToShape), true, true);
        }
    }

    public void fillPolygon(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        surfaceData.doPolygon(this, sg2d, xpoints, ypoints, npoints, true, true);
    }

    native void doShape(SurfaceData sData, int length, FloatBuffer coordinates, IntBuffer types, int windingRule, boolean isfill, boolean shouldApplyOffset);

    void drawfillShape(SunGraphics2D sg2d, Shape s, boolean isfill, boolean shouldApplyOffset) {
        if (s == null) { throw new NullPointerException(); }

        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        // TODO:
        boolean sOptimizeShapes = true;
        if (sOptimizeShapes && OSXSurfaceData.IsSimpleColor(sg2d.paint)) {
            if (s instanceof Rectangle2D) {
                Rectangle2D rectangle = (Rectangle2D) s;

                float x = (float) rectangle.getX();
                float y = (float) rectangle.getY();
                float w = (float) rectangle.getWidth();
                float h = (float) rectangle.getHeight();
                if (isfill) {
                    fillRect(sg2d, x, y, w, h);
                } else {
                    drawRect(sg2d, x, y, w, h);
                }
            } else if (s instanceof Ellipse2D) {
                Ellipse2D ellipse = (Ellipse2D) s;

                float x = (float) ellipse.getX();
                float y = (float) ellipse.getY();
                float w = (float) ellipse.getWidth();
                float h = (float) ellipse.getHeight();

                if (isfill) {
                    fillOval(sg2d, x, y, w, h);
                } else {
                    drawOval(sg2d, x, y, w, h);
                }
            } else if (s instanceof Arc2D) {
                Arc2D arc = (Arc2D) s;

                float x = (float) arc.getX();
                float y = (float) arc.getY();
                float w = (float) arc.getWidth();
                float h = (float) arc.getHeight();
                float as = (float) arc.getAngleStart();
                float ae = (float) arc.getAngleExtent();

                if (isfill) {
                    fillArc(sg2d, x, y, w, h, as, ae, arc.getArcType());
                } else {
                    drawArc(sg2d, x, y, w, h, as, ae, arc.getArcType());
                }
            } else if (s instanceof RoundRectangle2D) {
                RoundRectangle2D roundrect = (RoundRectangle2D) s;

                float x = (float) roundrect.getX();
                float y = (float) roundrect.getY();
                float w = (float) roundrect.getWidth();
                float h = (float) roundrect.getHeight();
                float aw = (float) roundrect.getArcWidth();
                float ah = (float) roundrect.getArcHeight();

                if (isfill) {
                    fillRoundRect(sg2d, x, y, w, h, aw, ah);
                } else {
                    drawRoundRect(sg2d, x, y, w, h, aw, ah);
                }
            } else if (s instanceof Line2D) {
                Line2D line = (Line2D) s;

                float x1 = (float) line.getX1();
                float y1 = (float) line.getY1();
                float x2 = (float) line.getX2();
                float y2 = (float) line.getY2();

                drawLine(sg2d, x1, y1, x2, y2);
            } else if (s instanceof Point2D) {
                Point2D point = (Point2D) s;

                float x = (float) point.getX();
                float y = (float) point.getY();

                drawLine(sg2d, x, y, x, y);
            } else {
                GeneralPath gp;

                if (s instanceof GeneralPath) {
                    gp = (GeneralPath) s;
                } else {
                    gp = new GeneralPath(s);
                }

                PathIterator pi = gp.getPathIterator(null);
                if (pi.isDone() == false) {
                    surfaceData.drawfillShape(this, sg2d, gp, isfill, shouldApplyOffset);
                }
            }
        } else {
            GeneralPath gp;

            if (s instanceof GeneralPath) {
                gp = (GeneralPath) s;
            } else {
                gp = new GeneralPath(s);
            }

            PathIterator pi = gp.getPathIterator(null);
            if (pi.isDone() == false) {
                surfaceData.drawfillShape(this, sg2d, gp, isfill, shouldApplyOffset);
            }
        }
    }

    public void draw(SunGraphics2D sg2d, Shape s) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();
        if ((sg2d.strokeState != SunGraphics2D.STROKE_CUSTOM) && (OSXSurfaceData.IsSimpleColor(sg2d.paint))) {
            drawfillShape(sg2d, s, false, true);
        } else {
            drawfillShape(sg2d, sg2d.stroke.createStrokedShape(s), true, true);
        }
    }

    public void fill(SunGraphics2D sg2d, Shape s) {
        drawfillShape(sg2d, s, true, false);
    }

    native void doImage(SurfaceData sData, SurfaceData img, boolean fliph, boolean flipv, int w, int h, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh);

    // Copy img to scaled sg2d @ x,y with width height
    public boolean scaleImage(SunGraphics2D sg2d, Image img, int x, int y, int width, int height, Color bgColor) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();

        int sx = 0;
        int sy = 0;
        int iw = img.getWidth(null);
        int ih = img.getHeight(null);

        return scaleImage(sg2d, img, x, y, x + width, y + height, sx, sy, sx + iw, sy + ih, bgColor);
    }

    // Copy img, clipped to sx1, sy1 by sx2, sy2 to dx1, dy2 by dx2, dy2
    public boolean scaleImage(SunGraphics2D sg2d, Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgColor) {

        // System.err.println("scaleImage");
        // System.err.println("    sx1="+sx1+", sy1="+sy1+", sx2="+sx2+", sy2="+sy2);
        // System.err.println("    dx1="+dx1+", dy1="+dy1+", dx2="+dx2+", dy2="+dy2);

        int srcW, srcH, dstW, dstH;
        int srcX, srcY, dstX, dstY;
        boolean srcWidthFlip = false;
        boolean srcHeightFlip = false;
        boolean dstWidthFlip = false;
        boolean dstHeightFlip = false;

        if (sx2 > sx1) {
            srcW = sx2 - sx1;
            srcX = sx1;
        } else {
            srcWidthFlip = true;
            srcW = sx1 - sx2;
            srcX = sx2;
        }
        if (sy2 > sy1) {
            srcH = sy2 - sy1;
            srcY = sy1;
        } else {
            srcHeightFlip = true;
            srcH = sy1 - sy2;
            srcY = sy2;
        }
        if (dx2 > dx1) {
            dstW = dx2 - dx1;
            dstX = dx1;
        } else {
            dstW = dx1 - dx2;
            dstWidthFlip = true;
            dstX = dx2;
        }
        if (dy2 > dy1) {
            dstH = dy2 - dy1;
            dstY = dy1;
        } else {
            dstH = dy1 - dy2;
            dstHeightFlip = true;
            dstY = dy2;
        }
        if (srcW <= 0 || srcH <= 0) { return true; }

        boolean flipv = (srcHeightFlip != dstHeightFlip);
        boolean fliph = (srcWidthFlip != dstWidthFlip);

        return blitImage(sg2d, img, fliph, flipv, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH, bgColor);
    }

    protected boolean blitImage(SunGraphics2D sg2d, Image img, boolean fliph, boolean flipv, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, Color bgColor) {
        CPrinterSurfaceData surfaceData = (CPrinterSurfaceData)sg2d.getSurfaceData();
        OSXOffScreenSurfaceData imgSurfaceData = OSXOffScreenSurfaceData.createNewSurface((BufferedImage)img);
        surfaceData.blitImage(this, sg2d, imgSurfaceData, fliph, flipv, sx, sy, sw, sh, dx, dy, dw, dh, bgColor);
        return true;
    }

    // Copy img to sg2d @ x, y
    protected boolean copyImage(SunGraphics2D sg2d, Image img, int dx, int dy, Color bgColor) {
        if (img == null) { return true; }

        int sx = 0;
        int sy = 0;
        int width = img.getWidth(null);
        int height = img.getHeight(null);

        return blitImage(sg2d, img, false, false, sx, sy, width, height, dx, dy, width, height, bgColor);
    }

    // Copy img, clipped to sx, sy with width, height to sg2d @ dx, dy
    protected boolean copyImage(SunGraphics2D sg2d, Image img, int dx, int dy, int sx, int sy, int width, int height, Color bgColor) {
        return blitImage(sg2d, img, false, false, sx, sy, width, height, dx, dy, width, height, bgColor);
    }

    protected void transformImage(SunGraphics2D sg2d, Image img, int x, int y, BufferedImageOp op, AffineTransform xf, Color bgColor) {
        if (img != null) {
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);

            if ((op != null) && (img instanceof BufferedImage)) {
                if (((BufferedImage) img).getType() == BufferedImage.TYPE_CUSTOM) {
                    // BufferedImageOp can not handle custom images
                    BufferedImage dest = null;
                    dest = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB_PRE);
                    Graphics g = dest.createGraphics();
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    img = op.filter(dest, null);
                } else {
                    // sun.awt.image.BufImgSurfaceData.createData((BufferedImage)img).finishLazyDrawing();
                    img = op.filter((BufferedImage) img, null);
                }

                iw = img.getWidth(null);
                ih = img.getHeight(null);
            }

            if (xf != null) {
                AffineTransform reset = sg2d.getTransform();
                sg2d.transform(xf);
                scaleImage(sg2d, img, x, y, x + iw, y + ih, 0, 0, iw, ih, bgColor);
                sg2d.setTransform(reset);
            } else {
                scaleImage(sg2d, img, x, y, x + iw, y + ih, 0, 0, iw, ih, bgColor);
            }
        } else {
            throw new NullPointerException();
        }
    }

    // copied from DrawImage.java
    protected boolean imageReady(sun.awt.image.ToolkitImage sunimg, ImageObserver observer) {
        if (sunimg.hasError()) {
            if (observer != null) {
                observer.imageUpdate(sunimg, ImageObserver.ERROR | ImageObserver.ABORT, -1, -1, -1, -1);
            }
            return false;
        }
        return true;
    }

    // copied from DrawImage.java
    public boolean copyImage(SunGraphics2D sg2d, Image img, int x, int y, Color bgColor, ImageObserver observer) {
        if (img == null) { throw new NullPointerException(); }

        if (!(img instanceof sun.awt.image.ToolkitImage)) { return copyImage(sg2d, img, x, y, bgColor); }

        sun.awt.image.ToolkitImage sunimg = (sun.awt.image.ToolkitImage) img;
        if (!imageReady(sunimg, observer)) { return false; }
        ImageRepresentation ir = sunimg.getImageRep();
        return ir.drawToBufImage(sg2d, sunimg, x, y, bgColor, observer);
    }

    // copied from DrawImage.java
    public boolean copyImage(SunGraphics2D sg2d, Image img, int dx, int dy, int sx, int sy, int width, int height, Color bgColor, ImageObserver observer) {
        if (img == null) { throw new NullPointerException(); }

        if (!(img instanceof sun.awt.image.ToolkitImage)) { return copyImage(sg2d, img, dx, dy, sx, sy, width, height, bgColor); }

        sun.awt.image.ToolkitImage sunimg = (sun.awt.image.ToolkitImage) img;
        if (!imageReady(sunimg, observer)) { return false; }
        ImageRepresentation ir = sunimg.getImageRep();
        return ir.drawToBufImage(sg2d, sunimg, dx, dy, (dx + width), (dy + height), sx, sy, (sx + width), (sy + height), null, observer);
    }

    // copied from DrawImage.java
    public boolean scaleImage(SunGraphics2D sg2d, Image img, int x, int y, int width, int height, Color bgColor, ImageObserver observer) {
        if (img == null) { throw new NullPointerException(); }

        if (!(img instanceof sun.awt.image.ToolkitImage)) { return scaleImage(sg2d, img, x, y, width, height, bgColor); }

        sun.awt.image.ToolkitImage sunimg = (sun.awt.image.ToolkitImage) img;
        if (!imageReady(sunimg, observer)) { return false; }
        ImageRepresentation ir = sunimg.getImageRep();
        return ir.drawToBufImage(sg2d, sunimg, x, y, width, height, bgColor, observer);
    }

    // copied from DrawImage.java
    public boolean scaleImage(SunGraphics2D sg2d, Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgColor, ImageObserver observer) {
        if (img == null) { throw new NullPointerException(); }

        if (!(img instanceof sun.awt.image.ToolkitImage)) { return scaleImage(sg2d, img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgColor); }

        sun.awt.image.ToolkitImage sunimg = (sun.awt.image.ToolkitImage) img;
        if (!imageReady(sunimg, observer)) { return false; }
        ImageRepresentation ir = sunimg.getImageRep();
        return ir.drawToBufImage(sg2d, sunimg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgColor, observer);
    }

    // copied from DrawImage.java
    public boolean transformImage(SunGraphics2D sg2d, Image img, AffineTransform atfm, ImageObserver observer) {
        if (img == null) { throw new NullPointerException(); }

        if (!(img instanceof sun.awt.image.ToolkitImage)) {
            transformImage(sg2d, img, 0, 0, null, atfm, null);
            return true;
        }

        sun.awt.image.ToolkitImage sunimg = (sun.awt.image.ToolkitImage) img;
        if (!imageReady(sunimg, observer)) { return false; }
        ImageRepresentation ir = sunimg.getImageRep();
        return ir.drawToBufImage(sg2d, sunimg, atfm, observer);
    }

    // copied from DrawImage.java
    public void transformImage(SunGraphics2D sg2d, BufferedImage img, BufferedImageOp op, int x, int y) {
        if (img != null) {
            transformImage(sg2d, img, x, y, op, null, null);
        } else {
            throw new NullPointerException();
        }
    }

    public CRenderer traceWrap() {
        return new Tracer();
    }

    public static class Tracer extends CRenderer {
        void doLine(SurfaceData sData, float x1, float y1, float x2, float y2) {
            GraphicsPrimitive.tracePrimitive("QuartzLine");
            super.doLine(sData, x1, y1, x2, y2);
        }

        void doRect(SurfaceData sData, float x, float y, float width, float height, boolean isfill) {
            GraphicsPrimitive.tracePrimitive("QuartzRect");
            super.doRect(sData, x, y, width, height, isfill);
        }

        void doRoundRect(SurfaceData sData, float x, float y, float width, float height, float arcW, float arcH, boolean isfill) {
            GraphicsPrimitive.tracePrimitive("QuartzRoundRect");
            super.doRoundRect(sData, x, y, width, height, arcW, arcH, isfill);
        }

        void doOval(SurfaceData sData, float x, float y, float width, float height, boolean isfill) {
            GraphicsPrimitive.tracePrimitive("QuartzOval");
            super.doOval(sData, x, y, width, height, isfill);
        }

        void doArc(SurfaceData sData, float x, float y, float width, float height, float angleStart, float angleExtent, int type, boolean isfill) {
            GraphicsPrimitive.tracePrimitive("QuartzArc");
            super.doArc(sData, x, y, width, height, angleStart, angleExtent, type, isfill);
        }

        void doPoly(SurfaceData sData, int[] xpoints, int[] ypoints, int npoints, boolean ispolygon, boolean isfill) {
            GraphicsPrimitive.tracePrimitive("QuartzDoPoly");
            super.doPoly(sData, xpoints, ypoints, npoints, ispolygon, isfill);
        }

        void doShape(SurfaceData sData, int length, FloatBuffer coordinates, IntBuffer types, int windingRule, boolean isfill, boolean shouldApplyOffset) {
            GraphicsPrimitive.tracePrimitive("QuartzFillOrDrawShape");
            super.doShape(sData, length, coordinates, types, windingRule, isfill, shouldApplyOffset);
        }

        void doImage(SurfaceData sData, SurfaceData img, boolean fliph, boolean flipv, int w, int h, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
            GraphicsPrimitive.tracePrimitive("QuartzDrawImage");
            super.doImage(sData, img, fliph, flipv, w, h, sx, sy, sw, sh, dx, dy, dw, dh);
        }
    }
}
