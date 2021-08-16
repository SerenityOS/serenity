/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.RenderingHints;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class handles the renderable aspects of an operation with help
 * from its associated instance of a ContextualRenderedImageFactory.
 */
public class RenderableImageOp implements RenderableImage {

    /** A ParameterBlock containing source and parameters. */
    ParameterBlock paramBlock;

    /** The associated ContextualRenderedImageFactory. */
    ContextualRenderedImageFactory myCRIF;

    /** The bounding box of the results of this RenderableImageOp. */
    Rectangle2D boundingBox;


    /**
     * Constructs a RenderedImageOp given a
     * ContextualRenderedImageFactory object, and
     * a ParameterBlock containing RenderableImage sources and other
     * parameters.  Any RenderedImage sources referenced by the
     * ParameterBlock will be ignored.
     *
     * @param CRIF a ContextualRenderedImageFactory object
     * @param paramBlock a ParameterBlock containing this operation's source
     *        images and other parameters necessary for the operation
     *        to run.
     */
    public RenderableImageOp(ContextualRenderedImageFactory CRIF,
                             ParameterBlock paramBlock) {
        this.myCRIF = CRIF;
        this.paramBlock = (ParameterBlock) paramBlock.clone();
    }

    /**
     * Returns a vector of RenderableImages that are the sources of
     * image data for this RenderableImage. Note that this method may
     * return an empty vector, to indicate that the image has no sources,
     * or null, to indicate that no information is available.
     *
     * @return a (possibly empty) Vector of RenderableImages, or null.
     */
    public Vector<RenderableImage> getSources() {
        return getRenderableSources();
    }

    private Vector<RenderableImage> getRenderableSources() {
        Vector<RenderableImage> sources = null;

        if (paramBlock.getNumSources() > 0) {
            sources = new Vector<>();
            int i = 0;
            while (i < paramBlock.getNumSources()) {
                Object o = paramBlock.getSource(i);
                if (o instanceof RenderableImage) {
                    sources.add((RenderableImage)o);
                    i++;
                } else {
                    break;
                }
            }
        }
        return sources;
    }

    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     */
    public Object getProperty(String name) {
        return myCRIF.getProperty(paramBlock, name);
    }

    /**
     * Return a list of names recognized by getProperty.
     * @return a list of property names.
     */
    public String[] getPropertyNames() {
        return myCRIF.getPropertyNames();
    }

    /**
     * Returns true if successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  The CRIF's isDynamic method will be called.
     * @return {@code true} if successive renderings with the
     *         same arguments might produce different results;
     *         {@code false} otherwise.
     */
    public boolean isDynamic() {
        return myCRIF.isDynamic();
    }

    /**
     * Gets the width in user coordinate space.  By convention, the
     * usual width of a RenderableImage is equal to the image's aspect
     * ratio (width divided by height).
     *
     * @return the width of the image in user coordinates.
     */
    public float getWidth() {
        if (boundingBox == null) {
            boundingBox = myCRIF.getBounds2D(paramBlock);
        }
        return (float)boundingBox.getWidth();
    }

    /**
     * Gets the height in user coordinate space.  By convention, the
     * usual height of a RenderedImage is equal to 1.0F.
     *
     * @return the height of the image in user coordinates.
     */
    public float getHeight() {
        if (boundingBox == null) {
            boundingBox = myCRIF.getBounds2D(paramBlock);
        }
        return (float)boundingBox.getHeight();
    }

    /**
     * Gets the minimum X coordinate of the rendering-independent image data.
     */
    public float getMinX() {
        if (boundingBox == null) {
            boundingBox = myCRIF.getBounds2D(paramBlock);
        }
        return (float)boundingBox.getMinX();
    }

    /**
     * Gets the minimum Y coordinate of the rendering-independent image data.
     */
    public float getMinY() {
        if (boundingBox == null) {
            boundingBox = myCRIF.getBounds2D(paramBlock);
        }
        return (float)boundingBox.getMinY();
    }

    /**
     * Change the current ParameterBlock of the operation, allowing
     * editing of image rendering chains.  The effects of such a
     * change will be visible when a new rendering is created from
     * this RenderableImageOp or any dependent RenderableImageOp.
     *
     * @param paramBlock the new ParameterBlock.
     * @return the old ParameterBlock.
     * @see #getParameterBlock
     */
    public ParameterBlock setParameterBlock(ParameterBlock paramBlock) {
        ParameterBlock oldParamBlock = this.paramBlock;
        this.paramBlock = (ParameterBlock)paramBlock.clone();
        return oldParamBlock;
    }

    /**
     * Returns a reference to the current parameter block.
     * @return the {@code ParameterBlock} of this
     *         {@code RenderableImageOp}.
     * @see #setParameterBlock(ParameterBlock)
     */
    public ParameterBlock getParameterBlock() {
        return paramBlock;
    }

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
    public RenderedImage createScaledRendering(int w, int h,
                                               RenderingHints hints) {
        // DSR -- code to try to get a unit scale
        double sx = (double)w/getWidth();
        double sy = (double)h/getHeight();
        if (Math.abs(sx/sy - 1.0) < 0.01) {
            sx = sy;
        }
        AffineTransform usr2dev = AffineTransform.getScaleInstance(sx, sy);
        RenderContext newRC = new RenderContext(usr2dev, hints);
        return createRendering(newRC);
    }

    /**
     * Gets a RenderedImage instance of this image with a default
     * width and height in pixels.  The RenderContext is built
     * automatically with an appropriate usr2dev transform and an area
     * of interest of the full image.  All the rendering hints come
     * from hints passed in.  Implementors of this interface must be
     * sure that there is a defined default width and height.
     *
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createDefaultRendering() {
        AffineTransform usr2dev = new AffineTransform(); // Identity
        RenderContext newRC = new RenderContext(usr2dev);
        return createRendering(newRC);
    }

    /**
     * Creates a RenderedImage which represents this
     * RenderableImageOp (including its Renderable sources) rendered
     * according to the given RenderContext.
     *
     * <p> This method supports chaining of either Renderable or
     * RenderedImage operations.  If sources in
     * the ParameterBlock used to construct the RenderableImageOp are
     * RenderableImages, then a three step process is followed:
     *
     * <ol>
     * <li> mapRenderContext() is called on the associated CRIF for
     * each RenderableImage source;
     * <li> createRendering() is called on each of the RenderableImage sources
     * using the backwards-mapped RenderContexts obtained in step 1,
     * resulting in a rendering of each source;
     * <li> ContextualRenderedImageFactory.create() is called
     * with a new ParameterBlock containing the parameters of
     * the RenderableImageOp and the RenderedImages that were created by the
     * createRendering() calls.
     * </ol>
     *
     * <p> If the elements of the source Vector of
     * the ParameterBlock used to construct the RenderableImageOp are
     * instances of RenderedImage, then the CRIF.create() method is
     * called immediately using the original ParameterBlock.
     * This provides a basis case for the recursion.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * (from the RenderContext) were used to create the image.
     * In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param renderContext The RenderContext to use to perform the rendering.
     * @return a RenderedImage containing the desired output image.
     */
    public RenderedImage createRendering(RenderContext renderContext) {
        RenderedImage image = null;
        RenderContext rcOut = null;

        // Clone the original ParameterBlock; if the ParameterBlock
        // contains RenderableImage sources, they will be replaced by
        // RenderedImages.
        ParameterBlock renderedParamBlock = (ParameterBlock)paramBlock.clone();
        Vector<? extends Object> sources = getRenderableSources();

        try {
            // This assumes that if there is no renderable source, that there
            // is a rendered source in paramBlock

            if (sources != null) {
                Vector<Object> renderedSources = new Vector<>();
                for (int i = 0; i < sources.size(); i++) {
                    rcOut = myCRIF.mapRenderContext(i, renderContext,
                                                    paramBlock, this);
                    RenderedImage rdrdImage =
                        ((RenderableImage)sources.elementAt(i)).createRendering(rcOut);
                    if (rdrdImage == null) {
                        return null;
                    }

                    // Add this rendered image to the ParameterBlock's
                    // list of RenderedImages.
                    renderedSources.addElement(rdrdImage);
                }

                if (renderedSources.size() > 0) {
                    renderedParamBlock.setSources(renderedSources);
                }
            }

            return myCRIF.create(renderContext, renderedParamBlock);
        } catch (ArrayIndexOutOfBoundsException e) {
            // This should never happen
            return null;
        }
    }
}
