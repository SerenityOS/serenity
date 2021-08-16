/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import javax.imageio.metadata.IIOMetadata;

/**
 * A simple container class to aggregate an image, a set of
 * thumbnail (preview) images, and an object representing metadata
 * associated with the image.
 *
 * <p> The image data may take the form of either a
 * {@code RenderedImage}, or a {@code Raster}.  Reader
 * methods that return an {@code IIOImage} will always return a
 * {@code BufferedImage} using the {@code RenderedImage}
 * reference.  Writer methods that accept an {@code IIOImage}
 * will always accept a {@code RenderedImage}, and may optionally
 * accept a {@code Raster}.
 *
 * <p> Exactly one of {@code getRenderedImage} and
 * {@code getRaster} will return a non-{@code null} value.
 * Subclasses are responsible for ensuring this behavior.
 *
 * @see ImageReader#readAll(int, ImageReadParam)
 * @see ImageReader#readAll(java.util.Iterator)
 * @see ImageWriter#write(javax.imageio.metadata.IIOMetadata,
 *                        IIOImage, ImageWriteParam)
 * @see ImageWriter#write(IIOImage)
 * @see ImageWriter#writeToSequence(IIOImage, ImageWriteParam)
 * @see ImageWriter#writeInsert(int, IIOImage, ImageWriteParam)
 *
 */
public class IIOImage {

    /**
     * The {@code RenderedImage} being referenced.
     */
    protected RenderedImage image;

    /**
     * The {@code Raster} being referenced.
     */
    protected Raster raster;

    /**
     * A {@code List} of {@code BufferedImage} thumbnails,
     * or {@code null}.  Non-{@code BufferedImage} objects
     * must not be stored in this {@code List}.
     */
    protected List<? extends BufferedImage> thumbnails = null;

    /**
     * An {@code IIOMetadata} object containing metadata
     * associated with the image.
     */
    protected IIOMetadata metadata;

    /**
     * Constructs an {@code IIOImage} containing a
     * {@code RenderedImage}, and thumbnails and metadata
     * associated with it.
     *
     * <p> All parameters are stored by reference.
     *
     * <p> The {@code thumbnails} argument must either be
     * {@code null} or contain only {@code BufferedImage}
     * objects.
     *
     * @param image a {@code RenderedImage}.
     * @param thumbnails a {@code List} of {@code BufferedImage}s,
     * or {@code null}.
     * @param metadata an {@code IIOMetadata} object, or
     * {@code null}.
     *
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     */
    public IIOImage(RenderedImage image,
                    List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.image = image;
        this.raster = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /**
     * Constructs an {@code IIOImage} containing a
     * {@code Raster}, and thumbnails and metadata
     * associated with it.
     *
     * <p> All parameters are stored by reference.
     *
     * @param raster a {@code Raster}.
     * @param thumbnails a {@code List} of {@code BufferedImage}s,
     * or {@code null}.
     * @param metadata an {@code IIOMetadata} object, or
     * {@code null}.
     *
     * @exception IllegalArgumentException if {@code raster} is
     * {@code null}.
     */
    public IIOImage(Raster raster,
                    List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (raster == null) {
            throw new IllegalArgumentException("raster == null!");
        }
        this.raster = raster;
        this.image = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /**
     * Returns the currently set {@code RenderedImage}, or
     * {@code null} if only a {@code Raster} is available.
     *
     * @return a {@code RenderedImage}, or {@code null}.
     *
     * @see #setRenderedImage
     */
    public RenderedImage getRenderedImage() {
        synchronized(this) {
            return image;
        }
    }

    /**
     * Sets the current {@code RenderedImage}.  The value is
     * stored by reference.  Any existing {@code Raster} is
     * discarded.
     *
     * @param image a {@code RenderedImage}.
     *
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     *
     * @see #getRenderedImage
     */
    public void setRenderedImage(RenderedImage image) {
        synchronized(this) {
            if (image == null) {
                throw new IllegalArgumentException("image == null!");
            }
            this.image = image;
            this.raster = null;
        }
    }

    /**
     * Returns {@code true} if this {@code IIOImage} stores
     * a {@code Raster} rather than a {@code RenderedImage}.
     *
     * @return {@code true} if a {@code Raster} is
     * available.
     */
    public boolean hasRaster() {
        synchronized(this) {
            return (raster != null);
        }
    }

    /**
     * Returns the currently set {@code Raster}, or
     * {@code null} if only a {@code RenderedImage} is
     * available.
     *
     * @return a {@code Raster}, or {@code null}.
     *
     * @see #setRaster
     */
    public Raster getRaster() {
        synchronized(this) {
            return raster;
        }
    }

    /**
     * Sets the current {@code Raster}.  The value is
     * stored by reference.  Any existing {@code RenderedImage} is
     * discarded.
     *
     * @param raster a {@code Raster}.
     *
     * @exception IllegalArgumentException if {@code raster} is
     * {@code null}.
     *
     * @see #getRaster
     */
    public void setRaster(Raster raster) {
        synchronized(this) {
            if (raster == null) {
                throw new IllegalArgumentException("raster == null!");
            }
            this.raster = raster;
            this.image = null;
        }
    }

    /**
     * Returns the number of thumbnails stored in this
     * {@code IIOImage}.
     *
     * @return the number of thumbnails, as an {@code int}.
     */
    public int getNumThumbnails() {
        return thumbnails == null ? 0 : thumbnails.size();
    }

    /**
     * Returns a thumbnail associated with the main image.
     *
     * @param index the index of the desired thumbnail image.
     *
     * @return a thumbnail image, as a {@code BufferedImage}.
     *
     * @exception IndexOutOfBoundsException if the supplied index is
     * negative or larger than the largest valid index.
     * @exception ClassCastException if a
     * non-{@code BufferedImage} object is encountered in the
     * list of thumbnails at the given index.
     *
     * @see #getThumbnails
     * @see #setThumbnails
     */
    public BufferedImage getThumbnail(int index) {
        if (thumbnails == null) {
            throw new IndexOutOfBoundsException("No thumbnails available!");
        }
        return (BufferedImage)thumbnails.get(index);
    }

    /**
     * Returns the current {@code List} of thumbnail
     * {@code BufferedImage}s, or {@code null} if none is
     * set.  A live reference is returned.
     *
     * @return the current {@code List} of
     * {@code BufferedImage} thumbnails, or {@code null}.
     *
     * @see #getThumbnail(int)
     * @see #setThumbnails
     */
    public List<? extends BufferedImage> getThumbnails() {
        return thumbnails;
    }

    /**
     * Sets the list of thumbnails to a new {@code List} of
     * {@code BufferedImage}s, or to {@code null}.  The
     * reference to the previous {@code List} is discarded.
     *
     * <p> The {@code thumbnails} argument must either be
     * {@code null} or contain only {@code BufferedImage}
     * objects.
     *
     * @param thumbnails a {@code List} of
     * {@code BufferedImage} thumbnails, or {@code null}.
     *
     * @see #getThumbnail(int)
     * @see #getThumbnails
     */
    public void setThumbnails(List<? extends BufferedImage> thumbnails) {
        this.thumbnails = thumbnails;
    }

    /**
     * Returns a reference to the current {@code IIOMetadata}
     * object, or {@code null} is none is set.
     *
     * @return an {@code IIOMetadata} object, or {@code null}.
     *
     * @see #setMetadata
     */
    public IIOMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the {@code IIOMetadata} to a new object, or
     * {@code null}.
     *
     * @param metadata an {@code IIOMetadata} object, or
     * {@code null}.
     *
     * @see #getMetadata
     */
    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }
}
