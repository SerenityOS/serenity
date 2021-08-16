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

/* ********************************************************************
 **********************************************************************
 **********************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997                      ***
 *** As  an unpublished  work pursuant to Title 17 of the United    ***
 *** States Code.  All rights reserved.                             ***
 **********************************************************************
 **********************************************************************
 **********************************************************************/

package java.awt.image.renderable;
import java.util.Vector;
import java.awt.RenderingHints;
import java.awt.image.*;

/**
 * A RenderableImage is a common interface for rendering-independent
 * images (a notion which subsumes resolution independence).  That is,
 * images which are described and have operations applied to them
 * independent of any specific rendering of the image.  For example, a
 * RenderableImage can be rotated and cropped in
 * resolution-independent terms.  Then, it can be rendered for various
 * specific contexts, such as a draft preview, a high-quality screen
 * display, or a printer, each in an optimal fashion.
 *
 * <p> A RenderedImage is returned from a RenderableImage via the
 * createRendering() method, which takes a RenderContext.  The
 * RenderContext specifies how the RenderedImage should be
 * constructed.  Note that it is not possible to extract pixels
 * directly from a RenderableImage.
 *
 * <p> The createDefaultRendering() and createScaledRendering() methods are
 * convenience methods that construct an appropriate RenderContext
 * internally.  All of the rendering methods may return a reference to a
 * previously produced rendering.
 */
public interface RenderableImage {

    /**
     * String constant that can be used to identify a property on
     * a RenderedImage obtained via the createRendering or
     * createScaledRendering methods.  If such a property exists,
     * the value of the property will be a RenderingHints object
     * specifying which hints were observed in creating the rendering.
     */
     static final String HINTS_OBSERVED = "HINTS_OBSERVED";

    /**
     * Returns a vector of RenderableImages that are the sources of
     * image data for this RenderableImage. Note that this method may
     * return an empty vector, to indicate that the image has no sources,
     * or null, to indicate that no information is available.
     *
     * @return a (possibly empty) Vector of RenderableImages, or null.
     */
    Vector<RenderableImage> getSources();

    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     */
    Object getProperty(String name);

    /**
     * Returns a list of names recognized by getProperty.
     * @return a list of property names.
     */
    String[] getPropertyNames();

    /**
     * Returns true if successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  It is always safe to return true.
     * @return {@code true} if successive renderings with the
     *         same arguments might produce different results;
     *         {@code false} otherwise.
     */
    boolean isDynamic();

    /**
     * Gets the width in user coordinate space.  By convention, the
     * usual width of a RenderableImage is equal to the image's aspect
     * ratio (width divided by height).
     *
     * @return the width of the image in user coordinates.
     */
    float getWidth();

    /**
     * Gets the height in user coordinate space.  By convention, the
     * usual height of a RenderedImage is equal to 1.0F.
     *
     * @return the height of the image in user coordinates.
     */
    float getHeight();

    /**
     * Gets the minimum X coordinate of the rendering-independent image data.
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    float getMinX();

    /**
     * Gets the minimum Y coordinate of the rendering-independent image data.
     * @return the minimum Y coordinate of the rendering-independent image
     * data.
     */
    float getMinY();

    /**
     * Creates a RenderedImage instance of this image with width w, and
     * height h in pixels.  The RenderContext is built automatically
     * with an appropriate usr2dev transform and an area of interest
     * of the full image.  All the rendering hints come from hints
     * passed in.
     *
     * <p> If w == 0, it will be taken to equal
     * Math.round(h*(getWidth()/getHeight())).
     * Similarly, if h == 0, it will be taken to equal
     * Math.round(w*(getHeight()/getWidth())).  One of
     * w or h must be non-zero or else an IllegalArgumentException
     * will be thrown.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * were used to create the image.  In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param w the width of rendered image in pixels, or 0.
     * @param h the height of rendered image in pixels, or 0.
     * @param hints a RenderingHints object containing hints.
     * @return a RenderedImage containing the rendered data.
     */
    RenderedImage createScaledRendering(int w, int h, RenderingHints hints);

    /**
     * Returns a RenderedImage instance of this image with a default
     * width and height in pixels.  The RenderContext is built
     * automatically with an appropriate usr2dev transform and an area
     * of interest of the full image.  The rendering hints are
     * empty.  createDefaultRendering may make use of a stored
     * rendering for speed.
     *
     * @return a RenderedImage containing the rendered data.
     */
    RenderedImage createDefaultRendering();

    /**
     * Creates a RenderedImage that represented a rendering of this image
     * using a given RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * (from the RenderContext) were used to create the image.
     * In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    RenderedImage createRendering(RenderContext renderContext);
}
