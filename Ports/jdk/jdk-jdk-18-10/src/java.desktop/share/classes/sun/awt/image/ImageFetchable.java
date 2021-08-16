/*
 * Copyright (c) 1995, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

/**
 * This interface allows the ImageFetcher class to drive the production
 * of image data in an ImageProducer class by calling the doFetch()
 * method from one of a pool of threads which are created to facilitate
 * asynchronous delivery of image data outside of the standard system
 * threads which manage the applications User Interface.
 *
 * @see ImageFetcher
 * @see java.awt.image.ImageProducer
 *
 * @author      Jim Graham
 */
public interface ImageFetchable {
    /**
     * This method is called by one of the ImageFetcher threads to start
     * the flow of information from the ImageProducer to the ImageConsumer.
     * @see ImageFetcher
     * @see java.awt.image.ImageProducer
     */
    public void doFetch();
}
