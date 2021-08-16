/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;

import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;

import java.text.AttributedCharacterIterator;

import sun.java2d.Spans;

public class PeekGraphics extends Graphics2D
                          implements PrinterGraphics,
                                     ImageObserver,
                                     Cloneable {

    /**
     * Drawing methods will be forwarded to this object.
     */
    Graphics2D mGraphics;

    /**
     * The PrinterJob controlling the current printing.
     */
    PrinterJob mPrinterJob;

    /**
     * Keeps track of where drawing occurs on the page.
     */
    private Spans mDrawingArea = new Spans();

    /**
     * Track information about the types of drawing
     * performed by the printing application.
     */
    private PeekMetrics mPrintMetrics = new PeekMetrics();

    /**
     * If true the application will only be drawing AWT style
     * graphics, no Java2D graphics.
     */
    private boolean mAWTDrawingOnly = false;

    /**
     * The new PeekGraphics2D will forward state changing
     * calls to 'graphics'. 'printerJob' is stored away
     * so that the printing application can get the PrinterJob
     * if needed.
     */
    public PeekGraphics(Graphics2D graphics, PrinterJob printerJob) {

        mGraphics = graphics;
        mPrinterJob = printerJob;
    }

    /**
     * Return the Graphics2D object that does the drawing
     * for this instance.
     */
    public Graphics2D getDelegate() {
        return mGraphics;
    }

    /**
     * Set the Graphics2D instance which will do the
     * drawing.
     */
    public void setDelegate(Graphics2D graphics) {
        mGraphics = graphics;
    }

    public PrinterJob getPrinterJob() {
        return mPrinterJob;
    }

    /**
     * The caller promises that only AWT graphics will be drawn.
     * The print system can use this information to make general
     * assumptions about the types of graphics to be drawn without
     * requiring the application to draw the contents multiple
     * times.
     */
    public void setAWTDrawingOnly() {
        mAWTDrawingOnly = true;
    }

    public boolean getAWTDrawingOnly() {
        return mAWTDrawingOnly;
    }

    /**
     * Return a Spans instance describing the parts of the page in
     * to which drawing occurred.
     */
    public Spans getDrawingArea() {
        return mDrawingArea;
    }

    /**
     * Returns the device configuration associated with this Graphics2D.
     */
    public GraphicsConfiguration getDeviceConfiguration() {
        return ((RasterPrinterJob)mPrinterJob).getPrinterGraphicsConfig();
    }

/* The Delegated Graphics Methods */

    /**
     * Creates a new {@code Graphics} object that is
     * a copy of this {@code Graphics} object.
     * @return     a new graphics context that is a copy of
     *                       this graphics context.
     * @since      1.0
     */
    public Graphics create() {
        PeekGraphics newGraphics = null;

        try {
            newGraphics = (PeekGraphics) clone();
            newGraphics.mGraphics = (Graphics2D) mGraphics.create();

        /* This exception can not happen unless this
         * class no longer implements the Cloneable
         * interface.
         */
        } catch (CloneNotSupportedException e) {
            // can never happen.
        }

        return newGraphics;
    }

    /**
     * Translates the origin of the graphics context to the point
     * (<i>x</i>,&nbsp;<i>y</i>) in the current coordinate system.
     * Modifies this graphics context so that its new origin corresponds
     * to the point (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's
     * original coordinate system.  All coordinates used in subsequent
     * rendering operations on this graphics context will be relative
     * to this new origin.
     * @param  x   the <i>x</i> coordinate.
     * @param  y   the <i>y</i> coordinate.
     * @since   1.0
     */
    public void translate(int x, int y) {
        mGraphics.translate(x, y);
    }

    /**
     * Concatenates the current transform of this Graphics2D with a
     * translation transformation.
     * This is equivalent to calling transform(T), where T is an
     * AffineTransform represented by the following matrix:
     * <pre>
     *          [   1    0    tx  ]
     *          [   0    1    ty  ]
     *          [   0    0    1   ]
     * </pre>
     */
    public void translate(double tx, double ty) {
        mGraphics.translate(tx, ty);
    }

    /**
     * Concatenates the current transform of this Graphics2D with a
     * rotation transformation.
     * This is equivalent to calling transform(R), where R is an
     * AffineTransform represented by the following matrix:
     * <pre>
     *          [   cos(theta)    -sin(theta)    0   ]
     *          [   sin(theta)     cos(theta)    0   ]
     *          [       0              0         1   ]
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * x axis toward the positive y axis.
     * @param theta The angle of rotation in radians.
     */
    public void rotate(double theta) {
        mGraphics.rotate(theta);
    }

    /**
     * Concatenates the current transform of this Graphics2D with a
     * translated rotation transformation.
     * This is equivalent to the following sequence of calls:
     * <pre>
     *          translate(x, y);
     *          rotate(theta);
     *          translate(-x, -y);
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * x axis toward the positive y axis.
     * @param theta The angle of rotation in radians.
     * @param x The x coordinate of the origin of the rotation
     * @param y The x coordinate of the origin of the rotation
     */
    public void rotate(double theta, double x, double y) {
        mGraphics.rotate(theta, x, y);
    }

    /**
     * Concatenates the current transform of this Graphics2D with a
     * scaling transformation.
     * This is equivalent to calling transform(S), where S is an
     * AffineTransform represented by the following matrix:
     * <pre>
     *          [   sx   0    0   ]
     *          [   0    sy   0   ]
     *          [   0    0    1   ]
     * </pre>
     */
    public void scale(double sx, double sy) {
        mGraphics.scale(sx, sy);
    }

    /**
     * Concatenates the current transform of this Graphics2D with a
     * shearing transformation.
     * This is equivalent to calling transform(SH), where SH is an
     * AffineTransform represented by the following matrix:
     * <pre>
     *          [   1   shx   0   ]
     *          [  shy   1    0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param shx The factor by which coordinates are shifted towards the
     * positive X axis direction according to their Y coordinate
     * @param shy The factor by which coordinates are shifted towards the
     * positive Y axis direction according to their X coordinate
     */
    public void shear(double shx, double shy) {
        mGraphics.shear(shx, shy);
    }

    /**
     * Gets this graphics context's current color.
     * @return    this graphics context's current color.
     * @see       java.awt.Color
     * @see       java.awt.Graphics#setColor
     * @since     1.0
     */
    public Color getColor() {
        return mGraphics.getColor();
    }

    /**
     * Sets this graphics context's current color to the specified
     * color. All subsequent graphics operations using this graphics
     * context use this specified color.
     * @param     c   the new rendering color.
     * @see       java.awt.Color
     * @see       java.awt.Graphics#getColor
     * @since     1.0
     */
    public void setColor(Color c) {
        mGraphics.setColor(c);
    }

    /**
     * Sets the paint mode of this graphics context to overwrite the
     * destination with this graphics context's current color.
     * This sets the logical pixel operation function to the paint or
     * overwrite mode.  All subsequent rendering operations will
     * overwrite the destination with the current color.
     * @since   1.0
     */
    public void setPaintMode() {
        mGraphics.setPaintMode();
    }

    /**
     * Sets the paint mode of this graphics context to alternate between
     * this graphics context's current color and the new specified color.
     * This specifies that logical pixel operations are performed in the
     * XOR mode, which alternates pixels between the current color and
     * a specified XOR color.
     * <p>
     * When drawing operations are performed, pixels which are the
     * current color are changed to the specified color, and vice versa.
     * <p>
     * Pixels that are of colors other than those two colors are changed
     * in an unpredictable but reversible manner; if the same figure is
     * drawn twice, then all pixels are restored to their original values.
     * @param     c1 the XOR alternation color
     * @since     1.0
     */
    public void setXORMode(Color c1) {
        mGraphics.setXORMode(c1);
    }

    /**
     * Gets the current font.
     * @return    this graphics context's current font.
     * @see       java.awt.Font
     * @see       java.awt.Graphics#setFont
     * @since     1.0
     */
    public Font getFont() {
        return mGraphics.getFont();
    }

    /**
     * Sets this graphics context's font to the specified font.
     * All subsequent text operations using this graphics context
     * use this font.
     * @param  font   the font.
     * @see     java.awt.Graphics#getFont
     * @see     java.awt.Graphics#drawChars(char[], int, int, int, int)
     * @see     java.awt.Graphics#drawString(String, int, int)
     * @see     java.awt.Graphics#drawBytes(byte[], int, int, int, int)
     * @since   1.0
    */
    public void setFont(Font font) {
        mGraphics.setFont(font);
    }

    /**
     * Gets the font metrics for the specified font.
     * @return    the font metrics for the specified font.
     * @param     f the specified font
     * @see       java.awt.Graphics#getFont
     * @see       java.awt.FontMetrics
     * @see       java.awt.Graphics#getFontMetrics()
     * @since     1.0
     */
    public FontMetrics getFontMetrics(Font f) {
        return mGraphics.getFontMetrics(f);
    }

    /**
    * Get the rendering context of the font
    * within this Graphics2D context.
    */
    public FontRenderContext getFontRenderContext() {
        return mGraphics.getFontRenderContext();
    }

    /**
     * Returns the bounding rectangle of the current clipping area.
     * The coordinates in the rectangle are relative to the coordinate
     * system origin of this graphics context.
     * @return      the bounding rectangle of the current clipping area.
     * @see         java.awt.Graphics#getClip
     * @see         java.awt.Graphics#clipRect
     * @see         java.awt.Graphics#setClip(int, int, int, int)
     * @see         java.awt.Graphics#setClip(Shape)
     * @since       1.1
     */
    public Rectangle getClipBounds() {
        return mGraphics.getClipBounds();
    }


    /**
     * Intersects the current clip with the specified rectangle.
     * The resulting clipping area is the intersection of the current
     * clipping area and the specified rectangle.
     * This method can only be used to make the current clip smaller.
     * To set the current clip larger, use any of the setClip methods.
     * Rendering operations have no effect outside of the clipping area.
     * @param x the x coordinate of the rectangle to intersect the clip with
     * @param y the y coordinate of the rectangle to intersect the clip with
     * @param width the width of the rectangle to intersect the clip with
     * @param height the height of the rectangle to intersect the clip with
     * @see #setClip(int, int, int, int)
     * @see #setClip(Shape)
     */
    public void clipRect(int x, int y, int width, int height) {
        mGraphics.clipRect(x, y, width, height);
    }


    /**
     * Sets the current clip to the rectangle specified by the given
     * coordinates.
     * Rendering operations have no effect outside of the clipping area.
     * @param       x the <i>x</i> coordinate of the new clip rectangle.
     * @param       y the <i>y</i> coordinate of the new clip rectangle.
     * @param       width the width of the new clip rectangle.
     * @param       height the height of the new clip rectangle.
     * @see         java.awt.Graphics#clipRect
     * @see         java.awt.Graphics#setClip(Shape)
     * @since       1.1
     */
    public void setClip(int x, int y, int width, int height) {
        mGraphics.setClip(x, y, width, height);
    }

    /**
     * Gets the current clipping area.
     * @return      a {@code Shape} object representing the
     *                      current clipping area.
     * @see         java.awt.Graphics#getClipBounds
     * @see         java.awt.Graphics#clipRect
     * @see         java.awt.Graphics#setClip(int, int, int, int)
     * @see         java.awt.Graphics#setClip(Shape)
     * @since       1.1
     */
    public Shape getClip() {
        return mGraphics.getClip();
    }


    /**
     * Sets the current clipping area to an arbitrary clip shape.
     * Not all objects which implement the {@code Shape}
     * interface can be used to set the clip.  The only
     * {@code Shape} objects which are guaranteed to be
     * supported are {@code Shape} objects which are
     * obtained via the {@code getClip} method and via
     * {@code Rectangle} objects.
     * @see         java.awt.Graphics#getClip()
     * @see         java.awt.Graphics#clipRect
     * @see         java.awt.Graphics#setClip(int, int, int, int)
     * @since       1.1
     */
    public void setClip(Shape clip) {
        mGraphics.setClip(clip);
    }


    /**
     * Copies an area of the component by a distance specified by
     * {@code dx} and {@code dy}. From the point specified
     * by {@code x} and {@code y}, this method
     * copies downwards and to the right.  To copy an area of the
     * component to the left or upwards, specify a negative value for
     * {@code dx} or {@code dy}.
     * If a portion of the source rectangle lies outside the bounds
     * of the component, or is obscured by another window or component,
     * {@code copyArea} will be unable to copy the associated
     * pixels. The area that is omitted can be refreshed by calling
     * the component's {@code paint} method.
     * @param       x the <i>x</i> coordinate of the source rectangle.
     * @param       y the <i>y</i> coordinate of the source rectangle.
     * @param       width the width of the source rectangle.
     * @param       height the height of the source rectangle.
     * @param       dx the horizontal distance to copy the pixels.
     * @param       dy the vertical distance to copy the pixels.
     * @since       1.0
     */
    public void copyArea(int x, int y, int width, int height,
                         int dx, int dy) {
        // This method is not supported for printing so we do nothing here.
    }

    /**
     * Draws a line, using the current color, between the points
     * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code>
     * in this graphics context's coordinate system.
     * @param   x1  the first point's <i>x</i> coordinate.
     * @param   y1  the first point's <i>y</i> coordinate.
     * @param   x2  the second point's <i>x</i> coordinate.
     * @param   y2  the second point's <i>y</i> coordinate.
     * @since   1.0
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        addStrokeShape(new Line2D.Float(x1, y1, x2, y2));
        mPrintMetrics.draw(this);
    }



    /**
     * Fills the specified rectangle.
     * The left and right edges of the rectangle are at
     * {@code x} and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>.
     * The top and bottom edges are at
     * {@code y} and <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>.
     * The resulting rectangle covers an area
     * {@code width} pixels wide by
     * {@code height} pixels tall.
     * The rectangle is filled using the graphics context's current color.
     * @param         x   the <i>x</i> coordinate
     *                         of the rectangle to be filled.
     * @param         y   the <i>y</i> coordinate
     *                         of the rectangle to be filled.
     * @param         width   the width of the rectangle to be filled.
     * @param         height   the height of the rectangle to be filled.
     * @see           java.awt.Graphics#fillRect
     * @see           java.awt.Graphics#clearRect
     * @since         1.0
     */
    public void fillRect(int x, int y, int width, int height) {

        addDrawingRect(new Rectangle2D.Float(x, y, width, height));
        mPrintMetrics.fill(this);

    }

    /**
     * Clears the specified rectangle by filling it with the background
     * color of the current drawing surface. This operation does not
     * use the current paint mode.
     * <p>
     * Beginning with Java&nbsp;1.1, the background color
     * of offscreen images may be system dependent. Applications should
     * use {@code setColor} followed by {@code fillRect} to
     * ensure that an offscreen image is cleared to a specific color.
     * @param       x the <i>x</i> coordinate of the rectangle to clear.
     * @param       y the <i>y</i> coordinate of the rectangle to clear.
     * @param       width the width of the rectangle to clear.
     * @param       height the height of the rectangle to clear.
     * @see         java.awt.Graphics#fillRect(int, int, int, int)
     * @see         java.awt.Graphics#drawRect
     * @see         java.awt.Graphics#setColor(java.awt.Color)
     * @see         java.awt.Graphics#setPaintMode
     * @see         java.awt.Graphics#setXORMode(java.awt.Color)
     * @since       1.0
     */
    public void clearRect(int x, int y, int width, int height) {
        Rectangle2D.Float rect = new Rectangle2D.Float(x, y, width, height);
        addDrawingRect(rect);
        mPrintMetrics.clear(this);
    }

    /**
     * Draws an outlined round-cornered rectangle using this graphics
     * context's current color. The left and right edges of the rectangle
     * are at {@code x} and <code>x&nbsp;+&nbsp;width</code>,
     * respectively. The top and bottom edges of the rectangle are at
     * {@code y} and <code>y&nbsp;+&nbsp;height</code>.
     * @param      x the <i>x</i> coordinate of the rectangle to be drawn.
     * @param      y the <i>y</i> coordinate of the rectangle to be drawn.
     * @param      width the width of the rectangle to be drawn.
     * @param      height the height of the rectangle to be drawn.
     * @param      arcWidth the horizontal diameter of the arc
     *                    at the four corners.
     * @param      arcHeight the vertical diameter of the arc
     *                    at the four corners.
     * @see        java.awt.Graphics#fillRoundRect
     * @since      1.0
     */
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        addStrokeShape(new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight));
        mPrintMetrics.draw(this);

    }

    /**
     * Fills the specified rounded corner rectangle with the current color.
     * The left and right edges of the rectangle
     * are at {@code x} and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>,
     * respectively. The top and bottom edges of the rectangle are at
     * {@code y} and <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>.
     * @param       x the <i>x</i> coordinate of the rectangle to be filled.
     * @param       y the <i>y</i> coordinate of the rectangle to be filled.
     * @param       width the width of the rectangle to be filled.
     * @param       height the height of the rectangle to be filled.
     * @param       arcWidth the horizontal diameter
     *                     of the arc at the four corners.
     * @param       arcHeight the vertical diameter
     *                     of the arc at the four corners.
     * @see         java.awt.Graphics#drawRoundRect
     * @since       1.0
     */
    public void fillRoundRect(int x, int y, int width, int height,
                                       int arcWidth, int arcHeight) {
        Rectangle2D.Float rect = new Rectangle2D.Float(x, y,width, height);
        addDrawingRect(rect);
        mPrintMetrics.fill(this);
    }

    /**
     * Draws the outline of an oval.
     * The result is a circle or ellipse that fits within the
     * rectangle specified by the {@code x}, {@code y},
     * {@code width}, and {@code height} arguments.
     * <p>
     * The oval covers an area that is
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * and <code>height&nbsp;+&nbsp;1</code> pixels tall.
     * @param       x the <i>x</i> coordinate of the upper left
     *                     corner of the oval to be drawn.
     * @param       y the <i>y</i> coordinate of the upper left
     *                     corner of the oval to be drawn.
     * @param       width the width of the oval to be drawn.
     * @param       height the height of the oval to be drawn.
     * @see         java.awt.Graphics#fillOval
     * @since       1.0
     */
    public void drawOval(int x, int y, int width, int height) {
        addStrokeShape(new Rectangle2D.Float(x, y,  width, height));
        mPrintMetrics.draw(this);
    }

    /**
     * Fills an oval bounded by the specified rectangle with the
     * current color.
     * @param       x the <i>x</i> coordinate of the upper left corner
     *                     of the oval to be filled.
     * @param       y the <i>y</i> coordinate of the upper left corner
     *                     of the oval to be filled.
     * @param       width the width of the oval to be filled.
     * @param       height the height of the oval to be filled.
     * @see         java.awt.Graphics#drawOval
     * @since       1.0
     */
    public void fillOval(int x, int y, int width, int height) {
        Rectangle2D.Float rect = new Rectangle2D.Float(x, y, width, height);
        addDrawingRect(rect);
        mPrintMetrics.fill(this);

    }


    /**
     * Draws the outline of a circular or elliptical arc
     * covering the specified rectangle.
     * <p>
     * The resulting arc begins at {@code startAngle} and extends
     * for {@code arcAngle} degrees, using the current color.
     * Angles are interpreted such that 0&nbsp;degrees
     * is at the 3&nbsp;o'clock position.
     * A positive value indicates a counter-clockwise rotation
     * while a negative value indicates a clockwise rotation.
     * <p>
     * The center of the arc is the center of the rectangle whose origin
     * is (<i>x</i>,&nbsp;<i>y</i>) and whose size is specified by the
     * {@code width} and {@code height} arguments.
     * <p>
     * The resulting arc covers an area
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * by <code>height&nbsp;+&nbsp;1</code> pixels tall.
     * @param        x the <i>x</i> coordinate of the
     *                    upper-left corner of the arc to be drawn.
     * @param        y the <i>y</i>  coordinate of the
     *                    upper-left corner of the arc to be drawn.
     * @param        width the width of the arc to be drawn.
     * @param        height the height of the arc to be drawn.
     * @param        startAngle the beginning angle.
     * @param        arcAngle the angular extent of the arc,
     *                    relative to the start angle.
     * @see         java.awt.Graphics#fillArc
     * @since       1.0
     */
    public void drawArc(int x, int y, int width, int height,
                                 int startAngle, int arcAngle) {
        addStrokeShape(new Rectangle2D.Float(x, y,  width, height));
        mPrintMetrics.draw(this);

    }

    /**
     * Fills a circular or elliptical arc covering the specified rectangle.
     * <p>
     * The resulting arc begins at {@code startAngle} and extends
     * for {@code arcAngle} degrees.
     * Angles are interpreted such that 0&nbsp;degrees
     * is at the 3&nbsp;o'clock position.
     * A positive value indicates a counter-clockwise rotation
     * while a negative value indicates a clockwise rotation.
     * <p>
     * The center of the arc is the center of the rectangle whose origin
     * is (<i>x</i>,&nbsp;<i>y</i>) and whose size is specified by the
     * {@code width} and {@code height} arguments.
     * <p>
     * The resulting arc covers an area
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * by <code>height&nbsp;+&nbsp;1</code> pixels tall.
     * @param        x the <i>x</i> coordinate of the
     *                    upper-left corner of the arc to be filled.
     * @param        y the <i>y</i>  coordinate of the
     *                    upper-left corner of the arc to be filled.
     * @param        width the width of the arc to be filled.
     * @param        height the height of the arc to be filled.
     * @param        startAngle the beginning angle.
     * @param        arcAngle the angular extent of the arc,
     *                    relative to the start angle.
     * @see         java.awt.Graphics#drawArc
     * @since       1.0
     */
    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        Rectangle2D.Float rect = new Rectangle2D.Float(x, y,width, height);
        addDrawingRect(rect);
        mPrintMetrics.fill(this);

    }

    /**
     * Draws a sequence of connected lines defined by
     * arrays of <i>x</i> and <i>y</i> coordinates.
     * Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines a point.
     * The figure is not closed if the first point
     * differs from the last point.
     * @param       xPoints an array of <i>x</i> points
     * @param       yPoints an array of <i>y</i> points
     * @param       nPoints the total number of points
     * @see         java.awt.Graphics#drawPolygon(int[], int[], int)
     * @since       1.1
     */
   public void drawPolyline(int[] xPoints, int[] yPoints,
                             int nPoints) {
        if (nPoints > 0) {
            int x = xPoints[0];
            int y = yPoints[0];

            for (int i = 1; i < nPoints; i++) {
                drawLine(x, y, xPoints[i], yPoints[i]);
                x = xPoints[i];
                y = yPoints[i];
            }
        }

    }

    /**
     * Draws a closed polygon defined by
     * arrays of <i>x</i> and <i>y</i> coordinates.
     * Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines a point.
     * <p>
     * This method draws the polygon defined by {@code nPoint} line
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code>
     * line segments are line segments from
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code>
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for
     * 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;{@code nPoints}.
     * The figure is automatically closed by drawing a line connecting
     * the final point to the first point, if those points are different.
     * @param        xPoints   a an array of {@code x} coordinates.
     * @param        yPoints   a an array of {@code y} coordinates.
     * @param        nPoints   a the total number of points.
     * @see          java.awt.Graphics#fillPolygon
     * @see          java.awt.Graphics#drawPolyline
     * @since        1.0
     */
    public void drawPolygon(int[] xPoints, int[] yPoints,
                            int nPoints) {
        if (nPoints > 0) {
            drawPolyline(xPoints, yPoints, nPoints);
            drawLine(xPoints[nPoints - 1], yPoints[nPoints - 1],
                     xPoints[0], yPoints[0]);
        }

    }

    /**
     * Fills a closed polygon defined by
     * arrays of <i>x</i> and <i>y</i> coordinates.
     * <p>
     * This method draws the polygon defined by {@code nPoint} line
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code>
     * line segments are line segments from
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code>
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for
     * 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;{@code nPoints}.
     * The figure is automatically closed by drawing a line connecting
     * the final point to the first point, if those points are different.
     * <p>
     * The area inside the polygon is defined using an
     * even-odd fill rule, also known as the alternating rule.
     * @param        xPoints   a an array of {@code x} coordinates.
     * @param        yPoints   a an array of {@code y} coordinates.
     * @param        nPoints   a the total number of points.
     * @see          java.awt.Graphics#drawPolygon(int[], int[], int)
     * @since        1.0
     */
    public void fillPolygon(int[] xPoints, int[] yPoints,
                            int nPoints) {
        if (nPoints > 0) {
            int minX = xPoints[0];
            int minY = yPoints[0];
            int maxX = xPoints[0];
            int maxY = yPoints[0];

            for (int i = 1; i < nPoints; i++) {

                if (xPoints[i] < minX) {
                    minX = xPoints[i];
                } else if (xPoints[i] > maxX) {
                    maxX = xPoints[i];
                }

                if (yPoints[i] < minY) {
                    minY = yPoints[i];
                } else if (yPoints[i] > maxY) {
                    maxY = yPoints[i];
                }
            }

            addDrawingRect(minX, minY, maxX - minX, maxY - minY);
        }

        mPrintMetrics.fill(this);

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

        drawString(str, (float)x, (float)y);
    }

    /**
     * Draws the text given by the specified iterator, using this
     * graphics context's current color. The iterator has to specify a font
     * for each character. The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in this
     * graphics context's coordinate system.
     * The rendering attributes applied include the clip, transform,
     * paint or color, and composite attributes.
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs may be draw from right to left, in which case the
     * coordinate supplied is the location of the leftmost character
     * on the baseline.
     * @param iterator the iterator whose text is to be drawn
     * @param x,y the coordinates where the iterator's text should be drawn.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public void drawString(AttributedCharacterIterator iterator,
                                    int x, int y) {

        drawString(iterator,  (float)x, (float)y);
    }

    /**
     * Draws the text given by the specified iterator, using this
     * graphics context's current color. The iterator has to specify a font
     * for each character. The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in this
     * graphics context's coordinate system.
     * The rendering attributes applied include the clip, transform,
     * paint or color, and composite attributes.
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs may be draw from right to left, in which case the
     * coordinate supplied is the location of the leftmost character
     * on the baseline.
     * @param iterator the iterator whose text is to be drawn
     * @param x,y the coordinates where the iterator's text should be drawn.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public void drawString(AttributedCharacterIterator iterator,
                                    float x, float y) {
        if (iterator == null) {
            throw new
                NullPointerException("AttributedCharacterIterator is null");
        }

        TextLayout layout = new TextLayout(iterator, getFontRenderContext());
        layout.draw(this, x, y);
    }


    /**
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at
     * (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's coordinate
     * space. Transparent pixels in the image do not affect whatever
     * pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * complete image has not yet been loaded, and it has not been dithered
     * and converted for the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * @param    img the specified image to be drawn.
     * @param    x   the <i>x</i> coordinate.
     * @param    y   the <i>y</i> coordinate.
     * @param    observer    object to be notified as more of
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    1.0
     */
    public boolean drawImage(Image img, int x, int y,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }

        /* The ImageWaiter creation does not return until the
         * image is loaded.
         */
        ImageWaiter dim = new ImageWaiter(img);

        addDrawingRect(x, y, dim.getWidth(), dim.getHeight());
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, x, y, observer);
    }


    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this
     * graphics context's coordinate space, and is scaled if
     * necessary. Transparent pixels do not affect whatever pixels
     * are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete, then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the image observer by calling its {@code imageUpdate} method.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    img    the specified image to be drawn.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    width  the width of the rectangle.
     * @param    height the height of the rectangle.
     * @param    observer    object to be notified as more of
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    1.0
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }
        addDrawingRect(x, y, width, height);
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, x, y, width, height, observer);

    }

    /**
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at
     * (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's coordinate
     * space.  Transparent pixels are drawn in the specified
     * background color.
     * <p>
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * complete image has not yet been loaded, and it has not been dithered
     * and converted for the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * @param    img    the specified image to be drawn.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    bgcolor the background color to paint under the
     *                         non-opaque portions of the image.
     * @param    observer    object to be notified as more of
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    1.0
     */
   public boolean drawImage(Image img, int x, int y,
                             Color bgcolor,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }

        /* The ImageWaiter creation does not return until the
         * image is loaded.
         */
        ImageWaiter dim = new ImageWaiter(img);

        addDrawingRect(x, y, dim.getWidth(), dim.getHeight());
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, x, y, bgcolor, observer);
    }


    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this
     * graphics context's coordinate space, and is scaled if
     * necessary. Transparent pixels are drawn in the specified
     * background color.
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    img       the specified image to be drawn.
     * @param    x         the <i>x</i> coordinate.
     * @param    y         the <i>y</i> coordinate.
     * @param    width     the width of the rectangle.
     * @param    height    the height of the rectangle.
     * @param    bgcolor   the background color to paint under the
     *                         non-opaque portions of the image.
     * @param    observer    object to be notified as more of
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    1.0
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             Color bgcolor,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }

        addDrawingRect(x, y, width, height);
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, x, y, width, height, bgcolor, observer);

    }

    /**
     * Draws as much of the specified area of the specified image as is
     * currently available, scaling it on the fly to fit inside the
     * specified area of the destination drawable surface. Transparent pixels
     * do not affect whatever pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * image area to be drawn has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * <p>
     * This method always uses the unscaled version of the image
     * to render the scaled rectangle and performs the required
     * scaling on the fly. It does not use a cached, scaled version
     * of the image for this operation. Scaling of the image from source
     * to destination is performed such that the first coordinate
     * of the source rectangle is mapped to the first coordinate of
     * the destination rectangle, and the second source coordinate is
     * mapped to the second destination coordinate. The subimage is
     * scaled and flipped as needed to preserve those mappings.
     * @param       img the specified image to be drawn
     * @param       dx1 the <i>x</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dy1 the <i>y</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dx2 the <i>x</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       dy2 the <i>y</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       sx1 the <i>x</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sy1 the <i>y</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sx2 the <i>x</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       sy2 the <i>y</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       observer object to be notified as more of the image is
     *                    scaled and converted.
     * @see         java.awt.Image
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since       1.1
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }

        int width = dx2 - dx1;
        int height = dy2 - dy1;

        addDrawingRect(dx1, dy1, width, height);
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, dx1, dy1, dx2, dy2,
                               sx1, sy1, sx2, sy2, observer);

    }


    /**
     * Draws as much of the specified area of the specified image as is
     * currently available, scaling it on the fly to fit inside the
     * specified area of the destination drawable surface.
     * <p>
     * Transparent pixels are drawn in the specified background color.
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * image area to be drawn has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * <p>
     * This method always uses the unscaled version of the image
     * to render the scaled rectangle and performs the required
     * scaling on the fly. It does not use a cached, scaled version
     * of the image for this operation. Scaling of the image from source
     * to destination is performed such that the first coordinate
     * of the source rectangle is mapped to the first coordinate of
     * the destination rectangle, and the second source coordinate is
     * mapped to the second destination coordinate. The subimage is
     * scaled and flipped as needed to preserve those mappings.
     * @param       img the specified image to be drawn
     * @param       dx1 the <i>x</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dy1 the <i>y</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dx2 the <i>x</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       dy2 the <i>y</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       sx1 the <i>x</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sy1 the <i>y</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sx2 the <i>x</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       sy2 the <i>y</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       bgcolor the background color to paint under the
     *                    non-opaque portions of the image.
     * @param       observer object to be notified as more of the image is
     *                    scaled and converted.
     * @see         java.awt.Image
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since       1.1
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer) {

        if (img == null) {
            return true;
        }

        int width = dx2 - dx1;
        int height = dy2 - dy1;

        addDrawingRect(dx1, dy1, width, height);
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, dx1, dy1, dx2, dy2,
                               sx1, sy1, sx2, sy2, bgcolor, observer);

    }


    /**
     * Draws an image, applying a transform from image space into user space
     * before drawing.
     * The transformation from user space into device space is done with
     * the current transform in the Graphics2D.
     * The given transformation is applied to the image before the
     * transform attribute in the Graphics2D state is applied.
     * The rendering attributes applied include the clip, transform,
     * and composite attributes. Note that the result is
     * undefined, if the given transform is noninvertible.
     * @param img The image to be drawn.
     * @param xform The transformation from image space into user space.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawRenderedImage(RenderedImage img,
                                  AffineTransform xform) {

        if (img == null) {
            return;
        }

        mPrintMetrics.drawImage(this, img);
        mDrawingArea.addInfinite();
    }


    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {

        if (img == null) {
            return;
        }

        mPrintMetrics.drawImage(this, img);
        mDrawingArea.addInfinite();
    }

    /**
     * Disposes of this graphics context and releases
     * any system resources that it is using.
     * A {@code Graphics} object cannot be used after
     * {@code dispose} has been called.
     * <p>
     * When a Java program runs, a large number of {@code Graphics}
     * objects can be created within a short time frame.
     * Although the finalization process of the garbage collector
     * also disposes of the same system resources, it is preferable
     * to manually free the associated resources by calling this
     * method rather than to rely on a finalization process which
     * may not run to completion for a long period of time.
     * <p>
     * Graphics objects which are provided as arguments to the
     * {@code paint} and {@code update} methods
     * of components are automatically released by the system when
     * those methods return. For efficiency, programmers should
     * call {@code dispose} when finished using
     * a {@code Graphics} object only if it was created
     * directly from a component or another {@code Graphics} object.
     * @see         java.awt.Graphics#finalize
     * @see         java.awt.Component#paint
     * @see         java.awt.Component#update
     * @see         java.awt.Component#getGraphics
     * @see         java.awt.Graphics#create
     * @since       1.0
     */
    public void dispose() {
        mGraphics.dispose();
    }

    /**
     * Empty finalizer as no clean up needed here.
     */
    @SuppressWarnings("deprecation")
    public void finalize() {
    }

/* The Delegated Graphics2D Methods */

    /**
     * Strokes the outline of a Shape using the settings of the current
     * graphics state.  The rendering attributes applied include the
     * clip, transform, paint or color, composite and stroke attributes.
     * @param s The shape to be drawn.
     * @see #setStroke
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     * @see #setComposite
     */
    public void draw(Shape s) {
        addStrokeShape(s);
        mPrintMetrics.draw(this);
    }


    /**
     * Draws an image, applying a transform from image space into user space
     * before drawing.
     * The transformation from user space into device space is done with
     * the current transform in the Graphics2D.
     * The given transformation is applied to the image before the
     * transform attribute in the Graphics2D state is applied.
     * The rendering attributes applied include the clip, transform,
     * and composite attributes. Note that the result is
     * undefined, if the given transform is noninvertible.
     * @param img The image to be drawn.
     * @param xform The transformation from image space into user space.
     * @param obs The image observer to be notified as more of the image
     * is converted.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs) {

        if (img == null) {
            return true;
        }

        mDrawingArea.addInfinite();
        mPrintMetrics.drawImage(this, img);

        return mGraphics.drawImage(img, xform, obs);


//      if (mDrawingArea[0] != null) {
//          Rectangle2D.Double bbox = new Rectangle2D.Double();
//          Point2D leftTop = new Point2D.Double(0, 0);
//          Point2D rightBottom = new Point2D.Double(getImageWidth(img),
//                                                   getImageHeight(img));

//          xform.transform(leftTop, leftTop);
//          xform.transform(rightBottom, rightBottom);

//          bbox.setBoundsFromDiagonal(leftTop, rightBottom);
//          addDrawingRect(bbox);

//      }
    }


    /**
     * Draws a BufferedImage that is filtered with a BufferedImageOp.
     * The rendering attributes applied include the clip, transform
     * and composite attributes.  This is equivalent to:
     * <pre>
     * img1 = op.filter(img, null);
     * drawImage(img1, new AffineTransform(1f,0f,0f,1f,x,y), null);
     * </pre>
     * @param op The filter to be applied to the image before drawing.
     * @param img The BufferedImage to be drawn.
     * @param x,y The location in user space where the image should be drawn.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawImage(BufferedImage img,
                          BufferedImageOp op,
                          int x,
                          int y) {

        if (img == null) {
            return;
        }

        mPrintMetrics.drawImage(this, (RenderedImage) img);
        mDrawingArea.addInfinite();
    }


    /**
     * Draws a string of text.
     * The rendering attributes applied include the clip, transform,
     * paint or color, font and composite attributes.
     * @param str The string to be drawn.
     * @param x,y The coordinates where the string should be drawn.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see java.awt.Graphics#setFont
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawString(String str,
                           float x,
                           float y) {

        if (str.length() == 0) {
            return;
        }
        /* Logical bounds close enough and is used for GlyphVector */
        FontRenderContext frc = getFontRenderContext();
        Rectangle2D bbox = getFont().getStringBounds(str, frc);
        addDrawingRect(bbox, x, y);
        mPrintMetrics.drawText(this);
    }

    /**
     * Draws a GlyphVector.
     * The rendering attributes applied include the clip, transform,
     * paint or color, and composite attributes.  The GlyphVector specifies
     * individual glyphs from a Font.
     * @param g The GlyphVector to be drawn.
     * @param x,y The coordinates where the glyphs should be drawn.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawGlyphVector(GlyphVector g,
                           float x,
                           float y) {

        Rectangle2D bbox = g.getLogicalBounds();
        addDrawingRect(bbox, x, y);
        mPrintMetrics.drawText(this);

    }

    /**
     * Fills the interior of a Shape using the settings of the current
     * graphics state. The rendering attributes applied include the
     * clip, transform, paint or color, and composite.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void fill(Shape s) {
        addDrawingRect(s.getBounds());
        mPrintMetrics.fill(this);

    }


    /**
     * Checks to see if the outline of a Shape intersects the specified
     * Rectangle in device space.
     * The rendering attributes taken into account include the
     * clip, transform, and stroke attributes.
     * @param rect The area in device space to check for a hit.
     * @param s The shape to check for a hit.
     * @param onStroke Flag to choose between testing the stroked or
     * the filled shape.
     * @return True if there is a hit, false otherwise.
     * @see #setStroke
     * @see #fill
     * @see #draw
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     */
    public boolean hit(Rectangle rect,
                       Shape s,
                       boolean onStroke) {

        return mGraphics.hit(rect, s, onStroke);
    }

    /**
     * Sets the Composite in the current graphics state. Composite is used
     * in all drawing methods such as drawImage, drawString, draw,
     * and fill.  It specifies how new pixels are to be combined with
     * the existing pixels on the graphics device in the rendering process.
     * @param comp The Composite object to be used for drawing.
     * @see java.awt.Graphics#setXORMode
     * @see java.awt.Graphics#setPaintMode
     * @see java.awt.AlphaComposite
     */
    public void setComposite(Composite comp) {
        mGraphics.setComposite(comp);
    }


    /**
     * Sets the Paint in the current graphics state.
     * @param paint The Paint object to be used to generate color in
     * the rendering process.
     * @see java.awt.Graphics#setColor
     * @see java.awt.GradientPaint
     * @see java.awt.TexturePaint
     */
    public void setPaint(Paint paint) {
        mGraphics.setPaint(paint);
    }

    /**
     * Sets the Stroke in the current graphics state.
     * @param s The Stroke object to be used to stroke a Shape in
     * the rendering process.
     * @see BasicStroke
     */
    public void setStroke(Stroke s) {
        mGraphics.setStroke(s);
    }

    /**
     * Sets the preferences for the rendering algorithms.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * @param hintCategory The category of hint to be set.
     * @param hintValue The value indicating preferences for the specified
     * hint category.
     * @see RenderingHints
     */
    public void setRenderingHint(Key hintCategory, Object hintValue) {
        mGraphics.setRenderingHint(hintCategory, hintValue);
    }

    /**
     * Returns the preferences for the rendering algorithms.
     * @param hintCategory The category of hint to be set.
     * @return The preferences for rendering algorithms.
     * @see RenderingHints
     */
    public Object getRenderingHint(Key hintCategory) {
        return mGraphics.getRenderingHint(hintCategory);
    }

    /**
     * Sets the preferences for the rendering algorithms.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * @param hints The rendering hints to be set
     * @see RenderingHints
     */
    public void setRenderingHints(Map<?,?> hints) {
        mGraphics.setRenderingHints(hints);
    }

    /**
     * Adds a number of preferences for the rendering algorithms.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * @param hints The rendering hints to be set
     * @see RenderingHints
     */
    public void addRenderingHints(Map<?,?> hints) {
        mGraphics.addRenderingHints(hints);
    }

    /**
     * Gets the preferences for the rendering algorithms.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * @see RenderingHints
     */
    public RenderingHints getRenderingHints() {
        return mGraphics.getRenderingHints();
    }

    /**
     * Composes a Transform object with the transform in this
     * Graphics2D according to the rule last-specified-first-applied.
     * If the currrent transform is Cx, the result of composition
     * with Tx is a new transform Cx'.  Cx' becomes the current
     * transform for this Graphics2D.
     * Transforming a point p by the updated transform Cx' is
     * equivalent to first transforming p by Tx and then transforming
     * the result by the original transform Cx.  In other words,
     * Cx'(p) = Cx(Tx(p)).
     * A copy of the Tx is made, if necessary, so further
     * modifications to Tx do not affect rendering.
     * @param Tx The Transform object to be composed with the current
     * transform.
     * @see #setTransform
     * @see AffineTransform
     */
    public void transform(AffineTransform Tx) {
        mGraphics.transform(Tx);
    }

    /**
     * Sets the Transform in the current graphics state.
     * @param Tx The Transform object to be used in the rendering process.
     * @see #transform
     * @see AffineTransform
     */
    public void setTransform(AffineTransform Tx) {
        mGraphics.setTransform(Tx);
    }

    /**
     * Returns the current Transform in the Graphics2D state.
     * @see #transform
     * @see #setTransform
     */
    public AffineTransform getTransform() {
        return mGraphics.getTransform();
    }

    /**
     * Returns the current Paint in the Graphics2D state.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     */
    public Paint getPaint() {
        return mGraphics.getPaint();
    }

    /**
     * Returns the current Composite in the Graphics2D state.
     * @see #setComposite
     */
    public Composite getComposite() {
        return mGraphics.getComposite();
    }

    /**
     * Sets the background color in this context used for clearing a region.
     * When Graphics2D is constructed for a component, the backgroung color is
     * inherited from the component. Setting the background color in the
     * Graphics2D context only affects the subsequent clearRect() calls and
     * not the background color of the component. To change the background
     * of the component, use appropriate methods of the component.
     * @param color The background color that should be used in
     * subsequent calls to clearRect().
     * @see #getBackground
     * @see Graphics#clearRect
     */
    public void setBackground(Color color) {
        mGraphics.setBackground(color);
    }

    /**
     * Returns the background color used for clearing a region.
     * @see #setBackground
     */
    public Color getBackground() {
        return mGraphics.getBackground();
    }

    /**
     * Returns the current Stroke in the Graphics2D state.
     * @see #setStroke
     */
    public Stroke getStroke() {
        return mGraphics.getStroke();
    }

    /**
     * Intersects the current clip with the interior of the specified Shape
     * and sets the current clip to the resulting intersection.
     * The indicated shape is transformed with the current transform in the
     * Graphics2D state before being intersected with the current clip.
     * This method is used to make the current clip smaller.
     * To make the clip larger, use any setClip method.
     * @param s The Shape to be intersected with the current clip.
     */
     public void clip(Shape s) {
        mGraphics.clip(s);
     }

     /**
      * Return true if the Rectangle {@code rect}
      * intersects the area into which the application
      * has drawn.
      */
     public boolean hitsDrawingArea(Rectangle rect) {

         return mDrawingArea.intersects((float) rect.getMinY(),
                                        (float) rect.getMaxY());
     }

     /**
      * Return the object holding the summary of the
      * drawing done by the printing application.
      */
     public PeekMetrics getMetrics() {
        return mPrintMetrics;
     }

 /* Support Routines for Calculating the Drawing Area */

   /**
     * Shift the rectangle 'rect' to the position ('x', 'y')
     * and add the resulting rectangle to the area representing
     * the part of the page which is drawn into.
     */
    private void addDrawingRect(Rectangle2D rect, float x, float y) {

        addDrawingRect((float) (rect.getX() + x),
                       (float) (rect.getY() + y),
                       (float) rect.getWidth(),
                       (float) rect.getHeight());

    }

    private void addDrawingRect(float x, float y, float width, float height) {

        Rectangle2D.Float bbox = new Rectangle2D.Float(x, y, width, height);
        addDrawingRect(bbox);
    }

    /**
     * Add the rectangle 'rect' to the area representing
     * the part of the page which is drawn into.
     */
    private void addDrawingRect(Rectangle2D rect) {

        /*  For testing purposes the following line can be uncommented.
            When uncommented it causes the entire page to be rasterized
            thus eliminating errors caused by a faulty bounding box
            calculation.
        */
        //mDrawingArea.addInfinite();



        AffineTransform matrix = getTransform();

        Shape transShape = matrix.createTransformedShape(rect);

        Rectangle2D transRect = transShape.getBounds2D();

        mDrawingArea.add((float) transRect.getMinY(),
                         (float) transRect.getMaxY());


    }

    /**
     * Add the stroked shape to the area representing
     * the part of the page which is drawn into.
     */
    private void addStrokeShape(Shape s) {
        Shape transShape = getStroke().createStrokedShape(s);
        addDrawingRect(transShape.getBounds2D());
    }

    /* Image Observer */

    /**
     * Notify this object when the height or width become available
     * for an image.
     */
    public synchronized boolean imageUpdate(Image img, int infoFlags,
                                            int x, int y,
                                            int width, int height) {

        boolean gotInfo = false;

        if((infoFlags & (WIDTH | HEIGHT)) != 0) {
            gotInfo = true;
            notify();
        }

        return gotInfo;
    }

    private synchronized int getImageWidth(Image img) {

        /* Wait for the width the image to
         * become available.
         */
        while (img.getWidth(this) == -1) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }


        return img.getWidth(this);
    }

    private synchronized int getImageHeight(Image img) {

        /* Wait for the height the image to
         * become available.
         */
        while (img.getHeight(this) == -1) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }


        return img.getHeight(this);
    }

    /**
     * This private class does not return from its constructor
     * until 'img's width and height are available.
     */
    protected class ImageWaiter implements ImageObserver {

        private int mWidth;
        private int mHeight;
        private boolean badImage = false;

        ImageWaiter(Image img) {
            waitForDimensions(img);
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        private synchronized void waitForDimensions(Image img) {
            mHeight = img.getHeight(this);
            mWidth = img.getWidth(this);
            while (!badImage && (mWidth < 0 || mHeight < 0)) {
                try {
                    Thread.sleep(50);
                } catch(InterruptedException e) {
                    // do nothing.
                }
                mHeight = img.getHeight(this);
                mWidth = img.getWidth(this);
            }
            if (badImage) {
                mHeight = 0;
                mWidth = 0;
            }
        }

        public synchronized boolean imageUpdate(Image image, int flags,
                                                int x, int y, int w, int h) {

            boolean dontCallMeAgain = (flags & (HEIGHT | ABORT | ERROR)) != 0;
            badImage = (flags & (ABORT | ERROR)) != 0;

            return dontCallMeAgain;
        }

    }
}
