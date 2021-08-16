/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.ColorModel;

/**
 * The {@code Composite} interface, along with
 * {@link CompositeContext}, defines the methods to compose a draw
 * primitive with the underlying graphics area.
 * After the {@code Composite} is set in the
 * {@link Graphics2D} context, it combines a shape, text, or an image
 * being rendered with the colors that have already been rendered
 * according to pre-defined rules. The classes
 * implementing this interface provide the rules and a method to create
 * the context for a particular operation.
 * {@code CompositeContext} is an environment used by the
 * compositing operation, which is created by the {@code Graphics2D}
 * prior to the start of the operation.  {@code CompositeContext}
 * contains private information and resources needed for a compositing
 * operation.  When the {@code CompositeContext} is no longer needed,
 * the {@code Graphics2D} object disposes of it in order to reclaim
 * resources allocated for the operation.
 * <p>
 * Instances of classes implementing {@code Composite} must be
 * immutable because the {@code Graphics2D} does not clone
 * these objects when they are set as an attribute with the
 * {@code setComposite} method or when the {@code Graphics2D}
 * object is cloned.  This is to avoid undefined rendering behavior of
 * {@code Graphics2D}, resulting from the modification of
 * the {@code Composite} object after it has been set in the
 * {@code Graphics2D} context.
 * <p>
 * Since this interface must expose the contents of pixels on the
 * target device or image to potentially arbitrary code, the use of
 * custom objects which implement this interface when rendering directly
 * to a screen device is governed by the {@code readDisplayPixels}
 * {@link AWTPermission}.  The permission check will occur when such
 * a custom object is passed to the {@code setComposite} method
 * of a {@code Graphics2D} retrieved from a {@link Component}.
 * @see AlphaComposite
 * @see CompositeContext
 * @see Graphics2D#setComposite
 */
public interface Composite {

    /**
     * Creates a context containing state that is used to perform
     * the compositing operation.  In a multi-threaded environment,
     * several contexts can exist simultaneously for a single
     * {@code Composite} object.
     * @param srcColorModel  the {@link ColorModel} of the source
     * @param dstColorModel  the {@code ColorModel} of the destination
     * @param hints the hint that the context object uses to choose between
     * rendering alternatives
     * @return the {@code CompositeContext} object used to perform the
     * compositing operation.
     */
    public CompositeContext createContext(ColorModel srcColorModel,
                                          ColorModel dstColorModel,
                                          RenderingHints hints);

}
