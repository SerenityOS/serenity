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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.ImageInputStream;

/**
 * An abstract superclass for parsing and decoding of images.  This
 * class must be subclassed by classes that read in images in the
 * context of the Java Image I/O framework.
 *
 * <p> {@code ImageReader} objects are normally instantiated by
 * the service provider interface (SPI) class for the specific format.
 * Service provider classes (e.g., instances of
 * {@code ImageReaderSpi}) are registered with the
 * {@code IIORegistry}, which uses them for format recognition
 * and presentation of available format readers and writers.
 *
 * <p> When an input source is set (using the {@code setInput}
 * method), it may be marked as "seek forward only".  This setting
 * means that images contained within the input source will only be
 * read in order, possibly allowing the reader to avoid caching
 * portions of the input containing data associated with images that
 * have been read previously.
 *
 * @see ImageWriter
 * @see javax.imageio.spi.IIORegistry
 * @see javax.imageio.spi.ImageReaderSpi
 *
 */
public abstract class ImageReader {

    /**
     * The {@code ImageReaderSpi} that instantiated this object,
     * or {@code null} if its identity is not known or none
     * exists.  By default it is initialized to {@code null}.
     */
    protected ImageReaderSpi originatingProvider;

    /**
     * The {@code ImageInputStream} or other
     * {@code Object} by {@code setInput} and retrieved
     * by {@code getInput}.  By default it is initialized to
     * {@code null}.
     */
    protected Object input = null;

    /**
     * {@code true} if the current input source has been marked
     * as allowing only forward seeking by {@code setInput}.  By
     * default, the value is {@code false}.
     *
     * @see #minIndex
     * @see #setInput
     */
    protected boolean seekForwardOnly = false;

    /**
     * {@code true} if the current input source has been marked
     * as allowing metadata to be ignored by {@code setInput}.
     * By default, the value is {@code false}.
     *
     * @see #setInput
     */
    protected boolean ignoreMetadata = false;

    /**
     * The smallest valid index for reading, initially 0.  When
     * {@code seekForwardOnly} is {@code true}, various methods
     * may throw an {@code IndexOutOfBoundsException} on an
     * attempt to access data associate with an image having a lower
     * index.
     *
     * @see #seekForwardOnly
     * @see #setInput
     */
    protected int minIndex = 0;

    /**
     * An array of {@code Locale}s which may be used to localize
     * warning messages, or {@code null} if localization is not
     * supported.
     */
    protected Locale[] availableLocales = null;

    /**
     * The current {@code Locale} to be used for localization, or
     * {@code null} if none has been set.
     */
    protected Locale locale = null;

    /**
     * A {@code List} of currently registered
     * {@code IIOReadWarningListener}s, initialized by default to
     * {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<IIOReadWarningListener> warningListeners = null;

    /**
     * A {@code List} of the {@code Locale}s associated with
     * each currently registered {@code IIOReadWarningListener},
     * initialized by default to {@code null}, which is
     * synonymous with an empty {@code List}.
     */
    protected List<Locale> warningLocales = null;

    /**
     * A {@code List} of currently registered
     * {@code IIOReadProgressListener}s, initialized by default
     * to {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<IIOReadProgressListener> progressListeners = null;

    /**
     * A {@code List} of currently registered
     * {@code IIOReadUpdateListener}s, initialized by default to
     * {@code null}, which is synonymous with an empty
     * {@code List}.
     */
    protected List<IIOReadUpdateListener> updateListeners = null;

    /**
     * If {@code true}, the current read operation should be
     * aborted.
     */
    private boolean abortFlag = false;

    /**
     * Constructs an {@code ImageReader} and sets its
     * {@code originatingProvider} field to the supplied value.
     *
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature {@code (ImageReaderSpi,Object)}
     * in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * {@code IllegalArgumentException} should be thrown.
     *
     * @param originatingProvider the {@code ImageReaderSpi} that is
     * invoking this constructor, or {@code null}.
     */
    protected ImageReader(ImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /**
     * Returns a {@code String} identifying the format of the
     * input source.
     *
     * <p> The default implementation returns
     * {@code originatingProvider.getFormatNames()[0]}.
     * Implementations that may not have an originating service
     * provider, or which desire a different naming policy should
     * override this method.
     *
     * @exception IOException if an error occurs reading the
     * information from the input source.
     *
     * @return the format name, as a {@code String}.
     */
    public String getFormatName() throws IOException {
        return originatingProvider.getFormatNames()[0];
    }

    /**
     * Returns the {@code ImageReaderSpi} that was passed in on
     * the constructor.  Note that this value may be {@code null}.
     *
     * @return an {@code ImageReaderSpi}, or {@code null}.
     *
     * @see ImageReaderSpi
     */
    public ImageReaderSpi getOriginatingProvider() {
        return originatingProvider;
    }

    /**
     * Sets the input source to use to the given
     * {@code ImageInputStream} or other {@code Object}.
     * The input source must be set before any of the query or read
     * methods are used.  If {@code input} is {@code null},
     * any currently set input source will be removed.  In any case,
     * the value of {@code minIndex} will be initialized to 0.
     *
     * <p> The {@code seekForwardOnly} parameter controls whether
     * the value returned by {@code getMinIndex} will be
     * increased as each image (or thumbnail, or image metadata) is
     * read.  If {@code seekForwardOnly} is true, then a call to
     * {@code read(index)} will throw an
     * {@code IndexOutOfBoundsException} if {@code index < this.minIndex};
     * otherwise, the value of
     * {@code minIndex} will be set to {@code index}.  If
     * {@code seekForwardOnly} is {@code false}, the value of
     * {@code minIndex} will remain 0 regardless of any read
     * operations.
     *
     * <p> The {@code ignoreMetadata} parameter, if set to
     * {@code true}, allows the reader to disregard any metadata
     * encountered during the read.  Subsequent calls to the
     * {@code getStreamMetadata} and
     * {@code getImageMetadata} methods may return
     * {@code null}, and an {@code IIOImage} returned from
     * {@code readAll} may return {@code null} from their
     * {@code getMetadata} method.  Setting this parameter may
     * allow the reader to work more efficiently.  The reader may
     * choose to disregard this setting and return metadata normally.
     *
     * <p> Subclasses should take care to remove any cached
     * information based on the previous stream, such as header
     * information or partially decoded image data.
     *
     * <p> Use of a general {@code Object} other than an
     * {@code ImageInputStream} is intended for readers that
     * interact directly with a capture device or imaging protocol.
     * The set of legal classes is advertised by the reader's service
     * provider's {@code getInputTypes} method; most readers
     * will return a single-element array containing only
     * {@code ImageInputStream.class} to indicate that they
     * accept only an {@code ImageInputStream}.
     *
     * <p> The default implementation checks the {@code input}
     * argument against the list returned by
     * {@code originatingProvider.getInputTypes()} and fails
     * if the argument is not an instance of one of the classes
     * in the list.  If the originating provider is set to
     * {@code null}, the input is accepted only if it is an
     * {@code ImageInputStream}.
     *
     * @param input the {@code ImageInputStream} or other
     * {@code Object} to use for future decoding.
     * @param seekForwardOnly if {@code true}, images and metadata
     * may only be read in ascending order from this input source.
     * @param ignoreMetadata if {@code true}, metadata
     * may be ignored during reads.
     *
     * @exception IllegalArgumentException if {@code input} is
     * not an instance of one of the classes returned by the
     * originating service provider's {@code getInputTypes}
     * method, or is not an {@code ImageInputStream}.
     *
     * @see ImageInputStream
     * @see #getInput
     * @see javax.imageio.spi.ImageReaderSpi#getInputTypes
     */
    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        if (input != null) {
            boolean found = false;
            if (originatingProvider != null) {
                Class<?>[] classes = originatingProvider.getInputTypes();
                for (int i = 0; i < classes.length; i++) {
                    if (classes[i].isInstance(input)) {
                        found = true;
                        break;
                    }
                }
            } else {
                if (input instanceof ImageInputStream) {
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Incorrect input type!");
            }

            this.seekForwardOnly = seekForwardOnly;
            this.ignoreMetadata = ignoreMetadata;
            this.minIndex = 0;
        }

        this.input = input;
    }

    /**
     * Sets the input source to use to the given
     * {@code ImageInputStream} or other {@code Object}.
     * The input source must be set before any of the query or read
     * methods are used.  If {@code input} is {@code null},
     * any currently set input source will be removed.  In any case,
     * the value of {@code minIndex} will be initialized to 0.
     *
     * <p> The {@code seekForwardOnly} parameter controls whether
     * the value returned by {@code getMinIndex} will be
     * increased as each image (or thumbnail, or image metadata) is
     * read.  If {@code seekForwardOnly} is true, then a call to
     * {@code read(index)} will throw an
     * {@code IndexOutOfBoundsException} if {@code index < this.minIndex};
     * otherwise, the value of
     * {@code minIndex} will be set to {@code index}.  If
     * {@code seekForwardOnly} is {@code false}, the value of
     * {@code minIndex} will remain 0 regardless of any read
     * operations.
     *
     * <p> This method is equivalent to
     * {@code setInput(input, seekForwardOnly, false)}.
     *
     * @param input the {@code ImageInputStream} or other
     * {@code Object} to use for future decoding.
     * @param seekForwardOnly if {@code true}, images and metadata
     * may only be read in ascending order from this input source.
     *
     * @exception IllegalArgumentException if {@code input} is
     * not an instance of one of the classes returned by the
     * originating service provider's {@code getInputTypes}
     * method, or is not an {@code ImageInputStream}.
     *
     * @see #getInput
     */
    public void setInput(Object input,
                         boolean seekForwardOnly) {
        setInput(input, seekForwardOnly, false);
    }

    /**
     * Sets the input source to use to the given
     * {@code ImageInputStream} or other {@code Object}.
     * The input source must be set before any of the query or read
     * methods are used.  If {@code input} is {@code null},
     * any currently set input source will be removed.  In any case,
     * the value of {@code minIndex} will be initialized to 0.
     *
     * <p> This method is equivalent to
     * {@code setInput(input, false, false)}.
     *
     * @param input the {@code ImageInputStream} or other
     * {@code Object} to use for future decoding.
     *
     * @exception IllegalArgumentException if {@code input} is
     * not an instance of one of the classes returned by the
     * originating service provider's {@code getInputTypes}
     * method, or is not an {@code ImageInputStream}.
     *
     * @see #getInput
     */
    public void setInput(Object input) {
        setInput(input, false, false);
    }

    /**
     * Returns the {@code ImageInputStream} or other
     * {@code Object} previously set as the input source.  If the
     * input source has not been set, {@code null} is returned.
     *
     * @return the {@code Object} that will be used for future
     * decoding, or {@code null}.
     *
     * @see ImageInputStream
     * @see #setInput
     */
    public Object getInput() {
        return input;
    }

    /**
     * Returns {@code true} if the current input source has been
     * marked as seek forward only by passing {@code true} as the
     * {@code seekForwardOnly} argument to the
     * {@code setInput} method.
     *
     * @return {@code true} if the input source is seek forward
     * only.
     *
     * @see #setInput
     */
    public boolean isSeekForwardOnly() {
        return seekForwardOnly;
    }

    /**
     * Returns {@code true} if the current input source has been
     * marked as allowing metadata to be ignored by passing
     * {@code true} as the {@code ignoreMetadata} argument
     * to the {@code setInput} method.
     *
     * @return {@code true} if the metadata may be ignored.
     *
     * @see #setInput
     */
    public boolean isIgnoringMetadata() {
        return ignoreMetadata;
    }

    /**
     * Returns the lowest valid index for reading an image, thumbnail,
     * or image metadata.  If {@code seekForwardOnly()} is
     * {@code false}, this value will typically remain 0,
     * indicating that random access is possible.  Otherwise, it will
     * contain the value of the most recently accessed index, and
     * increase in a monotonic fashion.
     *
     * @return the minimum legal index for reading.
     */
    public int getMinIndex() {
        return minIndex;
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
        if (availableLocales == null) {
            return null;
        } else {
            return availableLocales.clone();
        }
    }

    /**
     * Sets the current {@code Locale} of this
     * {@code ImageReader} to the given value.  A value of
     * {@code null} removes any previous setting, and indicates
     * that the reader should localize as it sees fit.
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
     * @return the current {@code Locale}, or {@code null}.
     *
     * @see #setLocale
     */
    public Locale getLocale() {
        return locale;
    }

    // Image queries

    /**
     * Returns the number of images, not including thumbnails, available
     * from the current input source.
     *
     * <p> Note that some image formats (such as animated GIF) do not
     * specify how many images are present in the stream.  Thus
     * determining the number of images will require the entire stream
     * to be scanned and may require memory for buffering.  If images
     * are to be processed in order, it may be more efficient to
     * simply call {@code read} with increasing indices until an
     * {@code IndexOutOfBoundsException} is thrown to indicate
     * that no more images are available.  The
     * {@code allowSearch} parameter may be set to
     * {@code false} to indicate that an exhaustive search is not
     * desired; the return value will be {@code -1} to indicate
     * that a search is necessary.  If the input has been specified
     * with {@code seekForwardOnly} set to {@code true},
     * this method throws an {@code IllegalStateException} if
     * {@code allowSearch} is set to {@code true}.
     *
     * @param allowSearch if {@code true}, the true number of
     * images will be returned even if a search is required.  If
     * {@code false}, the reader may return {@code -1}
     * without performing the search.
     *
     * @return the number of images, as an {@code int}, or
     * {@code -1} if {@code allowSearch} is
     * {@code false} and a search would be required.
     *
     * @exception IllegalStateException if the input source has not been set,
     * or if the input has been specified with {@code seekForwardOnly}
     * set to {@code true}.
     * @exception IOException if an error occurs reading the
     * information from the input source.
     *
     * @see #setInput
     */
    public abstract int getNumImages(boolean allowSearch) throws IOException;

    /**
     * Returns the width in pixels of the given image within the input
     * source.
     *
     * <p> If the image can be rendered to a user-specified size, then
     * this method returns the default width.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return the width of the image, as an {@code int}.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the width
     * information from the input source.
     */
    public abstract int getWidth(int imageIndex) throws IOException;

    /**
     * Returns the height in pixels of the given image within the
     * input source.
     *
     * <p> If the image can be rendered to a user-specified size, then
     * this method returns the default height.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return the height of the image, as an {@code int}.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the height
     * information from the input source.
     */
    public abstract int getHeight(int imageIndex) throws IOException;

    /**
     * Returns {@code true} if the storage format of the given
     * image places no inherent impediment on random access to pixels.
     * For most compressed formats, such as JPEG, this method should
     * return {@code false}, as a large section of the image in
     * addition to the region of interest may need to be decoded.
     *
     * <p> This is merely a hint for programs that wish to be
     * efficient; all readers must be able to read arbitrary regions
     * as specified in an {@code ImageReadParam}.
     *
     * <p> Note that formats that return {@code false} from
     * this method may nonetheless allow tiling (<i>e.g.</i> Restart
     * Markers in JPEG), and random access will likely be reasonably
     * efficient on tiles.  See {@link #isImageTiled isImageTiled}.
     *
     * <p> A reader for which all images are guaranteed to support
     * easy random access, or are guaranteed not to support easy
     * random access, may return {@code true} or
     * {@code false} respectively without accessing any image
     * data.  In such cases, it is not necessary to throw an exception
     * even if no input source has been set or the image index is out
     * of bounds.
     *
     * <p> The default implementation returns {@code false}.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return {@code true} if reading a region of interest of
     * the given image is likely to be efficient.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return false;
    }

    /**
     * Returns the aspect ratio of the given image (that is, its width
     * divided by its height) as a {@code float}.  For images
     * that are inherently resizable, this method provides a way to
     * determine the appropriate width given a desired height, or vice
     * versa.  For non-resizable images, the true width and height
     * are used.
     *
     * <p> The default implementation simply returns
     * {@code (float)getWidth(imageIndex)/getHeight(imageIndex)}.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return a {@code float} indicating the aspect ratio of the
     * given image.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public float getAspectRatio(int imageIndex) throws IOException {
        return (float)getWidth(imageIndex)/getHeight(imageIndex);
    }

    /**
     * Returns an <code>ImageTypeSpecifier</code> indicating the
     * <code>SampleModel</code> and <code>ColorModel</code> which most
     * closely represents the "raw" internal format of the image.  If
     * there is no close match then a type which preserves the most
     * information from the image should be returned.  The returned value
     * should also be included in the list of values returned by
     * {@code getImageTypes}.
     *
     * <p> The default implementation simply returns the first entry
     * from the list provided by {@code getImageType}.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return an {@code ImageTypeSpecifier}.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the format
     * information from the input source.
     */
    public ImageTypeSpecifier getRawImageType(int imageIndex)
        throws IOException {
        return getImageTypes(imageIndex).next();
    }

    /**
     * Returns an {@code Iterator} containing possible image
     * types to which the given image may be decoded, in the form of
     * {@code ImageTypeSpecifiers}s.  At least one legal image
     * type will be returned.
     *
     * <p> The first element of the iterator should be the most
     * "natural" type for decoding the image with as little loss as
     * possible.  For example, for a JPEG image the first entry should
     * be an RGB image, even though the image data is stored
     * internally in a YCbCr color space.
     *
     * @param imageIndex the index of the image to be
     * {@code retrieved}.
     *
     * @return an {@code Iterator} containing at least one
     * {@code ImageTypeSpecifier} representing suggested image
     * types for decoding the current given image.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the format
     * information from the input source.
     *
     * @see ImageReadParam#setDestination(BufferedImage)
     * @see ImageReadParam#setDestinationType(ImageTypeSpecifier)
     */
    public abstract Iterator<ImageTypeSpecifier>
        getImageTypes(int imageIndex) throws IOException;

    /**
     * Returns a default {@code ImageReadParam} object
     * appropriate for this format.  All subclasses should define a
     * set of default values for all parameters and return them with
     * this call.  This method may be called before the input source
     * is set.
     *
     * <p> The default implementation constructs and returns a new
     * {@code ImageReadParam} object that does not allow source
     * scaling (<i>i.e.</i>, it returns
     * {@code new ImageReadParam()}.
     *
     * @return an {@code ImageReadParam} object which may be used
     * to control the decoding process using a set of default settings.
     */
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    /**
     * Returns an {@code IIOMetadata} object representing the
     * metadata associated with the input source as a whole (i.e., not
     * associated with any particular image), or {@code null} if
     * the reader does not support reading metadata, is set to ignore
     * metadata, or if no metadata is available.
     *
     * @return an {@code IIOMetadata} object, or {@code null}.
     *
     * @exception IOException if an error occurs during reading.
     */
    public abstract IIOMetadata getStreamMetadata() throws IOException;

    /**
     * Returns an {@code IIOMetadata} object representing the
     * metadata associated with the input source as a whole (i.e.,
     * not associated with any particular image).  If no such data
     * exists, {@code null} is returned.
     *
     * <p> The resulting metadata object is only responsible for
     * returning documents in the format named by
     * {@code formatName}.  Within any documents that are
     * returned, only nodes whose names are members of
     * {@code nodeNames} are required to be returned.  In this
     * way, the amount of metadata processing done by the reader may
     * be kept to a minimum, based on what information is actually
     * needed.
     *
     * <p> If {@code formatName} is not the name of a supported
     * metadata format, {@code null} is returned.
     *
     * <p> In all cases, it is legal to return a more capable metadata
     * object than strictly necessary.  The format name and node names
     * are merely hints that may be used to reduce the reader's
     * workload.
     *
     * <p> The default implementation simply returns the result of
     * calling {@code getStreamMetadata()}, after checking that
     * the format name is supported.  If it is not,
     * {@code null} is returned.
     *
     * @param formatName a metadata format name that may be used to retrieve
     * a document from the returned {@code IIOMetadata} object.
     * @param nodeNames a {@code Set} containing the names of
     * nodes that may be contained in a retrieved document.
     *
     * @return an {@code IIOMetadata} object, or {@code null}.
     *
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code nodeNames}
     * is {@code null}.
     * @exception IOException if an error occurs during reading.
     */
    public IIOMetadata getStreamMetadata(String formatName,
                                         Set<String> nodeNames)
        throws IOException
    {
        return getMetadata(formatName, nodeNames, true, 0);
    }

    private IIOMetadata getMetadata(String formatName,
                                    Set<String> nodeNames,
                                    boolean wantStream,
                                    int imageIndex) throws IOException {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (nodeNames == null) {
            throw new IllegalArgumentException("nodeNames == null!");
        }
        IIOMetadata metadata =
            wantStream
            ? getStreamMetadata()
            : getImageMetadata(imageIndex);
        if (metadata != null) {
            if (metadata.isStandardMetadataFormatSupported() &&
                formatName.equals
                (IIOMetadataFormatImpl.standardMetadataFormatName)) {
                return metadata;
            }
            String nativeName = metadata.getNativeMetadataFormatName();
            if (nativeName != null && formatName.equals(nativeName)) {
                return metadata;
            }
            String[] extraNames = metadata.getExtraMetadataFormatNames();
            if (extraNames != null) {
                for (int i = 0; i < extraNames.length; i++) {
                    if (formatName.equals(extraNames[i])) {
                        return metadata;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an {@code IIOMetadata} object containing metadata
     * associated with the given image, or {@code null} if the
     * reader does not support reading metadata, is set to ignore
     * metadata, or if no metadata is available.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be retrieved.
     *
     * @return an {@code IIOMetadata} object, or
     * {@code null}.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public abstract IIOMetadata getImageMetadata(int imageIndex)
        throws IOException;

    /**
     * Returns an {@code IIOMetadata} object representing the
     * metadata associated with the given image, or {@code null}
     * if the reader does not support reading metadata or none
     * is available.
     *
     * <p> The resulting metadata object is only responsible for
     * returning documents in the format named by
     * {@code formatName}.  Within any documents that are
     * returned, only nodes whose names are members of
     * {@code nodeNames} are required to be returned.  In this
     * way, the amount of metadata processing done by the reader may
     * be kept to a minimum, based on what information is actually
     * needed.
     *
     * <p> If {@code formatName} is not the name of a supported
     * metadata format, {@code null} may be returned.
     *
     * <p> In all cases, it is legal to return a more capable metadata
     * object than strictly necessary.  The format name and node names
     * are merely hints that may be used to reduce the reader's
     * workload.
     *
     * <p> The default implementation simply returns the result of
     * calling {@code getImageMetadata(imageIndex)}, after
     * checking that the format name is supported.  If it is not,
     * {@code null} is returned.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be retrieved.
     * @param formatName a metadata format name that may be used to retrieve
     * a document from the returned {@code IIOMetadata} object.
     * @param nodeNames a {@code Set} containing the names of
     * nodes that may be contained in a retrieved document.
     *
     * @return an {@code IIOMetadata} object, or {@code null}.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code nodeNames}
     * is {@code null}.
     * @exception IOException if an error occurs during reading.
     */
    public IIOMetadata getImageMetadata(int imageIndex,
                                        String formatName,
                                        Set<String> nodeNames)
        throws IOException {
        return getMetadata(formatName, nodeNames, false, imageIndex);
    }

    /**
     * Reads the image indexed by {@code imageIndex} and returns
     * it as a complete {@code BufferedImage}, using a default
     * {@code ImageReadParam}.  This is a convenience method
     * that calls {@code read(imageIndex, null)}.
     *
     * <p> The image returned will be formatted according to the first
     * {@code ImageTypeSpecifier} returned from
     * {@code getImageTypes}.
     *
     * <p> Any registered {@code IIOReadProgressListener} objects
     * will be notified by calling their {@code imageStarted}
     * method, followed by calls to their {@code imageProgress}
     * method as the read progresses.  Finally their
     * {@code imageComplete} method will be called.
     * {@code IIOReadUpdateListener} objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * {@code IIOReadWarningListener} objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * @param imageIndex the index of the image to be retrieved.
     *
     * @return the desired portion of the image as a
     * {@code BufferedImage}.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public BufferedImage read(int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    /**
     * Reads the image indexed by {@code imageIndex} and returns
     * it as a complete {@code BufferedImage}, using a supplied
     * {@code ImageReadParam}.
     *
     * <p> The actual {@code BufferedImage} returned will be
     * chosen using the algorithm defined by the
     * {@code getDestination} method.
     *
     * <p> Any registered {@code IIOReadProgressListener} objects
     * will be notified by calling their {@code imageStarted}
     * method, followed by calls to their {@code imageProgress}
     * method as the read progresses.  Finally their
     * {@code imageComplete} method will be called.
     * {@code IIOReadUpdateListener} objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * {@code IIOReadWarningListener} objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling {@code getSourceBands}
     * and {@code getDestinationBands} on the supplied
     * {@code ImageReadParam}.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * {@code IllegalArgumentException} is thrown.
     *
     * <p> If the supplied {@code ImageReadParam} contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an {@code ImageReadParam} used to control
     * the reading process, or {@code null}.
     *
     * @return the desired portion of the image as a
     * {@code BufferedImage}.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * {@code param.getSourceBands} and
     * {@code param.getDestinationBands} differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if the resulting image would
     * have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     */
    public abstract BufferedImage read(int imageIndex, ImageReadParam param)
        throws IOException;

    /**
     * Reads the image indexed by {@code imageIndex} and returns
     * an {@code IIOImage} containing the image, thumbnails, and
     * associated image metadata, using a supplied
     * {@code ImageReadParam}.
     *
     * <p> The actual {@code BufferedImage} referenced by the
     * returned {@code IIOImage} will be chosen using the
     * algorithm defined by the {@code getDestination} method.
     *
     * <p> Any registered {@code IIOReadProgressListener} objects
     * will be notified by calling their {@code imageStarted}
     * method, followed by calls to their {@code imageProgress}
     * method as the read progresses.  Finally their
     * {@code imageComplete} method will be called.
     * {@code IIOReadUpdateListener} objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * {@code IIOReadWarningListener} objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling {@code getSourceBands}
     * and {@code getDestinationBands} on the supplied
     * {@code ImageReadParam}.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * {@code IllegalArgumentException} is thrown.
     *
     * <p> Thumbnails will be returned in their entirety regardless of
     * the region settings.
     *
     * <p> If the supplied {@code ImageReadParam} contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), those
     * values will be ignored.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an {@code ImageReadParam} used to control
     * the reading process, or {@code null}.
     *
     * @return an {@code IIOImage} containing the desired portion
     * of the image, a set of thumbnails, and associated image
     * metadata.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * {@code param.getSourceBands} and
     * {@code param.getDestinationBands} differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if the resulting image
     * would have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     */
    public IIOImage readAll(int imageIndex, ImageReadParam param)
        throws IOException {
        if (imageIndex < getMinIndex()) {
            throw new IndexOutOfBoundsException("imageIndex < getMinIndex()!");
        }

        BufferedImage im = read(imageIndex, param);

        ArrayList<BufferedImage> thumbnails = null;
        int numThumbnails = getNumThumbnails(imageIndex);
        if (numThumbnails > 0) {
            thumbnails = new ArrayList<>();
            for (int j = 0; j < numThumbnails; j++) {
                thumbnails.add(readThumbnail(imageIndex, j));
            }
        }

        IIOMetadata metadata = getImageMetadata(imageIndex);
        return new IIOImage(im, thumbnails, metadata);
    }

    /**
     * Returns an {@code Iterator} containing all the images,
     * thumbnails, and metadata, starting at the index given by
     * {@code getMinIndex}, from the input source in the form of
     * {@code IIOImage} objects.  An {@code Iterator}
     * containing {@code ImageReadParam} objects is supplied; one
     * element is consumed for each image read from the input source
     * until no more images are available.  If the read param
     * {@code Iterator} runs out of elements, but there are still
     * more images available from the input source, default read
     * params are used for the remaining images.
     *
     * <p> If {@code params} is {@code null}, a default read
     * param will be used for all images.
     *
     * <p> The actual {@code BufferedImage} referenced by the
     * returned {@code IIOImage} will be chosen using the
     * algorithm defined by the {@code getDestination} method.
     *
     * <p> Any registered {@code IIOReadProgressListener} objects
     * will be notified by calling their {@code sequenceStarted}
     * method once.  Then, for each image decoded, there will be a
     * call to {@code imageStarted}, followed by calls to
     * {@code imageProgress} as the read progresses, and finally
     * to {@code imageComplete}.  The
     * {@code sequenceComplete} method will be called after the
     * last image has been decoded.
     * {@code IIOReadUpdateListener} objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * {@code IIOReadWarningListener} objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling {@code getSourceBands}
     * and {@code getDestinationBands} on the supplied
     * {@code ImageReadParam}.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * {@code IllegalArgumentException} is thrown.
     *
     * <p> Thumbnails will be returned in their entirety regardless of the
     * region settings.
     *
     * <p> If any of the supplied {@code ImageReadParam}s contain
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * @param params an {@code Iterator} containing
     * {@code ImageReadParam} objects.
     *
     * @return an {@code Iterator} representing the
     * contents of the input source as {@code IIOImage}s.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IllegalArgumentException if any
     * non-{@code null} element of {@code params} is not an
     * {@code ImageReadParam}.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * {@code param.getSourceBands} and
     * {@code param.getDestinationBands} differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if a resulting image would
     * have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     *
     * @see ImageReadParam
     * @see IIOImage
     */
    public Iterator<IIOImage>
        readAll(Iterator<? extends ImageReadParam> params)
        throws IOException
    {
        List<IIOImage> output = new ArrayList<>();

        int imageIndex = getMinIndex();

        // Inform IIOReadProgressListeners we're starting a sequence
        processSequenceStarted(imageIndex);

        while (true) {
            // Inform IIOReadProgressListeners and IIOReadUpdateListeners
            // that we're starting a new image

            ImageReadParam param = null;
            if (params != null && params.hasNext()) {
                Object o = params.next();
                if (o != null) {
                    if (o instanceof ImageReadParam) {
                        param = (ImageReadParam)o;
                    } else {
                        throw new IllegalArgumentException
                            ("Non-ImageReadParam supplied as part of params!");
                    }
                }
            }

            BufferedImage bi = null;
            try {
                bi = read(imageIndex, param);
            } catch (IndexOutOfBoundsException e) {
                break;
            }

            ArrayList<BufferedImage> thumbnails = null;
            int numThumbnails = getNumThumbnails(imageIndex);
            if (numThumbnails > 0) {
                thumbnails = new ArrayList<>();
                for (int j = 0; j < numThumbnails; j++) {
                    thumbnails.add(readThumbnail(imageIndex, j));
                }
            }

            IIOMetadata metadata = getImageMetadata(imageIndex);
            IIOImage im = new IIOImage(bi, thumbnails, metadata);
            output.add(im);

            ++imageIndex;
        }

        // Inform IIOReadProgressListeners we're ending a sequence
        processSequenceComplete();

        return output.iterator();
    }

    /**
     * Returns {@code true} if this plug-in supports reading
     * just a {@link java.awt.image.Raster Raster} of pixel data.
     * If this method returns {@code false}, calls to
     * {@link #readRaster readRaster} or {@link #readTileRaster readTileRaster}
     * will throw an {@code UnsupportedOperationException}.
     *
     * <p> The default implementation returns {@code false}.
     *
     * @return {@code true} if this plug-in supports reading raw
     * {@code Raster}s.
     *
     * @see #readRaster
     * @see #readTileRaster
     */
    public boolean canReadRaster() {
        return false;
    }

    /**
     * Returns a new {@code Raster} object containing the raw pixel data
     * from the image stream, without any color conversion applied.  The
     * application must determine how to interpret the pixel data by other
     * means.  Any destination or image-type parameters in the supplied
     * {@code ImageReadParam} object are ignored, but all other
     * parameters are used exactly as in the {@link #read read}
     * method, except that any destination offset is used as a logical rather
     * than a physical offset.  The size of the returned {@code Raster}
     * will always be that of the source region clipped to the actual image.
     * Logical offsets in the stream itself are ignored.
     *
     * <p> This method allows formats that normally apply a color
     * conversion, such as JPEG, and formats that do not normally have an
     * associated colorspace, such as remote sensing or medical imaging data,
     * to provide access to raw pixel data.
     *
     * <p> Any registered {@code readUpdateListener}s are ignored, as
     * there is no {@code BufferedImage}, but all other listeners are
     * called exactly as they are for the {@link #read read} method.
     *
     * <p> If {@link #canReadRaster canReadRaster()} returns
     * {@code false}, this method throws an
     * {@code UnsupportedOperationException}.
     *
     * <p> If the supplied {@code ImageReadParam} contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * <p> The default implementation throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index of the image to be read.
     * @param param an {@code ImageReadParam} used to control
     * the reading process, or {@code null}.
     *
     * @return the desired portion of the image as a
     * {@code Raster}.
     *
     * @exception UnsupportedOperationException if this plug-in does not
     * support reading raw {@code Raster}s.
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     *
     * @see #canReadRaster
     * @see #read
     * @see java.awt.image.Raster
     */
    public Raster readRaster(int imageIndex, ImageReadParam param)
        throws IOException {
        throw new UnsupportedOperationException("readRaster not supported!");
    }

    /**
     * Returns {@code true} if the image is organized into
     * <i>tiles</i>, that is, equal-sized non-overlapping rectangles.
     *
     * <p> A reader plug-in may choose whether or not to expose tiling
     * that is present in the image as it is stored.  It may even
     * choose to advertise tiling when none is explicitly present.  In
     * general, tiling should only be advertised if there is some
     * advantage (in speed or space) to accessing individual tiles.
     * Regardless of whether the reader advertises tiling, it must be
     * capable of reading an arbitrary rectangular region specified in
     * an {@code ImageReadParam}.
     *
     * <p> A reader for which all images are guaranteed to be tiled,
     * or are guaranteed not to be tiled, may return {@code true}
     * or {@code false} respectively without accessing any image
     * data.  In such cases, it is not necessary to throw an exception
     * even if no input source has been set or the image index is out
     * of bounds.
     *
     * <p> The default implementation just returns {@code false}.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return {@code true} if the image is tiled.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public boolean isImageTiled(int imageIndex) throws IOException {
        return false;
    }

    /**
     * Returns the width of a tile in the given image.
     *
     * <p> The default implementation simply returns
     * {@code getWidth(imageIndex)}, which is correct for
     * non-tiled images.  Readers that support tiling should override
     * this method.
     *
     * @return the width of a tile.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileWidth(int imageIndex) throws IOException {
        return getWidth(imageIndex);
    }

    /**
     * Returns the height of a tile in the given image.
     *
     * <p> The default implementation simply returns
     * {@code getHeight(imageIndex)}, which is correct for
     * non-tiled images.  Readers that support tiling should override
     * this method.
     *
     * @return the height of a tile.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileHeight(int imageIndex) throws IOException {
        return getHeight(imageIndex);
    }

    /**
     * Returns the X coordinate of the upper-left corner of tile (0,
     * 0) in the given image.
     *
     * <p> A reader for which the tile grid X offset always has the
     * same value (usually 0), may return the value without accessing
     * any image data.  In such cases, it is not necessary to throw an
     * exception even if no input source has been set or the image
     * index is out of bounds.
     *
     * <p> The default implementation simply returns 0, which is
     * correct for non-tiled images and tiled images in most formats.
     * Readers that support tiling with non-(0, 0) offsets should
     * override this method.
     *
     * @return the X offset of the tile grid.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileGridXOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Returns the Y coordinate of the upper-left corner of tile (0,
     * 0) in the given image.
     *
     * <p> A reader for which the tile grid Y offset always has the
     * same value (usually 0), may return the value without accessing
     * any image data.  In such cases, it is not necessary to throw an
     * exception even if no input source has been set or the image
     * index is out of bounds.
     *
     * <p> The default implementation simply returns 0, which is
     * correct for non-tiled images and tiled images in most formats.
     * Readers that support tiling with non-(0, 0) offsets should
     * override this method.
     *
     * @return the Y offset of the tile grid.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileGridYOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Reads the tile indicated by the {@code tileX} and
     * {@code tileY} arguments, returning it as a
     * {@code BufferedImage}.  If the arguments are out of range,
     * an {@code IllegalArgumentException} is thrown.  If the
     * image is not tiled, the values 0, 0 will return the entire
     * image; any other values will cause an
     * {@code IllegalArgumentException} to be thrown.
     *
     * <p> This method is merely a convenience equivalent to calling
     * {@code read(int, ImageReadParam)} with a read param
     * specifying a source region having offsets of
     * {@code tileX*getTileWidth(imageIndex)},
     * {@code tileY*getTileHeight(imageIndex)} and width and
     * height of {@code getTileWidth(imageIndex)},
     * {@code getTileHeight(imageIndex)}; and subsampling
     * factors of 1 and offsets of 0.  To subsample a tile, call
     * {@code read} with a read param specifying this region
     * and different subsampling parameters.
     *
     * <p> The default implementation returns the entire image if
     * {@code tileX} and {@code tileY} are 0, or throws
     * an {@code IllegalArgumentException} otherwise.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param tileX the column index (starting with 0) of the tile
     * to be retrieved.
     * @param tileY the row index (starting with 0) of the tile
     * to be retrieved.
     *
     * @return the tile as a {@code BufferedImage}.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is out of bounds.
     * @exception IllegalArgumentException if the tile indices are
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public BufferedImage readTile(int imageIndex,
                                  int tileX, int tileY) throws IOException {
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return read(imageIndex);
    }

    /**
     * Returns a new {@code Raster} object containing the raw
     * pixel data from the tile, without any color conversion applied.
     * The application must determine how to interpret the pixel data by other
     * means.
     *
     * <p> If {@link #canReadRaster canReadRaster()} returns
     * {@code false}, this method throws an
     * {@code UnsupportedOperationException}.
     *
     * <p> The default implementation checks if reading
     * {@code Raster}s is supported, and if so calls {@link
     * #readRaster readRaster(imageIndex, null)} if
     * {@code tileX} and {@code tileY} are 0, or throws an
     * {@code IllegalArgumentException} otherwise.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param tileX the column index (starting with 0) of the tile
     * to be retrieved.
     * @param tileY the row index (starting with 0) of the tile
     * to be retrieved.
     *
     * @return the tile as a {@code Raster}.
     *
     * @exception UnsupportedOperationException if this plug-in does not
     * support reading raw {@code Raster}s.
     * @exception IllegalArgumentException if the tile indices are
     * out of bounds.
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if {@code imageIndex}
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     *
     * @see #readTile
     * @see #readRaster
     * @see java.awt.image.Raster
     */
    public Raster readTileRaster(int imageIndex,
                                 int tileX, int tileY) throws IOException {
        if (!canReadRaster()) {
            throw new UnsupportedOperationException
                ("readTileRaster not supported!");
        }
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return readRaster(imageIndex, null);
    }

    // RenderedImages

    /**
     * Returns a {@code RenderedImage} object that contains the
     * contents of the image indexed by {@code imageIndex}.  By
     * default, the returned image is simply the
     * {@code BufferedImage} returned by
     * {@code read(imageIndex, param)}.
     *
     * <p> The semantics of this method may differ from those of the
     * other {@code read} methods in several ways.  First, any
     * destination image and/or image type set in the
     * {@code ImageReadParam} may be ignored.  Second, the usual
     * listener calls are not guaranteed to be made, or to be
     * meaningful if they are.  This is because the returned image may
     * not be fully populated with pixel data at the time it is
     * returned, or indeed at any time.
     *
     * <p> If the supplied {@code ImageReadParam} contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * <p> The default implementation just calls
     * {@link #read read(imageIndex, param)}.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an {@code ImageReadParam} used to control
     * the reading process, or {@code null}.
     *
     * @return a {@code RenderedImage} object providing a view of
     * the image.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * {@code param.getSourceBands} and
     * {@code param.getDestinationBands} differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if the resulting image
     * would have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     */
    public RenderedImage readAsRenderedImage(int imageIndex,
                                             ImageReadParam param)
        throws IOException {
        return read(imageIndex, param);
    }

    // Thumbnails

    /**
     * Returns {@code true} if the image format understood by
     * this reader supports thumbnail preview images associated with
     * it.  The default implementation returns {@code false}.
     *
     * <p> If this method returns {@code false},
     * {@code hasThumbnails} and {@code getNumThumbnails}
     * will return {@code false} and {@code 0},
     * respectively, and {@code readThumbnail} will throw an
     * {@code UnsupportedOperationException}, regardless of their
     * arguments.
     *
     * <p> A reader that does not support thumbnails need not
     * implement any of the thumbnail-related methods.
     *
     * @return {@code true} if thumbnails are supported.
     */
    public boolean readerSupportsThumbnails() {
        return false;
    }

    /**
     * Returns {@code true} if the given image has thumbnail
     * preview images associated with it.  If the format does not
     * support thumbnails ({@code readerSupportsThumbnails}
     * returns {@code false}), {@code false} will be
     * returned regardless of whether an input source has been set or
     * whether {@code imageIndex} is in bounds.
     *
     * <p> The default implementation returns {@code true} if
     * {@code getNumThumbnails} returns a value greater than 0.
     *
     * @param imageIndex the index of the image being queried.
     *
     * @return {@code true} if the given image has thumbnails.
     *
     * @exception IllegalStateException if the reader supports
     * thumbnails but the input source has not been set.
     * @exception IndexOutOfBoundsException if the reader supports
     * thumbnails but {@code imageIndex} is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public boolean hasThumbnails(int imageIndex) throws IOException {
        return getNumThumbnails(imageIndex) > 0;
    }

    /**
     * Returns the number of thumbnail preview images associated with
     * the given image.  If the format does not support thumbnails,
     * ({@code readerSupportsThumbnails} returns
     * {@code false}), {@code 0} will be returned regardless
     * of whether an input source has been set or whether
     * {@code imageIndex} is in bounds.
     *
     * <p> The default implementation returns 0 without checking its
     * argument.
     *
     * @param imageIndex the index of the image being queried.
     *
     * @return the number of thumbnails associated with the given
     * image.
     *
     * @exception IllegalStateException if the reader supports
     * thumbnails but the input source has not been set.
     * @exception IndexOutOfBoundsException if the reader supports
     * thumbnails but {@code imageIndex} is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getNumThumbnails(int imageIndex)
        throws IOException {
        return 0;
    }

    /**
     * Returns the width of the thumbnail preview image indexed by
     * {@code thumbnailIndex}, associated with the image indexed
     * by {@code ImageIndex}.
     *
     * <p> If the reader does not support thumbnails,
     * ({@code readerSupportsThumbnails} returns
     * {@code false}), an {@code UnsupportedOperationException}
     * will be thrown.
     *
     * <p> The default implementation simply returns
     * {@code readThumbnail(imageindex, thumbnailIndex).getWidth()}.
     * Subclasses should therefore
     * override this method if possible in order to avoid forcing the
     * thumbnail to be read.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param thumbnailIndex the index of the thumbnail to be retrieved.
     *
     * @return the width of the desired thumbnail as an {@code int}.
     *
     * @exception UnsupportedOperationException if thumbnails are not
     * supported.
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if either of the supplied
     * indices are out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex)
        throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getWidth();
    }

    /**
     * Returns the height of the thumbnail preview image indexed by
     * {@code thumbnailIndex}, associated with the image indexed
     * by {@code ImageIndex}.
     *
     * <p> If the reader does not support thumbnails,
     * ({@code readerSupportsThumbnails} returns
     * {@code false}), an {@code UnsupportedOperationException}
     * will be thrown.
     *
     * <p> The default implementation simply returns
     * {@code readThumbnail(imageindex, thumbnailIndex).getHeight()}.
     * Subclasses should therefore override
     * this method if possible in order to avoid
     * forcing the thumbnail to be read.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param thumbnailIndex the index of the thumbnail to be retrieved.
     *
     * @return the height of the desired thumbnail as an {@code int}.
     *
     * @exception UnsupportedOperationException if thumbnails are not
     * supported.
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if either of the supplied
     * indices are out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex)
        throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getHeight();
    }

    /**
     * Returns the thumbnail preview image indexed by
     * {@code thumbnailIndex}, associated with the image indexed
     * by {@code ImageIndex} as a {@code BufferedImage}.
     *
     * <p> Any registered {@code IIOReadProgressListener} objects
     * will be notified by calling their
     * {@code thumbnailStarted}, {@code thumbnailProgress},
     * and {@code thumbnailComplete} methods.
     *
     * <p> If the reader does not support thumbnails,
     * ({@code readerSupportsThumbnails} returns
     * {@code false}), an {@code UnsupportedOperationException}
     * will be thrown regardless of whether an input source has been
     * set or whether the indices are in bounds.
     *
     * <p> The default implementation throws an
     * {@code UnsupportedOperationException}.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param thumbnailIndex the index of the thumbnail to be retrieved.
     *
     * @return the desired thumbnail as a {@code BufferedImage}.
     *
     * @exception UnsupportedOperationException if thumbnails are not
     * supported.
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if either of the supplied
     * indices are out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public BufferedImage readThumbnail(int imageIndex,
                                       int thumbnailIndex)
        throws IOException {
        throw new UnsupportedOperationException("Thumbnails not supported!");
    }

    // Abort

    /**
     * Requests that any current read operation be aborted.  The
     * contents of the image following the abort will be undefined.
     *
     * <p> Readers should call {@code clearAbortRequest} at the
     * beginning of each read operation, and poll the value of
     * {@code abortRequested} regularly during the read.
     */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /**
     * Returns {@code true} if a request to abort the current
     * read operation has been made since the reader was instantiated or
     * {@code clearAbortRequest} was called.
     *
     * @return {@code true} if the current read operation should
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

    // Add an element to a list, creating a new list if the
    // existing list is null, and return the list.
    static <T> List<T> addToList(List<T> l, T elt) {
        if (l == null) {
            l = new ArrayList<>();
        }
        l.add(elt);
        return l;
    }


    // Remove an element from a list, discarding the list if the
    // resulting list is empty, and return the list or null.
    static <T> List<T> removeFromList(List<T> l, T elt) {
        if (l == null) {
            return l;
        }
        l.remove(elt);
        if (l.size() == 0) {
            l = null;
        }
        return l;
    }

    /**
     * Adds an {@code IIOReadWarningListener} to the list of
     * registered warning listeners.  If {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.  Messages sent to the given listener will be
     * localized, if possible, to match the current
     * {@code Locale}.  If no {@code Locale} has been set,
     * warning messages may be localized as the reader sees fit.
     *
     * @param listener an {@code IIOReadWarningListener} to be registered.
     *
     * @see #removeIIOReadWarningListener
     */
    public void addIIOReadWarningListener(IIOReadWarningListener listener) {
        if (listener == null) {
            return;
        }
        warningListeners = addToList(warningListeners, listener);
        warningLocales = addToList(warningLocales, getLocale());
    }

    /**
     * Removes an {@code IIOReadWarningListener} from the list of
     * registered error listeners.  If the listener was not previously
     * registered, or if {@code listener} is {@code null},
     * no exception will be thrown and no action will be taken.
     *
     * @param listener an IIOReadWarningListener to be unregistered.
     *
     * @see #addIIOReadWarningListener
     */
    public void removeIIOReadWarningListener(IIOReadWarningListener listener) {
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
     * {@code IIOReadWarningListener} objects.
     *
     * <p> The default implementation sets the
     * {@code warningListeners} and {@code warningLocales}
     * instance variables to {@code null}.
     */
    public void removeAllIIOReadWarningListeners() {
        warningListeners = null;
        warningLocales = null;
    }

    /**
     * Adds an {@code IIOReadProgressListener} to the list of
     * registered progress listeners.  If {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an IIOReadProgressListener to be registered.
     *
     * @see #removeIIOReadProgressListener
     */
    public void addIIOReadProgressListener(IIOReadProgressListener listener) {
        if (listener == null) {
            return;
        }
        progressListeners = addToList(progressListeners, listener);
    }

    /**
     * Removes an {@code IIOReadProgressListener} from the list
     * of registered progress listeners.  If the listener was not
     * previously registered, or if {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an IIOReadProgressListener to be unregistered.
     *
     * @see #addIIOReadProgressListener
     */
    public void
        removeIIOReadProgressListener (IIOReadProgressListener listener) {
        if (listener == null || progressListeners == null) {
            return;
        }
        progressListeners = removeFromList(progressListeners, listener);
    }

    /**
     * Removes all currently registered
     * {@code IIOReadProgressListener} objects.
     *
     * <p> The default implementation sets the
     * {@code progressListeners} instance variable to
     * {@code null}.
     */
    public void removeAllIIOReadProgressListeners() {
        progressListeners = null;
    }

    /**
     * Adds an {@code IIOReadUpdateListener} to the list of
     * registered update listeners.  If {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.  The listener will receive notification of pixel
     * updates as images and thumbnails are decoded, including the
     * starts and ends of progressive passes.
     *
     * <p> If no update listeners are present, the reader may choose
     * to perform fewer updates to the pixels of the destination
     * images and/or thumbnails, which may result in more efficient
     * decoding.
     *
     * <p> For example, in progressive JPEG decoding each pass
     * contains updates to a set of coefficients, which would have to
     * be transformed into pixel values and converted to an RGB color
     * space for each pass if listeners are present.  If no listeners
     * are present, the coefficients may simply be accumulated and the
     * final results transformed and color converted one time only.
     *
     * <p> The final results of decoding will be the same whether or
     * not intermediate updates are performed.  Thus if only the final
     * image is desired it may be preferable not to register any
     * {@code IIOReadUpdateListener}s.  In general, progressive
     * updating is most effective when fetching images over a network
     * connection that is very slow compared to local CPU processing;
     * over a fast connection, progressive updates may actually slow
     * down the presentation of the image.
     *
     * @param listener an IIOReadUpdateListener to be registered.
     *
     * @see #removeIIOReadUpdateListener
     */
    public void
        addIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null) {
            return;
        }
        updateListeners = addToList(updateListeners, listener);
    }

    /**
     * Removes an {@code IIOReadUpdateListener} from the list of
     * registered update listeners.  If the listener was not
     * previously registered, or if {@code listener} is
     * {@code null}, no exception will be thrown and no action
     * will be taken.
     *
     * @param listener an IIOReadUpdateListener to be unregistered.
     *
     * @see #addIIOReadUpdateListener
     */
    public void removeIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null || updateListeners == null) {
            return;
        }
        updateListeners = removeFromList(updateListeners, listener);
    }

    /**
     * Removes all currently registered
     * {@code IIOReadUpdateListener} objects.
     *
     * <p> The default implementation sets the
     * {@code updateListeners} instance variable to
     * {@code null}.
     */
    public void removeAllIIOReadUpdateListeners() {
        updateListeners = null;
    }

    /**
     * Broadcasts the start of an sequence of image reads to all
     * registered {@code IIOReadProgressListener}s by calling
     * their {@code sequenceStarted} method.  Subclasses may use
     * this method as a convenience.
     *
     * @param minIndex the lowest index being read.
     */
    protected void processSequenceStarted(int minIndex) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.sequenceStarted(this, minIndex);
        }
    }

    /**
     * Broadcasts the completion of an sequence of image reads to all
     * registered {@code IIOReadProgressListener}s by calling
     * their {@code sequenceComplete} method.  Subclasses may use
     * this method as a convenience.
     */
    protected void processSequenceComplete() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.sequenceComplete(this);
        }
    }

    /**
     * Broadcasts the start of an image read to all registered
     * {@code IIOReadProgressListener}s by calling their
     * {@code imageStarted} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param imageIndex the index of the image about to be read.
     */
    protected void processImageStarted(int imageIndex) {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.imageStarted(this, imageIndex);
        }
    }

    /**
     * Broadcasts the current percentage of image completion to all
     * registered {@code IIOReadProgressListener}s by calling
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
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.imageProgress(this, percentageDone);
        }
    }

    /**
     * Broadcasts the completion of an image read to all registered
     * {@code IIOReadProgressListener}s by calling their
     * {@code imageComplete} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processImageComplete() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.imageComplete(this);
        }
    }

    /**
     * Broadcasts the start of a thumbnail read to all registered
     * {@code IIOReadProgressListener}s by calling their
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
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailStarted(this, imageIndex, thumbnailIndex);
        }
    }

    /**
     * Broadcasts the current percentage of thumbnail completion to
     * all registered {@code IIOReadProgressListener}s by calling
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
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailProgress(this, percentageDone);
        }
    }

    /**
     * Broadcasts the completion of a thumbnail read to all registered
     * {@code IIOReadProgressListener}s by calling their
     * {@code thumbnailComplete} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processThumbnailComplete() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.thumbnailComplete(this);
        }
    }

    /**
     * Broadcasts that the read has been aborted to all registered
     * {@code IIOReadProgressListener}s by calling their
     * {@code readAborted} method.  Subclasses may use this
     * method as a convenience.
     */
    protected void processReadAborted() {
        if (progressListeners == null) {
            return;
        }
        int numListeners = progressListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadProgressListener listener =
                progressListeners.get(i);
            listener.readAborted(this);
        }
    }

    /**
     * Broadcasts the beginning of a progressive pass to all
     * registered {@code IIOReadUpdateListener}s by calling their
     * {@code passStarted} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param theImage the {@code BufferedImage} being updated.
     * @param pass the index of the current pass, starting with 0.
     * @param minPass the index of the first pass that will be decoded.
     * @param maxPass the index of the last pass that will be decoded.
     * @param minX the X coordinate of the upper-left pixel included
     * in the pass.
     * @param minY the X coordinate of the upper-left pixel included
     * in the pass.
     * @param periodX the horizontal separation between pixels.
     * @param periodY the vertical separation between pixels.
     * @param bands an array of {@code int}s indicating the
     * set of affected bands of the destination.
     */
    protected void processPassStarted(BufferedImage theImage,
                                      int pass,
                                      int minPass, int maxPass,
                                      int minX, int minY,
                                      int periodX, int periodY,
                                      int[] bands) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.passStarted(this, theImage, pass,
                                 minPass,
                                 maxPass,
                                 minX, minY,
                                 periodX, periodY,
                                 bands);
        }
    }

    /**
     * Broadcasts the update of a set of samples to all registered
     * {@code IIOReadUpdateListener}s by calling their
     * {@code imageUpdate} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param theImage the {@code BufferedImage} being updated.
     * @param minX the X coordinate of the upper-left pixel included
     * in the pass.
     * @param minY the X coordinate of the upper-left pixel included
     * in the pass.
     * @param width the total width of the area being updated, including
     * pixels being skipped if {@code periodX > 1}.
     * @param height the total height of the area being updated,
     * including pixels being skipped if {@code periodY > 1}.
     * @param periodX the horizontal separation between pixels.
     * @param periodY the vertical separation between pixels.
     * @param bands an array of {@code int}s indicating the
     * set of affected bands of the destination.
     */
    protected void processImageUpdate(BufferedImage theImage,
                                      int minX, int minY,
                                      int width, int height,
                                      int periodX, int periodY,
                                      int[] bands) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.imageUpdate(this,
                                 theImage,
                                 minX, minY,
                                 width, height,
                                 periodX, periodY,
                                 bands);
        }
    }

    /**
     * Broadcasts the end of a progressive pass to all
     * registered {@code IIOReadUpdateListener}s by calling their
     * {@code passComplete} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param theImage the {@code BufferedImage} being updated.
     */
    protected void processPassComplete(BufferedImage theImage) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.passComplete(this, theImage);
        }
    }

    /**
     * Broadcasts the beginning of a thumbnail progressive pass to all
     * registered {@code IIOReadUpdateListener}s by calling their
     * {@code thumbnailPassStarted} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     * @param pass the index of the current pass, starting with 0.
     * @param minPass the index of the first pass that will be decoded.
     * @param maxPass the index of the last pass that will be decoded.
     * @param minX the X coordinate of the upper-left pixel included
     * in the pass.
     * @param minY the X coordinate of the upper-left pixel included
     * in the pass.
     * @param periodX the horizontal separation between pixels.
     * @param periodY the vertical separation between pixels.
     * @param bands an array of {@code int}s indicating the
     * set of affected bands of the destination.
     */
    protected void processThumbnailPassStarted(BufferedImage theThumbnail,
                                               int pass,
                                               int minPass, int maxPass,
                                               int minX, int minY,
                                               int periodX, int periodY,
                                               int[] bands) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.thumbnailPassStarted(this, theThumbnail, pass,
                                          minPass,
                                          maxPass,
                                          minX, minY,
                                          periodX, periodY,
                                          bands);
        }
    }

    /**
     * Broadcasts the update of a set of samples in a thumbnail image
     * to all registered {@code IIOReadUpdateListener}s by
     * calling their {@code thumbnailUpdate} method.  Subclasses may
     * use this method as a convenience.
     *
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     * @param minX the X coordinate of the upper-left pixel included
     * in the pass.
     * @param minY the X coordinate of the upper-left pixel included
     * in the pass.
     * @param width the total width of the area being updated, including
     * pixels being skipped if {@code periodX > 1}.
     * @param height the total height of the area being updated,
     * including pixels being skipped if {@code periodY > 1}.
     * @param periodX the horizontal separation between pixels.
     * @param periodY the vertical separation between pixels.
     * @param bands an array of {@code int}s indicating the
     * set of affected bands of the destination.
     */
    protected void processThumbnailUpdate(BufferedImage theThumbnail,
                                          int minX, int minY,
                                          int width, int height,
                                          int periodX, int periodY,
                                          int[] bands) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.thumbnailUpdate(this,
                                     theThumbnail,
                                     minX, minY,
                                     width, height,
                                     periodX, periodY,
                                     bands);
        }
    }

    /**
     * Broadcasts the end of a thumbnail progressive pass to all
     * registered {@code IIOReadUpdateListener}s by calling their
     * {@code thumbnailPassComplete} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     */
    protected void processThumbnailPassComplete(BufferedImage theThumbnail) {
        if (updateListeners == null) {
            return;
        }
        int numListeners = updateListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadUpdateListener listener =
                updateListeners.get(i);
            listener.thumbnailPassComplete(this, theThumbnail);
        }
    }

    /**
     * Broadcasts a warning message to all registered
     * {@code IIOReadWarningListener}s by calling their
     * {@code warningOccurred} method.  Subclasses may use this
     * method as a convenience.
     *
     * @param warning the warning message to send.
     *
     * @exception IllegalArgumentException if {@code warning}
     * is {@code null}.
     */
    protected void processWarningOccurred(String warning) {
        if (warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int numListeners = warningListeners.size();
        for (int i = 0; i < numListeners; i++) {
            IIOReadWarningListener listener =
                warningListeners.get(i);

            listener.warningOccurred(this, warning);
        }
    }

    /**
     * Broadcasts a localized warning message to all registered
     * {@code IIOReadWarningListener}s by calling their
     * {@code warningOccurred} method with a string taken
     * from a {@code ResourceBundle}.  Subclasses may use this
     * method as a convenience.
     *
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
    protected void processWarningOccurred(String baseName,
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
            IIOReadWarningListener listener =
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

            listener.warningOccurred(this, warning);
        }
    }

    // State management

    /**
     * Restores the {@code ImageReader} to its initial state.
     *
     * <p> The default implementation calls
     * {@code setInput(null, false)},
     * {@code setLocale(null)},
     * {@code removeAllIIOReadUpdateListeners()},
     * {@code removeAllIIOReadWarningListeners()},
     * {@code removeAllIIOReadProgressListeners()}, and
     * {@code clearAbortRequest}.
     */
    public void reset() {
        setInput(null, false, false);
        setLocale(null);
        removeAllIIOReadUpdateListeners();
        removeAllIIOReadProgressListeners();
        removeAllIIOReadWarningListeners();
        clearAbortRequest();
    }

    /**
     * Allows any resources held by this object to be released.  The
     * result of calling any other method (other than
     * {@code finalize}) subsequent to a call to this method
     * is undefined.
     *
     * <p>It is important for applications to call this method when they
     * know they will no longer be using this {@code ImageReader}.
     * Otherwise, the reader may continue to hold on to resources
     * indefinitely.
     *
     * <p>The default implementation of this method in the superclass does
     * nothing.  Subclass implementations should ensure that all resources,
     * especially native resources, are released.
     */
    public void dispose() {
    }

    // Utility methods

    /**
     * A utility method that may be used by readers to compute the
     * region of the source image that should be read, taking into
     * account any source region and subsampling offset settings in
     * the supplied {@code ImageReadParam}.  The actual
     * subsampling factors, destination size, and destination offset
     * are <em>not</em> taken into consideration, thus further
     * clipping must take place.  The {@link #computeRegions computeRegions}
     * method performs all necessary clipping.
     *
     * @param param the {@code ImageReadParam} being used, or
     * {@code null}.
     * @param srcWidth the width of the source image.
     * @param srcHeight the height of the source image.
     *
     * @return the source region as a {@code Rectangle}.
     */
    protected static Rectangle getSourceRegion(ImageReadParam param,
                                               int srcWidth,
                                               int srcHeight) {
        Rectangle sourceRegion = new Rectangle(0, 0, srcWidth, srcHeight);
        if (param != null) {
            Rectangle region = param.getSourceRegion();
            if (region != null) {
                sourceRegion = sourceRegion.intersection(region);
            }

            int subsampleXOffset = param.getSubsamplingXOffset();
            int subsampleYOffset = param.getSubsamplingYOffset();
            sourceRegion.x += subsampleXOffset;
            sourceRegion.y += subsampleYOffset;
            sourceRegion.width -= subsampleXOffset;
            sourceRegion.height -= subsampleYOffset;
        }

        return sourceRegion;
    }

    /**
     * Computes the source region of interest and the destination
     * region of interest, taking the width and height of the source
     * image, an optional destination image, and an optional
     * {@code ImageReadParam} into account.  The source region
     * begins with the entire source image.  Then that is clipped to
     * the source region specified in the {@code ImageReadParam},
     * if one is specified.
     *
     * <p> If either of the destination offsets are negative, the
     * source region is clipped so that its top left will coincide
     * with the top left of the destination image, taking subsampling
     * into account.  Then the result is clipped to the destination
     * image on the right and bottom, if one is specified, taking
     * subsampling and destination offsets into account.
     *
     * <p> Similarly, the destination region begins with the source
     * image, is translated to the destination offset given in the
     * {@code ImageReadParam} if there is one, and finally is
     * clipped to the destination image, if there is one.
     *
     * <p> If either the source or destination regions end up having a
     * width or height of 0, an {@code IllegalArgumentException}
     * is thrown.
     *
     * <p> The {@link #getSourceRegion getSourceRegion>}
     * method may be used if only source clipping is desired.
     *
     * @param param an {@code ImageReadParam}, or {@code null}.
     * @param srcWidth the width of the source image.
     * @param srcHeight the height of the source image.
     * @param image a {@code BufferedImage} that will be the
     * destination image, or {@code null}.
     * @param srcRegion a {@code Rectangle} that will be filled with
     * the source region of interest.
     * @param destRegion a {@code Rectangle} that will be filled with
     * the destination region of interest.
     * @exception IllegalArgumentException if {@code srcRegion}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code dstRegion}
     * is {@code null}.
     * @exception IllegalArgumentException if the resulting source or
     * destination region is empty.
     */
    protected static void computeRegions(ImageReadParam param,
                                         int srcWidth,
                                         int srcHeight,
                                         BufferedImage image,
                                         Rectangle srcRegion,
                                         Rectangle destRegion) {
        if (srcRegion == null) {
            throw new IllegalArgumentException("srcRegion == null!");
        }
        if (destRegion == null) {
            throw new IllegalArgumentException("destRegion == null!");
        }

        // Start with the entire source image
        srcRegion.setBounds(0, 0, srcWidth, srcHeight);

        // Destination also starts with source image, as that is the
        // maximum extent if there is no subsampling
        destRegion.setBounds(0, 0, srcWidth, srcHeight);

        // Clip that to the param region, if there is one
        int periodX = 1;
        int periodY = 1;
        int gridX = 0;
        int gridY = 0;
        if (param != null) {
            Rectangle paramSrcRegion = param.getSourceRegion();
            if (paramSrcRegion != null) {
                srcRegion.setBounds(srcRegion.intersection(paramSrcRegion));
            }
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();
            gridX = param.getSubsamplingXOffset();
            gridY = param.getSubsamplingYOffset();
            srcRegion.translate(gridX, gridY);
            srcRegion.width -= gridX;
            srcRegion.height -= gridY;
            destRegion.setLocation(param.getDestinationOffset());
        }

        // Now clip any negative destination offsets, i.e. clip
        // to the top and left of the destination image
        if (destRegion.x < 0) {
            int delta = -destRegion.x*periodX;
            srcRegion.x += delta;
            srcRegion.width -= delta;
            destRegion.x = 0;
        }
        if (destRegion.y < 0) {
            int delta = -destRegion.y*periodY;
            srcRegion.y += delta;
            srcRegion.height -= delta;
            destRegion.y = 0;
        }

        // Now clip the destination Region to the subsampled width and height
        int subsampledWidth = (srcRegion.width + periodX - 1)/periodX;
        int subsampledHeight = (srcRegion.height + periodY - 1)/periodY;
        destRegion.width = subsampledWidth;
        destRegion.height = subsampledHeight;

        // Now clip that to right and bottom of the destination image,
        // if there is one, taking subsampling into account
        if (image != null) {
            Rectangle destImageRect = new Rectangle(0, 0,
                                                    image.getWidth(),
                                                    image.getHeight());
            destRegion.setBounds(destRegion.intersection(destImageRect));
            if (destRegion.isEmpty()) {
                throw new IllegalArgumentException
                    ("Empty destination region!");
            }

            int deltaX = destRegion.x + subsampledWidth - image.getWidth();
            if (deltaX > 0) {
                srcRegion.width -= deltaX*periodX;
            }
            int deltaY =  destRegion.y + subsampledHeight - image.getHeight();
            if (deltaY > 0) {
                srcRegion.height -= deltaY*periodY;
            }
        }
        if (srcRegion.isEmpty() || destRegion.isEmpty()) {
            throw new IllegalArgumentException("Empty region!");
        }
    }

    /**
     * A utility method that may be used by readers to test the
     * validity of the source and destination band settings of an
     * {@code ImageReadParam}.  This method may be called as soon
     * as the reader knows both the number of bands of the source
     * image as it exists in the input stream, and the number of bands
     * of the destination image that being written.
     *
     * <p> The method retrieves the source and destination band
     * setting arrays from param using the {@code getSourceBands}
     * and {@code getDestinationBands} methods (or considers them
     * to be {@code null} if {@code param} is
     * {@code null}).  If the source band setting array is
     * {@code null}, it is considered to be equal to the array
     * {@code { 0, 1, ..., numSrcBands - 1 }}, and similarly for
     * the destination band setting array.
     *
     * <p> The method then tests that both arrays are equal in length,
     * and that neither array contains a value larger than the largest
     * available band index.
     *
     * <p> Any failure results in an
     * {@code IllegalArgumentException} being thrown; success
     * results in the method returning silently.
     *
     * @param param the {@code ImageReadParam} being used to read
     * the image.
     * @param numSrcBands the number of bands of the image as it exists
     * int the input source.
     * @param numDstBands the number of bands in the destination image
     * being written.
     *
     * @exception IllegalArgumentException if {@code param}
     * contains an invalid specification of a source and/or
     * destination band subset.
     */
    protected static void checkReadParamBandSettings(ImageReadParam param,
                                                     int numSrcBands,
                                                     int numDstBands) {
        // A null param is equivalent to srcBands == dstBands == null.
        int[] srcBands = null;
        int[] dstBands = null;
        if (param != null) {
            srcBands = param.getSourceBands();
            dstBands = param.getDestinationBands();
        }

        int paramSrcBandLength =
            (srcBands == null) ? numSrcBands : srcBands.length;
        int paramDstBandLength =
            (dstBands == null) ? numDstBands : dstBands.length;

        if (paramSrcBandLength != paramDstBandLength) {
            throw new IllegalArgumentException("ImageReadParam num source & dest bands differ!");
        }

        if (srcBands != null) {
            for (int i = 0; i < srcBands.length; i++) {
                if (srcBands[i] >= numSrcBands) {
                    throw new IllegalArgumentException("ImageReadParam source bands contains a value >= the number of source bands!");
                }
            }
        }

        if (dstBands != null) {
            for (int i = 0; i < dstBands.length; i++) {
                if (dstBands[i] >= numDstBands) {
                    throw new IllegalArgumentException("ImageReadParam dest bands contains a value >= the number of dest bands!");
                }
            }
        }
    }

    /**
     * Returns the {@code BufferedImage} to which decoded pixel
     * data should be written.  The image is determined by inspecting
     * the supplied {@code ImageReadParam} if it is
     * non-{@code null}; if its {@code getDestination}
     * method returns a non-{@code null} value, that image is
     * simply returned.  Otherwise,
     * {@code param.getDestinationType} method is called to
     * determine if a particular image type has been specified.  If
     * so, the returned {@code ImageTypeSpecifier} is used after
     * checking that it is equal to one of those included in
     * {@code imageTypes}.
     *
     * <p> If {@code param} is {@code null} or the above
     * steps have not yielded an image or an
     * {@code ImageTypeSpecifier}, the first value obtained from
     * the {@code imageTypes} parameter is used.  Typically, the
     * caller will set {@code imageTypes} to the value of
     * {@code getImageTypes(imageIndex)}.
     *
     * <p> Next, the dimensions of the image are determined by a call
     * to {@code computeRegions}.  The actual width and height of
     * the image being decoded are passed in as the {@code width}
     * and {@code height} parameters.
     *
     * @param param an {@code ImageReadParam} to be used to get
     * the destination image or image type, or {@code null}.
     * @param imageTypes an {@code Iterator} of
     * {@code ImageTypeSpecifier}s indicating the legal image
     * types, with the default first.
     * @param width the true width of the image or tile being decoded.
     * @param height the true width of the image or tile being decoded.
     *
     * @return the {@code BufferedImage} to which decoded pixel
     * data should be written.
     *
     * @exception IIOException if the {@code ImageTypeSpecifier}
     * specified by {@code param} does not match any of the legal
     * ones from {@code imageTypes}.
     * @exception IllegalArgumentException if {@code imageTypes}
     * is {@code null} or empty, or if an object not of type
     * {@code ImageTypeSpecifier} is retrieved from it.
     * @exception IllegalArgumentException if the resulting image would
     * have a width or height less than 1.
     * @exception IllegalArgumentException if the product of
     * {@code width} and {@code height} is greater than
     * {@code Integer.MAX_VALUE}.
     */
    protected static BufferedImage
        getDestination(ImageReadParam param,
                       Iterator<ImageTypeSpecifier> imageTypes,
                       int width, int height)
        throws IIOException {
        if (imageTypes == null || !imageTypes.hasNext()) {
            throw new IllegalArgumentException("imageTypes null or empty!");
        }
        if ((long)width*height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                ("width*height > Integer.MAX_VALUE!");
        }

        BufferedImage dest = null;
        ImageTypeSpecifier imageType = null;

        // If param is non-null, use it
        if (param != null) {
            // Try to get the image itself
            dest = param.getDestination();
            if (dest != null) {
                return dest;
            }

            // No image, get the image type
            imageType = param.getDestinationType();
        }

        // No info from param, use fallback image type
        if (imageType == null) {
            Object o = imageTypes.next();
            if (!(o instanceof ImageTypeSpecifier)) {
                throw new IllegalArgumentException
                    ("Non-ImageTypeSpecifier retrieved from imageTypes!");
            }
            imageType = (ImageTypeSpecifier)o;
        } else {
            boolean foundIt = false;
            while (imageTypes.hasNext()) {
                ImageTypeSpecifier type =
                    imageTypes.next();
                if (type.equals(imageType)) {
                    foundIt = true;
                    break;
                }
            }

            if (!foundIt) {
                throw new IIOException
                    ("Destination type from ImageReadParam does not match!");
            }
        }

        Rectangle srcRegion = new Rectangle(0,0,0,0);
        Rectangle destRegion = new Rectangle(0,0,0,0);
        computeRegions(param,
                       width,
                       height,
                       null,
                       srcRegion,
                       destRegion);

        int destWidth = destRegion.x + destRegion.width;
        int destHeight = destRegion.y + destRegion.height;
        // Create a new image based on the type specifier
        return imageType.createBufferedImage(destWidth, destHeight);
    }
}
