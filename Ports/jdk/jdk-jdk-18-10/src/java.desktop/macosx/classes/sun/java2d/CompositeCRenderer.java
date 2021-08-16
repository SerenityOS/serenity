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
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;

import sun.awt.image.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;

public class CompositeCRenderer extends CRenderer implements PixelDrawPipe, PixelFillPipe, ShapeDrawPipe, DrawImagePipe, TextPipe {
    static final int fPadding = 4;
    static final int fPaddingHalf = fPadding / 2;

    private static AffineTransform sIdentityMatrix = new AffineTransform();

    AffineTransform ShapeTM = new AffineTransform();
    Rectangle2D ShapeBounds = new Rectangle2D.Float();

    Line2D line = new Line2D.Float();
    Rectangle2D rectangle = new Rectangle2D.Float();
    RoundRectangle2D roundrectangle = new RoundRectangle2D.Float();
    Ellipse2D ellipse = new Ellipse2D.Float();
    Arc2D arc = new Arc2D.Float();

    public synchronized void drawLine(SunGraphics2D sg2d, int x1, int y1, int x2, int y2) {
        // create shape corresponding to this primitive
        line.setLine(x1, y1, x2, y2);

        draw(sg2d, line);
    }

    public synchronized void drawRect(SunGraphics2D sg2d, int x, int y, int width, int height) {
        // create shape corresponding to this primitive
        rectangle.setRect(x, y, width, height);

        draw(sg2d, rectangle);
    }

    public synchronized void drawRoundRect(SunGraphics2D sg2d, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // create shape corresponding to this primitive
        roundrectangle.setRoundRect(x, y, width, height, arcWidth, arcHeight);

        draw(sg2d, roundrectangle);
    }

    public synchronized void drawOval(SunGraphics2D sg2d, int x, int y, int width, int height) {
        // create shape corresponding to this primitive
        ellipse.setFrame(x, y, width, height);

        draw(sg2d, ellipse);
    }

    public synchronized void drawArc(SunGraphics2D sg2d, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // create shape corresponding to this primitive
        arc.setArc(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);

        draw(sg2d, arc);
    }

    public synchronized void drawPolyline(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        doPolygon(sg2d, xpoints, ypoints, npoints, false, false);
    }

    public synchronized void drawPolygon(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        doPolygon(sg2d, xpoints, ypoints, npoints, true, false);
    }

    public synchronized void fillRect(SunGraphics2D sg2d, int x, int y, int width, int height) {
        // create shape corresponding to this primitive
        rectangle.setRect(x, y, width, height);

        fill(sg2d, rectangle);
    }

    public synchronized void fillRoundRect(SunGraphics2D sg2d, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // create shape corresponding to this primitive
        roundrectangle.setRoundRect(x, y, width, height, arcWidth, arcHeight);

        fill(sg2d, roundrectangle);
    }

    public synchronized void fillOval(SunGraphics2D sg2d, int x, int y, int width, int height) {
        // create shape corresponding to this primitive
        ellipse.setFrame(x, y, width, height);

        fill(sg2d, ellipse);
    }

    public synchronized void fillArc(SunGraphics2D sg2d, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // create shape corresponding to this primitive
        arc.setArc(x, y, width, height, startAngle, arcAngle, Arc2D.PIE);

        fill(sg2d, arc);
    }

    public synchronized void fillPolygon(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints) {
        doPolygon(sg2d, xpoints, ypoints, npoints, true, true);
    }

    public synchronized void doPolygon(SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints, boolean ispolygon, boolean isfill) {
        GeneralPath gp = new GeneralPath(Path2D.WIND_NON_ZERO, npoints);
        gp.moveTo(xpoints[0], ypoints[0]);
        for (int i = 1; i < npoints; i++) {
            gp.lineTo(xpoints[i], ypoints[i]);
        }
        if (ispolygon) {
            // according to the specs (only applies to polygons, not polylines)
            if ((xpoints[0] != xpoints[npoints - 1]) || (ypoints[0] != ypoints[npoints - 1])) {
                gp.lineTo(xpoints[0], ypoints[0]);
            }
        }

        doShape(sg2d, (OSXSurfaceData) sg2d.getSurfaceData(), (Shape) gp, isfill);
    }

    public synchronized void draw(SunGraphics2D sg2d, Shape shape) {
        doShape(sg2d, (OSXSurfaceData) sg2d.getSurfaceData(), shape, false);
    }

    public synchronized void fill(SunGraphics2D sg2d, Shape shape) {
        doShape(sg2d, (OSXSurfaceData) sg2d.getSurfaceData(), shape, true);
    }

    void doShape(SunGraphics2D sg2d, OSXSurfaceData surfaceData, Shape shape, boolean isfill) {
        Rectangle2D shapeBounds = shape.getBounds2D();

        // We don't want to draw with negative width and height (CRender doesn't do it and Windows doesn't do it either)
        // Drawing with negative w and h, can cause CG problems down the line <rdar://3960579> (vm)
        if ((shapeBounds.getWidth() < 0) || (shapeBounds.getHeight() < 0)) { return; }

        // get final destination compositing bounds (after all transformations if needed)
        Rectangle2D compositingBounds = padBounds(sg2d, shape);

        // constrain the bounds to be within surface bounds
        clipBounds(sg2d, compositingBounds);

        // if the compositing region is empty we skip all remaining compositing work:
        if (compositingBounds.isEmpty() == false) {
            BufferedImage srcPixels;
            // create a matching surface into which we'll render the primitive to be composited
            // with the desired dimension
            srcPixels = surfaceData.getCompositingSrcImage((int) (compositingBounds.getWidth()),
                    (int) (compositingBounds.getHeight()));

            Graphics2D g = srcPixels.createGraphics();

            // sync up graphics state
            ShapeTM.setToTranslation(-compositingBounds.getX(), -compositingBounds.getY());
            ShapeTM.concatenate(sg2d.transform);
            g.setTransform(ShapeTM);
            g.setRenderingHints(sg2d.getRenderingHints());
            g.setPaint(sg2d.getPaint());
            g.setStroke(sg2d.getStroke());

            // render the primitive to be composited
            if (isfill) {
                g.fill(shape);
            } else {
                g.draw(shape);
            }

            g.dispose();

            composite(sg2d, surfaceData, srcPixels, compositingBounds);
        }
    }

    public synchronized void drawString(SunGraphics2D sg2d, String str, double x, double y) {
        drawGlyphVector(sg2d, sg2d.getFont().createGlyphVector(sg2d.getFontRenderContext(), str), x, y);
    }

    public synchronized void drawChars(SunGraphics2D sg2d, char[] data, int offset, int length, int x, int y) {
        drawString(sg2d, new String(data, offset, length), x, y);
    }

    public synchronized void drawGlyphVector(SunGraphics2D sg2d, GlyphVector glyphVector, double x, double y) {
        drawGlyphVector(sg2d, glyphVector, (float) x, (float) y);
    }

    public synchronized void drawGlyphVector(SunGraphics2D sg2d, GlyphVector glyphVector, float x, float y) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();

        Shape shape = glyphVector.getOutline(x, y);

        // get final destination compositing bounds (after all transformations if needed)
        Rectangle2D compositingBounds = padBounds(sg2d, shape);

        // constrain the bounds to be within surface bounds
        clipBounds(sg2d, compositingBounds);

        // if the compositing region is empty we skip all remaining compositing work:
        if (compositingBounds.isEmpty() == false) {
            BufferedImage srcPixels;
            {
                // create matching image into which we'll render the primitive to be composited
                srcPixels = surfaceData.getCompositingSrcImage((int) compositingBounds.getWidth(), (int) compositingBounds.getHeight());

                Graphics2D g = srcPixels.createGraphics();

                // sync up graphics state
                ShapeTM.setToTranslation(-compositingBounds.getX(), -compositingBounds.getY());
                ShapeTM.concatenate(sg2d.transform);
                g.setTransform(ShapeTM);
                g.setPaint(sg2d.getPaint());
                g.setStroke(sg2d.getStroke());
                g.setFont(sg2d.getFont());
                g.setRenderingHints(sg2d.getRenderingHints());

                // render the primitive to be composited
                g.drawGlyphVector(glyphVector, x, y);
                g.dispose();
            }

            composite(sg2d, surfaceData, srcPixels, compositingBounds);
        }
    }

    protected boolean blitImage(SunGraphics2D sg2d, Image img, boolean fliph, boolean flipv, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, Color bgColor) {
        OSXSurfaceData surfaceData = (OSXSurfaceData) sg2d.getSurfaceData();

        // get final destination compositing bounds (after all transformations if needed)
        dx = (flipv == false) ? dx : dx - dw;
        dy = (fliph == false) ? dy : dy - dh;
        ShapeBounds.setFrame(dx, dy, dw, dh);
        Rectangle2D compositingBounds = ShapeBounds;
        boolean complexTransform = (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE);
        if (complexTransform == false) {
            double newX = Math.floor(compositingBounds.getX() + sg2d.transX);
            double newY = Math.floor(compositingBounds.getY() + sg2d.transY);
            double newW = Math.ceil(compositingBounds.getWidth()) + (newX < compositingBounds.getX() ? 1 : 0);
            double newH = Math.ceil(compositingBounds.getHeight()) + (newY < compositingBounds.getY() ? 1 : 0);
            compositingBounds.setRect(newX, newY, newW, newH);
        } else {
            Shape transformedShape = sg2d.transform.createTransformedShape(compositingBounds);
            compositingBounds = transformedShape.getBounds2D();
            double newX = Math.floor(compositingBounds.getX());
            double newY = Math.floor(compositingBounds.getY());
            double newW = Math.ceil(compositingBounds.getWidth()) + (newX < compositingBounds.getX() ? 1 : 0);
            double newH = Math.ceil(compositingBounds.getHeight()) + (newY < compositingBounds.getY() ? 1 : 0);
            compositingBounds.setRect(newX, newY, newW, newH);
        }

        // constrain the bounds to be within surface bounds
        clipBounds(sg2d, compositingBounds);

        // if the compositing region is empty we skip all remaining compositing work:
        if (compositingBounds.isEmpty() == false) {
            BufferedImage srcPixels;
            {
                // create matching image into which we'll render the primitive to be composited
                srcPixels = surfaceData.getCompositingSrcImage((int) compositingBounds.getWidth(), (int) compositingBounds.getHeight());

                Graphics2D g = srcPixels.createGraphics();

                // sync up graphics state
                ShapeTM.setToTranslation(-compositingBounds.getX(), -compositingBounds.getY());
                ShapeTM.concatenate(sg2d.transform);
                g.setTransform(ShapeTM);
                g.setRenderingHints(sg2d.getRenderingHints());
                g.setComposite(AlphaComposite.Src);

                int sx2 = (flipv == false) ? sx + sw : sx - sw;
                int sy2 = (fliph == false) ? sy + sh : sy - sh;
                g.drawImage(img, dx, dy, dx + dw, dy + dh, sx, sy, sx2, sy2, null);

                g.dispose();
            }

            composite(sg2d, surfaceData, srcPixels, compositingBounds);
        }

        return true;
    }

    Rectangle2D padBounds(SunGraphics2D sg2d, Shape shape) {
        shape = sg2d.transformShape(shape);

        int paddingHalf = fPaddingHalf;
        int padding = fPadding;
        if (sg2d.stroke != null) {
            if (sg2d.stroke instanceof BasicStroke) {
                int width = (int) (((BasicStroke) sg2d.stroke).getLineWidth() + 0.5f);
                int widthHalf = width / 2 + 1;
                paddingHalf += widthHalf;
                padding += 2 * widthHalf;
            } else {
                shape = sg2d.stroke.createStrokedShape(shape);
            }
        }
        Rectangle2D bounds = shape.getBounds2D();
        bounds.setRect(bounds.getX() - paddingHalf, bounds.getY() - paddingHalf, bounds.getWidth() + padding, bounds.getHeight() + padding);

        double newX = Math.floor(bounds.getX());
        double newY = Math.floor(bounds.getY());
        double newW = Math.ceil(bounds.getWidth()) + (newX < bounds.getX() ? 1 : 0);
        double newH = Math.ceil(bounds.getHeight()) + (newY < bounds.getY() ? 1 : 0);
        bounds.setRect(newX, newY, newW, newH);

        return bounds;
    }

    void clipBounds(SunGraphics2D sg2d, Rectangle2D bounds) {
        /*
         * System.err.println("clipBounds"); System.err.println("    transform="+sg2d.transform);
         * System.err.println("    getTransform()="+sg2d.getTransform());
         * System.err.println("    complexTransform="+(sg2d.transformState > SunGraphics2D.TRANSFORM_TRANSLATESCALE));
         * System.err.println("    transX="+sg2d.transX+" transY="+sg2d.transX);
         * System.err.println("    sg2d.constrainClip="+sg2d.constrainClip); if (sg2d.constrainClip != null) {
         * System.err
         * .println("    constrainClip: x="+sg2d.constrainClip.getLoX()+" y="+sg2d.constrainClip.getLoY()+" w="
         * +sg2d.constrainClip.getWidth()+" h="+sg2d.constrainClip.getHeight());}
         * System.err.println("    constrainX="+sg2d.constrainX+" constrainY="+sg2d.constrainY);
         * System.err.println("    usrClip="+sg2d.usrClip);
         * System.err.println("    devClip: x="+sg2d.devClip.getLoX()+" y="
         * +sg2d.devClip.getLoY()+" w="+sg2d.devClip.getWidth()+" h="+sg2d.devClip.getHeight());
         */
        Region intersection = sg2d.clipRegion.getIntersectionXYWH((int) bounds.getX(), (int) bounds.getY(), (int) bounds.getWidth(), (int) bounds.getHeight());
        bounds.setRect(intersection.getLoX(), intersection.getLoY(), intersection.getWidth(), intersection.getHeight());
    }

    BufferedImage getSurfacePixels(SunGraphics2D sg2d, OSXSurfaceData surfaceData, int x, int y, int w, int h) {
        // create an image to copy the surface pixels into
        BufferedImage dstInPixels = surfaceData.getCompositingDstInImage(w, h);

        // get the pixels from the dst surface
        return surfaceData.copyArea(sg2d, x, y, w, h, dstInPixels);
    }

    void composite(SunGraphics2D sg2d, OSXSurfaceData surfaceData, BufferedImage srcPixels, Rectangle2D compositingBounds) {
        // Thread.dumpStack();
        // System.err.println("composite");
        // System.err.println("    compositingBounds="+compositingBounds);
        int x = (int) compositingBounds.getX();
        int y = (int) compositingBounds.getY();
        int w = (int) compositingBounds.getWidth();
        int h = (int) compositingBounds.getHeight();

        boolean succeded = false;

        Composite composite = sg2d.getComposite();
        if (composite instanceof XORComposite) {
            // 1st native XOR try
            // we try to perform XOR using surface pixels directly
            try {
                succeded = surfaceData.xorSurfacePixels(sg2d, srcPixels, x, y, w, h, ((XORComposite) composite).getXorColor().getRGB());
            } catch (Exception e) {
                succeded = false;
            }
        }

        if (succeded == false) {
            // create image with the original pixels of surface
            BufferedImage dstInPixels = getSurfacePixels(sg2d, surfaceData, x, y, w, h);
            BufferedImage dstOutPixels = null;

            if (composite instanceof XORComposite) {
                // 2nd native XOR try
                // we try to perform XOR on image's pixels (which were copied from surface first)
                try {
                    OSXSurfaceData osxsd = (OSXSurfaceData) (BufImgSurfaceData.createData(dstInPixels));
                    succeded = osxsd.xorSurfacePixels(sg2d, srcPixels, 0, 0, w, h, ((XORComposite) composite).getXorColor().getRGB());
                    dstOutPixels = dstInPixels;
                } catch (Exception e) {
                    succeded = false;
                }
            }

            // either 2nd native XOR failed OR we have a case of custom compositing
            if (succeded == false) {
                // create an image into which we'll composite result: we MUST use a different destination (compositing
                // is NOT "in place" operation)
                dstOutPixels = surfaceData.getCompositingDstOutImage(w, h);

                // prepare rasters for compositing
                WritableRaster srcRaster = srcPixels.getRaster();
                WritableRaster dstInRaster = dstInPixels.getRaster();
                WritableRaster dstOutRaster = dstOutPixels.getRaster();

                CompositeContext compositeContext = composite.createContext(srcPixels.getColorModel(), dstOutPixels.getColorModel(), sg2d.getRenderingHints());
                compositeContext.compose(srcRaster, dstInRaster, dstOutRaster);
                compositeContext.dispose();

                // gznote: radar bug number
                // "cut out" the shape we're interested in
                // applyMask(BufImgSurfaceData.createData(dstOutPixels), BufImgSurfaceData.createData(srcPixels), w, h);
            }

            // blit the results back to the dst surface
            Composite savedComposite = sg2d.getComposite();
            AffineTransform savedTM = sg2d.getTransform();
            int savedCX = sg2d.constrainX;
            int savedCY = sg2d.constrainY;
            {
                sg2d.setComposite(AlphaComposite.SrcOver);
                // all the compositing is done in the coordinate space of the component. the x and the y are the
                // position of that component in the surface
                // so we need to set the sg2d.transform to identity and we must set the contrainX/Y to 0 for the
                // setTransform() to not be constrained
                sg2d.constrainX = 0;
                sg2d.constrainY = 0;
                sg2d.setTransform(sIdentityMatrix);
                sg2d.drawImage(dstOutPixels, x, y, x + w, y + h, 0, 0, w, h, null);
            }
            sg2d.constrainX = savedCX;
            sg2d.constrainY = savedCY;
            sg2d.setTransform(savedTM);
            sg2d.setComposite(savedComposite);
        }
    }
}
