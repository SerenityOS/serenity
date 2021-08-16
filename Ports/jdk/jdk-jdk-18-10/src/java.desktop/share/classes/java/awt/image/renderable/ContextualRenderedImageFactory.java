/*
 * Copyright (c) 1998, 2008, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * ContextualRenderedImageFactory provides an interface for the
 * functionality that may differ between instances of
 * RenderableImageOp.  Thus different operations on RenderableImages
 * may be performed by a single class such as RenderedImageOp through
 * the use of multiple instances of ContextualRenderedImageFactory.
 * The name ContextualRenderedImageFactory is commonly shortened to
 * "CRIF."
 *
 * <p> All operations that are to be used in a rendering-independent
 * chain must implement ContextualRenderedImageFactory.
 *
 * <p> Classes that implement this interface must provide a
 * constructor with no arguments.
 */
public interface ContextualRenderedImageFactory extends RenderedImageFactory {

    /**
     * Maps the operation's output RenderContext into a RenderContext
     * for each of the operation's sources.  This is useful for
     * operations that can be expressed in whole or in part simply as
     * alterations in the RenderContext, such as an affine mapping, or
     * operations that wish to obtain lower quality renderings of
     * their sources in order to save processing effort or
     * transmission bandwidth.  Some operations, such as blur, can also
     * use this mechanism to avoid obtaining sources of higher quality
     * than necessary.
     *
     * @param i the index of the source image.
     * @param renderContext the RenderContext being applied to the operation.
     * @param paramBlock a ParameterBlock containing the operation's
     *        sources and parameters.
     * @param image the RenderableImage being rendered.
     * @return a {@code RenderContext} for
     *         the source at the specified index of the parameters
     *         Vector contained in the specified ParameterBlock.
     */
    RenderContext mapRenderContext(int i,
                                   RenderContext renderContext,
                                   ParameterBlock paramBlock,
                                   RenderableImage image);

    /**
     * Creates a rendering, given a RenderContext and a ParameterBlock
     * containing the operation's sources and parameters.  The output
     * is a RenderedImage that takes the RenderContext into account to
     * determine its dimensions and placement on the image plane.
     * This method houses the "intelligence" that allows a
     * rendering-independent operation to adapt to a specific
     * RenderContext.
     *
     * @param renderContext The RenderContext specifying the rendering
     * @param paramBlock a ParameterBlock containing the operation's
     *        sources and parameters
     * @return a {@code RenderedImage} from the sources and parameters
     *         in the specified ParameterBlock and according to the
     *         rendering instructions in the specified RenderContext.
     */
    RenderedImage create(RenderContext renderContext,
                         ParameterBlock paramBlock);

    /**
     * Returns the bounding box for the output of the operation,
     * performed on a given set of sources, in rendering-independent
     * space.  The bounds are returned as a Rectangle2D, that is, an
     * axis-aligned rectangle with floating-point corner coordinates.
     *
     * @param paramBlock a ParameterBlock containing the operation's
     *        sources and parameters.
     * @return a Rectangle2D specifying the rendering-independent
     *         bounding box of the output.
     */
    Rectangle2D getBounds2D(ParameterBlock paramBlock);

    /**
     * Gets the appropriate instance of the property specified by the name
     * parameter.  This method must determine which instance of a property to
     * return when there are multiple sources that each specify the property.
     *
     * @param paramBlock a ParameterBlock containing the operation's
     *        sources and parameters.
     * @param name a String naming the desired property.
     * @return an object reference to the value of the property requested.
     */
    Object getProperty(ParameterBlock paramBlock, String name);

    /**
     * Returns a list of names recognized by getProperty.
     * @return the list of property names.
     */
    String[] getPropertyNames();

    /**
     * Returns true if successive renderings (that is, calls to
     * create(RenderContext, ParameterBlock)) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  It is always safe to return true.
     * @return {@code true} if successive renderings with the
     *         same arguments might produce different results;
     *         {@code false} otherwise.
     */
    boolean isDynamic();
}
