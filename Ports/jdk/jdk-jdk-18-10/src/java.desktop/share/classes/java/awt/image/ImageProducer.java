/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.image;

/**
 * The interface for objects which can produce the image data for Images.
 * Each image contains an ImageProducer which is used to reconstruct
 * the image whenever it is needed, for example, when a new size of the
 * Image is scaled, or when the width or height of the Image is being
 * requested.
 *
 * @see ImageConsumer
 *
 * @author      Jim Graham
 */
public interface ImageProducer {
    /**
     * Registers an {@code ImageConsumer} with the
     * {@code ImageProducer} for access to the image data
     * during a later reconstruction of the {@code Image}.
     * The {@code ImageProducer} may, at its discretion,
     * start delivering the image data to the consumer
     * using the {@code ImageConsumer} interface immediately,
     * or when the next available image reconstruction is triggered
     * by a call to the {@code startProduction} method.
     * @param ic the specified {@code ImageConsumer}
     * @see #startProduction
     */
    public void addConsumer(ImageConsumer ic);

    /**
     * Determines if a specified {@code ImageConsumer}
     * object is currently registered with this
     * {@code ImageProducer} as one of its consumers.
     * @param ic the specified {@code ImageConsumer}
     * @return {@code true} if the specified
     *         {@code ImageConsumer} is registered with
     *         this {@code ImageProducer};
     *         {@code false} otherwise.
     */
    public boolean isConsumer(ImageConsumer ic);

    /**
     * Removes the specified {@code ImageConsumer} object
     * from the list of consumers currently registered to
     * receive image data.  It is not considered an error
     * to remove a consumer that is not currently registered.
     * The {@code ImageProducer} should stop sending data
     * to this consumer as soon as is feasible.
     * @param ic the specified {@code ImageConsumer}
     */
    public void removeConsumer(ImageConsumer ic);

    /**
     * Registers the specified {@code ImageConsumer} object
     * as a consumer and starts an immediate reconstruction of
     * the image data which will then be delivered to this
     * consumer and any other consumer which might have already
     * been registered with the producer.  This method differs
     * from the addConsumer method in that a reproduction of
     * the image data should be triggered as soon as possible.
     * @param ic the specified {@code ImageConsumer}
     * @see #addConsumer
     */
    public void startProduction(ImageConsumer ic);

    /**
     * Requests, on behalf of the {@code ImageConsumer},
     * that the {@code ImageProducer} attempt to resend
     * the image data one more time in TOPDOWNLEFTRIGHT order
     * so that higher quality conversion algorithms which
     * depend on receiving pixels in order can be used to
     * produce a better output version of the image.  The
     * {@code ImageProducer} is free to
     * ignore this call if it cannot resend the data in that
     * order.  If the data can be resent, the
     * {@code ImageProducer} should respond by executing
     * the following minimum set of {@code ImageConsumer}
     * method calls:
     * <pre>{@code
     *  ic.setHints(TOPDOWNLEFTRIGHT | < otherhints >);
     *  ic.setPixels(...);      // As many times as needed
     *  ic.imageComplete();
     * }</pre>
     * @param ic the specified {@code ImageConsumer}
     * @see ImageConsumer#setHints
     */
    public void requestTopDownLeftRightResend(ImageConsumer ic);
}
