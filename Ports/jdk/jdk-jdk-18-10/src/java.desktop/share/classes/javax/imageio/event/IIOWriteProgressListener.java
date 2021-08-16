/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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
import javax.imageio.ImageWriter;

/**
 * An interface used by {@code ImageWriter} implementations to notify
 * callers of their image writing methods of progress.
 *
 * @see javax.imageio.ImageWriter#write
 *
 */
public interface IIOWriteProgressListener extends EventListener {

    /**
     * Reports that an image write operation is beginning.  All
     * {@code ImageWriter} implementations are required to call
     * this method exactly once when beginning an image write
     * operation.
     *
     * @param source the {@code ImageWriter} object calling this
     * method.
     * @param imageIndex the index of the image being written within
     * its containing input file or stream.
     */
    void imageStarted(ImageWriter source, int imageIndex);

    /**
     * Reports the approximate degree of completion of the current
     * {@code write} call within the associated
     * {@code ImageWriter}.
     *
     * <p> The degree of completion is expressed as an index
     * indicating which image is being written, and a percentage
     * varying from {@code 0.0F} to {@code 100.0F}
     * indicating how much of the current image has been output.  The
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
     * <p> Each particular {@code ImageWriter} implementation may
     * call this method at whatever frequency it desires.  A rule of
     * thumb is to call it around each 5 percent mark.
     *
     * @param source the {@code ImageWriter} object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    void imageProgress(ImageWriter source,
                       float percentageDone);

    /**
     * Reports that the image write operation has completed.  All
     * {@code ImageWriter} implementations are required to call
     * this method exactly once upon completion of each image write
     * operation.
     *
     * @param source the {@code ImageWriter} object calling this method.
     */
    void imageComplete(ImageWriter source);

    /**
     * Reports that a thumbnail write operation is beginning.  All
     * {@code ImageWriter} implementations are required to call
     * this method exactly once when beginning a thumbnail write
     * operation.
     *
     * @param source the {@code ImageWrite} object calling this method.
     * @param imageIndex the index of the image being written within its
     * containing input file or stream.
     * @param thumbnailIndex the index of the thumbnail being written.
     */
    void thumbnailStarted(ImageWriter source,
                          int imageIndex, int thumbnailIndex);

    /**
     * Reports the approximate degree of completion of the current
     * thumbnail write within the associated {@code ImageWriter}.
     * The semantics are identical to those of
     * {@code imageProgress}.
     *
     * @param source the {@code ImageWriter} object calling this
     * method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    void thumbnailProgress(ImageWriter source, float percentageDone);

    /**
     * Reports that a thumbnail write operation has completed.  All
     * {@code ImageWriter} implementations are required to call
     * this method exactly once upon completion of each thumbnail
     * write operation.
     *
     * @param source the {@code ImageWriter} object calling this
     * method.
     */
    void thumbnailComplete(ImageWriter source);

    /**
     * Reports that a write has been aborted via the writer's
     * {@code abort} method.  No further notifications will be
     * given.
     *
     * @param source the {@code ImageWriter} object calling this
     * method.
     */
    void writeAborted(ImageWriter source);
}
