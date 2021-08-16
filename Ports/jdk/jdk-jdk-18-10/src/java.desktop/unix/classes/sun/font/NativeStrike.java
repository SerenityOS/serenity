/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.NoninvertibleTransformException;

class NativeStrike extends PhysicalStrike {

     NativeFont nativeFont;
     int numGlyphs;
     AffineTransform invertDevTx;
     AffineTransform fontTx;

     /* The following method prepares data used in obtaining FontMetrics.
      * This is the one case in which we allow anything other than a
      * simple scale to be used with a native font. We do this because in
      * order to ensure that clients get the overall metrics they expect
      * for a font whatever coordinate system (combination of font and
      * device transform) they use.
      * X11 fonts can only have a scale applied (remind : non-uniform?)
      * We strip out everything else and if necessary obtain an inverse
      * tx which we use to return metrics for the font in the transformed
      * coordinate system of the font. ie we pass X11 a simple scale, and
      * then apply the non-scale part of the font TX to that result.
      */
     private int getNativePointSize() {
         /* Make a copy of the glyphTX in which we will store the
          * font transform, inverting the devTx if necessary
          */
         double[] mat = new double[4];
         desc.glyphTx.getMatrix(mat);
         fontTx = new AffineTransform(mat);

         /* Now work backwards to get the font transform */
         if (!desc.devTx.isIdentity() &&
             desc.devTx.getType() != AffineTransform.TYPE_TRANSLATION) {
             try {
                 invertDevTx = desc.devTx.createInverse();
                 fontTx.concatenate(invertDevTx);
             } catch (NoninvertibleTransformException e) {
                 e.printStackTrace();
             }
         }

         /* At this point the fontTx may be a simple +ve scale, or it
          * may be something more complex.
          */
         Point2D.Float pt = new Point2D.Float(1f,1f);
         fontTx.deltaTransform(pt, pt);
         double ptSize = Math.abs(pt.y);
         int ttype = fontTx.getType();
         if ((ttype & ~AffineTransform.TYPE_UNIFORM_SCALE) != 0 ||
             fontTx.getScaleY() <= 0) {
             /* We need to create an inverse transform that doesn't
              * include the point size (strictly the uniform scale)
              */
             fontTx.scale(1/ptSize, 1/ptSize);
         } else {
             fontTx = null; // no need
         }
         return (int)ptSize;
     }

     NativeStrike(NativeFont nativeFont, FontStrikeDesc desc) {
         super(nativeFont, desc);
         this.nativeFont = nativeFont;


         /* If this is a delegate for bitmaps, we expect to have
          * been invoked only for a simple scale. If that's not
          * true, just bail
          */
         if (nativeFont.isBitmapDelegate) {
             int ttype = desc.glyphTx.getType();
             if ((ttype & ~AffineTransform.TYPE_UNIFORM_SCALE) != 0 ||
                 desc.glyphTx.getScaleX() <= 0) {
             numGlyphs = 0;
             return;
             }
         }

         int ptSize = getNativePointSize();
         byte [] nameBytes = nativeFont.getPlatformNameBytes(ptSize);
         double scale = Math.abs(desc.devTx.getScaleX());
         pScalerContext = createScalerContext(nameBytes, ptSize, scale);
         if (pScalerContext == 0L) {
             SunFontManager.getInstance().deRegisterBadFont(nativeFont);
             pScalerContext = createNullScalerContext();
             numGlyphs = 0;
             if (FontUtilities.isLogging()) {
                 FontUtilities.logSevere("Could not create native strike " +
                                         new String(nameBytes));
             }
             return;
         }
         numGlyphs = nativeFont.getMapper().getNumGlyphs();
         this.disposer = new NativeStrikeDisposer(nativeFont, desc,
                                                  pScalerContext);
     }

     /* The asymmetry of the following methods is to help preserve
      * performance with minimal textual changes to the calling code
      * when moving initialisation of these arrays out of the constructor.
      * This may be restructured later when there's more room for changes
      */
     private boolean usingIntGlyphImages() {
         if (intGlyphImages != null) {
            return true;
        } else if (longAddresses) {
            return false;
        } else {
            /* We could obtain minGlyphIndex and index relative to that
             * if we need to save space.
             */
            int glyphLenArray = getMaxGlyph(pScalerContext);

            /* This shouldn't be necessary - its a precaution */
            if (glyphLenArray < numGlyphs) {
                glyphLenArray = numGlyphs;
            }
            intGlyphImages = new int[glyphLenArray];
            this.disposer.intGlyphImages = intGlyphImages;
            return true;
        }
     }

     private long[] getLongGlyphImages() {
        if (longGlyphImages == null && longAddresses) {

            /* We could obtain minGlyphIndex and index relative to that
             * if we need to save space.
             */
            int glyphLenArray = getMaxGlyph(pScalerContext);

            /* This shouldn't be necessary - its a precaution */
            if (glyphLenArray < numGlyphs) {
                glyphLenArray = numGlyphs;
            }
            longGlyphImages = new long[glyphLenArray];
            this.disposer.longGlyphImages = longGlyphImages;
        }
        return longGlyphImages;
     }

     NativeStrike(NativeFont nativeFont, FontStrikeDesc desc,
                  boolean nocache) {
         super(nativeFont, desc);
         this.nativeFont = nativeFont;

         int ptSize = (int)desc.glyphTx.getScaleY();
         double scale = desc.devTx.getScaleX(); // uniform scale
         byte [] nameBytes = nativeFont.getPlatformNameBytes(ptSize);
         pScalerContext = createScalerContext(nameBytes, ptSize, scale);

         int numGlyphs = nativeFont.getMapper().getNumGlyphs();
     }

     /* We want the native font to be responsible for reporting the
      * font metrics, even if it often delegates to another font.
      * The code here isn't yet implementing exactly that. If the glyph
      * transform was something native couldn't handle, there's no native
      * context from which to obtain metrics. Need to revise this to obtain
      * the metrics and transform them. But currently in such a case it
      * gets the metrics from a different font - its glyph delegate font.
      */
     StrikeMetrics getFontMetrics() {
         if (strikeMetrics == null) {
             if (pScalerContext != 0) {
                 strikeMetrics = nativeFont.getFontMetrics(pScalerContext);
             }
             if (strikeMetrics != null && fontTx != null) {
                 strikeMetrics.convertToUserSpace(fontTx);
             }
         }
         return strikeMetrics;
     }

     private native long createScalerContext(byte[] nameBytes,
                                             int ptSize, double scale);

     private native int getMaxGlyph(long pScalerContext);

     private native long createNullScalerContext();

     void getGlyphImagePtrs(int[] glyphCodes, long[] images,int  len) {
         for (int i=0; i<len; i++) {
             images[i] = getGlyphImagePtr(glyphCodes[i]);
         }
     }

     long getGlyphImagePtr(int glyphCode) {
         long glyphPtr;

         if (usingIntGlyphImages()) {
             if ((glyphPtr = intGlyphImages[glyphCode] & INTMASK) != 0L) {
                 return glyphPtr;
             } else {
                 glyphPtr = nativeFont.getGlyphImage(pScalerContext,glyphCode);
                 /* Synchronize in case some other thread has updated this
                  * cache entry already - unlikely but possible.
                  */
                 synchronized (this) {
                     if (intGlyphImages[glyphCode] == 0) {
                         intGlyphImages[glyphCode] = (int)glyphPtr;
                         return glyphPtr;
                     } else {
                         StrikeCache.freeIntPointer((int)glyphPtr);
                         return intGlyphImages[glyphCode] & INTMASK;
                     }
                 }
             }
         }
         /* must be using long (8 byte) addresses */
         else if ((glyphPtr = getLongGlyphImages()[glyphCode]) != 0L) {
             return glyphPtr;
         } else {
             glyphPtr = nativeFont.getGlyphImage(pScalerContext, glyphCode);

             synchronized (this) {
                 if (longGlyphImages[glyphCode] == 0L) {
                     longGlyphImages[glyphCode] = glyphPtr;
                     return glyphPtr;
                 } else {
                     StrikeCache.freeLongPointer(glyphPtr);
                     return longGlyphImages[glyphCode];
                 }
             }
         }
     }

     /* This is used when a FileFont uses the native names to create a
      * delegate NativeFont/Strike to get images from native. This is used
      * because Solaris TrueType fonts have external PCF bitmaps rather than
      * embedded bitmaps. This is really only important for CJK fonts as
      * for most scripts the external X11 bitmaps aren't much better - if
      * at all - than the results from hinting the outlines.
      */
     long getGlyphImagePtrNoCache(int glyphCode) {
         return nativeFont.getGlyphImageNoDefault(pScalerContext, glyphCode);
     }

     void getGlyphImageBounds(int glyphcode, Point2D.Float pt,
                              Rectangle result) {
     }

     Point2D.Float getGlyphMetrics(int glyphCode) {
         Point2D.Float pt = new Point2D.Float(getGlyphAdvance(glyphCode), 0f);
         return pt;
     }

     float getGlyphAdvance(int glyphCode) {
         return nativeFont.getGlyphAdvance(pScalerContext, glyphCode);
     }

     Rectangle2D.Float getGlyphOutlineBounds(int glyphCode) {
         return nativeFont.getGlyphOutlineBounds(pScalerContext, glyphCode);
     }

     GeneralPath getGlyphOutline(int glyphCode, float x, float y) {
         return new GeneralPath();
     }

     GeneralPath getGlyphVectorOutline(int[] glyphs, float x, float y) {
         return new GeneralPath();
     }

}
