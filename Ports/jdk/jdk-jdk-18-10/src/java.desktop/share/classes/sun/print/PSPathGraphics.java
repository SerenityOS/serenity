/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Transparency;

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;

import java.awt.image.BufferedImage;
import sun.awt.image.ByteComponentRaster;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * This class converts paths into PostScript
 * by breaking all graphics into fills and
 * clips of paths.
 */

class PSPathGraphics extends PathGraphics {

    /**
     * For a drawing application the initial user space
     * resolution is 72dpi.
     */
    private static final int DEFAULT_USER_RES = 72;

    PSPathGraphics(Graphics2D graphics, PrinterJob printerJob,
                   Printable painter, PageFormat pageFormat, int pageIndex,
                   boolean canRedraw) {
        super(graphics, printerJob, painter, pageFormat, pageIndex, canRedraw);
    }

    /**
     * Creates a new {@code Graphics} object that is
     * a copy of this {@code Graphics} object.
     * @return     a new graphics context that is a copy of
     *                       this graphics context.
     * @since      1.0
     */
    public Graphics create() {

        return new PSPathGraphics((Graphics2D) getDelegate().create(),
                                  getPrinterJob(),
                                  getPrintable(),
                                  getPageFormat(),
                                  getPageIndex(),
                                  canDoRedraws());
    }


    /**
     * Override the inherited implementation of fill
     * so that we can generate PostScript in user space
     * rather than device space.
     */
    public void fill(Shape s, Color color) {
        deviceFill(s.getPathIterator(new AffineTransform()), color);
    }

    /**
     * Draws the text given by the specified string, using this
     * graphics context's current font and color. The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in this
     * graphics context's coordinate system.
     * @param       str      the string to be drawn.
     * @param       x        the <i>x</i> coordinate.
     * @param       y        the <i>y</i> coordinate.
     * @see         java.awt.Graphics#drawBytes
     * @see         java.awt.Graphics#drawChars
     * @since       1.0
     */
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    /**
     * Renders the text specified by the specified {@code String},
     * using the current {@code Font} and {@code Paint} attributes
     * in the {@code Graphics2D} context.
     * The baseline of the first character is at position
     * (<i>x</i>,&nbsp;<i>y</i>) in the User Space.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, {@code Paint}, {@code Font} and
     * {@code Composite} attributes. For characters in script systems
     * such as Hebrew and Arabic, the glyphs can be rendered from right to
     * left, in which case the coordinate supplied is the location of the
     * leftmost character on the baseline.
     * @param str the {@code String} to be rendered
     * @param x,&nbsp;y the coordinates where the {@code String}
     * should be rendered
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see java.awt.Graphics#setFont
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
     public void drawString(String str, float x, float y) {
         drawString(str, x, y, getFont(), getFontRenderContext(), 0f);
     }


    protected boolean canDrawStringToWidth() {
        return true;
    }

    protected int platformFontCount(Font font, String str) {
        PSPrinterJob psPrinterJob = (PSPrinterJob) getPrinterJob();
        return psPrinterJob.platformFontCount(font,  str);
    }

    protected void drawString(String str, float x, float y,
                              Font font, FontRenderContext frc, float w) {
        if (str.length() == 0) {
            return;
        }

        /* If the Font has layout attributes we need to delegate to TextLayout.
         * TextLayout renders text as GlyphVectors. We try to print those
         * using printer fonts - ie using Postscript text operators so
         * we may be reinvoked. In that case the "!printingGlyphVector" test
         * prevents us recursing and instead sends us into the body of the
         * method where we can safely ignore layout attributes as those
         * are already handled by TextLayout.
         */
        if (font.hasLayoutAttributes() && !printingGlyphVector) {
            TextLayout layout = new TextLayout(str, font, frc);
            layout.draw(this, x, y);
            return;
        }

        Font oldFont = getFont();
        if (!oldFont.equals(font)) {
            setFont(font);
        } else {
            oldFont = null;
        }

        boolean drawnWithPS = false;

        float translateX = 0f, translateY = 0f;
        boolean fontisTransformed = getFont().isTransformed();

        if (fontisTransformed) {
            AffineTransform fontTx = getFont().getTransform();
            int transformType = fontTx.getType();
            /* TYPE_TRANSLATION is a flag bit but we can do "==" here
             * because we want to detect when its just that bit set and
             *
             */
            if (transformType == AffineTransform.TYPE_TRANSLATION) {
                translateX = (float)(fontTx.getTranslateX());
                translateY = (float)(fontTx.getTranslateY());
                if (Math.abs(translateX) < 0.00001) translateX = 0f;
                if (Math.abs(translateY) < 0.00001) translateY = 0f;
                fontisTransformed = false;
            }
        }

        boolean directToPS = !fontisTransformed;

        if (!PSPrinterJob.shapeTextProp && directToPS) {

            PSPrinterJob psPrinterJob = (PSPrinterJob) getPrinterJob();
            if (psPrinterJob.setFont(getFont())) {

                /* Set the text color.
                 * We should not be in this shape printing path
                 * if the application is drawing with non-solid
                 * colors. We should be in the raster path. Because
                 * we are here in the shape path, the cast of the
                 * paint to a Color should be fine.
                 */
                try {
                    psPrinterJob.setColor((Color)getPaint());
                } catch (ClassCastException e) {
                    if (oldFont != null) {
                        setFont(oldFont);
                    }
                    throw new IllegalArgumentException(
                                                "Expected a Color instance");
                }

                psPrinterJob.setTransform(getTransform());
                psPrinterJob.setClip(getClip());

                drawnWithPS = psPrinterJob.textOut(this, str,
                                                   x+translateX, y+translateY,
                                                   font, frc, w);
            }
        }

        /* The text could not be converted directly to PS text
         * calls so decompose the text into a shape.
         */
        if (drawnWithPS == false) {
            if (oldFont != null) {
                setFont(oldFont);
                oldFont = null;
            }
            super.drawString(str, x, y, font, frc, w);
        }

        if (oldFont != null) {
            setFont(oldFont);
        }
    }

    /**
     * The various {@code drawImage()} methods for
     * {@code WPathGraphics} are all decomposed
     * into an invocation of {@code drawImageToPlatform}.
     * The portion of the passed in image defined by
     * {@code srcX, srcY, srcWidth, and srcHeight}
     * is transformed by the supplied AffineTransform and
     * drawn using PS to the printer context.
     *
     * @param   image   The image to be drawn.
     *                  This method does nothing if {@code img} is null.
     * @param   xform   Used to transform the image before drawing.
     *                  This can be null.
     * @param   bgcolor This color is drawn where the image has transparent
     *                  pixels. If this parameter is null then the
     *                  pixels already in the destination should show
     *                  through.
     * @param   srcX    With srcY this defines the upper-left corner
     *                  of the portion of the image to be drawn.
     *
     * @param   srcY    With srcX this defines the upper-left corner
     *                  of the portion of the image to be drawn.
     * @param   srcWidth    The width of the portion of the image to
     *                      be drawn.
     * @param   srcHeight   The height of the portion of the image to
     *                      be drawn.
     * @param   handlingTransparency if being recursively called to
     *                    print opaque region of transparent image
     */
    protected boolean drawImageToPlatform(Image image, AffineTransform xform,
                                          Color bgcolor,
                                          int srcX, int srcY,
                                          int srcWidth, int srcHeight,
                                          boolean handlingTransparency) {

        BufferedImage img = getBufferedImage(image);
        if (img == null) {
            return true;
        }

        PSPrinterJob psPrinterJob = (PSPrinterJob) getPrinterJob();

        /* The full transform to be applied to the image is the
         * caller's transform concatenated on to the transform
         * from user space to device space. If the caller didn't
         * supply a transform then we just act as if they passed
         * in the identify transform.
         */
        AffineTransform fullTransform = getTransform();
        if (xform == null) {
            xform = new AffineTransform();
        }
        fullTransform.concatenate(xform);

        /* Split the full transform into a pair of
         * transforms. The first transform holds effects
         * such as rotation and shearing. The second transform
         * is setup to hold only the scaling effects.
         * These transforms are created such that a point,
         * p, in user space, when transformed by 'fullTransform'
         * lands in the same place as when it is transformed
         * by 'rotTransform' and then 'scaleTransform'.
         *
         * The entire image transformation is not in Java in order
         * to minimize the amount of memory needed in the VM. By
         * dividing the transform in two, we rotate and shear
         * the source image in its own space and only go to
         * the, usually, larger, device space when we ask
         * PostScript to perform the final scaling.
         */
        double[] fullMatrix = new double[6];
        fullTransform.getMatrix(fullMatrix);

        /* Calculate the amount of scaling in the x
         * and y directions. This scaling is computed by
         * transforming a unit vector along each axis
         * and computing the resulting magnitude.
         * The computed values 'scaleX' and 'scaleY'
         * represent the amount of scaling PS will be asked
         * to perform.
         * Clamp this to the device scale for better quality printing.
         */
        Point2D.Float unitVectorX = new Point2D.Float(1, 0);
        Point2D.Float unitVectorY = new Point2D.Float(0, 1);
        fullTransform.deltaTransform(unitVectorX, unitVectorX);
        fullTransform.deltaTransform(unitVectorY, unitVectorY);

        Point2D.Float origin = new Point2D.Float(0, 0);
        double scaleX = unitVectorX.distance(origin);
        double scaleY = unitVectorY.distance(origin);

        double devResX = psPrinterJob.getXRes();
        double devResY = psPrinterJob.getYRes();
        double devScaleX = devResX / DEFAULT_USER_RES;
        double devScaleY = devResY / DEFAULT_USER_RES;

        /* check if rotated or sheared */
        int transformType = fullTransform.getType();
        boolean clampScale = ((transformType &
                               (AffineTransform.TYPE_GENERAL_ROTATION |
                                AffineTransform.TYPE_GENERAL_TRANSFORM)) != 0);
        if (clampScale) {
            if (scaleX > devScaleX) scaleX = devScaleX;
            if (scaleY > devScaleY) scaleY = devScaleY;
        }

        /* We do not need to draw anything if either scaling
         * factor is zero.
         */
        if (scaleX != 0 && scaleY != 0) {

            /* Here's the transformation we will do with Java2D,
            */
            AffineTransform rotTransform = new AffineTransform(
                                        fullMatrix[0] / scaleX,  //m00
                                        fullMatrix[1] / scaleY,  //m10
                                        fullMatrix[2] / scaleX,  //m01
                                        fullMatrix[3] / scaleY,  //m11
                                        fullMatrix[4] / scaleX,  //m02
                                        fullMatrix[5] / scaleY); //m12

            /* The scale transform is not used directly: we instead
             * directly multiply by scaleX and scaleY.
             *
             * Conceptually here is what the scaleTransform is:
             *
             * AffineTransform scaleTransform = new AffineTransform(
             *                      scaleX,                     //m00
             *                      0,                          //m10
             *                      0,                          //m01
             *                      scaleY,                     //m11
             *                      0,                          //m02
             *                      0);                         //m12
             */

            /* Convert the image source's rectangle into the rotated
             * and sheared space. Once there, we calculate a rectangle
             * that encloses the resulting shape. It is this rectangle
             * which defines the size of the BufferedImage we need to
             * create to hold the transformed image.
             */
            Rectangle2D.Float srcRect = new Rectangle2D.Float(srcX, srcY,
                                                              srcWidth,
                                                              srcHeight);

            Shape rotShape = rotTransform.createTransformedShape(srcRect);
            Rectangle2D rotBounds = rotShape.getBounds2D();

            /* add a fudge factor as some fp precision problems have
             * been observed which caused pixels to be rounded down and
             * out of the image.
             */
            rotBounds.setRect(rotBounds.getX(), rotBounds.getY(),
                              rotBounds.getWidth()+0.001,
                              rotBounds.getHeight()+0.001);

            int boundsWidth = (int) rotBounds.getWidth();
            int boundsHeight = (int) rotBounds.getHeight();

            if (boundsWidth > 0 && boundsHeight > 0) {


                /* If the image has transparent or semi-transparent
                 * pixels then we'll have the application re-render
                 * the portion of the page covered by the image.
                 * This will be done in a later call to print using the
                 * saved graphics state.
                 * However several special cases can be handled otherwise:
                 * - bitmask transparency with a solid background colour
                 * - images which have transparency color models but no
                 * transparent pixels
                 * - images with bitmask transparency and an IndexColorModel
                 * (the common transparent GIF case) can be handled by
                 * rendering just the opaque pixels.
                 */
                boolean drawOpaque = true;
                if (isCompositing(getComposite())) {
                    drawOpaque = false;
                } else if (!handlingTransparency && hasTransparentPixels(img)) {
                    drawOpaque = false;
                    if (isBitmaskTransparency(img)) {
                        if (bgcolor == null) {
                            if (drawBitmaskImage(img, xform, bgcolor,
                                                srcX, srcY,
                                                 srcWidth, srcHeight)) {
                                // image drawn, just return.
                                return true;
                            }
                        } else if (bgcolor.getTransparency()
                                   == Transparency.OPAQUE) {
                            drawOpaque = true;
                        }
                    }
                    if (!canDoRedraws()) {
                        drawOpaque = true;
                    }
                } else {
                    // if there's no transparent pixels there's no need
                    // for a background colour. This can avoid edge artifacts
                    // in rotation cases.
                    bgcolor = null;
                }
                // if src region extends beyond the image, the "opaque" path
                // may blit b/g colour (including white) where it shoudn't.
                if ((srcX+srcWidth > img.getWidth(null) ||
                     srcY+srcHeight > img.getHeight(null))
                    && canDoRedraws()) {
                    drawOpaque = false;
                }
                if (drawOpaque == false) {

                    fullTransform.getMatrix(fullMatrix);
                    AffineTransform tx =
                        new AffineTransform(
                                            fullMatrix[0] / devScaleX,  //m00
                                            fullMatrix[1] / devScaleY,  //m10
                                            fullMatrix[2] / devScaleX,  //m01
                                            fullMatrix[3] / devScaleY,  //m11
                                            fullMatrix[4] / devScaleX,  //m02
                                            fullMatrix[5] / devScaleY); //m12

                    Rectangle2D.Float rect =
                        new Rectangle2D.Float(srcX, srcY, srcWidth, srcHeight);

                    Shape shape = fullTransform.createTransformedShape(rect);
                    // Region isn't user space because its potentially
                    // been rotated for landscape.
                    Rectangle2D region = shape.getBounds2D();

                    region.setRect(region.getX(), region.getY(),
                                   region.getWidth()+0.001,
                                   region.getHeight()+0.001);

                    // Try to limit the amount of memory used to 8Mb, so
                    // if at device resolution this exceeds a certain
                    // image size then scale down the region to fit in
                    // that memory, but never to less than 72 dpi.

                    int w = (int)region.getWidth();
                    int h = (int)region.getHeight();
                    int nbytes = w * h * 3;
                    int maxBytes = 8 * 1024 * 1024;
                    double origDpi = (devResX < devResY) ? devResX : devResY;
                    int dpi = (int)origDpi;
                    double scaleFactor = 1;

                    double maxSFX = w/(double)boundsWidth;
                    double maxSFY = h/(double)boundsHeight;
                    double maxSF = (maxSFX > maxSFY) ? maxSFY : maxSFX;
                    int minDpi = (int)(dpi/maxSF);
                    if (minDpi < DEFAULT_USER_RES) minDpi = DEFAULT_USER_RES;

                    while (nbytes > maxBytes && dpi > minDpi) {
                        scaleFactor *= 2;
                        dpi /= 2;
                        nbytes /= 4;
                    }
                    if (dpi < minDpi) {
                        scaleFactor = (origDpi / minDpi);
                    }

                    region.setRect(region.getX()/scaleFactor,
                                   region.getY()/scaleFactor,
                                   region.getWidth()/scaleFactor,
                                   region.getHeight()/scaleFactor);

                    /*
                     * We need to have the clip as part of the saved state,
                     * either directly, or all the components that are
                     * needed to reconstitute it (image source area,
                     * image transform and current graphics transform).
                     * The clip is described in user space, so we need to
                     * save the current graphics transform anyway so just
                     * save these two.
                     */
                    psPrinterJob.saveState(getTransform(), getClip(),
                                           region, scaleFactor, scaleFactor);
                    return true;

                /* The image can be rendered directly by PS so we
                 * copy it into a BufferedImage (this takes care of
                 * ColorSpace and BufferedImageOp issues) and then
                 * send that to PS.
                 */
                } else {

                    /* Create a buffered image big enough to hold the portion
                     * of the source image being printed.
                     */
                    BufferedImage deepImage = new BufferedImage(
                                                    (int) rotBounds.getWidth(),
                                                    (int) rotBounds.getHeight(),
                                                    BufferedImage.TYPE_3BYTE_BGR);

                    /* Setup a Graphics2D on to the BufferedImage so that the
                     * source image when copied, lands within the image buffer.
                     */
                    Graphics2D imageGraphics = deepImage.createGraphics();
                    imageGraphics.clipRect(0, 0,
                                           deepImage.getWidth(),
                                           deepImage.getHeight());

                    imageGraphics.translate(-rotBounds.getX(),
                                            -rotBounds.getY());
                    imageGraphics.transform(rotTransform);

                    /* Fill the BufferedImage either with the caller supplied
                     * color, 'bgColor' or, if null, with white.
                     */
                    if (bgcolor == null) {
                        bgcolor = Color.white;
                    }

                    /* REMIND: no need to use scaling here. */
                    imageGraphics.drawImage(img,
                                            srcX, srcY,
                                            srcX + srcWidth, srcY + srcHeight,
                                            srcX, srcY,
                                            srcX + srcWidth, srcY + srcHeight,
                                            bgcolor, null);

                    /* In PSPrinterJob images are printed in device space
                     * and therefore we need to set a device space clip.
                     * FIX: this is an overly tight coupling of these
                     * two classes.
                     * The temporary clip set needs to be an intersection
                     * with the previous user clip.
                     * REMIND: two xfms may lose accuracy in clip path.
                     */
                    Shape holdClip = getClip();
                    Shape oldClip =
                        getTransform().createTransformedShape(holdClip);
                    AffineTransform sat = AffineTransform.getScaleInstance(
                                                             scaleX, scaleY);
                    Shape imgClip = sat.createTransformedShape(rotShape);
                    Area imgArea = new Area(imgClip);
                    Area oldArea = new Area(oldClip);
                    imgArea.intersect(oldArea);
                    psPrinterJob.setClip(imgArea);

                    /* Scale the bounding rectangle by the scale transform.
                     * Because the scaling transform has only x and y
                     * scaling components it is equivalent to multiply
                     * the x components of the bounding rectangle by
                     * the x scaling factor and to multiply the y components
                     * by the y scaling factor.
                     */
                    Rectangle2D.Float scaledBounds
                            = new Rectangle2D.Float(
                                    (float) (rotBounds.getX() * scaleX),
                                    (float) (rotBounds.getY() * scaleY),
                                    (float) (rotBounds.getWidth() * scaleX),
                                    (float) (rotBounds.getHeight() * scaleY));


                    /* Pull the raster data from the buffered image
                     * and pass it along to PS.
                     */
                    ByteComponentRaster tile =
                                   (ByteComponentRaster)deepImage.getRaster();

                    psPrinterJob.drawImageBGR(tile.getDataStorage(),
                                scaledBounds.x, scaledBounds.y,
                                (float)Math.rint(scaledBounds.width+0.5),
                                (float)Math.rint(scaledBounds.height+0.5),
                                0f, 0f,
                                deepImage.getWidth(), deepImage.getHeight(),
                                deepImage.getWidth(), deepImage.getHeight());

                    /* Reset the device clip to match user clip */
                    psPrinterJob.setClip(
                               getTransform().createTransformedShape(holdClip));


                    imageGraphics.dispose();
                }

            }
        }

        return true;
    }

    /** Redraw a rectanglular area using a proxy graphics
      * To do this we need to know the rectangular area to redraw and
      * the transform & clip in effect at the time of the original drawImage
      *
      */

    public void redrawRegion(Rectangle2D region, double scaleX, double scaleY,
                             Shape savedClip, AffineTransform savedTransform)

            throws PrinterException {

        PSPrinterJob psPrinterJob = (PSPrinterJob)getPrinterJob();
        Printable painter = getPrintable();
        PageFormat pageFormat = getPageFormat();
        int pageIndex = getPageIndex();

        /* Create a buffered image big enough to hold the portion
         * of the source image being printed.
         */
        BufferedImage deepImage = new BufferedImage(
                                        (int) region.getWidth(),
                                        (int) region.getHeight(),
                                        BufferedImage.TYPE_3BYTE_BGR);

        /* Get a graphics for the application to render into.
         * We initialize the buffer to white in order to
         * match the paper and then we shift the BufferedImage
         * so that it covers the area on the page where the
         * caller's Image will be drawn.
         */
        Graphics2D g = deepImage.createGraphics();
        ProxyGraphics2D proxy = new ProxyGraphics2D(g, psPrinterJob);
        proxy.setColor(Color.white);
        proxy.fillRect(0, 0, deepImage.getWidth(), deepImage.getHeight());
        proxy.clipRect(0, 0, deepImage.getWidth(), deepImage.getHeight());

        proxy.translate(-region.getX(), -region.getY());

        /* Calculate the resolution of the source image.
         */
        float sourceResX = (float)(psPrinterJob.getXRes() / scaleX);
        float sourceResY = (float)(psPrinterJob.getYRes() / scaleY);

        /* The application expects to see user space at 72 dpi.
         * so change user space from image source resolution to
         *  72 dpi.
         */
        proxy.scale(sourceResX / DEFAULT_USER_RES,
                    sourceResY / DEFAULT_USER_RES);
       proxy.translate(
            -psPrinterJob.getPhysicalPrintableX(pageFormat.getPaper())
               / psPrinterJob.getXRes() * DEFAULT_USER_RES,
            -psPrinterJob.getPhysicalPrintableY(pageFormat.getPaper())
               / psPrinterJob.getYRes() * DEFAULT_USER_RES);
       /* NB User space now has to be at 72 dpi for this calc to be correct */
        proxy.transform(new AffineTransform(getPageFormat().getMatrix()));

        proxy.setPaint(Color.black);

        painter.print(proxy, pageFormat, pageIndex);

        g.dispose();

        /* In PSPrinterJob images are printed in device space
         * and therefore we need to set a device space clip.
         */
        psPrinterJob.setClip(savedTransform.createTransformedShape(savedClip));


        /* Scale the bounding rectangle by the scale transform.
         * Because the scaling transform has only x and y
         * scaling components it is equivalent to multiply
         * the x components of the bounding rectangle by
         * the x scaling factor and to multiply the y components
         * by the y scaling factor.
         */
        Rectangle2D.Float scaledBounds
                = new Rectangle2D.Float(
                        (float) (region.getX() * scaleX),
                        (float) (region.getY() * scaleY),
                        (float) (region.getWidth() * scaleX),
                        (float) (region.getHeight() * scaleY));


        /* Pull the raster data from the buffered image
         * and pass it along to PS.
         */
        ByteComponentRaster tile = (ByteComponentRaster)deepImage.getRaster();

        psPrinterJob.drawImageBGR(tile.getDataStorage(),
                            scaledBounds.x, scaledBounds.y,
                            scaledBounds.width,
                            scaledBounds.height,
                            0f, 0f,
                            deepImage.getWidth(), deepImage.getHeight(),
                            deepImage.getWidth(), deepImage.getHeight());


    }


    /*
     * Fill the path defined by {@code pathIter}
     * with the specified color.
     * The path is provided in current user space.
     */
    protected void deviceFill(PathIterator pathIter, Color color) {

        PSPrinterJob psPrinterJob = (PSPrinterJob) getPrinterJob();
        psPrinterJob.deviceFill(pathIter, color, getTransform(), getClip());
    }

    /*
     * Draw the bounding rectangle using path by calling draw()
     * function and passing a rectangle shape.
     */
    protected void deviceFrameRect(int x, int y, int width, int height,
                                   Color color) {

        draw(new Rectangle2D.Float(x, y, width, height));
    }

    /*
     * Draw a line using path by calling draw() function and passing
     * a line shape.
     */
    protected void deviceDrawLine(int xBegin, int yBegin,
                                  int xEnd, int yEnd, Color color) {

        draw(new Line2D.Float(xBegin, yBegin, xEnd, yEnd));
    }

    /*
     * Fill the rectangle with the specified color by calling fill().
     */
    protected void deviceFillRect(int x, int y, int width, int height,
                                  Color color) {
        fill(new Rectangle2D.Float(x, y, width, height));
    }


    /*
     * This method should not be invoked by PSPathGraphics.
     * FIX: Rework PathGraphics so that this method is
     * not an abstract method there.
     */
    protected void deviceClip(PathIterator pathIter) {
    }

}
