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

package javax.imageio.spi;

import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * The service provider interface (SPI) for {@code ImageWriter}s.
 * For more information on service provider classes, see the class comment
 * for the {@code IIORegistry} class.
 *
 * <p> Each {@code ImageWriterSpi} provides several types of information
 * about the {@code ImageWriter} class with which it is associated.
 *
 * <p> The name of the vendor who defined the SPI class and a
 * brief description of the class are available via the
 * {@code getVendorName}, {@code getDescription},
 * and {@code getVersion} methods.
 * These methods may be internationalized to provide locale-specific
 * output.  These methods are intended mainly to provide short,
 * human-writable information that might be used to organize a pop-up
 * menu or other list.
 *
 * <p> Lists of format names, file suffixes, and MIME types associated
 * with the service may be obtained by means of the
 * {@code getFormatNames}, {@code getFileSuffixes}, and
 * {@code getMIMEType} methods.  These methods may be used to
 * identify candidate {@code ImageWriter}s for writing a
 * particular file or stream based on manual format selection, file
 * naming, or MIME associations.
 *
 * <p> A more reliable way to determine which {@code ImageWriter}s
 * are likely to be able to parse a particular data stream is provided
 * by the {@code canEncodeImage} method.  This methods allows the
 * service provider to inspect the actual image contents.
 *
 * <p> Finally, an instance of the {@code ImageWriter} class
 * associated with this service provider may be obtained by calling
 * the {@code createWriterInstance} method.  Any heavyweight
 * initialization, such as the loading of native libraries or creation
 * of large tables, should be deferred at least until the first
 * invocation of this method.
 *
 * @see IIORegistry
 * @see javax.imageio.ImageTypeSpecifier
 * @see javax.imageio.ImageWriter
 *
 */
public abstract class ImageWriterSpi extends ImageReaderWriterSpi {

    /**
     * A single-element array, initially containing
     * {@code ImageOutputStream.class}, to be returned from
     * {@code getOutputTypes}.
     * @deprecated Instead of using this field, directly create
     * the equivalent array {@code { ImageOutputStream.class }}.
     */
    @Deprecated
    public static final Class<?>[] STANDARD_OUTPUT_TYPE =
        { ImageOutputStream.class };

    /**
     * An array of {@code Class} objects to be returned from
     * {@code getOutputTypes}, initially {@code null}.
     */
    protected Class<?>[] outputTypes = null;

    /**
     * An array of strings to be returned from
     * {@code getImageReaderSpiNames}, initially
     * {@code null}.
     */
    protected String[] readerSpiNames = null;

    /**
     * The {@code Class} of the writer, initially
     * {@code null}.
     */
    private Class<?> writerClass = null;

    /**
     * Constructs a blank {@code ImageWriterSpi}.  It is up to
     * the subclass to initialize instance variables and/or override
     * method implementations in order to provide working versions of
     * all methods.
     */
    protected ImageWriterSpi() {
    }

    /**
     * Constructs an {@code ImageWriterSpi} with a given
     * set of values.
     *
     * @param vendorName the vendor name, as a non-{@code null}
     * {@code String}.
     * @param version a version identifier, as a non-{@code null}
     * {@code String}.
     * @param names a non-{@code null} array of
     * {@code String}s indicating the format names.  At least one
     * entry must be present.
     * @param suffixes an array of {@code String}s indicating the
     * common file suffixes.  If no suffixes are defined,
     * {@code null} should be supplied.  An array of length 0
     * will be normalized to {@code null}.
     * @param MIMETypes an array of {@code String}s indicating
     * the format's MIME types.  If no suffixes are defined,
     * {@code null} should be supplied.  An array of length 0
     * will be normalized to {@code null}.
     * @param writerClassName the fully-qualified name of the
     * associated {@code ImageWriterSpi} class, as a
     * non-{@code null String}.
     * @param outputTypes an array of {@code Class} objects of
     * length at least 1 indicating the legal output types.
     * @param readerSpiNames an array {@code String}s of length
     * at least 1 naming the classes of all associated
     * {@code ImageReader}s, or {@code null}.  An array of
     * length 0 is normalized to {@code null}.
     * @param supportsStandardStreamMetadataFormat a
     * {@code boolean} that indicates whether a stream metadata
     * object can use trees described by the standard metadata format.
     * @param nativeStreamMetadataFormatName a
     * {@code String}, or {@code null}, to be returned from
     * {@code getNativeStreamMetadataFormatName}.
     * @param nativeStreamMetadataFormatClassName a
     * {@code String}, or {@code null}, to be used to instantiate
     * a metadata format object to be returned from
     * {@code getNativeStreamMetadataFormat}.
     * @param extraStreamMetadataFormatNames an array of
     * {@code String}s, or {@code null}, to be returned from
     * {@code getExtraStreamMetadataFormatNames}.  An array of length
     * 0 is normalized to {@code null}.
     * @param extraStreamMetadataFormatClassNames an array of
     * {@code String}s, or {@code null}, to be used to instantiate
     * a metadata format object to be returned from
     * {@code getStreamMetadataFormat}.  An array of length
     * 0 is normalized to {@code null}.
     * @param supportsStandardImageMetadataFormat a
     * {@code boolean} that indicates whether an image metadata
     * object can use trees described by the standard metadata format.
     * @param nativeImageMetadataFormatName a
     * {@code String}, or {@code null}, to be returned from
     * {@code getNativeImageMetadataFormatName}.
     * @param nativeImageMetadataFormatClassName a
     * {@code String}, or {@code null}, to be used to instantiate
     * a metadata format object to be returned from
     * {@code getNativeImageMetadataFormat}.
     * @param extraImageMetadataFormatNames an array of
     * {@code String}s to be returned from
     * {@code getExtraImageMetadataFormatNames}.  An array of length 0
     * is normalized to {@code null}.
     * @param extraImageMetadataFormatClassNames an array of
     * {@code String}s, or {@code null}, to be used to instantiate
     * a metadata format object to be returned from
     * {@code getImageMetadataFormat}.  An array of length
     * 0 is normalized to {@code null}.
     *
     * @exception IllegalArgumentException if {@code vendorName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code version}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code names}
     * is {@code null} or has length 0.
     * @exception IllegalArgumentException if {@code writerClassName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code outputTypes}
     * is {@code null} or has length 0.
     */
    public ImageWriterSpi(String vendorName,
                          String version,
                          String[] names,
                          String[] suffixes,
                          String[] MIMETypes,
                          String writerClassName,
                          Class<?>[] outputTypes,
                          String[] readerSpiNames,
                          boolean supportsStandardStreamMetadataFormat,
                          String nativeStreamMetadataFormatName,
                          String nativeStreamMetadataFormatClassName,
                          String[] extraStreamMetadataFormatNames,
                          String[] extraStreamMetadataFormatClassNames,
                          boolean supportsStandardImageMetadataFormat,
                          String nativeImageMetadataFormatName,
                          String nativeImageMetadataFormatClassName,
                          String[] extraImageMetadataFormatNames,
                          String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version,
              names, suffixes, MIMETypes, writerClassName,
              supportsStandardStreamMetadataFormat,
              nativeStreamMetadataFormatName,
              nativeStreamMetadataFormatClassName,
              extraStreamMetadataFormatNames,
              extraStreamMetadataFormatClassNames,
              supportsStandardImageMetadataFormat,
              nativeImageMetadataFormatName,
              nativeImageMetadataFormatClassName,
              extraImageMetadataFormatNames,
              extraImageMetadataFormatClassNames);

        if (outputTypes == null) {
            throw new IllegalArgumentException
                ("outputTypes == null!");
        }
        if (outputTypes.length == 0) {
            throw new IllegalArgumentException
                ("outputTypes.length == 0!");
        }

        this.outputTypes = (outputTypes == STANDARD_OUTPUT_TYPE) ?
            new Class<?>[] { ImageOutputStream.class } :
            outputTypes.clone();

        // If length == 0, leave it null
        if (readerSpiNames != null && readerSpiNames.length > 0) {
            this.readerSpiNames = readerSpiNames.clone();
        }
    }

    /**
     * Returns {@code true} if the format that this writer
     * outputs preserves pixel data bit-accurately.  The default
     * implementation returns {@code true}.
     *
     * @return {@code true} if the format preserves full pixel
     * accuracy.
     */
    public boolean isFormatLossless() {
        return true;
    }

    /**
     * Returns an array of {@code Class} objects indicating what
     * types of objects may be used as arguments to the writer's
     * {@code setOutput} method.
     *
     * <p> For most writers, which only output to an
     * {@code ImageOutputStream}, a single-element array
     * containing {@code ImageOutputStream.class} should be
     * returned.
     *
     * @return a non-{@code null} array of
     * {@code Class} objects of length at least 1.
     */
    public Class<?>[] getOutputTypes() {
        return outputTypes.clone();
    }

    /**
     * Returns {@code true} if the {@code ImageWriter}
     * implementation associated with this service provider is able to
     * encode an image with the given layout.  The layout
     * (<i>i.e.</i>, the image's {@code SampleModel} and
     * {@code ColorModel}) is described by an
     * {@code ImageTypeSpecifier} object.
     *
     * <p> A return value of {@code true} is not an absolute
     * guarantee of successful encoding; the encoding process may still
     * produce errors due to factors such as I/O errors, inconsistent
     * or malformed data structures, etc.  The intent is that a
     * reasonable inspection of the basic structure of the image be
     * performed in order to determine if it is within the scope of
     * the encoding format.  For example, a service provider for a
     * format that can only encode greyscale would return
     * {@code false} if handed an RGB {@code BufferedImage}.
     * Similarly, a service provider for a format that can encode
     * 8-bit RGB imagery might refuse to encode an image with an
     * associated alpha channel.
     *
     * <p> Different {@code ImageWriter}s, and thus service
     * providers, may choose to be more or less strict.  For example,
     * they might accept an image with premultiplied alpha even though
     * it will have to be divided out of each pixel, at some loss of
     * precision, in order to be stored.
     *
     * @param type an {@code ImageTypeSpecifier} specifying the
     * layout of the image to be written.
     *
     * @return {@code true} if this writer is likely to be able
     * to encode images with the given layout.
     *
     * @exception IllegalArgumentException if {@code type}
     * is {@code null}.
     */
    public abstract boolean canEncodeImage(ImageTypeSpecifier type);

    /**
     * Returns {@code true} if the {@code ImageWriter}
     * implementation associated with this service provider is able to
     * encode the given {@code RenderedImage} instance.  Note
     * that this includes instances of
     * {@code java.awt.image.BufferedImage}.
     *
     * <p> See the discussion for
     * {@code canEncodeImage(ImageTypeSpecifier)} for information
     * on the semantics of this method.
     *
     * @param im an instance of {@code RenderedImage} to be encoded.
     *
     * @return {@code true} if this writer is likely to be able
     * to encode this image.
     *
     * @exception IllegalArgumentException if {@code im}
     * is {@code null}.
     */
    public boolean canEncodeImage(RenderedImage im) {
        return canEncodeImage(ImageTypeSpecifier.createFromRenderedImage(im));
    }

    /**
     * Returns an instance of the {@code ImageWriter}
     * implementation associated with this service provider.
     * The returned object will initially be in an initial state as if
     * its {@code reset} method had been called.
     *
     * <p> The default implementation simply returns
     * {@code createWriterInstance(null)}.
     *
     * @return an {@code ImageWriter} instance.
     *
     * @exception IOException if an error occurs during loading,
     * or initialization of the writer class, or during instantiation
     * or initialization of the writer object.
     */
    public ImageWriter createWriterInstance() throws IOException {
        return createWriterInstance(null);
    }

    /**
     * Returns an instance of the {@code ImageWriter}
     * implementation associated with this service provider.
     * The returned object will initially be in an initial state
     * as if its {@code reset} method had been called.
     *
     * <p> An {@code Object} may be supplied to the plug-in at
     * construction time.  The nature of the object is entirely
     * plug-in specific.
     *
     * <p> Typically, a plug-in will implement this method using code
     * such as {@code return new MyImageWriter(this)}.
     *
     * @param extension a plug-in specific extension object, which may
     * be {@code null}.
     *
     * @return an {@code ImageWriter} instance.
     *
     * @exception IOException if the attempt to instantiate
     * the writer fails.
     * @exception IllegalArgumentException if the
     * {@code ImageWriter}'s constructor throws an
     * {@code IllegalArgumentException} to indicate that the
     * extension object is unsuitable.
     */
    public abstract ImageWriter createWriterInstance(Object extension)
        throws IOException;

    /**
     * Returns {@code true} if the {@code ImageWriter} object
     * passed in is an instance of the {@code ImageWriter}
     * associated with this service provider.
     *
     * @param writer an {@code ImageWriter} instance.
     *
     * @return {@code true} if {@code writer} is recognized
     *
     * @exception IllegalArgumentException if {@code writer} is
     * {@code null}.
     */
    public boolean isOwnWriter(ImageWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        String name = writer.getClass().getName();
        return name.equals(pluginClassName);
    }

    /**
     * Returns an array of {@code String}s containing all the
     * fully qualified names of all the {@code ImageReaderSpi}
     * classes that can understand the internal metadata
     * representation used by the {@code ImageWriter} associated
     * with this service provider, or {@code null} if there are
     * no such {@code ImageReaders} specified.  If a
     * non-{@code null} value is returned, it must have non-zero
     * length.
     *
     * <p> The first item in the array must be the name of the service
     * provider for the "preferred" reader, as it will be used to
     * instantiate the {@code ImageReader} returned by
     * {@code ImageIO.getImageReader(ImageWriter)}.
     *
     * <p> This mechanism may be used to obtain
     * {@code ImageReaders} that will generated non-pixel
     * meta-data (see {@code IIOExtraDataInfo}) in a structure
     * understood by an {@code ImageWriter}.  By reading the
     * image and obtaining this data from one of the
     * {@code ImageReaders} obtained with this method and passing
     * it on to the {@code ImageWriter}, a client program can
     * read an image, modify it in some way, and write it back out
     * preserving all meta-data, without having to understand anything
     * about the internal structure of the meta-data, or even about
     * the image format.
     *
     * @return an array of {@code String}s of length at least 1
     * containing names of {@code ImageReaderSpi}s, or
     * {@code null}.
     *
     * @see javax.imageio.ImageIO#getImageReader(ImageWriter)
     * @see ImageReaderSpi#getImageWriterSpiNames()
     */
    public String[] getImageReaderSpiNames() {
        return readerSpiNames == null ?
            null : readerSpiNames.clone();
    }
}
