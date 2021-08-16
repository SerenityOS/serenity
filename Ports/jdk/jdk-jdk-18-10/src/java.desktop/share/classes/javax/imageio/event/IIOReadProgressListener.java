/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * An interface used by {@code ImageReader} implementations to
 * notify callers of their image and thumbnail reading methods of
 * progress.
 *
 * <p> This interface receives general indications of decoding
 * progress (via the {@code imageProgress} and
 * {@code thumbnailProgress} methods), and events indicating when
 * an entire image has been updated (via the
 * {@code imageStarted}, {@code imageComplete},
 * {@code thumbnailStarted} and {@code thumbnailComplete}
 * methods).  Applications that wish to be informed of pixel updates
 * as they happen (for example, during progressive decoding), should
 * provide an {@code IIOReadUpdateListener}.
 *
 * @see IIOReadUpdateListener
 * @see javax.imageio.ImageReader#addIIOReadProgressListener
 * @see javax.imageio.ImageReader#removeIIOReadProgressListener
 *
 */
public interface IIOReadProgressListener extends EventListener {

    /**
     * Reports that a sequence of read operations is beginning.
     * {@code ImageReader} implementations are required to call
     * this method exactly once from their
     * {@code readAll(Iterator)} method.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param minIndex the index of the first image to be read.
     */
    void sequenceStarted(ImageReader source, int minIndex);

    /**
     * Reports that a sequence of read operations has completed.
     * {@code ImageReader} implementations are required to call
     * this method exactly once from their
     * {@code readAll(Iterator)} method.
     *
     * @param source the {@code ImageReader} object calling this method.
     */
    void sequenceComplete(ImageReader source);

    /**
     * Reports that an image read operation is beginning.  All
     * {@code ImageReader} implementations are required to call
     * this method exactly once when beginning an image read
     * operation.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param imageIndex the index of the image being read within its
     * containing input file or stream.
     */
    void imageStarted(ImageReader source, int imageIndex);

    /**
     * Reports the approximate degree of completion of the current
     * {@code read} call of the associated
     * {@code ImageReader}.
     *
     * <p> The degree of completion is expressed as a percentage
     * varying from {@code 0.0F} to {@code 100.0F}.  The
     * percentage should ideally be calculated in terms of the
     * remaining time to completion, but it is usually more practical
     * to use a more well-defined metric such as pixels decoded or
     * portion of input stream consumed.  In any case, a sequence of
     * calls to this method during a given read operation should
     * supply a monotonically increasing sequence of percentage
     * values.  It is not necessary to supply the exact values
     * {@code 0} and {@code 100}, as these may be inferred
     * by the callee from other methods.
     *
     * <p> Each particular {@code ImageReader} implementation may
     * call this method at whatever frequency it desires.  A rule of
     * thumb is to call it around each 5 percent mark.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    void imageProgress(ImageReader source, float percentageDone);

    /**
     * Reports that the current image read operation has completed.
     * All {@code ImageReader} implementations are required to
     * call this method exactly once upon completion of each image
     * read operation.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     */
    void imageComplete(ImageReader source);

    /**
     * Reports that a thumbnail read operation is beginning.  All
     * {@code ImageReader} implementations are required to call
     * this method exactly once when beginning a thumbnail read
     * operation.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param imageIndex the index of the image being read within its
     * containing input file or stream.
     * @param thumbnailIndex the index of the thumbnail being read.
     */
    void thumbnailStarted(ImageReader source,
                          int imageIndex, int thumbnailIndex);

    /**
     * Reports the approximate degree of completion of the current
     * {@code getThumbnail} call within the associated
     * {@code ImageReader}.  The semantics are identical to those
     * of {@code imageProgress}.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    void thumbnailProgress(ImageReader source, float percentageDone);

    /**
     * Reports that a thumbnail read operation has completed.  All
     * {@code ImageReader} implementations are required to call
     * this method exactly once upon completion of each thumbnail read
     * operation.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     */
    void thumbnailComplete(ImageReader source);

    /**
     * Reports that a read has been aborted via the reader's
     * {@code abort} method.  No further notifications will be
     * given.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     */
    void readAborted(ImageReader source);
}
