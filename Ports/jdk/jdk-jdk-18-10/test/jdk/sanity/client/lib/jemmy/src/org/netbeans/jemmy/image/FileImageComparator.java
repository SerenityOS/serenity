/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.image;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.netbeans.jemmy.JemmyException;

/**
 * Allowes compares images in memory to ones stored in files and compare such
 * images one with another.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class FileImageComparator {

    ImageLoader loader;
    ImageComparator comparator;

    /**
     * Constructs a FileImageComparator object.
     *
     * @param comparator - ImageComparator to be used for image comparision.
     * @param loader - ImageLoader to be used for image loading.
     */
    public FileImageComparator(ImageComparator comparator, ImageLoader loader) {
        this.loader = loader;
        this.comparator = comparator;
    }

    /**
     * Compares an image with one stored in file. Comparision is performed by
     * ImageComparator passed into constructor. Image is loaded by ImageLoader
     * passed into constructor.
     *
     * @param image an image to compare.
     * @param fileName a file containing an image to compare.
     * @return true if images match each other.
     */
    public boolean compare(BufferedImage image, String fileName) {
        try {
            return comparator.compare(image, loader.load(fileName));
        } catch (IOException e) {
            throw (new JemmyException("IOException during image loading", e));
        }
    }

    /**
     * Compares two image stored in files.. Comparision is performed by
     * ImageComparator passed into constructor. Images are loaded by ImageLoader
     * passed into constructor.
     *
     * @param fileName1 a file containing an image to compare.
     * @param fileName2 a file containing an image to compare.
     * @return true if images match each other.
     */
    public boolean compare(String fileName1, String fileName2) {
        try {
            return (comparator.compare(loader.load(fileName1),
                    loader.load(fileName2)));
        } catch (IOException e) {
            throw (new JemmyException("IOException during image loading", e));
        }
    }
}
