/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.plugins.tiff.TIFFTagSet;

public class TIFFRenderedImage implements RenderedImage {

    private TIFFImageReader reader;
    private int imageIndex;
    private ImageReadParam tileParam;

    private int subsampleX;
    private int subsampleY;

    private boolean isSubsampling;

    private int width;
    private int height;
    private int tileWidth;
    private int tileHeight;

    private ImageTypeSpecifier its;

    public TIFFRenderedImage(TIFFImageReader reader,
                             int imageIndex,
                             ImageReadParam readParam,
                             int width, int height) throws IOException {
        this.reader = reader;
        this.imageIndex = imageIndex;
        this.tileParam = cloneImageReadParam(readParam, false);

        this.subsampleX = tileParam.getSourceXSubsampling();
        this.subsampleY = tileParam.getSourceYSubsampling();

        this.isSubsampling = this.subsampleX != 1 || this.subsampleY != 1;

        this.width = width/subsampleX;
        this.height = height/subsampleY;

        // If subsampling is being used, we may not match the
        // true tile grid exactly, but everything should still work
        this.tileWidth = reader.getTileWidth(imageIndex)/subsampleX;
        this.tileHeight = reader.getTileHeight(imageIndex)/subsampleY;

        Iterator<ImageTypeSpecifier> iter = reader.getImageTypes(imageIndex);
        this.its = iter.next();
        tileParam.setDestinationType(its);
    }

    /**
     * Creates a copy of {@code param}. The source subsampling and
     * and bands settings and the destination bands and offset settings
     * are copied. If {@code param} is a {@code TIFFImageReadParam}
     * then the {@code TIFFDecompressor} and
     * {@code TIFFColorConverter} settings are also copied; otherwise
     * they are explicitly set to {@code null}.
     *
     * @param param the parameters to be copied.
     * @param copyTagSets whether the {@code TIFFTagSet} settings
     * should be copied if set.
     * @return copied parameters.
     */
    private ImageReadParam cloneImageReadParam(ImageReadParam param,
                                               boolean copyTagSets) {
        // Create a new TIFFImageReadParam.
        TIFFImageReadParam newParam = new TIFFImageReadParam();

        // Copy the basic settings.
        newParam.setSourceSubsampling(param.getSourceXSubsampling(),
                                      param.getSourceYSubsampling(),
                                      param.getSubsamplingXOffset(),
                                      param.getSubsamplingYOffset());
        newParam.setSourceBands(param.getSourceBands());
        newParam.setDestinationBands(param.getDestinationBands());
        newParam.setDestinationOffset(param.getDestinationOffset());

        if (param instanceof TIFFImageReadParam && copyTagSets) {
            // Copy the settings from the input parameter.
            TIFFImageReadParam tparam = (TIFFImageReadParam) param;

            List<TIFFTagSet> tagSets = tparam.getAllowedTagSets();
            if (tagSets != null) {
                Iterator<TIFFTagSet> tagSetIter = tagSets.iterator();
                if (tagSetIter != null) {
                    while (tagSetIter.hasNext()) {
                        TIFFTagSet tagSet = tagSetIter.next();
                        newParam.addAllowedTagSet(tagSet);
                    }
                }
            }
        }

        return newParam;
    }

    public Vector<RenderedImage> getSources() {
        return null;
    }

    public Object getProperty(String name) {
        return java.awt.Image.UndefinedProperty;
    }

    public String[] getPropertyNames() {
        return null;
    }

    public ColorModel getColorModel() {
        return its.getColorModel();
    }

    public SampleModel getSampleModel() {
        return its.getSampleModel();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMinX() {
        return 0;
    }

    public int getMinY() {
        return 0;
    }

    public int getNumXTiles() {
        return (width + tileWidth - 1)/tileWidth;
    }

    public int getNumYTiles() {
        return (height + tileHeight - 1)/tileHeight;
    }

    public int getMinTileX() {
        return 0;
    }

    public int getMinTileY() {
        return 0;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileGridXOffset() {
        return 0;
    }

    public int getTileGridYOffset() {
        return 0;
    }

    public Raster getTile(int tileX, int tileY) {
        Rectangle tileRect = new Rectangle(tileX*tileWidth,
                                           tileY*tileHeight,
                                           tileWidth,
                                           tileHeight);
        return getData(tileRect);
    }

    public Raster getData() {
        return read(new Rectangle(0, 0, getWidth(), getHeight()));
    }

    public Raster getData(Rectangle rect) {
        return read(rect);
    }

    // This method needs to be synchronized as it updates the instance
    // variable 'tileParam'.
    public synchronized WritableRaster read(Rectangle rect) {
        tileParam.setSourceRegion(isSubsampling ?
                                  new Rectangle(subsampleX*rect.x,
                                                subsampleY*rect.y,
                                                subsampleX*rect.width,
                                                subsampleY*rect.height) :
                                  rect);

        try {
            BufferedImage bi = reader.read(imageIndex, tileParam);
            WritableRaster ras = bi.getRaster();
            return ras.createWritableChild(0, 0,
                                           ras.getWidth(), ras.getHeight(),
                                           rect.x, rect.y,
                                           null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public WritableRaster copyData(WritableRaster raster) {
        if (raster == null) {
            return read(new Rectangle(0, 0, getWidth(), getHeight()));
        } else {
            Raster src = read(raster.getBounds());
            raster.setRect(src);
            return raster;
        }
    }
}
