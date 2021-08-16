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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.spi.ImageWriterSpi;

/**
 * An abstract superclass for encoding and writing images.  This class
 * must be subclassed by classes that write out images in the context
 * of the Java Image I/O framework.
 *
 * <p> {@code ImageWriter} objects are normally instantiated by
 * the service provider class for the specific format.  Service
 * provider classes are registered with the {@code IIORegistry},
 * which uses them for format recognition and presentation of
 * available format readers and writers.
 *
 * @see ImageReader
 * @see ImageWriteParam
 * @see javax.imageio.spi.IIORegistry
 * @see javax.imageio.spi.ImageWriterSpi
 *
 */
public abstract class ImageWriter implements ImageTranscoder {

    /**
     * The {@code ImageWriterSpi} that instantiated this object,
     * or {@code null} if its identity is not known or none
     * exists.  By default it is initialized to {@code null}.
     */
    protected ImageWriterSpi originatingProvider = null;

    /**
     * The {@code ImageOutputStream} or other {@code Object}
     * set by {@code setOutput} and retrieved by
     * {@code getOutput}.  By default it is initialized to
     * {@code null}.
     */
    protected Object output = null;

    /**
     * An array of {@code Locale}s that may be used to localize
     * warning messages and compression setting values, or
     * {@code null} if localization is not supported.  By default
     * it is initialized to {@code null}.
     */
    protected Locale[] availableLocales = null;

    /**
     * The current {@code Locale} to be used for localization, or
     * {@code null} if none has been set.  By default it is
     * initialized to {@code null}.
     */
    protected Locale locale = null;

    /**
     * A {@code List} of currently registered
     * {@code IIOWriteWarningListener}s, initialized by default to
     * {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<IIOWriteWarningListener> warningListeners = null;

    /**
     * A {@code List} of {@code Locale}s, one for each
     * element of {@code warningListeners}, initialized by default
     * {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<Locale> warningLocales = null;

    /**
     * A {@code List} of currently registered
     * {@code IIOWriteProgressListener}s, initialized by default
     * {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<IIOWriteProgressListener> progressListeners = null;

    /**
     * If {@code true}, the current write operation should be
     * aborted.
     */
    private boolean abortFlag = false;

    /**
     * Constructs an {@code ImageWriter} and sets its
     * {@code originatingProvider} instance variable to the
     * supplied value.
     *
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature {@code (ImageWriterSpi, Object)}
     * in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * {@code IllegalArgumentException} should be thrown.
     *
     * @param originatingProvider the {@code ImageWriterSpi} that
     * is constructing this object, or {@code null}.
     */
    protected ImageWriter(ImageWriterSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /**
     * Returns the {@code ImageWriterSpi} object that created
     * this {@code ImageWriter}, or {@code null} if this
     * object was not created through the {@code IIORegistry}.
     *
     * <p> The default implementation returns the value of the
     * {@code originatingProvider} instance variable.
     *
     * @return an {@code ImageWriterSpi}, or {@code null}.
     *
     * @see ImageWriterSpi
     */
    public ImageWriterSpi getOriginatingProvider() {
        return originatingProvider;
    }

    /**
     * Sets the destination to the given
     * {@code ImageOutputStream} or other {@code Object}.
     * The destination is assumed to be ready to accept data, and will
     * not be closed at the end of each write. This allows distributed
     * imaging applications to transmit a series of images over a
     * single network connection.  If {@code output} is
     * {@code null}, any currently set output will be removed.
     *
     * <p> If {@code output} is an
     * {@code ImageOutputStream}, calls to the
     * {@code write}, {@code writeToSequence}, and
     * {@code prepareWriteEmpty}/{@code endWriteEmpty}
     * methods will preserve the existing contents of the stream.
     * Other write methods, such as {@code writeInsert},
     * {@code replaceStreamMetadata},
     * {@code replaceImageMetadata}, {@code replacePixels},
     * {@code prepareInsertEmpty}/{@code endInsertEmpty},
     * and {@code endWriteSequence}, require the full contents
     * of the stream to be readable and writable, and may alter any
     * portion of the stream.
     *
     * <p> Use of a general {@code Object} other than an
     * {@code ImageOutputStream} is intended for writers that
     * interact directly with an output device or imaging protocol.
     * The set of legal classes is advertised by the writer's service
     * provider's {@code getOutputTypes} method; most writers
     * will return a single-element array containing only
     * {@code ImageOutputStream.class} to indicate that they
     * accept only an {@code ImageOutputStream}.
     *
     * <p> The default implementation sets the {@code output}
     * instance variable to the value of {@code output} after
     * checking {@code output} against the set of classes
     * advertised by the originating provider, if there is one.
     *
     * @param output the {@code ImageOutputStream} or other
     * {@code Object} to use for future writing.
     *
     * @exception IllegalArgumentException if {@code output} is
     * not an instance of one of the classes returned by the
     * originating service provider's {@code getOutputTypes}
     * method.
     *
     * @see #getOutput
     */
    public void setOutput(Object output) {
        if (output != null) {
            ImageWriterSpi provider = getOriginatingProvider();
            if (provider != null) {
                Class<?>[] classes = provider.getOutputTypes();
                boolean found = false;
                for (int i = 0; i < classes.length; i++) {
                    if (classes[i].isInstance(output)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Illegal output type!");
                }
            }
        }

        this.output = output;
    }

    /**
     * Returns the {@code ImageOutputStream} or other
     * {@code Object} set by the most recent call to the
     * {@code setOutput} method.  If no destination has been
     * set, {@code null} is returned.
     *
     * <p> The default implementation returns the value of the
     * {@code output} instance variable.
     *
     * @return the {@code Object} that was specified using
     * {@code setOutput}, or {@code null}.
     *
     * @see #setOutput
     */
    public Object getOutput() {
        return output;
    }

    // Localization

    /**
     * Returns an array of {@code Locale}s that may be used to
     * localize warning listeners and compression settings.  A return
     * value of {@code null} indicates that localization is not
     * supported.
     *
     * <p> The default implementation returns a clone of the
     * {@code availableLocales} instance variable if it is
     * non-{@code null}, or else returns {@code null}.
     *
     * @return an array of {@code Locale}s that may be used as
     * arguments to {@code setLocale}, or {@code null}.
     */
    public Locale[] getAvailableLocales() {
        return (availableLocales == null) ?
            null : availableLocales.clone();
    }

    /**
     * Sets the current {@code Locale} of this
     * {@code ImageWriter} to the given value.  A value of
     * {@code null} removes any previous setting, and indicates
     * that the writer should localize as it sees fit.
     *
     * <p> The default implementation checks {@code locale}
     * against the values returned by
     * {@code getAvailableLocales}, and sets the
     * {@code locale} instance variable if it is found.  If
     * {@code locale} is {@code null}, the instance variable
     * is set to {@code null} without any checking.
     *
     * @param locale the desired {@code Locale}, or
     * {@code null}.
     *
     * @exception IllegalArgumentException if {@code locale} is
     * non-{@code null} but is not one of the values returned by
     * {@code getAvailableLocales}.
     *
     * @see #getLocale
     */
    public void setLocale(Locale locale) {
        if (locale != null) {
            Locale[] locales = getAvailableLocales();
            boolean found = false;
            if (locales != null) {
                for (int i = 0; i < locales.length; i++) {
                    if (locale.equals(locales[i])) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid locale!");
            }
        }
        this.locale = locale;
    }

    /**
     * Returns the currently set {@code Locale}, or
     * {@code null} if none has been set.
     *
     * <p> The default implementation returns the value of the
     * {@code locale} instance variable.
     *
     * @return the current {@code Locale}, or {@code null}.
     *
     * @see #setLocale
     */
    public Locale getLocale() {
        return locale;
    }

    // Write params

    /**
     * Returns a new {@code ImageWriteParam} object of the
     * appropriate type for this file format containing default
     * values, that is, those values that would be used
     * if no {@code ImageWriteParam} object were specified.  This
     * is useful as a starting point for tweaking just a few parameters
     * and otherwise leaving the default settings alone.
     *
     * <p> The default implementation constructs and returns a new
     * {@code ImageWriteParam} object that does not allow tiling,
     * progressive encoding, or compression, and that will be
     * localized for the current {@code Locale} (<i>i.e.</i>,
     * what you would get by calling
     * {@code new ImageWriteParam(getLocale())}.
     *
     * <p> Individual plug-ins may return an instance of
     * {@code ImageWriteParam} with additional optional features
     * enabled, or they may return an instance of a plug-in specific
     * subclass of {@code ImageWriteParam}.
     *
     * @return a new {@code ImageWriteParam} object containing
     * default values.
     */
    public ImageWriteParam getDefaultWriteParam() {
        return new ImageWriteParam(getLocale());
    }

    // Metadata

    /**
     * Returns an {@code IIOMetadata} object containing default
     * values for encoding a stream of images.  The contents of the
     * object may be manipulated using either the XML tree structure
     * returned by the {@code IIOMetadata.getAsTree} method, an
     * {@code IIOMetadataController} object, or via plug-in
     * specific interfaces, and the resulting data supplied to one of
     * the {@code write} methods that take a stream metadata
     * parameter.
     *
     * <p> An optional {@code ImageWriteParam} may be supplied
     * for cases where it may affect the structure of the stream
     * metadata.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> Writers that do not make use of stream metadata
     * (<i>e.g.</i>, writers for single-image formats) should return
     * {@code null}.
     *
     * @param param an {@code ImageWriteParam} that will be used to
     * encode the image, or {@code null}.
     *
     * @return an {@code IIOMetadata} object.
     */
    public abstract IIOMetadata
        getDefaultStreamMetadata(ImageWriteParam param);

    /**
     * Returns an {@code IIOMetadata} object containing default
     * values for encoding an image of the given type.  The contents
     * of the object may be manipulated using either the XML tree
     * structure returned by the {@code IIOMetadata.getAsTree}
     * method, an {@code IIOMetadataController} object, or via
     * plug-in specific interfaces, and the resulting data supplied to
     * one of the {@code write} methods that take a stream
     * metadata parameter.
     *
     * <p> An optional {@code ImageWriteParam} may be supplied
     * for cases where it may affect the structure of the image
     * metadata.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * @param imageType an {@code ImageTypeSpecifier} indicating the
     * format of the image to be written later.
     * @param param an {@code ImageWriteParam} that will be used to
     * encode the image, or {@code null}.
     *
     * @return an {@code IIOMetadata} object.
     */
    public abstract IIOMetadata
        getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                ImageWriteParam param);

    // comment inherited
    public abstract IIOMetadata convertStreamMetadata(IIOMetadata inData,
                                                      ImageWriteParam param);

    // comment inherited
    public abstract IIOMetadata
        convertImageMetadata(IIOMetadata inData,
                             ImageTypeSpecifier imageType,
                             ImageWriteParam param);

    // Thumbnails

    /**
     * Returns the number of thumbnails supported by the format being
     * written, given the image type and any additional write
     * parameters and metadata objects that will be used during
     * encoding.  A return value of {@code -1} indicates that
     * insufficient information is available.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * for cases where it may affect thumbnail handling.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation returns 0.
     *
     * @param imageType an {@code ImageTypeSpecifier} indicating
     * the type of image to be written, or {@code null}.
     * @param param the {@code ImageWriteParam} that will be used for
     * writing, or {@code null}.
     * @param streamMetadata an {@code IIOMetadata} object that will
     * be used for writing, or {@code null}.
     * @param imageMetadata an {@code IIOMetadata} object that will
     * be used for writing, or {@code null}.
     *
     * @return the number of thumbnails that may be written given the
     * supplied parameters, or {@code -1} if insufficient
     * information is available.
     */
    public int getNumThumbnailsSupported(ImageTypeSpecifier imageType,
                                         ImageWriteParam param,
                                         IIOMetadata streamMetadata,
                                         IIOMetadata imageMetadata) {
        return 0;
    }

    /**
     * Returns an array of {@code Dimension}s indicating the
     * legal size ranges for thumbnail images as they will be encoded
     * in the output file or stream.  This information is merely
     * advisory; the writer will resize any supplied thumbnails as
     * necessary.
     *
     * <p> The information is returned as a set of pairs; the first
     * element of a pair contains an (inclusive) minimum width and
     * height, and the second element contains an (inclusive) maximum
     * width and height.  Together, each pair defines a valid range of
     * sizes.  To specify a fixed size, the same width and height will
     * appear for both elements.  A return value of {@code null}
     * indicates that the size is arbitrary or unknown.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * for cases where it may affect thumbnail handling.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation returns {@code null}.
     *
     * @param imageType an {@code ImageTypeSpecifier} indicating the
     * type of image to be written, or {@code null}.
     * @param param the {@code ImageWriteParam} that will be used for
     * writing, or {@code null}.
     * @param streamMetadata an {@code IIOMetadata} object that will
     * be used for writing, or {@code null}.
     * @param imageMetadata an {@code IIOMetadata} object that will
     * be used for writing, or {@code null}.
     *
     * @return an array of {@code Dimension}s with an even length
     * of at least two, or {@code null}.
     */
    public Dimension[] getPreferredThumbnailSizes(ImageTypeSpecifier imageType,
                                                  ImageWriteParam param,
                                                  IIOMetadata streamMetadata,
                                                  IIOMetadata imageMetadata) {
        return null;
    }

    /**
     * Returns {@code true} if the methods that take an
     * {@code IIOImage} parameter are capable of dealing with a
     * {@code Raster} (as opposed to {@code RenderedImage})
     * source image.  If this method returns {@code false}, then
     * those methods will throw an
     * {@code UnsupportedOperationException} if supplied with an
     * {@code IIOImage} containing a {@code Raster}.
     *
     * <p> The default implementation returns {@code false}.
     *
     * @return {@code true} if {@code Raster} sources are
     * supported.
     */
    public boolean canWriteRasters() {
        return false;
    }

    /**
     * Appends a complete image stream containing a single image and
     * associated stream and image metadata and thumbnails to the
     * output.  Any necessary header information is included.  If the
     * output is an {@code ImageOutputStream}, its existing
     * contents prior to the current seek position are not affected,
     * and need not be readable or writable.
     *
     * <p> The output must have been set beforehand using the
     * {@code setOutput} method.
     *
     * <p> Stream metadata may optionally be supplied; if it is
     * {@code null}, default stream metadata will be used.
     *
     * <p> If {@code canWriteRasters} returns {@code true},
     * the {@code IIOImage} may contain a {@code Raster}
     * source.  Otherwise, it must contain a
     * {@code RenderedImage} source.
     *
     * <p> The supplied thumbnails will be resized if needed, and any
     * thumbnails in excess of the supported number will be ignored.
     * If the format requires additional thumbnails that are not
     * provided, the writer should generate them internally.
     *
     * <p>  An {@code ImageWriteParam} may
     * optionally be supplied to control the writing process.  If
     * {@code param} is {@code null}, a default write param
     * will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * @param streamMetadata an {@code IIOMetadata} object representing
     * stream metadata, or {@code null} to use default values.
     * @param image an {@code IIOImage} object containing an
     * image, thumbnails, and metadata to be written.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if {@code image}
     * contains a {@code Raster} and {@code canWriteRasters}
     * returns {@code false}.
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     * @exception IOException if an error occurs during writing.
     */
    public abstract void write(IIOMetadata streamMetadata,
                               IIOImage image,
                               ImageWriteParam param) throws IOException;

    /**
     * Appends a complete image stream containing a single image with
     * default metadata and thumbnails to the output.  This method is
     * a shorthand for {@code write(null, image, null)}.
     *
     * @param image an {@code IIOImage} object containing an
     * image, thumbnails, and metadata to be written.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     * @exception UnsupportedOperationException if {@code image}
     * contains a {@code Raster} and {@code canWriteRasters}
     * returns {@code false}.
     * @exception IOException if an error occurs during writing.
     */
    public void write(IIOImage image) throws IOException {
        write(null, image, null);
    }

    /**
     * Appends a complete image stream consisting of a single image
     * with default metadata and thumbnails to the output.  This
     * method is a shorthand for
     * {@code write(null, new IIOImage(image, null, null), null)}.
     *
     * @param image a {@code RenderedImage} to be written.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     * @exception IOException if an error occurs during writing.
     */
    public void write(RenderedImage image) throws IOException {
        write(null, new IIOImage(image, null, null), null);
    }

    // Check that the output has been set, then throw an
    // UnsupportedOperationException.
    private void unsupported() {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    // Sequence writes

    /**
     * Returns {@code true} if the writer is able to append an
     * image to an image stream that already contains header
     * information and possibly prior images.
     *
     * <p> If {@code canWriteSequence} returns {@code false},
     * {@code writeToSequence} and {@code endWriteSequence}
     * will throw an {@code UnsupportedOperationException}.
     *
     * <p> The default implementation returns {@code false}.
     *
     * @return {@code true} if images may be appended sequentially.
     */
    public boolean canWriteSequence() {
        return false;
    }

    /**
     * Prepares a stream to accept a series of subsequent
     * {@code writeToSequence} calls, using the provided stream
     * metadata object.  The metadata will be written to the stream if
     * it should precede the image data.  If the argument is {@code null},
     * default stream metadata is used.
     *
     * <p> If the output is an {@code ImageOutputStream}, the existing
     * contents of the output prior to the current seek position are
     * flushed, and need not be readable or writable.  If the format
     * requires that {@code endWriteSequence} be able to rewind to
     * patch up the header information, such as for a sequence of images
     * in a single TIFF file, then the metadata written by this method
     * must remain in a writable portion of the stream.  Other formats
     * may flush the stream after this method and after each image.
     *
     * <p> If {@code canWriteSequence} returns {@code false},
     * this method will throw an
     * {@code UnsupportedOperationException}.
     *
     * <p> The output must have been set beforehand using either
     * the {@code setOutput} method.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param streamMetadata A stream metadata object, or {@code null}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canWriteSequence} returns {@code false}.
     * @exception IOException if an error occurs writing the stream
     * metadata.
     */
    public void prepareWriteSequence(IIOMetadata streamMetadata)
        throws IOException {
        unsupported();
    }

    /**
     * Appends a single image and possibly associated metadata and
     * thumbnails, to the output.  If the output is an
     * {@code ImageOutputStream}, the existing contents of the
     * output prior to the current seek position may be flushed, and
     * need not be readable or writable, unless the plug-in needs to
     * be able to patch up the header information when
     * {@code endWriteSequence} is called (<i>e.g.</i> TIFF).
     *
     * <p> If {@code canWriteSequence} returns {@code false},
     * this method will throw an
     * {@code UnsupportedOperationException}.
     *
     * <p> The output must have been set beforehand using
     * the {@code setOutput} method.
     *
     * <p> {@code prepareWriteSequence} must have been called
     * beforehand, or an {@code IllegalStateException} is thrown.
     *
     * <p> If {@code canWriteRasters} returns {@code true},
     * the {@code IIOImage} may contain a {@code Raster}
     * source.  Otherwise, it must contain a
     * {@code RenderedImage} source.
     *
     * <p> The supplied thumbnails will be resized if needed, and any
     * thumbnails in excess of the supported number will be ignored.
     * If the format requires additional thumbnails that are not
     * provided, the writer will generate them internally.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param image an {@code IIOImage} object containing an
     * image, thumbnails, and metadata to be written.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set, or {@code prepareWriteSequence} has not been called.
     * @exception UnsupportedOperationException if
     * {@code canWriteSequence} returns {@code false}.
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     * @exception UnsupportedOperationException if {@code image}
     * contains a {@code Raster} and {@code canWriteRasters}
     * returns {@code false}.
     * @exception IOException if an error occurs during writing.
     */
    public void writeToSequence(IIOImage image, ImageWriteParam param)
        throws IOException {
        unsupported();
    }

    /**
     * Completes the writing of a sequence of images begun with
     * {@code prepareWriteSequence}.  Any stream metadata that
     * should come at the end of the sequence of images is written out,
     * and any header information at the beginning of the sequence is
     * patched up if necessary.  If the output is an
     * {@code ImageOutputStream}, data through the stream metadata
     * at the end of the sequence are flushed and need not be readable
     * or writable.
     *
     * <p> If {@code canWriteSequence} returns {@code false},
     * this method will throw an
     * {@code UnsupportedOperationException}.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @exception IllegalStateException if the output has not
     * been set, or {@code prepareWriteSequence} has not been called.
     * @exception UnsupportedOperationException if
     * {@code canWriteSequence} returns {@code false}.
     * @exception IOException if an error occurs during writing.
     */
    public void endWriteSequence() throws IOException {
        unsupported();
    }

    // Metadata replacement

    /**
     * Returns {@code true} if it is possible to replace the
     * stream metadata already present in the output.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}.
     *
     * @return {@code true} if replacement of stream metadata is
     * allowed.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IOException if an I/O error occurs during the query.
     */
    public boolean canReplaceStreamMetadata() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Replaces the stream metadata in the output with new
     * information.  If the output is an
     * {@code ImageOutputStream}, the prior contents of the
     * stream are examined and possibly edited to make room for the
     * new data.  All of the prior contents of the output must be
     * available for reading and writing.
     *
     * <p> If {@code canReplaceStreamMetadata} returns
     * {@code false}, an
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param streamMetadata an {@code IIOMetadata} object representing
     * stream metadata, or {@code null} to use default values.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if the
     * {@code canReplaceStreamMetadata} returns
     * {@code false}.  modes do not include
     * @exception IOException if an error occurs during writing.
     */
    public void replaceStreamMetadata(IIOMetadata streamMetadata)
        throws IOException {
        unsupported();
    }

    /**
     * Returns {@code true} if it is possible to replace the
     * image metadata associated with an existing image with index
     * {@code imageIndex}.  If this method returns
     * {@code false}, a call to
     * {@code replaceImageMetadata(imageIndex)} will throw an
     * {@code UnsupportedOperationException}.
     *
     * <p> A writer that does not support any image metadata
     * replacement may return {@code false} without performing
     * bounds checking on the index.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}
     * without checking the value of {@code imageIndex}.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be replaced.
     *
     * @return {@code true} if the image metadata of the given
     * image can be replaced.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IndexOutOfBoundsException if the writer supports
     * image metadata replacement in general, but
     * {@code imageIndex} is less than 0 or greater than the
     * largest available index.
     * @exception IOException if an I/O error occurs during the query.
     */
    public boolean canReplaceImageMetadata(int imageIndex)
        throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Replaces the image metadata associated with an existing image.
     *
     * <p> If {@code canReplaceImageMetadata(imageIndex)} returns
     * {@code false}, an
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be replaced.
     * @param imageMetadata an {@code IIOMetadata} object
     * representing image metadata, or {@code null}.
     *
     * @exception IllegalStateException if the output has not been
     * set.
     * @exception UnsupportedOperationException if
     * {@code canReplaceImageMetadata} returns
     * {@code false}.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is less than 0 or greater than the largest available index.
     * @exception IOException if an error occurs during writing.
     */
    public void replaceImageMetadata(int imageIndex,
                                     IIOMetadata imageMetadata)
        throws IOException {
        unsupported();
    }

    // Image insertion

    /**
     * Returns {@code true} if the writer supports the insertion
     * of a new image at the given index.  Existing images with
     * indices greater than or equal to the insertion index will have
     * their indices increased by 1.  A value for
     * {@code imageIndex} of {@code -1} may be used to
     * signify an index one larger than the current largest index.
     *
     * <p> A writer that does not support any image insertion may
     * return {@code false} without performing bounds checking on
     * the index.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}
     * without checking the value of {@code imageIndex}.
     *
     * @param imageIndex the index at which the image is to be
     * inserted.
     *
     * @return {@code true} if an image may be inserted at the
     * given index.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IndexOutOfBoundsException if the writer supports
     * image insertion in general, but {@code imageIndex} is less
     * than -1 or greater than the largest available index.
     * @exception IOException if an I/O error occurs during the query.
     */
    public boolean canInsertImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Inserts a new image into an existing image stream.  Existing
     * images with an index greater than {@code imageIndex} are
     * preserved, and their indices are each increased by 1.  A value
     * for {@code imageIndex} of -1 may be used to signify an
     * index one larger than the previous largest index; that is, it
     * will cause the image to be logically appended to the end of the
     * sequence.  If the output is an {@code ImageOutputStream},
     * the entirety of the stream must be both readable and writeable.
     *
     * <p> If {@code canInsertImage(imageIndex)} returns
     * {@code false}, an
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index at which to write the image.
     * @param image an {@code IIOImage} object containing an
     * image, thumbnails, and metadata to be written.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canInsertImage(imageIndex)} returns {@code false}.
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is less than -1 or greater than the largest available index.
     * @exception UnsupportedOperationException if {@code image}
     * contains a {@code Raster} and {@code canWriteRasters}
     * returns {@code false}.
     * @exception IOException if an error occurs during writing.
     */
    public void writeInsert(int imageIndex,
                            IIOImage image,
                            ImageWriteParam param) throws IOException {
        unsupported();
    }

    // Image removal

    /**
     * Returns {@code true} if the writer supports the removal
     * of an existing image at the given index.  Existing images with
     * indices greater than the insertion index will have
     * their indices decreased by 1.
     *
     * <p> A writer that does not support any image removal may
     * return {@code false} without performing bounds checking on
     * the index.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}
     * without checking the value of {@code imageIndex}.
     *
     * @param imageIndex the index of the image to be removed.
     *
     * @return {@code true} if it is possible to remove the given
     * image.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception IndexOutOfBoundsException if the writer supports
     * image removal in general, but {@code imageIndex} is less
     * than 0 or greater than the largest available index.
     * @exception IOException if an I/O error occurs during the
     * query.
     */
    public boolean canRemoveImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Removes an image from the stream.
     *
     * <p> If {@code canRemoveImage(imageIndex)} returns false,
     * an {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The removal may or may not cause a reduction in the actual
     * file size.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index of the image to be removed.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canRemoveImage(imageIndex)} returns {@code false}.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is less than 0 or greater than the largest available index.
     * @exception IOException if an I/O error occurs during the
     * removal.
     */
    public void removeImage(int imageIndex) throws IOException {
        unsupported();
    }

    // Empty images

    /**
     * Returns {@code true} if the writer supports the writing of
     * a complete image stream consisting of a single image with
     * undefined pixel values and associated metadata and thumbnails
     * to the output.  The pixel values may be defined by future
     * calls to the {@code replacePixels} methods.  If the output
     * is an {@code ImageOutputStream}, its existing contents
     * prior to the current seek position are not affected, and need
     * not be readable or writable.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}.
     *
     * @return {@code true} if the writing of complete image
     * stream with contents to be defined later is supported.
     *
     * @exception IllegalStateException if the output has not been
     * set.
     * @exception IOException if an I/O error occurs during the
     * query.
     */
    public boolean canWriteEmpty() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Begins the writing of a complete image stream, consisting of a
     * single image with undefined pixel values and associated
     * metadata and thumbnails, to the output.  The pixel values will
     * be defined by future calls to the {@code replacePixels}
     * methods.  If the output is an {@code ImageOutputStream},
     * its existing contents prior to the current seek position are
     * not affected, and need not be readable or writable.
     *
     * <p> The writing is not complete until a call to
     * {@code endWriteEmpty} occurs.  Calls to
     * {@code prepareReplacePixels}, {@code replacePixels},
     * and {@code endReplacePixels} may occur between calls to
     * {@code prepareWriteEmpty} and {@code endWriteEmpty}.
     * However, calls to {@code prepareWriteEmpty} cannot be
     * nested, and calls to {@code prepareWriteEmpty} and
     * {@code prepareInsertEmpty} may not be interspersed.
     *
     * <p> If {@code canWriteEmpty} returns {@code false},
     * an {@code UnsupportedOperationException} will be thrown.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param streamMetadata an {@code IIOMetadata} object representing
     * stream metadata, or {@code null} to use default values.
     * @param imageType an {@code ImageTypeSpecifier} describing
     * the layout of the image.
     * @param width the width of the image.
     * @param height the height of the image.
     * @param imageMetadata an {@code IIOMetadata} object
     * representing image metadata, or {@code null}.
     * @param thumbnails a {@code List} of
     * {@code BufferedImage} thumbnails for this image, or
     * {@code null}.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canWriteEmpty} returns {@code false}.
     * @exception IllegalStateException if a previous call to
     * {@code prepareWriteEmpty} has been made without a
     * corresponding call to {@code endWriteEmpty}.
     * @exception IllegalStateException if a previous call to
     * {@code prepareInsertEmpty} has been made without a
     * corresponding call to {@code endInsertEmpty}.
     * @exception IllegalArgumentException if {@code imageType}
     * is {@code null} or {@code thumbnails} contains
     * {@code null} references or objects other than
     * {@code BufferedImage}s.
     * @exception IllegalArgumentException if width or height are less
     * than 1.
     * @exception IOException if an I/O error occurs during writing.
     */
    public void prepareWriteEmpty(IIOMetadata streamMetadata,
                                  ImageTypeSpecifier imageType,
                                  int width, int height,
                                  IIOMetadata imageMetadata,
                                  List<? extends BufferedImage> thumbnails,
                                  ImageWriteParam param) throws IOException {
        unsupported();
    }

    /**
     * Completes the writing of a new image that was begun with a
     * prior call to {@code prepareWriteEmpty}.
     *
     * <p> If {@code canWriteEmpty()} returns {@code false},
     * an {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canWriteEmpty(imageIndex)} returns
     * {@code false}.
     * @exception IllegalStateException if a previous call to
     * {@code prepareWriteEmpty} without a corresponding call to
     * {@code endWriteEmpty} has not been made.
     * @exception IllegalStateException if a previous call to
     * {@code prepareInsertEmpty} without a corresponding call to
     * {@code endInsertEmpty} has been made.
     * @exception IllegalStateException if a call to
     * {@code prepareReiplacePixels} has been made without a
     * matching call to {@code endReplacePixels}.
     * @exception IOException if an I/O error occurs during writing.
     */
    public void endWriteEmpty() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        throw new IllegalStateException("No call to prepareWriteEmpty!");
    }

    /**
     * Returns {@code true} if the writer supports the insertion
     * of a new, empty image at the given index.  The pixel values of
     * the image are undefined, and may be specified in pieces using
     * the {@code replacePixels} methods.  Existing images with
     * indices greater than or equal to the insertion index will have
     * their indices increased by 1.  A value for
     * {@code imageIndex} of {@code -1} may be used to
     * signify an index one larger than the current largest index.
     *
     * <p> A writer that does not support insertion of empty images
     * may return {@code false} without performing bounds
     * checking on the index.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}
     * without checking the value of {@code imageIndex}.
     *
     * @param imageIndex the index at which the image is to be
     * inserted.
     *
     * @return {@code true} if an empty image may be inserted at
     * the given index.
     *
     * @exception IllegalStateException if the output has not been
     * set.
     * @exception IndexOutOfBoundsException if the writer supports
     * empty image insertion in general, but {@code imageIndex}
     * is less than -1 or greater than the largest available index.
     * @exception IOException if an I/O error occurs during the
     * query.
     */
    public boolean canInsertEmpty(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Begins the insertion of a new image with undefined pixel values
     * into an existing image stream.  Existing images with an index
     * greater than {@code imageIndex} are preserved, and their
     * indices are each increased by 1.  A value for
     * {@code imageIndex} of -1 may be used to signify an index
     * one larger than the previous largest index; that is, it will
     * cause the image to be logically appended to the end of the
     * sequence.  If the output is an {@code ImageOutputStream},
     * the entirety of the stream must be both readable and writeable.
     *
     * <p> The image contents may be
     * supplied later using the {@code replacePixels} method.
     * The insertion is not complete until a call to
     * {@code endInsertEmpty} occurs.  Calls to
     * {@code prepareReplacePixels}, {@code replacePixels},
     * and {@code endReplacePixels} may occur between calls to
     * {@code prepareInsertEmpty} and
     * {@code endInsertEmpty}.  However, calls to
     * {@code prepareInsertEmpty} cannot be nested, and calls to
     * {@code prepareWriteEmpty} and
     * {@code prepareInsertEmpty} may not be interspersed.
     *
     * <p> If {@code canInsertEmpty(imageIndex)} returns
     * {@code false}, an
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index at which to write the image.
     * @param imageType an {@code ImageTypeSpecifier} describing
     * the layout of the image.
     * @param width the width of the image.
     * @param height the height of the image.
     * @param imageMetadata an {@code IIOMetadata} object
     * representing image metadata, or {@code null}.
     * @param thumbnails a {@code List} of
     * {@code BufferedImage} thumbnails for this image, or
     * {@code null}.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canInsertEmpty(imageIndex)} returns
     * {@code false}.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is less than -1 or greater than the largest available index.
     * @exception IllegalStateException if a previous call to
     * {@code prepareInsertEmpty} has been made without a
     * corresponding call to {@code endInsertEmpty}.
     * @exception IllegalStateException if a previous call to
     * {@code prepareWriteEmpty} has been made without a
     * corresponding call to {@code endWriteEmpty}.
     * @exception IllegalArgumentException if {@code imageType}
     * is {@code null} or {@code thumbnails} contains
     * {@code null} references or objects other than
     * {@code BufferedImage}s.
     * @exception IllegalArgumentException if width or height are less
     * than 1.
     * @exception IOException if an I/O error occurs during writing.
     */
    public void prepareInsertEmpty(int imageIndex,
                                   ImageTypeSpecifier imageType,
                                   int width, int height,
                                   IIOMetadata imageMetadata,
                                   List<? extends BufferedImage> thumbnails,
                                   ImageWriteParam param) throws IOException {
        unsupported();
    }

    /**
     * Completes the insertion of a new image that was begun with a
     * prior call to {@code prepareInsertEmpty}.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canInsertEmpty(imageIndex)} returns
     * {@code false}.
     * @exception IllegalStateException if a previous call to
     * {@code prepareInsertEmpty} without a corresponding call to
     * {@code endInsertEmpty} has not been made.
     * @exception IllegalStateException if a previous call to
     * {@code prepareWriteEmpty} without a corresponding call to
     * {@code endWriteEmpty} has been made.
     * @exception IllegalStateException if a call to
     * {@code prepareReplacePixels} has been made without a
     * matching call to {@code endReplacePixels}.
     * @exception IOException if an I/O error occurs during writing.
     */
    public void endInsertEmpty() throws IOException {
        unsupported();
    }

    // Pixel replacement

    /**
     * Returns {@code true} if the writer allows pixels of the
     * given image to be replaced using the {@code replacePixels}
     * methods.
     *
     * <p> A writer that does not support any pixel replacement may
     * return {@code false} without performing bounds checking on
     * the index.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise returns {@code false}
     * without checking the value of {@code imageIndex}.
     *
     * @param imageIndex the index of the image whose pixels are to be
     * replaced.
     *
     * @return {@code true} if the pixels of the given
     * image can be replaced.
     *
     * @exception IllegalStateException if the output has not been
     * set.
     * @exception IndexOutOfBoundsException if the writer supports
     * pixel replacement in general, but {@code imageIndex} is
     * less than 0 or greater than the largest available index.
     * @exception IOException if an I/O error occurs during the query.
     */
    public boolean canReplacePixels(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Prepares the writer to handle a series of calls to the
     * {@code replacePixels} methods.  The affected pixel area
     * will be clipped against the supplied
     *
     * <p> If {@code canReplacePixels} returns
     * {@code false}, and
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index of the image whose pixels are to be
     * replaced.
     * @param region a {@code Rectangle} that will be used to clip
     * future pixel regions.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canReplacePixels(imageIndex)} returns
     * {@code false}.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is less than 0 or greater than the largest available index.
     * @exception IllegalStateException if there is a previous call to
     * {@code prepareReplacePixels} without a matching call to
     * {@code endReplacePixels} (<i>i.e.</i>, nesting is not
     * allowed).
     * @exception IllegalArgumentException if {@code region} is
     * {@code null} or has a width or height less than 1.
     * @exception IOException if an I/O error occurs during the
     * preparation.
     */
    public void prepareReplacePixels(int imageIndex,
                                     Rectangle region)  throws IOException {
        unsupported();
    }

    /**
     * Replaces a portion of an image already present in the output
     * with a portion of the given image.  The image data must match,
     * or be convertible to, the image layout of the existing image.
     *
     * <p> The destination region is specified in the
     * {@code param} argument, and will be clipped to the image
     * boundaries and the region supplied to
     * {@code prepareReplacePixels}.  At least one pixel of the
     * source must not be clipped, or an exception is thrown.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> This method may only be called after a call to
     * {@code prepareReplacePixels}, or else an
     * {@code IllegalStateException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param image a {@code RenderedImage} containing source
     * pixels.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canReplacePixels(imageIndex)} returns
     * {@code false}.
     * @exception IllegalStateException if there is no previous call to
     * {@code prepareReplacePixels} without a matching call to
     * {@code endReplacePixels}.
     * @exception IllegalArgumentException if any of the following are true:
     * <ul>
     * <li> {@code image} is {@code null}.
     * <li> the intersected region does not contain at least one pixel.
     * <li> the layout of {@code image} does not match, or this
     * writer cannot convert it to, the existing image layout.
     * </ul>
     * @exception IOException if an I/O error occurs during writing.
     */
    public void replacePixels(RenderedImage image, ImageWriteParam param)
        throws IOException {
        unsupported();
    }

    /**
     * Replaces a portion of an image already present in the output
     * with a portion of the given {@code Raster}.  The image
     * data must match, or be convertible to, the image layout of the
     * existing image.
     *
     * <p> An {@code ImageWriteParam} may optionally be supplied
     * to control the writing process.  If {@code param} is
     * {@code null}, a default write param will be used.
     *
     * <p> The destination region is specified in the
     * {@code param} argument, and will be clipped to the image
     * boundaries and the region supplied to
     * {@code prepareReplacePixels}.  At least one pixel of the
     * source must not be clipped, or an exception is thrown.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not supported by this writer (<i>e.g.</i>
     * progressive encoding or any format-specific settings), they
     * will be ignored.
     *
     * <p> This method may only be called after a call to
     * {@code prepareReplacePixels}, or else an
     * {@code IllegalStateException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @param raster a {@code Raster} containing source
     * pixels.
     * @param param an {@code ImageWriteParam}, or
     * {@code null} to use a default
     * {@code ImageWriteParam}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canReplacePixels(imageIndex)} returns
     * {@code false}.
     * @exception IllegalStateException if there is no previous call to
     * {@code prepareReplacePixels} without a matching call to
     * {@code endReplacePixels}.
     * @exception UnsupportedOperationException if
     * {@code canWriteRasters} returns {@code false}.
     * @exception IllegalArgumentException if any of the following are true:
     * <ul>
     * <li> {@code raster} is {@code null}.
     * <li> the intersected region does not contain at least one pixel.
     * <li> the layout of {@code raster} does not match, or this
     * writer cannot convert it to, the existing image layout.
     * </ul>
     * @exception IOException if an I/O error occurs during writing.
     */
    public void replacePixels(Raster raster, ImageWriteParam param)
        throws IOException {
        unsupported();
    }

    /**
     * Terminates a sequence of calls to {@code replacePixels}.
     *
     * <p> If {@code canReplacePixels} returns
     * {@code false}, and
     * {@code UnsupportedOperationException} will be thrown.
     *
     * <p> The default implementation throws an
     * {@code IllegalStateException} if the output is
     * {@code null}, and otherwise throws an
     * {@code UnsupportedOperationException}.
     *
     * @exception IllegalStateException if the output has not
     * been set.
     * @exception UnsupportedOperationException if
     * {@code canReplacePixels(imageIndex)} returns
     * {@code false}.
     * @exception IllegalStateException if there is no previous call
     * to {@code prepareReplacePixels} without a matching call to
     * {@code endReplacePixels}.
     * @exception IOException if an I/O error occurs during writing.
     */
    public void endReplacePixels() throws IOException {
        unsupported();
    }

    // Abort

    /**
     * Requests that any current write operation be aborted.  The
     * contents of the output following the abort will be undefined.
     *
     * <p> Writers should call {@code clearAbortRequest} at the
     * beginning of each write operation, and poll the value of
     * {@code abortRequested} regularly during the write.
     */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /**
     * Returns {@code true} if a request to abort the current
     * write operation has been made since the writer was instantiated or
     * {@code clearAbortRequest} was called.
     *
     * @return {@code true} if the current write operation should
     * be aborted.
     *
     * @see #abort
     * @see #clearAbortRequest
     */
    protected synchronized boolean abortRequested() {
        return this.abortFlag;
    }

    /**
     * Clears any previous abort request.  After this method has been
     * called, {@code abortRequested} will return
     * {@code false}.
     *
     * @see #abort
     * @see #abortRequested
     */
    protected synchronized void clearAbortRequest() {
        this.abortFlag = false;
    }

    // Listeners

    /**
     * Adds an {@code IIOWriteWarningListener} to the list of
     * registered warning listeners.  If {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.  Messages sent to the given listener will be
     * localized, if possible, to match the current
     * {@code Locale}.  If no {@code Locale} has been set,
     * warning messages may be localized as the writer sees fit.
     *
     * @param listener an {@code IIOWriteWarningListener} to be
     * registered.
     *
     * @see #removeIIOWriteWarningListener
     */
    public void addIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null) {
            return;
        }
        warningListeners = ImageReader.addToList(warningListeners, listener);
        warningLocales = ImageReader.addToList(warningLocales, getLocale());
    }

    /**
     * Removes an {@code IIOWriteWarningListener} from the list
     * of registered warning listeners.  If the listener was not
     * previously registered, or if {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an {@code IIOWriteWarningListener} to be
     * deregistered.
     *
     * @see #addIIOWriteWarningListener
     */
    public
        void removeIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null || warningListeners == null) {
            return;
        }
        int index = warningListeners.indexOf(listener);
        if (index != -1) {
            warningListeners.remove(index);
            warningLocales.remove(index);
            if (warningListeners.size() == 0) {
                warningListeners = null;
                warningLocales = null;
            }
        }
    }

    /**
     * Removes all currently registered
     * {@code IIOWriteWarningListener} objects.
     *
     * <p> The default implementation sets the
     * {@code warningListeners} and {@code warningLocales}
     * instance variables to {@code null}.
     */
    public void removeAllIIOWriteWarningListeners() {
        this.warningListeners = null;
        this.warningLocales = null;
    }

    /**
     * Adds an {@code IIOWriteProgressListener} to the list of
     * registered progress listeners.  If {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an {@code IIOWriteProgressListener} to be
     * registered.
     *
     * @see #removeIIOWriteProgressListener
     */
    public void
        addIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null) {
            return;
        }
        progressListeners = ImageReader.addToList(progressListeners, listener);
    }

    /**
     * Removes an {@code IIOWriteProgressListener} from the list
     * of registered progress listeners.  If the listener was not
     * previously registered, or if {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an {@code IIOWriteProgressListener} to be
     * deregistered.
     *
     * @see #addIIOWriteProgressListener
     */
    public void
        removeIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null || progressListeners == null) {
            return;
        }
        progressListeners =
            ImageReader.removeFromList(progressListeners, listener);
    }

    /**
     * Removes all currently registered
     * {@code IIOWriteProgressListener} objects.
     *
     * <p> The default implementation sets the
     * {@code progressListeners} instance variable to
     * {@code null}.
     */
    public void removeAllIIOWriteProgressListeners() {
        this.progressListeners = null;
    }

    /**
     * Broadcasts the start of an image write to all registered
     * {@code IIOWriteProgressListener}s by calling their
     * {@code imageStarted} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param imageIndex the index of the image about to be written.
     */
    protected void processImageStarted(int imageIndex) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.imageStarted(this, imageIndex);
        }
    }

    /**
     * Broadcasts the current percentage of image completion to all
     * registered {@code IIOWriteProgressListener}s by calling
     * their {@code imageProgress} method.  Subclasses may use
     * this method as a convenience.
     *
     * @param percentageDone the current percentage of completion,
     * as a {@code float}.
     */
    protected void processImageProgress(float percentageDone) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.imageProgress(this, percentageDone);
        }
    }

    /**
     * Broadcasts the completion of an image write to all registered
     * {@code IIOWriteProgressListener}s by calling their
     * {@code imageComplete} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processImageComplete() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.imageComplete(this);
        }
    }

    /**
     * Broadcasts the start of a thumbnail write to all registered
     * {@code IIOWriteProgressListener}s by calling their
     * {@code thumbnailStarted} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param imageIndex the index of the image associated with the
     * thumbnail.
     * @param thumbnailIndex the index of the thumbnail.
     */
    protected void processThumbnailStarted(int imageIndex,
                                           int thumbnailIndex) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailStarted(this, imageIndex, thumbnailIndex);
        }
    }

    /**
     * Broadcasts the current percentage of thumbnail completion to
     * all registered {@code IIOWriteProgressListener}s by calling
     * their {@code thumbnailProgress} method.  Subclasses may
     * use this method as a convenience.
     *
     * @param percentageDone the current percentage of completion,
     * as a {@code float}.
     */
    protected void processThumbnailProgress(float percentageDone) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailProgress(this, percentageDone);
        }
    }

    /**
     * Broadcasts the completion of a thumbnail write to all registered
     * {@code IIOWriteProgressListener}s by calling their
     * {@code thumbnailComplete} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processThumbnailComplete() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailComplete(this);
        }
    }

    /**
     * Broadcasts that the write has been aborted to all registered
     * {@code IIOWriteProgressListener}s by calling their
     * {@code writeAborted} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processWriteAborted() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteProgressListener listener =
                progressListeners.get(i);
            listener.writeAborted(this);
        }
    }

    /**
     * Broadcasts a warning message to all registered
     * {@code IIOWriteWarningListener}s by calling their
     * {@code warningOccurred} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param imageIndex the index of the image on which the warning
     * occurred.
     * @param warning the warning message.
     *
     * @exception IllegalArgumentException if {@code warning}
     * is {@code null}.
     */
    protected void processWarningOccurred(int imageIndex,
                                          String warning) {
        if (warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int numListeners = warningListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteWarningListener listener =
                warningListeners.get(i);

            listener.warningOccurred(this, imageIndex, warning);
        }
    }

    /**
     * Broadcasts a localized warning message to all registered
     * {@code IIOWriteWarningListener}s by calling their
     * {@code warningOccurred} method with a string taken
     * from a {@code ResourceBundle}.  Subclasses may use this
     * method as a convenience.
     *
     * @param imageIndex the index of the image on which the warning
     * occurred.
     * @param baseName the base name of a set of
     * {@code ResourceBundle}s containing localized warning
     * messages.
     * @param keyword the keyword used to index the warning message
     * within the set of {@code ResourceBundle}s.
     *
     * @exception IllegalArgumentException if {@code baseName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code keyword}
     * is {@code null}.
     * @exception IllegalArgumentException if no appropriate
     * {@code ResourceBundle} may be located.
     * @exception IllegalArgumentException if the named resource is
     * not found in the located {@code ResourceBundle}.
     * @exception IllegalArgumentException if the object retrieved
     * from the {@code ResourceBundle} is not a
     * {@code String}.
     */
    protected void processWarningOccurred(int imageIndex,
                                          String baseName,
                                          String keyword) {
        if (warningListeners == null) {
            return;
        }
        if (baseName == null) {
            throw new IllegalArgumentException("baseName == null!");
        }
        if (keyword == null) {
            throw new IllegalArgumentException("keyword == null!");
        }
        int numListeners = warningListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOWriteWarningListener listener =
                warningListeners.get(i);
            Locale locale = warningLocales.get(i);
            if (locale == null) {
                locale = Locale.getDefault();
            }

            /*
             * Only the plugin knows the messages that are provided, so we
             * can always locate the resource bundles from the same loader
             * as that for the plugin code itself.
             */
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(baseName, locale, this.getClass().getModule());
            } catch (MissingResourceException mre) {
                throw new IllegalArgumentException("Bundle not found!", mre);
            }

            String warning = null;
            try {
                warning = bundle.getString(keyword);
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("Resource is not a String!", cce);
            } catch (MissingResourceException mre) {
                throw new IllegalArgumentException("Resource is missing!", mre);
            }

            listener.warningOccurred(this, imageIndex, warning);
        }
    }

    // State management

    /**
     * Restores the {@code ImageWriter} to its initial state.
     *
     * <p> The default implementation calls
     * {@code setOutput(null)}, {@code setLocale(null)},
     * {@code removeAllIIOWriteWarningListeners()},
     * {@code removeAllIIOWriteProgressListeners()}, and
     * {@code clearAbortRequest}.
     */
    public void reset() {
        setOutput(null);
        setLocale(null);
        removeAllIIOWriteWarningListeners();
        removeAllIIOWriteProgressListeners();
        clearAbortRequest();
    }

    /**
     * Allows any resources held by this object to be released.  The
     * result of calling any other method (other than
     * {@code finalize}) subsequent to a call to this method
     * is undefined.
     *
     * <p>It is important for applications to call this method when they
     * know they will no longer be using this {@code ImageWriter}.
     * Otherwise, the writer may continue to hold on to resources
     * indefinitely.
     *
     * <p>The default implementation of this method in the superclass does
     * nothing.  Subclass implementations should ensure that all resources,
     * especially native resources, are released.
     */
    public void dispose() {
    }
}
