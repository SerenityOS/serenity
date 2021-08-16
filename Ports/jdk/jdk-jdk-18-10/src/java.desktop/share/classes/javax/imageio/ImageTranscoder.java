/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

import javax.imageio.metadata.IIOMetadata;

/**
 * An interface providing metadata transcoding capability.
 *
 * <p> Any image may be transcoded (written to a different format
 * than the one it was originally stored in) simply by performing
 * a read operation followed by a write operation.  However, loss
 * of data may occur in this process due to format differences.
 *
 * <p> In general, the best results will be achieved when
 * format-specific metadata objects can be created to encapsulate as
 * much information about the image and its associated metadata as
 * possible, in terms that are understood by the specific
 * {@code ImageWriter} used to perform the encoding.
 *
 * <p> An {@code ImageTranscoder} may be used to convert the
 * {@code IIOMetadata} objects supplied by the
 * {@code ImageReader} (representing per-stream and per-image
 * metadata) into corresponding objects suitable for encoding by a
 * particular {@code ImageWriter}.  In the case where the methods
 * of this interface are being called directly on an
 * {@code ImageWriter}, the output will be suitable for that
 * writer.
 *
 * <p> The internal details of converting an {@code IIOMetadata}
 * object into a writer-specific format will vary according to the
 * context of the operation.  Typically, an {@code ImageWriter}
 * will inspect the incoming object to see if it implements additional
 * interfaces with which the writer is familiar.  This might be the
 * case, for example, if the object was obtained by means of a read
 * operation on a reader plug-in written by the same vendor as the
 * writer.  In this case, the writer may access the incoming object by
 * means of its plug-in specific interfaces.  In this case, the
 * re-encoding may be close to lossless if the image file format is
 * kept constant.  If the format is changing, the writer may still
 * attempt to preserve as much information as possible.
 *
 * <p> If the incoming object does not implement any additional
 * interfaces known to the writer, the writer has no choice but to
 * access it via the standard {@code IIOMetadata} interfaces such
 * as the tree view provided by {@code IIOMetadata.getAsTree}.
 * In this case, there is likely to be significant loss of
 * information.
 *
 * <p> An independent {@code ImageTranscoder} essentially takes
 * on the same role as the writer plug-in in the above examples.  It
 * must be familiar with the private interfaces used by both the
 * reader and writer plug-ins, and manually instantiate an object that
 * will be usable by the writer.  The resulting metadata objects may
 * be used by the writer directly.
 *
 * <p> No independent implementations of {@code ImageTranscoder}
 * are provided as part of the standard API.  Instead, the intention
 * of this interface is to provide a way for implementations to be
 * created and discovered by applications as the need arises.
 *
 */
public interface ImageTranscoder {

    /**
     * Returns an {@code IIOMetadata} object that may be used for
     * encoding and optionally modified using its document interfaces
     * or other interfaces specific to the writer plug-in that will be
     * used for encoding.
     *
     * <p> An optional {@code ImageWriteParam} may be supplied
     * for cases where it may affect the structure of the stream
     * metadata.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not understood by this writer or
     * transcoder, they will be ignored.
     *
     * @param inData an {@code IIOMetadata} object representing
     * stream metadata, used to initialize the state of the returned
     * object.
     * @param param an {@code ImageWriteParam} that will be used to
     * encode the image, or {@code null}.
     *
     * @return an {@code IIOMetadata} object, or
     * {@code null} if the plug-in does not provide metadata
     * encoding capabilities.
     *
     * @exception IllegalArgumentException if {@code inData} is
     * {@code null}.
     */
    IIOMetadata convertStreamMetadata(IIOMetadata inData,
                                      ImageWriteParam param);

    /**
     * Returns an {@code IIOMetadata} object that may be used for
     * encoding and optionally modified using its document interfaces
     * or other interfaces specific to the writer plug-in that will be
     * used for encoding.
     *
     * <p> An optional {@code ImageWriteParam} may be supplied
     * for cases where it may affect the structure of the image
     * metadata.
     *
     * <p> If the supplied {@code ImageWriteParam} contains
     * optional setting values not understood by this writer or
     * transcoder, they will be ignored.
     *
     * @param inData an {@code IIOMetadata} object representing
     * image metadata, used to initialize the state of the returned
     * object.
     * @param imageType an {@code ImageTypeSpecifier} indicating
     * the layout and color information of the image with which the
     * metadata will be associated.
     * @param param an {@code ImageWriteParam} that will be used to
     * encode the image, or {@code null}.
     *
     * @return an {@code IIOMetadata} object,
     * or {@code null} if the plug-in does not provide
     * metadata encoding capabilities.
     *
     * @exception IllegalArgumentException if either of
     * {@code inData} or {@code imageType} is
     * {@code null}.
     */
    IIOMetadata convertImageMetadata(IIOMetadata inData,
                                     ImageTypeSpecifier imageType,
                                     ImageWriteParam param);
}
