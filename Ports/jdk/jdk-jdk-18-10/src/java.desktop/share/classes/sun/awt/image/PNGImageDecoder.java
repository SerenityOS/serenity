/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.image.*;
import java.awt.Color;

/** PNG - Portable Network Graphics - image file reader.
    See <a href=http://www.ietf.org/rfc/rfc2083.txt>RFC2083</a> for details. */
public class PNGImageDecoder extends ImageDecoder
{
    private static final int GRAY=0;
    private static final int PALETTE=1;
    private static final int COLOR=2;
    private static final int ALPHA=4;

    private static final int bKGDChunk = 0x624B4744;
    private static final int cHRMChunk = 0x6348524D;
    private static final int gAMAChunk = 0x67414D41;
    private static final int hISTChunk = 0x68495354;
    private static final int IDATChunk = 0x49444154;
    private static final int IENDChunk = 0x49454E44;
    private static final int IHDRChunk = 0x49484452;
    private static final int PLTEChunk = 0x504C5445;
    private static final int pHYsChunk = 0x70485973;
    private static final int sBITChunk = 0x73424954;
    private static final int tEXtChunk = 0x74455874;
    private static final int tIMEChunk = 0x74494D45;
    private static final int tRNSChunk = 0x74524E53;
    private static final int zTXtChunk = 0x7A545874;

    private int width;
    private int height;
    private int bitDepth;
    private int colorType;
    private int compressionMethod;
    private int filterMethod;
    private int interlaceMethod;
    private int gamma = 100000;
    private java.util.Hashtable<String, Object> properties;
    private ColorModel cm;
    private byte[] red_map, green_map, blue_map, alpha_map;
    private int transparentPixel = -1;
    private byte[]  transparentPixel_16 = null; // we need 6 bytes to store 16bpp value
    private static ColorModel[] greyModels = new ColorModel[4];
    private void property(String key,Object value) {
        if(value==null) return;
        if(properties==null) properties=new java.util.Hashtable<>();
        properties.put(key,value);
    }
    private void property(String key,float value) {
        property(key, Float.valueOf(value));
    }
    private void pngassert(boolean b) throws IOException {
        if(!b) {
            PNGException e = new PNGException("Broken file");
            e.printStackTrace();
            throw e;
        }
    }
    protected boolean handleChunk(int key, byte[] buf, int st, int len)
        throws IOException {
        switch(key) {
            case bKGDChunk:
                Color c = null;
                switch(colorType) {
                    case COLOR:
                    case COLOR|ALPHA:
                        pngassert(len==6);
                        c = new Color(buf[st]&0xff,buf[st+2]&0xff,buf[st+4]&0xff);
                        break;
                    case COLOR|PALETTE:
                    case COLOR|PALETTE|ALPHA:
                        pngassert(len==1);
                        int ix = buf[st]&0xFF;
                        pngassert(red_map!=null && ix<red_map.length);
                        c = new Color(red_map[ix]&0xff,green_map[ix]&0xff,blue_map[ix]&0xff);
                        break;
                    case GRAY:
                    case GRAY|ALPHA:
                        pngassert(len==2);
                        int t = buf[st]&0xFF;
                        c = new Color(t,t,t);
                        break;
                }
                if(c!=null) property("background",c);
                break;
            case cHRMChunk:
                property("chromaticities",
                    new Chromaticities(
                        getInt(st),
                        getInt(st+4),
                        getInt(st+8),
                        getInt(st+12),
                        getInt(st+16),
                        getInt(st+20),
                        getInt(st+24),
                        getInt(st+28)));
                break;
            case gAMAChunk:
                if(len!=4) throw new PNGException("bogus gAMA");
                gamma = getInt(st);
                if(gamma!=100000) property("gamma",gamma/100000.0f);
                break;
            case hISTChunk: break;
            case IDATChunk: return false;
            case IENDChunk: break;
            case IHDRChunk:
                if(len!=13
                    ||(width = getInt(st))==0
                    ||(height = getInt(st+4))==0
                    ) throw new PNGException("bogus IHDR");
                bitDepth = getByte(st+8);
                colorType = getByte(st+9);
                compressionMethod = getByte(st+10);
                filterMethod = getByte(st+11);
                interlaceMethod = getByte(st+12);
                break;
            case PLTEChunk:
                {   int tsize = len/3;
                    red_map = new byte[tsize];
                    green_map = new byte[tsize];
                    blue_map = new byte[tsize];
                    for(int i=0,j=st; i<tsize; i++, j+=3) {
                        red_map[i] = buf[j];
                        green_map[i] = buf[j+1];
                        blue_map[i] = buf[j+2];
                    }
                }
                break;
            case pHYsChunk: break;
            case sBITChunk: break;
            case tEXtChunk:
                int klen = 0;
                while(klen<len && buf[st+klen]!=0) klen++;
                if(klen<len) {
                    String tkey = new String(buf,st,klen);
                    String tvalue = new String(buf,st+klen+1,len-klen-1);
                    property(tkey,tvalue);
                }
                break;
            case tIMEChunk:
                property("modtime",new GregorianCalendar(
                    getShort(st+0),
                    getByte(st+2)-1,
                    getByte(st+3),
                    getByte(st+4),
                    getByte(st+5),
                    getByte(st+6)).getTime());
                break;
            case tRNSChunk:
                switch(colorType) {
                    case PALETTE|COLOR:
                    case PALETTE|COLOR|ALPHA:
                        int alen = len;
                        if(red_map!=null) alen = red_map.length;
                        alpha_map = new byte[alen];
                        System.arraycopy(buf,st,alpha_map,0,len<alen ? len : alen);
                        while (--alen>=len) alpha_map[alen] = (byte)0xFF;
                        break;
                    case COLOR: // doesn't deal with 16 bit colors properly
                    case COLOR|ALPHA: // doesn't deal with 16 bit colors properly
                        pngassert(len==6);
                        if (bitDepth == 16) {
                            transparentPixel_16 = new byte[6];
                            for (int i = 0; i < 6; i++) {
                                transparentPixel_16[i] = (byte)getByte(st + i);
                            }
                        } else {
                            transparentPixel =
                                      ((getShort(st + 0)&0xFF)<<16)
                                    | ((getShort(st + 2)&0xFF)<< 8)
                                    | ((getShort(st + 4)&0xFF)    );
                        }
                        break;
                    case GRAY:  // doesn't deal with 16 bit colors properly
                    case GRAY|ALPHA:  // doesn't deal with 16 bit colors properly
                        pngassert(len==2);
                        /* REMIND: Discarding the LSB for 16 bit depth here
                         * means that the all pixels which match the MSB
                         * will be treated as transparent.
                         */
                        int t = getShort(st);
                        t = 0xFF & ((bitDepth == 16) ? (t >> 8) : t);
                        transparentPixel = (t<<16) | (t<< 8) | t;
                        break;
                }
                break;
            case zTXtChunk: break;
        }
        return true;
    }
    @SuppressWarnings("serial") // JDK-implementation class
    public class PNGException extends IOException {
        PNGException(String s) { super(s); }
    }
  public void produceImage() throws IOException, ImageFormatException {
    try {
            for(int i=0; i<signature.length; i++)
              if((signature[i]&0xFF)!=underlyingInputStream.read())
                throw new PNGException("Chunk signature mismatch");

            InputStream is = new BufferedInputStream(new InflaterInputStream(inputStream,new Inflater()));

            getData();

            byte[] bPixels = null;
            int[] wPixels = null;
            int pixSize = width;
            int rowStride;
            int logDepth = 0;
            switch(bitDepth) {
                case  1: logDepth = 0; break;
                case  2: logDepth = 1; break;
                case  4: logDepth = 2; break;
                case  8: logDepth = 3; break;
                case 16: logDepth = 4; break;
                default: throw new PNGException("invalid depth");
            }
            if(interlaceMethod!=0) {pixSize *= height;rowStride=width;}
            else rowStride = 0;
            int combinedType = colorType|(bitDepth<<3);
            int bitMask = (1<<(bitDepth>=8?8:bitDepth))-1;
            //Figure out the color model
            switch(colorType) {
                case COLOR|PALETTE:
                case COLOR|PALETTE|ALPHA:
                    if(red_map==null) throw new PNGException("palette expected");
                    if(alpha_map==null)
                        cm = new IndexColorModel(bitDepth,red_map.length,
                            red_map,green_map,blue_map);
                    else
                        cm = new IndexColorModel(bitDepth,red_map.length,
                            red_map,green_map,blue_map,alpha_map);
                    bPixels = new byte[pixSize];
                    break;
                case GRAY:
                    {   int llog = logDepth>=4 ? 3 : logDepth;
                        if((cm=greyModels[llog]) == null) {
                            int size = 1<<(1<<llog);

                            byte[] ramp = new byte[size];
                            for(int i = 0; i<size; i++) ramp[i] = (byte)(255*i/(size-1));

                            if (transparentPixel == -1) {
                                cm = new IndexColorModel(bitDepth,ramp.length,ramp,ramp,ramp);
                            } else {
                                cm = new IndexColorModel(bitDepth,ramp.length,ramp,ramp,ramp,
                                                         (transparentPixel & 0xFF));
                            }
                            greyModels[llog] = cm;
                        }
                    }
                    bPixels = new byte[pixSize];
                    break;
                case COLOR:
                case COLOR|ALPHA:
                case GRAY|ALPHA:
                    cm = ColorModel.getRGBdefault();
                    wPixels = new int[pixSize];
                    break;
                default:
                    throw new PNGException("invalid color type");
            }
            setDimensions(width, height);
            setColorModel(cm);
            int flags = (interlaceMethod !=0
                       ? ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                       : ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES |
                         ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
            setHints(flags);
            headerComplete();

            int samplesPerPixel = ((colorType&PALETTE)!=0 ? 1
                                 : ((colorType&COLOR)!=0 ? 3 : 1)+((colorType&ALPHA)!=0?1:0));
            int bitsPerPixel = samplesPerPixel*bitDepth;
            int bytesPerPixel = (bitsPerPixel+7)>>3;
            int pass, passLimit;
            if(interlaceMethod==0) { pass = -1; passLimit = 0; }
            else { pass = 0; passLimit = 7; }
            while(++pass<=passLimit) {
                int row = startingRow[pass];
                int rowInc = rowIncrement[pass];
                int colInc = colIncrement[pass];
                int bWidth = blockWidth[pass];
                int bHeight = blockHeight[pass];
                int sCol = startingCol[pass];
                int rowPixelWidth = (width-sCol+(colInc-1))/colInc;
                int rowByteWidth = ((rowPixelWidth*bitsPerPixel)+7)>>3;
                if(rowByteWidth==0) continue;
                int pixelBufferInc = interlaceMethod==0 ? rowInc*width : 0;
                int rowOffset = rowStride*row;
                boolean firstRow = true;

                byte[] rowByteBuffer = new byte[rowByteWidth];
                byte[] prevRowByteBuffer = new byte[rowByteWidth];
                while (row < height) {
                    int rowFilter = is.read();
                    for (int rowFillPos=0;rowFillPos<rowByteWidth; ) {
                        int n = is.read(rowByteBuffer,rowFillPos,rowByteWidth-rowFillPos);
                        if(n<=0) throw new PNGException("missing data");
                        rowFillPos+=n;
                    }
                    filterRow(rowByteBuffer,
                              firstRow ? null : prevRowByteBuffer,
                              rowFilter, rowByteWidth, bytesPerPixel);
                    int col = sCol;
                    int spos=0;
                    int pixel = 0;
                    while (col < width) {
                        if(wPixels !=null) {
                            switch(combinedType) {
                                case COLOR|ALPHA|(8<<3):
                                    wPixels[col+rowOffset] =
                                          ((rowByteBuffer[spos  ]&0xFF)<<16)
                                        | ((rowByteBuffer[spos+1]&0xFF)<< 8)
                                        | ((rowByteBuffer[spos+2]&0xFF)    )
                                        | ((rowByteBuffer[spos+3]&0xFF)<<24);
                                    spos+=4;
                                    break;
                                case COLOR|ALPHA|(16<<3):
                                    wPixels[col+rowOffset] =
                                          ((rowByteBuffer[spos  ]&0xFF)<<16)
                                        | ((rowByteBuffer[spos+2]&0xFF)<< 8)
                                        | ((rowByteBuffer[spos+4]&0xFF)    )
                                        | ((rowByteBuffer[spos+6]&0xFF)<<24);
                                    spos+=8;
                                    break;
                                case COLOR|(8<<3):
                                    pixel =
                                          ((rowByteBuffer[spos  ]&0xFF)<<16)
                                        | ((rowByteBuffer[spos+1]&0xFF)<< 8)
                                        | ((rowByteBuffer[spos+2]&0xFF)    );
                                    if (pixel != transparentPixel) {
                                        pixel |= 0xff000000;
                                    }
                                    wPixels[col+rowOffset] = pixel;
                                    spos+=3;
                                    break;
                                case COLOR|(16<<3):
                                    pixel =
                                              ((rowByteBuffer[spos  ]&0xFF)<<16)
                                            | ((rowByteBuffer[spos+2]&0xFF)<< 8)
                                            | ((rowByteBuffer[spos+4]&0xFF)    );

                                    boolean isTransparent = (transparentPixel_16 != null);
                                    for (int i = 0; isTransparent && (i < 6); i++) {
                                        isTransparent &=
                                                (rowByteBuffer[spos + i] & 0xFF) == (transparentPixel_16[i] & 0xFF);
                                    }
                                    if (!isTransparent)  {
                                        pixel |= 0xff000000;
                                    }
                                    wPixels[col+rowOffset] = pixel;
                                    spos+=6;
                                    break;
                                case GRAY|ALPHA|(8<<3):
                                    { int tx = rowByteBuffer[spos]&0xFF;
                                      wPixels[col+rowOffset] =
                                          (tx<<16)|(tx<<8)|tx
                                        |((rowByteBuffer[spos+1]&0xFF)<<24); }
                                    spos+=2;
                                    break;
                                case GRAY|ALPHA|(16<<3):
                                    { int tx = rowByteBuffer[spos]&0xFF;
                                      wPixels[col+rowOffset] =
                                          (tx<<16)|(tx<<8)|tx
                                        |((rowByteBuffer[spos+2]&0xFF)<<24); }
                                    spos+=4;
                                    break;
                                default: throw new PNGException("illegal type/depth");
                            }
                        } else switch(bitDepth) {
                            case 1:
                                bPixels[col+rowOffset] =
                                    (byte)((rowByteBuffer[spos>>3]>>(7-(spos&7)))&1);
                                spos++;
                                break;
                            case 2:
                                bPixels[col+rowOffset] =
                                    (byte)((rowByteBuffer[spos>>2]>>((3-(spos&3))*2))&3);
                                spos++;
                                break;
                            case 4:
                                bPixels[col+rowOffset] =
                                    (byte)((rowByteBuffer[spos>>1]>>((1-(spos&1))*4))&15);
                                spos++;
                                break;
                            case 8: bPixels[col+rowOffset] = rowByteBuffer[spos++];
                                break;
                            case 16: bPixels[col+rowOffset] = rowByteBuffer[spos]; spos+=2;
                                break;
                            default: throw new PNGException("illegal type/depth");
                        }
                        col += colInc;
                    }
                    if(interlaceMethod==0)
                      if(wPixels!=null) {
                        sendPixels(0,row,width,1,wPixels,0,width);
                      } else {
                        sendPixels(0,row,width,1,bPixels,0,width);
                      }
                    row += rowInc;
                    rowOffset += rowInc*rowStride;
                    byte[] T = rowByteBuffer;
                    rowByteBuffer = prevRowByteBuffer;
                    prevRowByteBuffer = T;
                    firstRow = false;
                }
                if(interlaceMethod!=0)
                  if(wPixels!=null) {
                      sendPixels(0,0,width,height,wPixels,0,width);
                  } else {
                      sendPixels(0,0,width,height,bPixels,0,width);
                  }
            }

   /* Here, the function "visit(row,column,height,width)" obtains the
      next transmitted pixel and paints a rectangle of the specified
      height and width, whose upper-left corner is at the specified row
      and column, using the color indicated by the pixel.  Note that row
      and column are measured from 0,0 at the upper left corner. */

              imageComplete(ImageConsumer.STATICIMAGEDONE, true);
        } catch(IOException e) {
            if(!aborted) {
                property("error", e);
                imageComplete(ImageConsumer.IMAGEERROR|ImageConsumer.STATICIMAGEDONE, true);
                throw e;
            }
        } finally {
          try { close(); } catch(Throwable e){}
        }
    }

    private boolean sendPixels(int x, int y, int w, int h, int[] pixels,
                               int offset, int pixlength) {
        int count = setPixels(x, y, w, h, cm,
                              pixels, offset, pixlength);
        if (count <= 0) {
            aborted = true;
        }
        return !aborted;
    }
    private boolean sendPixels(int x, int y, int w, int h, byte[] pixels,
                               int offset, int pixlength) {
        int count = setPixels(x, y, w, h, cm,
                              pixels, offset, pixlength);
        if (count <= 0) {
            aborted = true;
        }
        return !aborted;
    }

    private void filterRow(byte[] rowByteBuffer, byte[] prevRow,
                           int rowFilter, int rowByteWidth, int bytesPerSample)
        throws IOException {
        int x = 0;
        switch (rowFilter) {
          case 0:
            break;
          case 1:
            for (x = bytesPerSample; x < rowByteWidth; x++)
                rowByteBuffer[x] += rowByteBuffer[x - bytesPerSample];
            break;
          case 2:
            if (prevRow != null)
                for ( ; x < rowByteWidth; x++)
                    rowByteBuffer[x] += prevRow[x];
            break;
          case 3:
            if (prevRow != null) {
                for ( ; x < bytesPerSample; x++)
                    rowByteBuffer[x] += (0xff & prevRow[x])>>1;
                for ( ; x < rowByteWidth; x++)
                    rowByteBuffer[x] += ((prevRow[x]&0xFF) + (rowByteBuffer[x - bytesPerSample]&0xFF))>>1;
            } else
                for (x = bytesPerSample; x < rowByteWidth; x++)
                    rowByteBuffer[x] += (rowByteBuffer[x - bytesPerSample]&0xFF)>>1;
            break;
          case 4:
            if (prevRow != null) {
                for ( ; x < bytesPerSample; x++)
                    rowByteBuffer[x] += prevRow[x];
                for ( ; x < rowByteWidth; x++) {
                    int a, b, c, p, pa, pb, pc, rval;
                    a = rowByteBuffer[x - bytesPerSample]&0xFF;
                    b = prevRow[x]&0xFF;
                    c = prevRow[x - bytesPerSample]&0xFF;
                    p = a + b - c;
                    pa = p > a ? p - a : a - p;
                    pb = p > b ? p - b : b - p;
                    pc = p > c ? p - c : c - p;
                    rowByteBuffer[x] += (pa <= pb) && (pa <= pc) ? a : pb <= pc ? b : c;
                }
            } else
                for (x = bytesPerSample; x < rowByteWidth; x++)
                    rowByteBuffer[x] += rowByteBuffer[x - bytesPerSample];
            break;
          default:
            throw new PNGException("Illegal filter");
        }
    }
    private static final byte[] startingRow =  { 0, 0, 0, 4, 0, 2, 0, 1 };
    private static final byte[] startingCol =  { 0, 0, 4, 0, 2, 0, 1, 0 };
    private static final byte[] rowIncrement = { 1, 8, 8, 8, 4, 4, 2, 2 };
    private static final byte[] colIncrement = { 1, 8, 8, 4, 4, 2, 2, 1 };
    private static final byte[] blockHeight =  { 1, 8, 8, 4, 4, 2, 2, 1 };
    private static final byte[] blockWidth =   { 1, 8, 4, 4, 2, 2, 1, 1 };

    int pos, limit;
    int chunkStart;
    int chunkKey, chunkLength, chunkCRC;
    boolean seenEOF;

    private static final byte[] signature = { (byte) 137, (byte) 80, (byte) 78,
        (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10 };

    PNGFilterInputStream inputStream;
    InputStream underlyingInputStream;

  public PNGImageDecoder(InputStreamImageSource src, InputStream input) throws IOException {
    super(src, input);
    inputStream = new PNGFilterInputStream(this, input);
    underlyingInputStream = inputStream.underlyingInputStream;
    }
    byte[] inbuf = new byte[4096];
    private void fill() throws IOException {
        if(!seenEOF) {
            if(pos>0 && pos<limit) {
                System.arraycopy(inbuf,pos,inbuf,0,limit-pos);
                limit = limit-pos;
                pos = 0;
            } else if(pos>=limit) {
                pos = 0; limit = 0;
            }
            int bsize = inbuf.length;
            while(limit<bsize) {
                int n = underlyingInputStream.read(inbuf,limit,bsize-limit);
                if(n<=0) { seenEOF=true; break; }
                limit += n;
            }
        }
    }
    private boolean need(int n) throws IOException {
        if(limit-pos>=n) return true;
        fill();
        if(limit-pos>=n) return true;
        if(seenEOF) return false;
        byte[] nin = new byte[n+100];
        System.arraycopy(inbuf,pos,nin,0,limit-pos);
        limit = limit-pos;
        pos = 0;
        inbuf = nin;
        fill();
        return limit-pos>=n;
    }
    private int getInt(int pos) {
        return ((inbuf[pos  ]&0xFF)<<24)
             | ((inbuf[pos+1]&0xFF)<<16)
             | ((inbuf[pos+2]&0xFF)<< 8)
             | ((inbuf[pos+3]&0xFF)    );
    }
    private int getShort(int pos) {
        return (short)(((inbuf[pos  ]&0xFF)<<8)
                     | ((inbuf[pos+1]&0xFF)   ));
    }
    private int getByte(int pos) {
        return inbuf[pos]&0xFF;
    }
    private boolean getChunk() throws IOException {
        chunkLength = 0;
        if (!need(8)) return false;
        chunkLength = getInt(pos);
        chunkKey = getInt(pos+4);
        if(chunkLength<0) throw new PNGException("bogus length: "+chunkLength);
        if (!need(chunkLength+12)) return false;
        chunkCRC = getInt(pos+8+chunkLength);
        chunkStart = pos+8;
        int calcCRC = crc(inbuf,pos+4,chunkLength+4);
        if(chunkCRC!=calcCRC && checkCRC) throw new PNGException("crc corruption");
        pos+=chunkLength+12;
        return true;
    }
    private void readAll() throws IOException {
        while(getChunk()) handleChunk(chunkKey,inbuf,chunkStart,chunkLength);
    }
    boolean getData() throws IOException {
        while(chunkLength==0 && getChunk())
            if(handleChunk(chunkKey,inbuf,chunkStart,chunkLength))
                chunkLength = 0;
        return chunkLength>0;
    }
    private static boolean checkCRC = true;
    public static boolean getCheckCRC() { return checkCRC; }
    public static void setCheckCRC(boolean c) { checkCRC = c; }

    protected void wrc(int c) {
        c = c&0xFF;
        if(c<=' '||c>'z') c = '?';
        System.out.write(c);
    }
    protected void wrk(int n) {
        wrc(n>>24);
        wrc(n>>16);
        wrc(n>>8);
        wrc(n);
    }
    public void print() {
        wrk(chunkKey);
        System.out.print(" "+chunkLength+"\n");
    }

    /* Table of CRCs of all 8-bit messages. */
    private static final int[] crc_table = new int[256];

    /* Make the table for a fast CRC. */
    static {
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++)
                if ((c & 1) != 0)
                    c = 0xedb88320 ^ (c >>> 1);
                else
                    c = c >>> 1;
            crc_table[n] = c;
        }
    }

    /* Update a running CRC with the bytes buf[0..len-1]--the CRC
    should be initialized to all 1's, and the transmitted value
    is the 1's complement of the final running CRC (see the
    crc() routine below)). */

    private static int update_crc(int crc, byte[] buf, int offset, int len) {
        int c = crc;
        while (--len>=0)
            c = crc_table[(c ^ buf[offset++]) & 0xff] ^ (c >>> 8);
        return c;
    }

    /* Return the CRC of the bytes buf[0..len-1]. */
    private static int crc(byte[] buf, int offset, int len) {
        return update_crc(0xffffffff, buf, offset, len) ^ 0xffffffff;
    }
    public static class Chromaticities {
        public float whiteX, whiteY, redX, redY, greenX, greenY, blueX, blueY;
        Chromaticities(int wx, int wy, int rx, int ry, int gx, int gy, int bx, int by) {
            whiteX = wx/100000.0f;
            whiteY = wy/100000.0f;
            redX = rx/100000.0f;
            redY = ry/100000.0f;
            greenX = gx/100000.0f;
            greenY = gy/100000.0f;
            blueX = bx/100000.0f;
            blueY = by/100000.0f;
        }
        public String toString() {
            return "Chromaticities(white="+whiteX+","+whiteY+";red="+
                redX+","+redY+";green="+
                greenX+","+greenY+";blue="+
                blueX+","+blueY+")";
        }
    }
}

// the following class are added to make it work with ImageDecoder architecture

class PNGFilterInputStream extends FilterInputStream {
  PNGImageDecoder owner;
  public InputStream underlyingInputStream;
  public PNGFilterInputStream(PNGImageDecoder owner, InputStream is) {
    super(is);
    underlyingInputStream = in;
    this.owner = owner;
  }

    public int available() throws IOException {
        return owner.limit-owner.pos+in.available();}
    public boolean markSupported() { return false; }
    public int read() throws IOException {
        if(owner.chunkLength<=0) if(!owner.getData()) return -1;
        owner.chunkLength--;
        return owner.inbuf[owner.chunkStart++]&0xFF;
    }
    public int read(byte[] b) throws IOException{return read(b,0,b.length);}
    public int read(byte[] b, int st, int len) throws IOException {
        if(owner.chunkLength<=0) if(!owner.getData()) return -1;
        if(owner.chunkLength<len) len = owner.chunkLength;
        System.arraycopy(owner.inbuf,owner.chunkStart,b,st,len);
        owner.chunkLength-=len;
        owner.chunkStart+=len;
        return len;
    }
  public long skip(long n) throws IOException {
        int i;
        for(i = 0; i<n && read()>=0; i++);
        return i;
    }


}
