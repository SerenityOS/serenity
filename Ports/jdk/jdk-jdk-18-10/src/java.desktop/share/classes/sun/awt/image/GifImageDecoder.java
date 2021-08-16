/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*-
 *      Reads GIF images from an InputStream and reports the
 *      image data to an InputStreamImageSource object.
 *
 * The algorithm is copyright of CompuServe.
 */
package sun.awt.image;

import java.util.Hashtable;
import java.io.InputStream;
import java.io.IOException;
import java.awt.image.*;

/**
 * Gif Image converter
 *
 * @author Arthur van Hoff
 * @author Jim Graham
 */
public class GifImageDecoder extends ImageDecoder {
    private static final boolean verbose = false;

    private static final int IMAGESEP           = 0x2c;
    private static final int EXBLOCK            = 0x21;
    private static final int EX_GRAPHICS_CONTROL= 0xf9;
    private static final int EX_COMMENT         = 0xfe;
    private static final int EX_APPLICATION     = 0xff;
    private static final int TERMINATOR         = 0x3b;
    private static final int TRANSPARENCYMASK   = 0x01;
    private static final int INTERLACEMASK      = 0x40;
    private static final int COLORMAPMASK       = 0x80;

    int num_global_colors;
    byte[] global_colormap;
    int trans_pixel = -1;
    IndexColorModel global_model;

    Hashtable<String, Object> props = new Hashtable<>();

    byte[] saved_image;
    IndexColorModel saved_model;

    int global_width;
    int global_height;
    int global_bgpixel;

    GifFrame curframe;

    public GifImageDecoder(InputStreamImageSource src, InputStream is) {
        super(src, is);
    }

    /**
     * An error has occurred. Throw an exception.
     */
    private static void error(String s1) throws ImageFormatException {
        throw new ImageFormatException(s1);
    }

    /**
     * Read a number of bytes into a buffer.
     * @return number of bytes that were not read due to EOF or error
     */
    private int readBytes(byte[] buf, int off, int len) {
        while (len > 0) {
            try {
                int n = input.read(buf, off, len);
                if (n < 0) {
                    break;
                }
                off += n;
                len -= n;
            } catch (IOException e) {
                break;
            }
        }
        return len;
    }

    private static final int ExtractByte(byte[] buf, int off) {
        return (buf[off] & 0xFF);
    }

    private static final int ExtractWord(byte[] buf, int off) {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);
    }

    /**
     * produce an image from the stream.
     */
    @SuppressWarnings({"fallthrough", "deprecation"})
    public void produceImage() throws IOException, ImageFormatException {
        try {
            readHeader();

            int totalframes = 0;
            int frameno = 0;
            int nloops = -1;
            int disposal_method = 0;
            int delay = -1;
            boolean loopsRead = false;
            boolean isAnimation = false;

            while (!aborted) {
                int code;

                switch (code = input.read()) {
                  case EXBLOCK:
                    switch (code = input.read()) {
                      case EX_GRAPHICS_CONTROL: {
                        byte[] buf = new byte[6];
                        if (readBytes(buf, 0, 6) != 0) {
                            return;//error("corrupt GIF file");
                        }
                        if ((buf[0] != 4) || (buf[5] != 0)) {
                            return;//error("corrupt GIF file (GCE size)");
                        }
                        // Get the index of the transparent color
                        delay = ExtractWord(buf, 2) * 10;
                        if (delay > 0 && !isAnimation) {
                            isAnimation = true;
                            ImageFetcher.startingAnimation();
                        }
                        disposal_method = (buf[1] >> 2) & 7;
                        if ((buf[1] & TRANSPARENCYMASK) != 0) {
                            trans_pixel = ExtractByte(buf, 4);
                        } else {
                            trans_pixel = -1;
                        }
                        break;
                      }

                      case EX_COMMENT:
                      case EX_APPLICATION:
                      default:
                        boolean loop_tag = false;
                        String comment = "";
                        while (true) {
                            int n = input.read();
                            if (n <= 0) {
                                break;
                            }
                            byte[] buf = new byte[n];
                            if (readBytes(buf, 0, n) != 0) {
                                return;//error("corrupt GIF file");
                            }
                            if (code == EX_COMMENT) {
                                comment += new String(buf, 0);
                            } else if (code == EX_APPLICATION) {
                                if (loop_tag) {
                                    if (n == 3 && buf[0] == 1) {
                                        if (loopsRead) {
                                            ExtractWord(buf, 1);
                                        }
                                        else {
                                            nloops = ExtractWord(buf, 1);
                                            loopsRead = true;
                                        }
                                    } else {
                                        loop_tag = false;
                                    }
                                }
                                if ("NETSCAPE2.0".equals(new String(buf, 0))) {
                                    loop_tag = true;
                                }
                            }
                        }
                        if (code == EX_COMMENT) {
                            props.put("comment", comment);
                        }
                        if (loop_tag && !isAnimation) {
                            isAnimation = true;
                            ImageFetcher.startingAnimation();
                        }
                        break;

                      case -1:
                        return; //error("corrupt GIF file");
                    }
                    break;

                  case IMAGESEP:
                    if (!isAnimation) {
                        input.mark(0); // we don't need the mark buffer
                    }
                    try {
                        if (!readImage(totalframes == 0,
                                       disposal_method,
                                       delay)) {
                            return;
                        }
                    } catch (Exception e) {
                        if (verbose) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    frameno++;
                    totalframes++;
                    break;

                  default:
                  case -1:
                    if (verbose) {
                        if (code == -1) {
                            System.err.println("Premature EOF in GIF file," +
                                               " frame " + frameno);
                        } else {
                            System.err.println("corrupt GIF file (parse) ["
                                               + code + "].");
                        }
                    }
                    if (frameno == 0) {
                        return;
                    }
                    // Fall through

                  case TERMINATOR:
                    if (nloops == 0 || nloops-- >= 0) {
                        try {
                            if (curframe != null) {
                                curframe.dispose();
                                curframe = null;
                            }
                            input.reset();
                            saved_image = null;
                            saved_model = null;
                            frameno = 0;
                            break;
                        } catch (IOException e) {
                            return; // Unable to reset input buffer
                        }
                    }
                    if (verbose && frameno != 1) {
                        System.out.println("processing GIF terminator,"
                                           + " frames: " + frameno
                                           + " total: " + totalframes);
                    }
                    imageComplete(ImageConsumer.STATICIMAGEDONE, true);
                    return;
                }
            }
        } finally {
            close();
        }
    }

    /**
     * Read Image header
     */
    private void readHeader() throws IOException, ImageFormatException {
        // Create a buffer
        byte[] buf = new byte[13];

        // Read the header
        if (readBytes(buf, 0, 13) != 0) {
            throw new IOException();
        }

        // Check header
        if ((buf[0] != 'G') || (buf[1] != 'I') || (buf[2] != 'F')) {
            error("not a GIF file.");
        }

        // Global width&height
        global_width = ExtractWord(buf, 6);
        global_height = ExtractWord(buf, 8);

        // colormap info
        int ch = ExtractByte(buf, 10);
        if ((ch & COLORMAPMASK) == 0) {
            // no global colormap so make up our own
            // If there is a local colormap, it will override what we
            // have here.  If there is not a local colormap, the rules
            // for GIF89 say that we can use whatever colormap we want.
            // This means that we should probably put in a full 256 colormap
            // at some point.  REMIND!
            num_global_colors = 2;
            global_bgpixel = 0;
            global_colormap = new byte[2*3];
            global_colormap[0] = global_colormap[1] = global_colormap[2] = (byte)0;
            global_colormap[3] = global_colormap[4] = global_colormap[5] = (byte)255;

        }
        else {
            num_global_colors = 1 << ((ch & 0x7) + 1);

            global_bgpixel = ExtractByte(buf, 11);

            if (buf[12] != 0) {
                props.put("aspectratio", ""+((ExtractByte(buf, 12) + 15) / 64.0));
            }

            // Read colors
            global_colormap = new byte[num_global_colors * 3];
            if (readBytes(global_colormap, 0, num_global_colors * 3) != 0) {
                throw new IOException();
            }
        }
        input.mark(Integer.MAX_VALUE); // set this mark in case this is an animated GIF
    }

    /**
     * The ImageConsumer hints flag for a non-interlaced GIF image.
     */
    private static final int normalflags =
        ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES |
        ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME;

    /**
     * The ImageConsumer hints flag for an interlaced GIF image.
     */
    private static final int interlaceflags =
        ImageConsumer.RANDOMPIXELORDER | ImageConsumer.COMPLETESCANLINES |
        ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME;

    private short[] prefix  = new short[4096];
    private byte[]  suffix  = new byte[4096];
    private byte[]  outCode = new byte[4097];

    private static native void initIDs();

    static {
        /* ensure that the necessary native libraries are loaded */
        NativeLibLoader.loadLibraries();
        initIDs();
    }

    private native boolean parseImage(int x, int y, int width, int height,
                                      boolean interlace, int initCodeSize,
                                      byte[] block, byte[] rasline,
                                      IndexColorModel model);

    private int sendPixels(int x, int y, int width, int height,
                           byte[] rasline, ColorModel model) {
        int rasbeg, rasend, x2;
        if (y < 0) {
            height += y;
            y = 0;
        }
        if (y + height > global_height) {
            height = global_height - y;
        }
        if (height <= 0) {
            return 1;
        }
        // rasline[0]     == pixel at coordinate (x,y)
        // rasline[width] == pixel at coordinate (x+width, y)
        if (x < 0) {
            rasbeg = -x;
            width += x;         // same as (width -= rasbeg)
            x2 = 0;             // same as (x2     = x + rasbeg)
        } else {
            rasbeg = 0;
            // width -= 0;      // same as (width -= rasbeg)
            x2 = x;             // same as (x2     = x + rasbeg)
        }
        // rasline[rasbeg]          == pixel at coordinate (x2,y)
        // rasline[width]           == pixel at coordinate (x+width, y)
        // rasline[rasbeg + width]  == pixel at coordinate (x2+width, y)
        if (x2 + width > global_width) {
            width = global_width - x2;
        }
        if (width <= 0) {
            return 1;
        }
        rasend = rasbeg + width;
        // rasline[rasbeg] == pixel at coordinate (x2,y)
        // rasline[rasend] == pixel at coordinate (x2+width, y)
        int off = y * global_width + x2;
        boolean save = (curframe.disposal_method == GifFrame.DISPOSAL_SAVE);
        if (trans_pixel >= 0 && !curframe.initialframe) {
            if (saved_image != null && model.equals(saved_model)) {
                for (int i = rasbeg; i < rasend; i++, off++) {
                    byte pixel = rasline[i];
                    if ((pixel & 0xff) == trans_pixel) {
                        rasline[i] = saved_image[off];
                    } else if (save) {
                        saved_image[off] = pixel;
                    }
                }
            } else {
                // We have to do this the hard way - only transmit
                // the non-transparent sections of the line...
                // Fix for 6301050: the interlacing is ignored in this case
                // in order to avoid artefacts in case of animated images.
                int runstart = -1;
                int count = 1;
                for (int i = rasbeg; i < rasend; i++, off++) {
                    byte pixel = rasline[i];
                    if ((pixel & 0xff) == trans_pixel) {
                        if (runstart >= 0) {
                            count = setPixels(x + runstart, y,
                                              i - runstart, 1,
                                              model, rasline,
                                              runstart, 0);
                            if (count == 0) {
                                break;
                            }
                        }
                        runstart = -1;
                    } else {
                        if (runstart < 0) {
                            runstart = i;
                        }
                        if (save) {
                            saved_image[off] = pixel;
                        }
                    }
                }
                if (runstart >= 0) {
                    count = setPixels(x + runstart, y,
                                      rasend - runstart, 1,
                                      model, rasline,
                                      runstart, 0);
                }
                return count;
            }
        } else if (save) {
            System.arraycopy(rasline, rasbeg, saved_image, off, width);
        }
        int count = setPixels(x2, y, width, height, model,
                              rasline, rasbeg, 0);
        return count;
    }

    /**
     * Read Image data
     */
    private boolean readImage(boolean first, int disposal_method, int delay)
        throws IOException
    {
        if (curframe != null && !curframe.dispose()) {
            abort();
            return false;
        }

        long tm = 0;

        if (verbose) {
            tm = System.currentTimeMillis();
        }

        // Allocate the buffer
        byte[] block = new byte[256 + 3];

        // Read the image descriptor
        if (readBytes(block, 0, 10) != 0) {
            throw new IOException();
        }
        int x = ExtractWord(block, 0);
        int y = ExtractWord(block, 2);
        int width = ExtractWord(block, 4);
        int height = ExtractWord(block, 6);

        /*
         * Majority of gif images have
         * same logical screen and frame dimensions.
         * Also, Photoshop and Mozilla seem to use the logical
         * screen dimension (from the global stream header)
         * if frame dimension is invalid.
         *
         * We use similar heuristic and trying to recover
         * frame width from logical screen dimension and
         * frame offset.
         */
        if (width == 0 && global_width != 0) {
            width = global_width - x;
        }
        if (height == 0 && global_height != 0) {
            height = global_height - y;
        }

        boolean interlace = (block[8] & INTERLACEMASK) != 0;

        IndexColorModel model = global_model;

        if ((block[8] & COLORMAPMASK) != 0) {
            // We read one extra byte above so now when we must
            // transfer that byte as the first colormap byte
            // and manually read the code size when we are done
            int num_local_colors = 1 << ((block[8] & 0x7) + 1);

            // Read local colors
            byte[] local_colormap = new byte[num_local_colors * 3];
            local_colormap[0] = block[9];
            if (readBytes(local_colormap, 1, num_local_colors * 3 - 1) != 0) {
                throw new IOException();
            }

            // Now read the "real" code size byte which follows
            // the local color table
            if (readBytes(block, 9, 1) != 0) {
                throw new IOException();
            }
            if (trans_pixel >= num_local_colors) {
                // Fix for 4233748: extend colormap to contain transparent pixel
                num_local_colors = trans_pixel + 1;
                local_colormap = grow_colormap(local_colormap, num_local_colors);
            }
            model = new IndexColorModel(8, num_local_colors, local_colormap,
                                        0, false, trans_pixel);
        } else if (model == null
                   || trans_pixel != model.getTransparentPixel()) {
            if (trans_pixel >= num_global_colors) {
                // Fix for 4233748: extend colormap to contain transparent pixel
                num_global_colors = trans_pixel + 1;
                global_colormap = grow_colormap(global_colormap, num_global_colors);
            }
            model = new IndexColorModel(8, num_global_colors, global_colormap,
                                        0, false, trans_pixel);
            global_model = model;
        }

        // Notify the consumers
        if (first) {
            if (global_width == 0) global_width = width;
            if (global_height == 0) global_height = height;

            setDimensions(global_width, global_height);
            setProperties(props);
            setColorModel(model);
            headerComplete();
        }

        if (disposal_method == GifFrame.DISPOSAL_SAVE && saved_image == null) {
            saved_image = new byte[global_width * global_height];
            /*
             * If height of current image is smaller than the global height,
             * fill the gap with transparent pixels.
             */
            if ((height < global_height) && (model != null)) {
                byte tpix = (byte)model.getTransparentPixel();
                if (tpix >= 0) {
                    byte[] trans_rasline = new byte[global_width];
                    for (int i=0; i<global_width;i++) {
                        trans_rasline[i] = tpix;
                    }

                    setPixels(0, 0, global_width, y,
                              model, trans_rasline, 0, 0);
                    setPixels(0, y+height, global_width,
                              global_height-height-y, model, trans_rasline,
                              0, 0);
                }
            }
        }

        int hints = (interlace ? interlaceflags : normalflags);
        setHints(hints);

        curframe = new GifFrame(this, disposal_method, delay,
                                (curframe == null), model,
                                x, y, width, height);

        // allocate the raster data
        byte[] rasline = new byte[width];

        if (verbose) {
            System.out.print("Reading a " + width + " by " + height + " " +
                      (interlace ? "" : "non-") + "interlaced image...");
        }
        int initCodeSize = ExtractByte(block, 9);
        if (initCodeSize >= 12) {
            if (verbose) {
                System.out.println("Invalid initial code size: " +
                                   initCodeSize);
            }
            return false;
        }
        boolean ret = parseImage(x, y, width, height,
                                 interlace, initCodeSize,
                                 block, rasline, model);

        if (!ret) {
            abort();
        }

        if (verbose) {
            System.out.println("done in "
                               + (System.currentTimeMillis() - tm)
                               + "ms");
        }

        return ret;
    }

    public static byte[] grow_colormap(byte[] colormap, int newlen) {
        byte[] newcm = new byte[newlen * 3];
        System.arraycopy(colormap, 0, newcm, 0, colormap.length);
        return newcm;
    }
}

class GifFrame {
    private static final boolean verbose = false;

    static final int DISPOSAL_NONE      = 0x00;
    static final int DISPOSAL_SAVE      = 0x01;
    static final int DISPOSAL_BGCOLOR   = 0x02;
    static final int DISPOSAL_PREVIOUS  = 0x03;

    GifImageDecoder decoder;

    int disposal_method;
    int delay;

    IndexColorModel model;

    int x;
    int y;
    int width;
    int height;

    boolean initialframe;

    public GifFrame(GifImageDecoder id, int dm, int dl, boolean init,
                    IndexColorModel cm, int x, int y, int w, int h) {
        this.decoder = id;
        this.disposal_method = dm;
        this.delay = dl;
        this.model = cm;
        this.initialframe = init;
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    private void setPixels(int x, int y, int w, int h,
                           ColorModel cm, byte[] pix, int off, int scan) {
        decoder.setPixels(x, y, w, h, cm, pix, off, scan);
    }

    public boolean dispose() {
        if (decoder.imageComplete(ImageConsumer.SINGLEFRAMEDONE, false) == 0) {
            return false;
        } else {
            if (delay > 0) {
                try {
                    if (verbose) {
                        System.out.println("sleeping: "+delay);
                    }
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return false;
                }
            } else {
                Thread.yield();
            }

            if (verbose && disposal_method != 0) {
                System.out.println("disposal method: "+disposal_method);
            }

            int global_width = decoder.global_width;
            int global_height = decoder.global_height;

            if (x < 0) {
                width += x;
                x = 0;
            }
            if (x + width > global_width) {
                width = global_width - x;
            }
            if (width <= 0) {
                disposal_method = DISPOSAL_NONE;
            } else {
                if (y < 0) {
                    height += y;
                    y = 0;
                }
                if (y + height > global_height) {
                    height = global_height - y;
                }
                if (height <= 0) {
                    disposal_method = DISPOSAL_NONE;
                }
            }

            switch (disposal_method) {
            case DISPOSAL_PREVIOUS:
                byte[] saved_image = decoder.saved_image;
                IndexColorModel saved_model = decoder.saved_model;
                if (saved_image != null) {
                    setPixels(x, y, width, height,
                              saved_model, saved_image,
                              y * global_width + x, global_width);
                }
                break;
            case DISPOSAL_BGCOLOR:
                byte tpix;
                if (model.getTransparentPixel() < 0) {
                    tpix = 0;
                } else {
                    tpix = (byte) model.getTransparentPixel();
                }
                byte[] rasline = new byte[width];
                if (tpix != 0) {
                    for (int i = 0; i < width; i++) {
                        rasline[i] = tpix;
                    }
                }

                // clear saved_image using transparent pixels
                // this will be used as the background in the next display
                if( decoder.saved_image != null ) {
                    for( int i = 0; i < global_width * global_height; i ++ )
                        decoder.saved_image[i] = tpix;
                }

                setPixels(x, y, width, height, model, rasline, 0, 0);
                break;
            case DISPOSAL_SAVE:
                decoder.saved_model = model;
                break;
            }
        }
        return true;
    }
}
