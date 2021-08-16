/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * A class describing how a stream is to be decoded.  Instances of
 * this class or its subclasses are used to supply prescriptive
 * "how-to" information to instances of {@code ImageReader}.
 *
 * <p> An image encoded as part of a file or stream may be thought of
 * extending out in multiple dimensions: the spatial dimensions of
 * width and height, a number of bands, and a number of progressive
 * decoding passes.  This class allows a contiguous (hyper)rectangular
 * subarea of the image in all of these dimensions to be selected for
 * decoding.  Additionally, the spatial dimensions may be subsampled
 * discontinuously.  Finally, color and format conversions may be
 * specified by controlling the {@code ColorModel} and
 * {@code SampleModel} of the destination image, either by
 * providing a {@code BufferedImage} or by using an
 * {@code ImageTypeSpecifier}.
 *
 * <p> An {@code ImageReadParam} object is used to specify how an
 * image, or a set of images, will be converted on input from
 * a stream in the context of the Java Image I/O framework.  A plug-in for a
 * specific image format will return instances of
 * {@code ImageReadParam} from the
 * {@code getDefaultReadParam} method of its
 * {@code ImageReader} implementation.
 *
 * <p> The state maintained by an instance of
 * {@code ImageReadParam} is independent of any particular image
 * being decoded.  When actual decoding takes place, the values set in
 * the read param are combined with the actual properties of the image
 * being decoded from the stream and the destination
 * {@code BufferedImage} that will receive the decoded pixel
 * data.  For example, the source region set using
 * {@code setSourceRegion} will first be intersected with the
 * actual valid source area.  The result will be translated by the
 * value returned by {@code getDestinationOffset}, and the
 * resulting rectangle intersected with the actual valid destination
 * area to yield the destination area that will be written.
 *
 * <p> The parameters specified by an {@code ImageReadParam} are
 * applied to an image as follows.  First, if a rendering size has
 * been set by {@code setSourceRenderSize}, the entire decoded
 * image is rendered at the size given by
 * {@code getSourceRenderSize}.  Otherwise, the image has its
 * natural size given by {@code ImageReader.getWidth} and
 * {@code ImageReader.getHeight}.
 *
 * <p> Next, the image is clipped against the source region
 * specified by {@code getSourceXOffset}, {@code getSourceYOffset},
 * {@code getSourceWidth}, and {@code getSourceHeight}.
 *
 * <p> The resulting region is then subsampled according to the
 * factors given in {@link IIOParam#setSourceSubsampling
 * IIOParam.setSourceSubsampling}.  The first pixel,
 * the number of pixels per row, and the number of rows all depend
 * on the subsampling settings.
 * Call the minimum X and Y coordinates of the resulting rectangle
 * ({@code minX}, {@code minY}), its width {@code w}
 * and its height {@code h}.
 *
 * <p> This rectangle is offset by
 * ({@code getDestinationOffset().x},
 * {@code getDestinationOffset().y}) and clipped against the
 * destination bounds.  If no destination image has been set, the
 * destination is defined to have a width of
 * {@code getDestinationOffset().x} + {@code w}, and a
 * height of {@code getDestinationOffset().y} + {@code h} so
 * that all pixels of the source region may be written to the
 * destination.
 *
 * <p> Pixels that land, after subsampling, within the destination
 * image, and that are written in one of the progressive passes
 * specified by {@code getSourceMinProgressivePass} and
 * {@code getSourceNumProgressivePasses} are passed along to the
 * next step.
 *
 * <p> Finally, the source samples of each pixel are mapped into
 * destination bands according to the algorithm described in the
 * comment for {@code setDestinationBands}.
 *
 * <p> Plug-in writers may extend the functionality of
 * {@code ImageReadParam} by providing a subclass that implements
 * additional, plug-in specific interfaces.  It is up to the plug-in
 * to document what interfaces are available and how they are to be
 * used.  Readers will silently ignore any extended features of an
 * {@code ImageReadParam} subclass of which they are not aware.
 * Also, they may ignore any optional features that they normally
 * disable when creating their own {@code ImageReadParam}
 * instances via {@code getDefaultReadParam}.
 *
 * <p> Note that unless a query method exists for a capability, it must
 * be supported by all {@code ImageReader} implementations
 * (<i>e.g.</i> source render size is optional, but subsampling must be
 * supported).
 *
 *
 * @see ImageReader
 * @see ImageWriter
 * @see ImageWriteParam
 */
public class ImageReadParam extends IIOParam {

    /**
     * {@code true} if this {@code ImageReadParam} allows
     * the source rendering dimensions to be set.  By default, the
     * value is {@code false}.  Subclasses must set this value
     * manually.
     *
     * <p> {@code ImageReader}s that do not support setting of
     * the source render size should set this value to
     * {@code false}.
     */
    protected boolean canSetSourceRenderSize = false;

    /**
     * The desired rendering width and height of the source, if
     * {@code canSetSourceRenderSize} is {@code true}, or
     * {@code null}.
     *
     * <p> {@code ImageReader}s that do not support setting of
     * the source render size may ignore this value.
     */
    protected Dimension sourceRenderSize = null;

    /**
     * The current destination {@code BufferedImage}, or
     * {@code null} if none has been set.  By default, the value
     * is {@code null}.
     */
    protected BufferedImage destination = null;

    /**
     * The set of destination bands to be used, as an array of
     * {@code int}s.  By default, the value is {@code null},
     * indicating all destination bands should be written in order.
     */
    protected int[] destinationBands = null;

    /**
     * The minimum index of a progressive pass to read from the
     * source.  By default, the value is set to 0, which indicates
     * that passes starting with the first available pass should be
     * decoded.
     *
     * <p> Subclasses should ensure that this value is
     * non-negative.
     */
    protected int minProgressivePass = 0;

    /**
     * The maximum number of progressive passes to read from the
     * source.  By default, the value is set to
     * {@code Integer.MAX_VALUE}, which indicates that passes up
     * to and including the last available pass should be decoded.
     *
     * <p> Subclasses should ensure that this value is positive.
     * Additionally, if the value is not
     * {@code Integer.MAX_VALUE}, then
     * {@code minProgressivePass + numProgressivePasses - 1}
     * should not exceed
     * {@code Integer.MAX_VALUE}.
     */
    protected int numProgressivePasses = Integer.MAX_VALUE;

    /**
     * Constructs an {@code ImageReadParam}.
     */
    public ImageReadParam() {}

    // Comment inherited
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        super.setDestinationType(destinationType);
        setDestination(null);
    }

    /**
     * Supplies a {@code BufferedImage} to be used as the
     * destination for decoded pixel data.  The currently set image
     * will be written to by the {@code read},
     * {@code readAll}, and {@code readRaster} methods, and
     * a reference to it will be returned by those methods.
     *
     * <p> Pixel data from the aforementioned methods will be written
     * starting at the offset specified by
     * {@code getDestinationOffset}.
     *
     * <p> If {@code destination} is {@code null}, a
     * newly-created {@code BufferedImage} will be returned by
     * those methods.
     *
     * <p> At the time of reading, the image is checked to verify that
     * its {@code ColorModel} and {@code SampleModel}
     * correspond to one of the {@code ImageTypeSpecifier}s
     * returned from the {@code ImageReader}'s
     * {@code getImageTypes} method.  If it does not, the reader
     * will throw an {@code IIOException}.
     *
     * @param destination the BufferedImage to be written to, or
     * {@code null}.
     *
     * @see #getDestination
     */
    public void setDestination(BufferedImage destination) {
        this.destination = destination;
    }

    /**
     * Returns the {@code BufferedImage} currently set by the
     * {@code setDestination} method, or {@code null}
     * if none is set.
     *
     * @return the BufferedImage to be written to.
     *
     * @see #setDestination
     */
    public BufferedImage getDestination() {
        return destination;
    }

    /**
     * Sets the indices of the destination bands where data
     * will be placed.  Duplicate indices are not allowed.
     *
     * <p> A {@code null} value indicates that all destination
     * bands will be used.
     *
     * <p> Choosing a destination band subset will not affect the
     * number of bands in the output image of a read if no destination
     * image is specified; the created destination image will still
     * have the same number of bands as if this method had never been
     * called.  If a different number of bands in the destination
     * image is desired, an image must be supplied using the
     * {@code ImageReadParam.setDestination} method.
     *
     * <p> At the time of reading or writing, an
     * {@code IllegalArgumentException} will be thrown by the
     * reader or writer if a value larger than the largest destination
     * band index has been specified, or if the number of source bands
     * and destination bands to be used differ.  The
     * {@code ImageReader.checkReadParamBandSettings} method may
     * be used to automate this test.
     *
     * @param destinationBands an array of integer band indices to be
     * used.
     *
     * @exception IllegalArgumentException if {@code destinationBands}
     * contains a negative or duplicate value.
     *
     * @see #getDestinationBands
     * @see #getSourceBands
     * @see ImageReader#checkReadParamBandSettings
     */
    public void setDestinationBands(int[] destinationBands) {
        if (destinationBands == null) {
            this.destinationBands = null;
        } else {
            int numBands = destinationBands.length;
            for (int i = 0; i < numBands; i++) {
                int band = destinationBands[i];
                if (band < 0) {
                    throw new IllegalArgumentException("Band value < 0!");
                }
                for (int j = i + 1; j < numBands; j++) {
                    if (band == destinationBands[j]) {
                        throw new IllegalArgumentException("Duplicate band value!");
                    }
                }
            }
            this.destinationBands = destinationBands.clone();
        }
    }

    /**
     * Returns the set of band indices where data will be placed.
     * If no value has been set, {@code null} is returned to
     * indicate that all destination bands will be used.
     *
     * @return the indices of the destination bands to be used,
     * or {@code null}.
     *
     * @see #setDestinationBands
     */
    public int[] getDestinationBands() {
        if (destinationBands == null) {
            return null;
        } else {
            return destinationBands.clone();
        }
    }

    /**
     * Returns {@code true} if this reader allows the source
     * image to be rendered at an arbitrary size as part of the
     * decoding process, by means of the
     * {@code setSourceRenderSize} method.  If this method
     * returns {@code false}, calls to
     * {@code setSourceRenderSize} will throw an
     * {@code UnsupportedOperationException}.
     *
     * @return {@code true} if setting source rendering size is
     * supported.
     *
     * @see #setSourceRenderSize
     */
    public boolean canSetSourceRenderSize() {
        return canSetSourceRenderSize;
    }

    /**
     * If the image is able to be rendered at an arbitrary size, sets
     * the source width and height to the supplied values.  Note that
     * the values returned from the {@code getWidth} and
     * {@code getHeight} methods on {@code ImageReader} are
     * not affected by this method; they will continue to return the
     * default size for the image.  Similarly, if the image is also
     * tiled the tile width and height are given in terms of the default
     * size.
     *
     * <p> Typically, the width and height should be chosen such that
     * the ratio of width to height closely approximates the aspect
     * ratio of the image, as returned from
     * {@code ImageReader.getAspectRatio}.
     *
     * <p> If this plug-in does not allow the rendering size to be
     * set, an {@code UnsupportedOperationException} will be
     * thrown.
     *
     * <p> To remove the render size setting, pass in a value of
     * {@code null} for {@code size}.
     *
     * @param size a {@code Dimension} indicating the desired
     * width and height.
     *
     * @exception IllegalArgumentException if either the width or the
     * height is negative or 0.
     * @exception UnsupportedOperationException if image resizing
     * is not supported by this plug-in.
     *
     * @see #getSourceRenderSize
     * @see ImageReader#getWidth
     * @see ImageReader#getHeight
     * @see ImageReader#getAspectRatio
     */
    public void setSourceRenderSize(Dimension size)
        throws UnsupportedOperationException {
        if (!canSetSourceRenderSize()) {
            throw new UnsupportedOperationException
                ("Can't set source render size!");
        }

        if (size == null) {
            this.sourceRenderSize = null;
        } else {
            if (size.width <= 0 || size.height <= 0) {
                throw new IllegalArgumentException("width or height <= 0!");
            }
            this.sourceRenderSize = (Dimension)size.clone();
        }
    }

    /**
     * Returns the width and height of the source image as it
     * will be rendered during decoding, if they have been set via the
     * {@code setSourceRenderSize} method.  A
     * {@code null} value indicates that no setting has been made.
     *
     * @return the rendered width and height of the source image
     * as a {@code Dimension}.
     *
     * @see #setSourceRenderSize
     */
    public Dimension getSourceRenderSize() {
        return (sourceRenderSize == null) ?
            null : (Dimension)sourceRenderSize.clone();
    }

    /**
     * Sets the range of progressive passes that will be decoded.
     * Passes outside of this range will be ignored.
     *
     * <p> A progressive pass is a re-encoding of the entire image,
     * generally at progressively higher effective resolutions, but
     * requiring greater transmission bandwidth.  The most common use
     * of progressive encoding is found in the JPEG format, where
     * successive passes include more detailed representations of the
     * high-frequency image content.
     *
     * <p> The actual number of passes to be decoded is determined
     * during decoding, based on the number of actual passes available
     * in the stream.  Thus if {@code minPass + numPasses - 1} is
     * larger than the index of the last available passes, decoding
     * will end with that pass.
     *
     * <p> A value of {@code numPasses} of
     * {@code Integer.MAX_VALUE} indicates that all passes from
     * {@code minPass} forward should be read.  Otherwise, the
     * index of the last pass (<i>i.e.</i>, {@code minPass + numPasses - 1})
     * must not exceed {@code Integer.MAX_VALUE}.
     *
     * <p> There is no {@code unsetSourceProgressivePasses}
     * method; the same effect may be obtained by calling
     * {@code setSourceProgressivePasses(0, Integer.MAX_VALUE)}.
     *
     * @param minPass the index of the first pass to be decoded.
     * @param numPasses the maximum number of passes to be decoded.
     *
     * @exception IllegalArgumentException if {@code minPass} is
     * negative, {@code numPasses} is negative or 0, or
     * {@code numPasses} is smaller than
     * {@code Integer.MAX_VALUE} but
     * {@code minPass + numPasses - 1} is greater than
     * {@code INTEGER.MAX_VALUE}.
     *
     * @see #getSourceMinProgressivePass
     * @see #getSourceMaxProgressivePass
     */
    public void setSourceProgressivePasses(int minPass, int numPasses) {
        if (minPass < 0) {
            throw new IllegalArgumentException("minPass < 0!");
        }
        if (numPasses <= 0) {
            throw new IllegalArgumentException("numPasses <= 0!");
        }
        if ((numPasses != Integer.MAX_VALUE) &&
            (((minPass + numPasses - 1) & 0x80000000) != 0)) {
            throw new IllegalArgumentException
                ("minPass + numPasses - 1 > INTEGER.MAX_VALUE!");
        }

        this.minProgressivePass = minPass;
        this.numProgressivePasses = numPasses;
    }

    /**
     * Returns the index of the first progressive pass that will be
     * decoded. If no value has been set, 0 will be returned (which is
     * the correct value).
     *
     * @return the index of the first pass that will be decoded.
     *
     * @see #setSourceProgressivePasses
     * @see #getSourceNumProgressivePasses
     */
    public int getSourceMinProgressivePass() {
        return minProgressivePass;
    }

    /**
     * If {@code getSourceNumProgressivePasses} is equal to
     * {@code Integer.MAX_VALUE}, returns
     * {@code Integer.MAX_VALUE}.  Otherwise, returns
     * {@code getSourceMinProgressivePass() +
     * getSourceNumProgressivePasses() - 1}.
     *
     * @return the index of the last pass to be read, or
     * {@code Integer.MAX_VALUE}.
     */
    public int getSourceMaxProgressivePass() {
        if (numProgressivePasses == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return minProgressivePass + numProgressivePasses - 1;
        }
    }

    /**
     * Returns the number of the progressive passes that will be
     * decoded. If no value has been set,
     * {@code Integer.MAX_VALUE} will be returned (which is the
     * correct value).
     *
     * @return the number of the passes that will be decoded.
     *
     * @see #setSourceProgressivePasses
     * @see #getSourceMinProgressivePass
     */
    public int getSourceNumProgressivePasses() {
        return numProgressivePasses;
    }
}
