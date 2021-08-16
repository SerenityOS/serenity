/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImageOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * This {@code Graphics2D} class extends the
 * {@link Graphics} class to provide more sophisticated
 * control over geometry, coordinate transformations, color management,
 * and text layout.  This is the fundamental class for rendering
 * 2-dimensional shapes, text and images on the  Java(tm) platform.
 *
 * <h2>Coordinate Spaces</h2>
 * All coordinates passed to a {@code Graphics2D} object are specified
 * in a device-independent coordinate system called User Space, which is
 * used by applications.  The {@code Graphics2D} object contains
 * an {@link AffineTransform} object as part of its rendering state
 * that defines how to convert coordinates from user space to
 * device-dependent coordinates in Device Space.
 * <p>
 * Coordinates in device space usually refer to individual device pixels
 * and are aligned on the infinitely thin gaps between these pixels.
 * Some {@code Graphics2D} objects can be used to capture rendering
 * operations for storage into a graphics metafile for playback on a
 * concrete device of unknown physical resolution at a later time.  Since
 * the resolution might not be known when the rendering operations are
 * captured, the {@code Graphics2D Transform} is set up
 * to transform user coordinates to a virtual device space that
 * approximates the expected resolution of the target device. Further
 * transformations might need to be applied at playback time if the
 * estimate is incorrect.
 * <p>
 * Some of the operations performed by the rendering attribute objects
 * occur in the device space, but all {@code Graphics2D} methods take
 * user space coordinates.
 * <p>
 * Every {@code Graphics2D} object is associated with a target that
 * defines where rendering takes place. A
 * {@link GraphicsConfiguration} object defines the characteristics
 * of the rendering target, such as pixel format and resolution.
 * The same rendering target is used throughout the life of a
 * {@code Graphics2D} object.
 * <p>
 * When creating a {@code Graphics2D} object,  the
 * {@code GraphicsConfiguration}
 * specifies the <a id="deftransform">default transform</a> for
 * the target of the {@code Graphics2D} (a
 * {@link Component} or {@link Image}).  This default transform maps the
 * user space coordinate system to screen and printer device coordinates
 * such that the origin maps to the upper left hand corner of the
 * target region of the device with increasing X coordinates extending
 * to the right and increasing Y coordinates extending downward.
 * The scaling of the default transform is set to identity for those devices
 * that are close to 72 dpi, such as screen devices.
 * The scaling of the default transform is set to approximately 72 user
 * space coordinates per square inch for high resolution devices, such as
 * printers.  For image buffers, the default transform is the
 * {@code Identity} transform.
 *
 * <h2>Rendering Process</h2>
 * The Rendering Process can be broken down into four phases that are
 * controlled by the {@code Graphics2D} rendering attributes.
 * The renderer can optimize many of these steps, either by caching the
 * results for future calls, by collapsing multiple virtual steps into
 * a single operation, or by recognizing various attributes as common
 * simple cases that can be eliminated by modifying other parts of the
 * operation.
 * <p>
 * The steps in the rendering process are:
 * <ol>
 * <li>
 * Determine what to render.
 * <li>
 * Constrain the rendering operation to the current {@code Clip}.
 * The {@code Clip} is specified by a {@link Shape} in user
 * space and is controlled by the program using the various clip
 * manipulation methods of {@code Graphics} and
 * {@code Graphics2D}.  This <i>user clip</i>
 * is transformed into device space by the current
 * {@code Transform} and combined with the
 * <i>device clip</i>, which is defined by the visibility of windows and
 * device extents.  The combination of the user clip and device clip
 * defines the <i>composite clip</i>, which determines the final clipping
 * region.  The user clip is not modified by the rendering
 * system to reflect the resulting composite clip.
 * <li>
 * Determine what colors to render.
 * <li>
 * Apply the colors to the destination drawing surface using the current
 * {@link Composite} attribute in the {@code Graphics2D} context.
 * </ol>
 * <br>
 * The three types of rendering operations, along with details of each
 * of their particular rendering processes are:
 * <ol>
 * <li>
 * <b><a id="rendershape">{@code Shape} operations</a></b>
 * <ol>
 * <li>
 * If the operation is a {@code draw(Shape)} operation, then
 * the  {@link Stroke#createStrokedShape(Shape) createStrokedShape}
 * method on the current {@link Stroke} attribute in the
 * {@code Graphics2D} context is used to construct a new
 * {@code Shape} object that contains the outline of the specified
 * {@code Shape}.
 * <li>
 * The {@code Shape} is transformed from user space to device space
 * using the current {@code Transform}
 * in the {@code Graphics2D} context.
 * <li>
 * The outline of the {@code Shape} is extracted using the
 * {@link Shape#getPathIterator(AffineTransform) getPathIterator} method of
 * {@code Shape}, which returns a
 * {@link java.awt.geom.PathIterator PathIterator}
 * object that iterates along the boundary of the {@code Shape}.
 * <li>
 * If the {@code Graphics2D} object cannot handle the curved segments
 * that the {@code PathIterator} object returns then it can call the
 * alternate
 * {@link Shape#getPathIterator(AffineTransform, double) getPathIterator}
 * method of {@code Shape}, which flattens the {@code Shape}.
 * <li>
 * The current {@link Paint} in the {@code Graphics2D} context
 * is queried for a {@link PaintContext}, which specifies the
 * colors to render in device space.
 * </ol>
 * <li>
 * <b><a id=rendertext>Text operations</a></b>
 * <ol>
 * <li>
 * The following steps are used to determine the set of glyphs required
 * to render the indicated {@code String}:
 * <ol>
 * <li>
 * If the argument is a {@code String}, then the current
 * {@code Font} in the {@code Graphics2D} context is asked to
 * convert the Unicode characters in the {@code String} into a set of
 * glyphs for presentation with whatever basic layout and shaping
 * algorithms the font implements.
 * <li>
 * If the argument is an
 * {@link AttributedCharacterIterator},
 * the iterator is asked to convert itself to a
 * {@link java.awt.font.TextLayout TextLayout}
 * using its embedded font attributes. The {@code TextLayout}
 * implements more sophisticated glyph layout algorithms that
 * perform Unicode bi-directional layout adjustments automatically
 * for multiple fonts of differing writing directions.
  * <li>
 * If the argument is a
 * {@link GlyphVector}, then the
 * {@code GlyphVector} object already contains the appropriate
 * font-specific glyph codes with explicit coordinates for the position of
 * each glyph.
 * </ol>
 * <li>
 * The current {@code Font} is queried to obtain outlines for the
 * indicated glyphs.  These outlines are treated as shapes in user space
 * relative to the position of each glyph that was determined in step 1.
 * <li>
 * The character outlines are filled as indicated above
 * under <a href="#rendershape">{@code Shape} operations</a>.
 * <li>
 * The current {@code Paint} is queried for a
 * {@code PaintContext}, which specifies
 * the colors to render in device space.
 * </ol>
 * <li>
 * <b><a id= renderingimage>{@code Image} Operations</a></b>
 * <ol>
 * <li>
 * The region of interest is defined by the bounding box of the source
 * {@code Image}.
 * This bounding box is specified in Image Space, which is the
 * {@code Image} object's local coordinate system.
 * <li>
 * If an {@code AffineTransform} is passed to
 * {@link #drawImage(java.awt.Image, java.awt.geom.AffineTransform, java.awt.image.ImageObserver) drawImage(Image, AffineTransform, ImageObserver)},
 * the {@code AffineTransform} is used to transform the bounding
 * box from image space to user space. If no {@code AffineTransform}
 * is supplied, the bounding box is treated as if it is already in user space.
 * <li>
 * The bounding box of the source {@code Image} is transformed from user
 * space into device space using the current {@code Transform}.
 * Note that the result of transforming the bounding box does not
 * necessarily result in a rectangular region in device space.
 * <li>
 * The {@code Image} object determines what colors to render,
 * sampled according to the source to destination
 * coordinate mapping specified by the current {@code Transform} and the
 * optional image transform.
 * </ol>
 * </ol>
 *
 * <h2>Default Rendering Attributes</h2>
 * The default values for the {@code Graphics2D} rendering attributes are:
 * <dl>
 * <dt><i>{@code Paint}</i>
 * <dd>The color of the {@code Component}.
 * <dt><i>{@code Font}</i>
 * <dd>The {@code Font} of the {@code Component}.
 * <dt><i>{@code Stroke}</i>
 * <dd>A square pen with a linewidth of 1, no dashing, miter segment joins
 * and square end caps.
 * <dt><i>{@code Transform}</i>
 * <dd>The
 * {@link GraphicsConfiguration#getDefaultTransform() getDefaultTransform}
 * for the {@code GraphicsConfiguration} of the {@code Component}.
 * <dt><i>{@code Composite}</i>
 * <dd>The {@link AlphaComposite#SRC_OVER} rule.
 * <dt><i>{@code Clip}</i>
 * <dd>No rendering {@code Clip}, the output is clipped to the
 * {@code Component}.
 * </dl>
 *
 * <h2>Rendering Compatibility Issues</h2>
 * The JDK(tm) 1.1 rendering model is based on a pixelization model
 * that specifies that coordinates
 * are infinitely thin, lying between the pixels.  Drawing operations are
 * performed using a one-pixel wide pen that fills the
 * pixel below and to the right of the anchor point on the path.
 * The JDK 1.1 rendering model is consistent with the
 * capabilities of most of the existing class of platform
 * renderers that need  to resolve integer coordinates to a
 * discrete pen that must fall completely on a specified number of pixels.
 * <p>
 * The Java 2D(tm) (Java(tm) 2 platform) API supports antialiasing renderers.
 * A pen with a width of one pixel does not need to fall
 * completely on pixel N as opposed to pixel N+1.  The pen can fall
 * partially on both pixels. It is not necessary to choose a bias
 * direction for a wide pen since the blending that occurs along the
 * pen traversal edges makes the sub-pixel position of the pen
 * visible to the user.  On the other hand, when antialiasing is
 * turned off by setting the
 * {@link RenderingHints#KEY_ANTIALIASING KEY_ANTIALIASING} hint key
 * to the
 * {@link RenderingHints#VALUE_ANTIALIAS_OFF VALUE_ANTIALIAS_OFF}
 * hint value, the renderer might need
 * to apply a bias to determine which pixel to modify when the pen
 * is straddling a pixel boundary, such as when it is drawn
 * along an integer coordinate in device space.  While the capabilities
 * of an antialiasing renderer make it no longer necessary for the
 * rendering model to specify a bias for the pen, it is desirable for the
 * antialiasing and non-antialiasing renderers to perform similarly for
 * the common cases of drawing one-pixel wide horizontal and vertical
 * lines on the screen.  To ensure that turning on antialiasing by
 * setting the
 * {@link RenderingHints#KEY_ANTIALIASING KEY_ANTIALIASING} hint
 * key to
 * {@link RenderingHints#VALUE_ANTIALIAS_ON VALUE_ANTIALIAS_ON}
 * does not cause such lines to suddenly become twice as wide and half
 * as opaque, it is desirable to have the model specify a path for such
 * lines so that they completely cover a particular set of pixels to help
 * increase their crispness.
 * <p>
 * Java 2D API maintains compatibility with JDK 1.1 rendering
 * behavior, such that legacy operations and existing renderer
 * behavior is unchanged under Java 2D API.  Legacy
 * methods that map onto general {@code draw} and
 * {@code fill} methods are defined, which clearly indicates
 * how {@code Graphics2D} extends {@code Graphics} based
 * on settings of {@code Stroke} and {@code Transform}
 * attributes and rendering hints.  The definition
 * performs identically under default attribute settings.
 * For example, the default {@code Stroke} is a
 * {@code BasicStroke} with a width of 1 and no dashing and the
 * default Transform for screen drawing is an Identity transform.
 * <p>
 * The following two rules provide predictable rendering behavior whether
 * aliasing or antialiasing is being used.
 * <ul>
 * <li> Device coordinates are defined to be between device pixels which
 * avoids any inconsistent results between aliased and antialiased
 * rendering.  If coordinates were defined to be at a pixel's center, some
 * of the pixels covered by a shape, such as a rectangle, would only be
 * half covered.
 * With aliased rendering, the half covered pixels would either be
 * rendered inside the shape or outside the shape.  With anti-aliased
 * rendering, the pixels on the entire edge of the shape would be half
 * covered.  On the other hand, since coordinates are defined to be
 * between pixels, a shape like a rectangle would have no half covered
 * pixels, whether or not it is rendered using antialiasing.
 * <li> Lines and paths stroked using the {@code BasicStroke}
 * object may be "normalized" to provide consistent rendering of the
 * outlines when positioned at various points on the drawable and
 * whether drawn with aliased or antialiased rendering.  This
 * normalization process is controlled by the
 * {@link RenderingHints#KEY_STROKE_CONTROL KEY_STROKE_CONTROL} hint.
 * The exact normalization algorithm is not specified, but the goals
 * of this normalization are to ensure that lines are rendered with
 * consistent visual appearance regardless of how they fall on the
 * pixel grid and to promote more solid horizontal and vertical
 * lines in antialiased mode so that they resemble their non-antialiased
 * counterparts more closely.  A typical normalization step might
 * promote antialiased line endpoints to pixel centers to reduce the
 * amount of blending or adjust the subpixel positioning of
 * non-antialiased lines so that the floating point line widths
 * round to even or odd pixel counts with equal likelihood.  This
 * process can move endpoints by up to half a pixel (usually towards
 * positive infinity along both axes) to promote these consistent
 * results.
 * </ul>
 * <p>
 * The following definitions of general legacy methods
 * perform identically to previously specified behavior under default
 * attribute settings:
 * <ul>
 * <li>
 * For {@code fill} operations, including {@code fillRect},
 * {@code fillRoundRect}, {@code fillOval},
 * {@code fillArc}, {@code fillPolygon}, and
 * {@code clearRect}, {@link #fill(Shape) fill} can now be called
 * with the desired {@code Shape}.  For example, when filling a
 * rectangle:
 * <pre>
 * fill(new Rectangle(x, y, w, h));
 * </pre>
 * is called.
 *
 * <li>
 * Similarly, for draw operations, including {@code drawLine},
 * {@code drawRect}, {@code drawRoundRect},
 * {@code drawOval}, {@code drawArc}, {@code drawPolyline},
 * and {@code drawPolygon}, {@link #draw(Shape) draw} can now be
 * called with the desired {@code Shape}.  For example, when drawing a
 * rectangle:
 * <pre>
 * draw(new Rectangle(x, y, w, h));
 * </pre>
 * is called.
 *
 * <li>
 * The {@code draw3DRect} and {@code fill3DRect} methods were
 * implemented in terms of the {@code drawLine} and
 * {@code fillRect} methods in the {@code Graphics} class which
 * would predicate their behavior upon the current {@code Stroke}
 * and {@code Paint} objects in a {@code Graphics2D} context.
 * This class overrides those implementations with versions that use
 * the current {@code Color} exclusively, overriding the current
 * {@code Paint} and which uses {@code fillRect} to describe
 * the exact same behavior as the preexisting methods regardless of the
 * setting of the current {@code Stroke}.
 * </ul>
 * The {@code Graphics} class defines only the {@code setColor}
 * method to control the color to be painted.  Since the Java 2D API extends
 * the {@code Color} object to implement the new {@code Paint}
 * interface, the existing
 * {@code setColor} method is now a convenience method for setting the
 * current {@code Paint} attribute to a {@code Color} object.
 * {@code setColor(c)} is equivalent to {@code setPaint(c)}.
 * <p>
 * The {@code Graphics} class defines two methods for controlling
 * how colors are applied to the destination.
 * <ol>
 * <li>
 * The {@code setPaintMode} method is implemented as a convenience
 * method to set the default {@code Composite}, equivalent to
 * {@code setComposite(new AlphaComposite.SrcOver)}.
 * <li>
 * The {@code setXORMode(Color xorcolor)} method is implemented
 * as a convenience method to set a special {@code Composite} object that
 * ignores the {@code Alpha} components of source colors and sets the
 * destination color to the value:
 * <pre>
 * dstpixel = (PixelOf(srccolor) ^ PixelOf(xorcolor) ^ dstpixel);
 * </pre>
 * </ol>
 *
 * @author Jim Graham
 * @see java.awt.RenderingHints
 */
public abstract class Graphics2D extends Graphics {

    /**
     * Constructs a new {@code Graphics2D} object.  Since
     * {@code Graphics2D} is an abstract class, and since it must be
     * customized by subclasses for different output devices,
     * {@code Graphics2D} objects cannot be created directly.
     * Instead, {@code Graphics2D} objects must be obtained from another
     * {@code Graphics2D} object, created by a
     * {@code Component}, or obtained from images such as
     * {@link BufferedImage} objects.
     * @see java.awt.Component#getGraphics
     * @see java.awt.Graphics#create
     */
    protected Graphics2D() {
    }

    /**
     * Draws a 3-D highlighted outline of the specified rectangle.
     * The edges of the rectangle are highlighted so that they
     * appear to be beveled and lit from the upper left corner.
     * <p>
     * The colors used for the highlighting effect are determined
     * based on the current color.
     * The resulting rectangle covers an area that is
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * by <code>height&nbsp;+&nbsp;1</code> pixels tall.  This method
     * uses the current {@code Color} exclusively and ignores
     * the current {@code Paint}.
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     * @param raised a boolean that determines whether the rectangle
     *                      appears to be raised above the surface
     *                      or sunk into the surface.
     * @see         java.awt.Graphics#fill3DRect
     */
    public void draw3DRect(int x, int y, int width, int height,
                           boolean raised) {
        Paint p = getPaint();
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();

        setColor(raised ? brighter : darker);
        //drawLine(x, y, x, y + height);
        fillRect(x, y, 1, height + 1);
        //drawLine(x + 1, y, x + width - 1, y);
        fillRect(x + 1, y, width - 1, 1);
        setColor(raised ? darker : brighter);
        //drawLine(x + 1, y + height, x + width, y + height);
        fillRect(x + 1, y + height, width, 1);
        //drawLine(x + width, y, x + width, y + height - 1);
        fillRect(x + width, y, 1, height);
        setPaint(p);
    }

    /**
     * Paints a 3-D highlighted rectangle filled with the current color.
     * The edges of the rectangle are highlighted so that it appears
     * as if the edges were beveled and lit from the upper left corner.
     * The colors used for the highlighting effect and for filling are
     * determined from the current {@code Color}.  This method uses
     * the current {@code Color} exclusively and ignores the current
     * {@code Paint}.
     * @param x the x coordinate of the rectangle to be filled.
     * @param y the y coordinate of the rectangle to be filled.
     * @param       width the width of the rectangle to be filled.
     * @param       height the height of the rectangle to be filled.
     * @param       raised a boolean value that determines whether the
     *                      rectangle appears to be raised above the surface
     *                      or etched into the surface.
     * @see         java.awt.Graphics#draw3DRect
     */
    public void fill3DRect(int x, int y, int width, int height,
                           boolean raised) {
        Paint p = getPaint();
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();

        if (!raised) {
            setColor(darker);
        } else if (p != c) {
            setColor(c);
        }
        fillRect(x+1, y+1, width-2, height-2);
        setColor(raised ? brighter : darker);
        //drawLine(x, y, x, y + height - 1);
        fillRect(x, y, 1, height);
        //drawLine(x + 1, y, x + width - 2, y);
        fillRect(x + 1, y, width - 2, 1);
        setColor(raised ? darker : brighter);
        //drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        fillRect(x + 1, y + height - 1, width - 1, 1);
        //drawLine(x + width - 1, y, x + width - 1, y + height - 2);
        fillRect(x + width - 1, y, 1, height - 1);
        setPaint(p);
    }

    /**
     * Strokes the outline of a {@code Shape} using the settings of the
     * current {@code Graphics2D} context.  The rendering attributes
     * applied include the {@code Clip}, {@code Transform},
     * {@code Paint}, {@code Composite} and
     * {@code Stroke} attributes.
     * @param s the {@code Shape} to be rendered
     * @see #setStroke
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     * @see #setComposite
     */
    public abstract void draw(Shape s);

    /**
     * Renders an image, applying a transform from image space into user space
     * before drawing.
     * The transformation from user space into device space is done with
     * the current {@code Transform} in the {@code Graphics2D}.
     * The specified transformation is applied to the image before the
     * transform attribute in the {@code Graphics2D} context is applied.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, and {@code Composite} attributes.
     * Note that no rendering is done if the specified transform is
     * noninvertible.
     * @param img the specified image to be rendered.
     *            This method does nothing if {@code img} is null.
     * @param xform the transformation from image space into user space
     * @param obs the {@link ImageObserver}
     * to be notified as more of the {@code Image}
     * is converted
     * @return {@code true} if the {@code Image} is
     * fully loaded and completely rendered, or if it's null;
     * {@code false} if the {@code Image} is still being loaded.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public abstract boolean drawImage(Image img,
                                      AffineTransform xform,
                                      ImageObserver obs);

    /**
     * Renders a {@code BufferedImage} that is
     * filtered with a
     * {@link BufferedImageOp}.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}
     * and {@code Composite} attributes.  This is equivalent to:
     * <pre>
     * img1 = op.filter(img, null);
     * drawImage(img1, new AffineTransform(1f,0f,0f,1f,x,y), null);
     * </pre>
     * @param op the filter to be applied to the image before rendering
     * @param img the specified {@code BufferedImage} to be rendered.
     *            This method does nothing if {@code img} is null.
     * @param x the x coordinate of the location in user space where
     * the upper left corner of the image is rendered
     * @param y the y coordinate of the location in user space where
     * the upper left corner of the image is rendered
     *
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public abstract void drawImage(BufferedImage img,
                                   BufferedImageOp op,
                                   int x,
                                   int y);

    /**
     * Renders a {@link RenderedImage},
     * applying a transform from image
     * space into user space before drawing.
     * The transformation from user space into device space is done with
     * the current {@code Transform} in the {@code Graphics2D}.
     * The specified transformation is applied to the image before the
     * transform attribute in the {@code Graphics2D} context is applied.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, and {@code Composite} attributes. Note
     * that no rendering is done if the specified transform is
     * noninvertible.
     * @param img the image to be rendered. This method does
     *            nothing if {@code img} is null.
     * @param xform the transformation from image space into user space
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public abstract void drawRenderedImage(RenderedImage img,
                                           AffineTransform xform);

    /**
     * Renders a
     * {@link RenderableImage},
     * applying a transform from image space into user space before drawing.
     * The transformation from user space into device space is done with
     * the current {@code Transform} in the {@code Graphics2D}.
     * The specified transformation is applied to the image before the
     * transform attribute in the {@code Graphics2D} context is applied.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, and {@code Composite} attributes. Note
     * that no rendering is done if the specified transform is
     * noninvertible.
     *<p>
     * Rendering hints set on the {@code Graphics2D} object might
     * be used in rendering the {@code RenderableImage}.
     * If explicit control is required over specific hints recognized by a
     * specific {@code RenderableImage}, or if knowledge of which hints
     * are used is required, then a {@code RenderedImage} should be
     * obtained directly from the {@code RenderableImage}
     * and rendered using
     *{@link #drawRenderedImage(RenderedImage, AffineTransform) drawRenderedImage}.
     * @param img the image to be rendered. This method does
     *            nothing if {@code img} is null.
     * @param xform the transformation from image space into user space
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     * @see #drawRenderedImage
     */
    public abstract void drawRenderableImage(RenderableImage img,
                                             AffineTransform xform);

    /**
     * Renders the text of the specified {@code String}, using the
     * current text attribute state in the {@code Graphics2D} context.
     * The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in
     * the User Space.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, {@code Paint}, {@code Font} and
     * {@code Composite} attributes.  For characters in script
     * systems such as Hebrew and Arabic, the glyphs can be rendered from
     * right to left, in which case the coordinate supplied is the
     * location of the leftmost character on the baseline.
     * @param str the string to be rendered
     * @param x the x coordinate of the location where the
     * {@code String} should be rendered
     * @param y the y coordinate of the location where the
     * {@code String} should be rendered
     * @throws NullPointerException if {@code str} is
     *         {@code null}
     * @see         java.awt.Graphics#drawBytes
     * @see         java.awt.Graphics#drawChars
     * @since       1.0
     */
    public abstract void drawString(String str, int x, int y);

    /**
     * Renders the text specified by the specified {@code String},
     * using the current text attribute state in the {@code Graphics2D} context.
     * The baseline of the first character is at position
     * (<i>x</i>,&nbsp;<i>y</i>) in the User Space.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, {@code Paint}, {@code Font} and
     * {@code Composite} attributes. For characters in script systems
     * such as Hebrew and Arabic, the glyphs can be rendered from right to
     * left, in which case the coordinate supplied is the location of the
     * leftmost character on the baseline.
     * @param str the {@code String} to be rendered
     * @param x the x coordinate of the location where the
     * {@code String} should be rendered
     * @param y the y coordinate of the location where the
     * {@code String} should be rendered
     * @throws NullPointerException if {@code str} is
     *         {@code null}
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see java.awt.Graphics#setFont
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public abstract void drawString(String str, float x, float y);

    /**
     * Renders the text of the specified iterator applying its attributes
     * in accordance with the specification of the {@link TextAttribute} class.
     * <p>
     * The baseline of the first character is at position
     * (<i>x</i>,&nbsp;<i>y</i>) in User Space.
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left, in which case the
     * coordinate supplied is the location of the leftmost character
     * on the baseline.
     * @param iterator the iterator whose text is to be rendered
     * @param x the x coordinate where the iterator's text is to be
     * rendered
     * @param y the y coordinate where the iterator's text is to be
     * rendered
     * @throws NullPointerException if {@code iterator} is
     *         {@code null}
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public abstract void drawString(AttributedCharacterIterator iterator,
                                    int x, int y);

    /**
     * Renders the text of the specified iterator applying its attributes
     * in accordance with the specification of the {@link TextAttribute} class.
     * <p>
     * The baseline of the first character is at position
     * (<i>x</i>,&nbsp;<i>y</i>) in User Space.
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left, in which case the
     * coordinate supplied is the location of the leftmost character
     * on the baseline.
     * @param iterator the iterator whose text is to be rendered
     * @param x the x coordinate where the iterator's text is to be
     * rendered
     * @param y the y coordinate where the iterator's text is to be
     * rendered
     * @throws NullPointerException if {@code iterator} is
     *         {@code null}
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public abstract void drawString(AttributedCharacterIterator iterator,
                                    float x, float y);

    /**
     * Renders the text of the specified
     * {@link GlyphVector} using
     * the {@code Graphics2D} context's rendering attributes.
     * The rendering attributes applied include the {@code Clip},
     * {@code Transform}, {@code Paint}, and
     * {@code Composite} attributes.  The {@code GlyphVector}
     * specifies individual glyphs from a {@link Font}.
     * The {@code GlyphVector} can also contain the glyph positions.
     * This is the fastest way to render a set of characters to the
     * screen.
     * @param g the {@code GlyphVector} to be rendered
     * @param x the x position in User Space where the glyphs should
     * be rendered
     * @param y the y position in User Space where the glyphs should
     * be rendered
     * @throws NullPointerException if {@code g} is {@code null}.
     *
     * @see java.awt.Font#createGlyphVector
     * @see java.awt.font.GlyphVector
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public abstract void drawGlyphVector(GlyphVector g, float x, float y);

    /**
     * Fills the interior of a {@code Shape} using the settings of the
     * {@code Graphics2D} context. The rendering attributes applied
     * include the {@code Clip}, {@code Transform},
     * {@code Paint}, and {@code Composite}.
     * @param s the {@code Shape} to be filled
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public abstract void fill(Shape s);

    /**
     * Checks whether or not the specified {@code Shape} intersects
     * the specified {@link Rectangle}, which is in device
     * space. If {@code onStroke} is false, this method checks
     * whether or not the interior of the specified {@code Shape}
     * intersects the specified {@code Rectangle}.  If
     * {@code onStroke} is {@code true}, this method checks
     * whether or not the {@code Stroke} of the specified
     * {@code Shape} outline intersects the specified
     * {@code Rectangle}.
     * The rendering attributes taken into account include the
     * {@code Clip}, {@code Transform}, and {@code Stroke}
     * attributes.
     * @param rect the area in device space to check for a hit
     * @param s the {@code Shape} to check for a hit
     * @param onStroke flag used to choose between testing the
     * stroked or the filled shape.  If the flag is {@code true}, the
     * {@code Stroke} outline is tested.  If the flag is
     * {@code false}, the filled {@code Shape} is tested.
     * @return {@code true} if there is a hit; {@code false}
     * otherwise.
     * @see #setStroke
     * @see #fill
     * @see #draw
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     */
    public abstract boolean hit(Rectangle rect,
                                Shape s,
                                boolean onStroke);

    /**
     * Returns the device configuration associated with this
     * {@code Graphics2D}.
     * @return the device configuration of this {@code Graphics2D}.
     */
    public abstract GraphicsConfiguration getDeviceConfiguration();

    /**
     * Sets the {@code Composite} for the {@code Graphics2D} context.
     * The {@code Composite} is used in all drawing methods such as
     * {@code drawImage}, {@code drawString}, {@code draw},
     * and {@code fill}.  It specifies how new pixels are to be combined
     * with the existing pixels on the graphics device during the rendering
     * process.
     * <p>If this {@code Graphics2D} context is drawing to a
     * {@code Component} on the display screen and the
     * {@code Composite} is a custom object rather than an
     * instance of the {@code AlphaComposite} class, and if
     * there is a security manager, its {@code checkPermission}
     * method is called with an {@code AWTPermission("readDisplayPixels")}
     * permission.
     * @throws SecurityException
     *         if a custom {@code Composite} object is being
     *         used to render to the screen and a security manager
     *         is set and its {@code checkPermission} method
     *         does not allow the operation.
     * @param comp the {@code Composite} object to be used for rendering
     * @see java.awt.Graphics#setXORMode
     * @see java.awt.Graphics#setPaintMode
     * @see #getComposite
     * @see AlphaComposite
     * @see SecurityManager#checkPermission
     * @see java.awt.AWTPermission
     */
    public abstract void setComposite(Composite comp);

    /**
     * Sets the {@code Paint} attribute for the
     * {@code Graphics2D} context.  Calling this method
     * with a {@code null Paint} object does
     * not have any effect on the current {@code Paint} attribute
     * of this {@code Graphics2D}.
     * @param paint the {@code Paint} object to be used to generate
     * color during the rendering process, or {@code null}
     * @see java.awt.Graphics#setColor
     * @see #getPaint
     * @see GradientPaint
     * @see TexturePaint
     */
    public abstract void setPaint( Paint paint );

    /**
     * Sets the {@code Stroke} for the {@code Graphics2D} context.
     * @param s the {@code Stroke} object to be used to stroke a
     * {@code Shape} during the rendering process
     * @see BasicStroke
     * @see #getStroke
     */
    public abstract void setStroke(Stroke s);

    /**
     * Sets the value of a single preference for the rendering algorithms.
     * Hint categories include controls for rendering quality and overall
     * time/quality trade-off in the rendering process.  Refer to the
     * {@code RenderingHints} class for definitions of some common
     * keys and values.
     * @param hintKey the key of the hint to be set.
     * @param hintValue the value indicating preferences for the specified
     * hint category.
     * @see #getRenderingHint(RenderingHints.Key)
     * @see RenderingHints
     */
    public abstract void setRenderingHint(Key hintKey, Object hintValue);

    /**
     * Returns the value of a single preference for the rendering algorithms.
     * Hint categories include controls for rendering quality and overall
     * time/quality trade-off in the rendering process.  Refer to the
     * {@code RenderingHints} class for definitions of some common
     * keys and values.
     * @param hintKey the key corresponding to the hint to get.
     * @return an object representing the value for the specified hint key.
     * Some of the keys and their associated values are defined in the
     * {@code RenderingHints} class.
     * @see RenderingHints
     * @see #setRenderingHint(RenderingHints.Key, Object)
     */
    public abstract Object getRenderingHint(Key hintKey);

    /**
     * Replaces the values of all preferences for the rendering
     * algorithms with the specified {@code hints}.
     * The existing values for all rendering hints are discarded and
     * the new set of known hints and values are initialized from the
     * specified {@link Map} object.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * Refer to the {@code RenderingHints} class for definitions of
     * some common keys and values.
     * @param hints the rendering hints to be set
     * @see #getRenderingHints
     * @see RenderingHints
     */
    public abstract void setRenderingHints(Map<?,?> hints);

    /**
     * Sets the values of an arbitrary number of preferences for the
     * rendering algorithms.
     * Only values for the rendering hints that are present in the
     * specified {@code Map} object are modified.
     * All other preferences not present in the specified
     * object are left unmodified.
     * Hint categories include controls for rendering quality and
     * overall time/quality trade-off in the rendering process.
     * Refer to the {@code RenderingHints} class for definitions of
     * some common keys and values.
     * @param hints the rendering hints to be set
     * @see RenderingHints
     */
    public abstract void addRenderingHints(Map<?,?> hints);

    /**
     * Gets the preferences for the rendering algorithms.  Hint categories
     * include controls for rendering quality and overall time/quality
     * trade-off in the rendering process.
     * Returns all of the hint key/value pairs that were ever specified in
     * one operation.  Refer to the
     * {@code RenderingHints} class for definitions of some common
     * keys and values.
     * @return a reference to an instance of {@code RenderingHints}
     * that contains the current preferences.
     * @see RenderingHints
     * @see #setRenderingHints(Map)
     */
    public abstract RenderingHints getRenderingHints();

    /**
     * Translates the origin of the {@code Graphics2D} context to the
     * point (<i>x</i>,&nbsp;<i>y</i>) in the current coordinate system.
     * Modifies the {@code Graphics2D} context so that its new origin
     * corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in the
     * {@code Graphics2D} context's former coordinate system.  All
     * coordinates used in subsequent rendering operations on this graphics
     * context are relative to this new origin.
     * @param  x the specified x coordinate
     * @param  y the specified y coordinate
     * @since   1.0
     */
    public abstract void translate(int x, int y);

    /**
     * Concatenates the current
     * {@code Graphics2D Transform}
     * with a translation transform.
     * Subsequent rendering is translated by the specified
     * distance relative to the previous position.
     * This is equivalent to calling transform(T), where T is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   1    0    tx  ]
     *          [   0    1    ty  ]
     *          [   0    0    1   ]
     * </pre>
     * @param tx the distance to translate along the x-axis
     * @param ty the distance to translate along the y-axis
     */
    public abstract void translate(double tx, double ty);

    /**
     * Concatenates the current {@code Graphics2D}
     * {@code Transform} with a rotation transform.
     * Subsequent rendering is rotated by the specified radians relative
     * to the previous origin.
     * This is equivalent to calling {@code transform(R)}, where R is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   cos(theta)    -sin(theta)    0   ]
     *          [   sin(theta)     cos(theta)    0   ]
     *          [       0              0         1   ]
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * x axis toward the positive y axis.
     * @param theta the angle of rotation in radians
     */
    public abstract void rotate(double theta);

    /**
     * Concatenates the current {@code Graphics2D}
     * {@code Transform} with a translated rotation
     * transform.  Subsequent rendering is transformed by a transform
     * which is constructed by translating to the specified location,
     * rotating by the specified radians, and translating back by the same
     * amount as the original translation.  This is equivalent to the
     * following sequence of calls:
     * <pre>
     *          translate(x, y);
     *          rotate(theta);
     *          translate(-x, -y);
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * x axis toward the positive y axis.
     * @param theta the angle of rotation in radians
     * @param x the x coordinate of the origin of the rotation
     * @param y the y coordinate of the origin of the rotation
     */
    public abstract void rotate(double theta, double x, double y);

    /**
     * Concatenates the current {@code Graphics2D}
     * {@code Transform} with a scaling transformation
     * Subsequent rendering is resized according to the specified scaling
     * factors relative to the previous scaling.
     * This is equivalent to calling {@code transform(S)}, where S is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   sx   0    0   ]
     *          [   0    sy   0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param sx the amount by which X coordinates in subsequent
     * rendering operations are multiplied relative to previous
     * rendering operations.
     * @param sy the amount by which Y coordinates in subsequent
     * rendering operations are multiplied relative to previous
     * rendering operations.
     */
    public abstract void scale(double sx, double sy);

    /**
     * Concatenates the current {@code Graphics2D}
     * {@code Transform} with a shearing transform.
     * Subsequent renderings are sheared by the specified
     * multiplier relative to the previous position.
     * This is equivalent to calling {@code transform(SH)}, where SH
     * is an {@code AffineTransform} represented by the following
     * matrix:
     * <pre>
     *          [   1   shx   0   ]
     *          [  shy   1    0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in
     * the positive X axis direction as a function of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in
     * the positive Y axis direction as a function of their X coordinate
     */
    public abstract void shear(double shx, double shy);

    /**
     * Composes an {@code AffineTransform} object with the
     * {@code Transform} in this {@code Graphics2D} according
     * to the rule last-specified-first-applied.  If the current
     * {@code Transform} is Cx, the result of composition
     * with Tx is a new {@code Transform} Cx'.  Cx' becomes the
     * current {@code Transform} for this {@code Graphics2D}.
     * Transforming a point p by the updated {@code Transform} Cx' is
     * equivalent to first transforming p by Tx and then transforming
     * the result by the original {@code Transform} Cx.  In other
     * words, Cx'(p) = Cx(Tx(p)).  A copy of the Tx is made, if necessary,
     * so further modifications to Tx do not affect rendering.
     * @param Tx the {@code AffineTransform} object to be composed with
     * the current {@code Transform}
     * @see #setTransform
     * @see AffineTransform
     */
    public abstract void transform(AffineTransform Tx);

    /**
     * Overwrites the Transform in the {@code Graphics2D} context.
     * WARNING: This method should <b>never</b> be used to apply a new
     * coordinate transform on top of an existing transform because the
     * {@code Graphics2D} might already have a transform that is
     * needed for other purposes, such as rendering Swing
     * components or applying a scaling transformation to adjust for the
     * resolution of a printer.
     * <p>To add a coordinate transform, use the
     * {@code transform}, {@code rotate}, {@code scale},
     * or {@code shear} methods.  The {@code setTransform}
     * method is intended only for restoring the original
     * {@code Graphics2D} transform after rendering, as shown in this
     * example:
     * <pre>
     * // Get the current transform
     * AffineTransform saveAT = g2.getTransform();
     * // Perform transformation
     * g2d.transform(...);
     * // Render
     * g2d.draw(...);
     * // Restore original transform
     * g2d.setTransform(saveAT);
     * </pre>
     *
     * @param Tx the {@code AffineTransform} that was retrieved
     *           from the {@code getTransform} method
     * @see #transform
     * @see #getTransform
     * @see AffineTransform
     */
    public abstract void setTransform(AffineTransform Tx);

    /**
     * Returns a copy of the current {@code Transform} in the
     * {@code Graphics2D} context.
     * @return the current {@code AffineTransform} in the
     *             {@code Graphics2D} context.
     * @see #transform
     * @see #setTransform
     */
    public abstract AffineTransform getTransform();

    /**
     * Returns the current {@code Paint} of the
     * {@code Graphics2D} context.
     * @return the current {@code Graphics2D Paint},
     * which defines a color or pattern.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     */
    public abstract Paint getPaint();

    /**
     * Returns the current {@code Composite} in the
     * {@code Graphics2D} context.
     * @return the current {@code Graphics2D Composite},
     *              which defines a compositing style.
     * @see #setComposite
     */
    public abstract Composite getComposite();

    /**
     * Sets the background color for the {@code Graphics2D} context.
     * The background color is used for clearing a region.
     * When a {@code Graphics2D} is constructed for a
     * {@code Component}, the background color is
     * inherited from the {@code Component}. Setting the background color
     * in the {@code Graphics2D} context only affects the subsequent
     * {@code clearRect} calls and not the background color of the
     * {@code Component}.  To change the background
     * of the {@code Component}, use appropriate methods of
     * the {@code Component}.
     * @param color the background color that is used in
     * subsequent calls to {@code clearRect}
     * @see #getBackground
     * @see java.awt.Graphics#clearRect
     */
    public abstract void setBackground(Color color);

    /**
     * Returns the background color used for clearing a region.
     * @return the current {@code Graphics2D Color},
     * which defines the background color.
     * @see #setBackground
     */
    public abstract Color getBackground();

    /**
     * Returns the current {@code Stroke} in the
     * {@code Graphics2D} context.
     * @return the current {@code Graphics2D Stroke},
     *                 which defines the line style.
     * @see #setStroke
     */
    public abstract Stroke getStroke();

    /**
     * Intersects the current {@code Clip} with the interior of the
     * specified {@code Shape} and sets the {@code Clip} to the
     * resulting intersection.  The specified {@code Shape} is
     * transformed with the current {@code Graphics2D}
     * {@code Transform} before being intersected with the current
     * {@code Clip}.  This method is used to make the current
     * {@code Clip} smaller.
     * To make the {@code Clip} larger, use {@code setClip}.
     * <p>The <i>user clip</i> modified by this method is independent of the
     * clipping associated with device bounds and visibility.  If no clip has
     * previously been set, or if the clip has been cleared using
     * {@link Graphics#setClip(Shape) setClip} with a {@code null}
     * argument, the specified {@code Shape} becomes the new
     * user clip.
     * <p>Since this method intersects the specified shape
     * with the current clip, it will throw {@code NullPointerException}
     * for a {@code null} shape unless the user clip is also {@code null}.
     * So calling this method with a {@code null} argument is not recommended.
     * @param s the {@code Shape} to be intersected with the current
     *          {@code Clip}. This method updates the current {@code Clip}.
     * @throws NullPointerException if {@code s} is {@code null}
     *         and a user clip is currently set.
     */
     public abstract void clip(Shape s);

     /**
     * Get the rendering context of the {@code Font} within this
     * {@code Graphics2D} context.
     * The {@link FontRenderContext}
     * encapsulates application hints such as anti-aliasing and
     * fractional metrics, as well as target device specific information
     * such as dots-per-inch.  This information should be provided by the
     * application when using objects that perform typographical
     * formatting, such as {@code Font} and
     * {@code TextLayout}.  This information should also be provided
     * by applications that perform their own layout and need accurate
     * measurements of various characteristics of glyphs such as advance
     * and line height when various rendering hints have been applied to
     * the text rendering.
     *
     * @return a reference to an instance of FontRenderContext.
     * @see java.awt.font.FontRenderContext
     * @see java.awt.Font#createGlyphVector
     * @see java.awt.font.TextLayout
     * @since     1.2
     */

    public abstract FontRenderContext getFontRenderContext();

}
