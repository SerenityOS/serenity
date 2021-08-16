/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;

public class TIFFYCbCrDecompressor extends TIFFDecompressor {
    // Store constants in S15.16 format
    private static final int FRAC_BITS = 16;
    private static final float FRAC_SCALE = (float)(1 << FRAC_BITS);

    private float lumaRed = 0.299f;
    private float lumaGreen = 0.587f;
    private float lumaBlue = 0.114f;

    private float referenceBlackY = 0.0f;
    private float referenceWhiteY = 255.0f;

    private float referenceBlackCb = 128.0f;
    private float referenceWhiteCb = 255.0f;

    private float referenceBlackCr = 128.0f;
    private float referenceWhiteCr = 255.0f;

    private float codingRangeY = 255.0f;

    private int[] iYTab = new int[256];
    private int[] iCbTab = new int[256];
    private int[] iCrTab = new int[256];

    private int[] iGYTab = new int[256];
    private int[] iGCbTab = new int[256];
    private int[] iGCrTab = new int[256];

    private int chromaSubsampleH = 2;
    private int chromaSubsampleV = 2;

    private boolean colorConvert;

    private TIFFDecompressor decompressor;

    private BufferedImage tmpImage;

    //
    // If 'decompressor' is not null then it reads the data from the
    // actual stream first and passes the result on to YCrCr decompression
    // and possibly color conversion.
    //

    public TIFFYCbCrDecompressor(TIFFDecompressor decompressor,
                                 boolean colorConvert) {
        this.decompressor = decompressor;
        this.colorConvert = colorConvert;
    }

    private void warning(String message) {
        if(this.reader instanceof TIFFImageReader) {
            ((TIFFImageReader)reader).forwardWarningMessage(message);
        }
    }

    //
    // "Chained" decompressor methods.
    //

    public void setReader(ImageReader reader) {
        if(decompressor != null) {
            decompressor.setReader(reader);
        }
        super.setReader(reader);
    }

    public void setMetadata(IIOMetadata metadata) {
        if(decompressor != null) {
            decompressor.setMetadata(metadata);
        }
        super.setMetadata(metadata);
    }

    public void setPhotometricInterpretation(int photometricInterpretation) {
        if(decompressor != null) {
            decompressor.setPhotometricInterpretation(photometricInterpretation);
        }
        super.setPhotometricInterpretation(photometricInterpretation);
    }

    public void setCompression(int compression) {
        if(decompressor != null) {
            decompressor.setCompression(compression);
        }
        super.setCompression(compression);
    }

    public void setPlanar(boolean planar) {
        if(decompressor != null) {
            decompressor.setPlanar(planar);
        }
        super.setPlanar(planar);
    }

    public void setSamplesPerPixel(int samplesPerPixel) {
        if(decompressor != null) {
            decompressor.setSamplesPerPixel(samplesPerPixel);
        }
        super.setSamplesPerPixel(samplesPerPixel);
    }

    public void setBitsPerSample(int[] bitsPerSample) {
        if(decompressor != null) {
            decompressor.setBitsPerSample(bitsPerSample);
        }
        super.setBitsPerSample(bitsPerSample);
    }

    public void setSampleFormat(int[] sampleFormat) {
        if(decompressor != null) {
            decompressor.setSampleFormat(sampleFormat);
        }
        super.setSampleFormat(sampleFormat);
    }

    public void setExtraSamples(int[] extraSamples) {
        if(decompressor != null) {
            decompressor.setExtraSamples(extraSamples);
        }
        super.setExtraSamples(extraSamples);
    }

    public void setColorMap(char[] colorMap) {
        if(decompressor != null) {
            decompressor.setColorMap(colorMap);
        }
        super.setColorMap(colorMap);
    }

    public void setStream(ImageInputStream stream) {
        if(decompressor != null) {
            decompressor.setStream(stream);
        } else {
            super.setStream(stream);
        }
    }

    public void setOffset(long offset) {
        if(decompressor != null) {
            decompressor.setOffset(offset);
        }
        super.setOffset(offset);
    }

    public void setByteCount(int byteCount) {
        if(decompressor != null) {
            decompressor.setByteCount(byteCount);
        }
        super.setByteCount(byteCount);
    }

    public void setSrcMinX(int srcMinX) {
        if(decompressor != null) {
            decompressor.setSrcMinX(srcMinX);
        }
        super.setSrcMinX(srcMinX);
    }

    public void setSrcMinY(int srcMinY) {
        if(decompressor != null) {
            decompressor.setSrcMinY(srcMinY);
        }
        super.setSrcMinY(srcMinY);
    }

    public void setSrcWidth(int srcWidth) {
        if(decompressor != null) {
            decompressor.setSrcWidth(srcWidth);
        }
        super.setSrcWidth(srcWidth);
    }

    public void setSrcHeight(int srcHeight) {
        if(decompressor != null) {
            decompressor.setSrcHeight(srcHeight);
        }
        super.setSrcHeight(srcHeight);
    }

    public void setSourceXOffset(int sourceXOffset) {
        if(decompressor != null) {
            decompressor.setSourceXOffset(sourceXOffset);
        }
        super.setSourceXOffset(sourceXOffset);
    }

    public void setDstXOffset(int dstXOffset) {
        if(decompressor != null) {
            decompressor.setDstXOffset(dstXOffset);
        }
        super.setDstXOffset(dstXOffset);
    }

    public void setSourceYOffset(int sourceYOffset) {
        if(decompressor != null) {
            decompressor.setSourceYOffset(sourceYOffset);
        }
        super.setSourceYOffset(sourceYOffset);
    }

    public void setDstYOffset(int dstYOffset) {
        if(decompressor != null) {
            decompressor.setDstYOffset(dstYOffset);
        }
        super.setDstYOffset(dstYOffset);
    }

    /* Should not need to override these mutators as subsampling
       should not be done by the wrapped decompressor.
    public void setSubsampleX(int subsampleX) {
        if(decompressor != null) {
            decompressor.setSubsampleX(subsampleX);
        }
        super.setSubsampleX(subsampleX);
    }

    public void setSubsampleY(int subsampleY) {
        if(decompressor != null) {
            decompressor.setSubsampleY(subsampleY);
        }
        super.setSubsampleY(subsampleY);
    }
    */

    public void setSourceBands(int[] sourceBands) {
        if(decompressor != null) {
            decompressor.setSourceBands(sourceBands);
        }
        super.setSourceBands(sourceBands);
    }

    public void setDestinationBands(int[] destinationBands) {
        if(decompressor != null) {
            decompressor.setDestinationBands(destinationBands);
        }
        super.setDestinationBands(destinationBands);
    }

    public void setImage(BufferedImage image) {
        if(decompressor != null) {
            ColorModel cm = image.getColorModel();
            tmpImage =
                new BufferedImage(cm,
                                  image.getRaster().createCompatibleWritableRaster(1, 1),
                                  cm.isAlphaPremultiplied(),
                                  null);
            decompressor.setImage(tmpImage);
        }
        super.setImage(image);
    }

    public void setDstMinX(int dstMinX) {
        if(decompressor != null) {
            decompressor.setDstMinX(dstMinX);
        }
        super.setDstMinX(dstMinX);
    }

    public void setDstMinY(int dstMinY) {
        if(decompressor != null) {
            decompressor.setDstMinY(dstMinY);
        }
        super.setDstMinY(dstMinY);
    }

    public void setDstWidth(int dstWidth) {
        if(decompressor != null) {
            decompressor.setDstWidth(dstWidth);
        }
        super.setDstWidth(dstWidth);
    }

    public void setDstHeight(int dstHeight) {
        if(decompressor != null) {
            decompressor.setDstHeight(dstHeight);
        }
        super.setDstHeight(dstHeight);
    }

    public void setActiveSrcMinX(int activeSrcMinX) {
        if(decompressor != null) {
            decompressor.setActiveSrcMinX(activeSrcMinX);
        }
        super.setActiveSrcMinX(activeSrcMinX);
    }

    public void setActiveSrcMinY(int activeSrcMinY) {
        if(decompressor != null) {
            decompressor.setActiveSrcMinY(activeSrcMinY);
        }
        super.setActiveSrcMinY(activeSrcMinY);
    }

    public void setActiveSrcWidth(int activeSrcWidth) {
        if(decompressor != null) {
            decompressor.setActiveSrcWidth(activeSrcWidth);
        }
        super.setActiveSrcWidth(activeSrcWidth);
    }

    public void setActiveSrcHeight(int activeSrcHeight) {
        if(decompressor != null) {
            decompressor.setActiveSrcHeight(activeSrcHeight);
        }
        super.setActiveSrcHeight(activeSrcHeight);
    }

    private byte clamp(int f) {
        if (f < 0) {
            return (byte)0;
        } else if (f > 255*65536) {
            return (byte)255;
        } else {
            return (byte)(f >> 16);
        }
    }

    public void beginDecoding() {
        if(decompressor != null) {
            decompressor.beginDecoding();
        }

        TIFFImageMetadata tmetadata = (TIFFImageMetadata)metadata;
        TIFFField f;

        f = tmetadata.getTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING);
        if (f != null) {
            if (f.getCount() == 2) {
                this.chromaSubsampleH = f.getAsInt(0);
                this.chromaSubsampleV = f.getAsInt(1);

                if (chromaSubsampleH != 1 && chromaSubsampleH != 2 &&
                    chromaSubsampleH != 4) {
                    warning("Y_CB_CR_SUBSAMPLING[0] has illegal value " +
                            chromaSubsampleH +
                            " (should be 1, 2, or 4), setting to 1");
                    chromaSubsampleH = 1;
                }

                if (chromaSubsampleV != 1 && chromaSubsampleV != 2 &&
                    chromaSubsampleV != 4) {
                    warning("Y_CB_CR_SUBSAMPLING[1] has illegal value " +
                            chromaSubsampleV +
                            " (should be 1, 2, or 4), setting to 1");
                    chromaSubsampleV = 1;
                }
            } else {
                warning("Y_CB_CR_SUBSAMPLING count != 2, " +
                        "assuming no subsampling");
            }
        }

        f =
           tmetadata.getTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_COEFFICIENTS);
        if (f != null) {
            if (f.getCount() == 3) {
                this.lumaRed = f.getAsFloat(0);
                this.lumaGreen = f.getAsFloat(1);
                this.lumaBlue = f.getAsFloat(2);
            } else {
                warning("Y_CB_CR_COEFFICIENTS count != 3, " +
                        "assuming default values for CCIR 601-1");
            }
        }

        f =
          tmetadata.getTIFFField(BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE);
        if (f != null) {
            if (f.getCount() == 6) {
                this.referenceBlackY = f.getAsFloat(0);
                this.referenceWhiteY = f.getAsFloat(1);
                this.referenceBlackCb = f.getAsFloat(2);
                this.referenceWhiteCb = f.getAsFloat(3);
                this.referenceBlackCr = f.getAsFloat(4);
                this.referenceWhiteCr = f.getAsFloat(5);
            } else {
                warning("REFERENCE_BLACK_WHITE count != 6, ignoring it");
            }
        } else {
                warning("REFERENCE_BLACK_WHITE not found, assuming 0-255/128-255/128-255");
        }

        this.colorConvert = true;

        float BCb = (2.0f - 2.0f*lumaBlue);
        float RCr = (2.0f - 2.0f*lumaRed);

        float GY = (1.0f - lumaBlue - lumaRed)/lumaGreen;
        float GCb = 2.0f*lumaBlue*(lumaBlue - 1.0f)/lumaGreen;
        float GCr = 2.0f*lumaRed*(lumaRed - 1.0f)/lumaGreen;

        for (int i = 0; i < 256; i++) {
            float fY = (i - referenceBlackY)*codingRangeY/
                (referenceWhiteY - referenceBlackY);
            float fCb = (i - referenceBlackCb)*127.0f/
                (referenceWhiteCb - referenceBlackCb);
            float fCr = (i - referenceBlackCr)*127.0f/
                (referenceWhiteCr - referenceBlackCr);

            iYTab[i] = (int)(fY*FRAC_SCALE);
            iCbTab[i] = (int)(fCb*BCb*FRAC_SCALE);
            iCrTab[i] = (int)(fCr*RCr*FRAC_SCALE);

            iGYTab[i] = (int)(fY*GY*FRAC_SCALE);
            iGCbTab[i] = (int)(fCb*GCb*FRAC_SCALE);
            iGCrTab[i] = (int)(fCr*GCr*FRAC_SCALE);
        }
    }

    public void decodeRaw(byte[] buf,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        int elementsPerPacket = chromaSubsampleH*chromaSubsampleV + 2;
        byte[] packet = new byte[elementsPerPacket];

        if(decompressor != null) {
            int bytesPerRow = 3*srcWidth;
            byte[] tmpBuf = new byte[bytesPerRow*srcHeight];
            decompressor.decodeRaw(tmpBuf, dstOffset, bitsPerPixel,
                                   bytesPerRow);
            ByteArrayInputStream byteStream =
                new ByteArrayInputStream(tmpBuf);
            stream = new MemoryCacheImageInputStream(byteStream);
        } else {
            stream.seek(offset);
        }

        for (int y = srcMinY; y < srcMinY + srcHeight; y += chromaSubsampleV) {
            // Decode chromaSubsampleV rows
            for (int x = srcMinX; x < srcMinX + srcWidth;
                 x += chromaSubsampleH) {
                try {
                    stream.readFully(packet);
                } catch (EOFException e) {
                    return;
                }

                byte Cb = packet[elementsPerPacket - 2];
                byte Cr = packet[elementsPerPacket - 1];

                int iCb  = 0, iCr = 0, iGCb = 0, iGCr = 0;

                if (colorConvert) {
                    int Cbp = Cb & 0xff;
                    int Crp = Cr & 0xff;

                    iCb = iCbTab[Cbp];
                    iCr = iCrTab[Crp];

                    iGCb = iGCbTab[Cbp];
                    iGCr = iGCrTab[Crp];
                }

                int yIndex = 0;
                for (int v = 0; v < chromaSubsampleV; v++) {
                    int idx = dstOffset + 3*(x - srcMinX) +
                        scanlineStride*(y - srcMinY + v);

                    // Check if we reached the last scanline
                    if (y + v >= srcMinY + srcHeight) {
                        break;
                    }

                    for (int h = 0; h < chromaSubsampleH; h++) {
                        if (x + h >= srcMinX + srcWidth) {
                            break;
                        }

                        byte Y = packet[yIndex++];

                        if (colorConvert) {
                            int Yp = Y & 0xff;
                            int iY = iYTab[Yp];
                            int iGY = iGYTab[Yp];

                            int iR = iY + iCr;
                            int iG = iGY + iGCb + iGCr;
                            int iB = iY + iCb;

                            byte r = clamp(iR);
                            byte g = clamp(iG);
                            byte b = clamp(iB);

                            buf[idx] = r;
                            buf[idx + 1] = g;
                            buf[idx + 2] = b;
                        } else {
                            buf[idx] = Y;
                            buf[idx + 1] = Cb;
                            buf[idx + 2] = Cr;
                        }

                        idx += 3;
                    }
                }
            }
        }
    }
}
