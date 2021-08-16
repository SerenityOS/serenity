/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 **********************************************************************
 **********************************************************************
 **********************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997                      ***
 *** As  an unpublished  work pursuant to Title 17 of the United    ***
 *** States Code.  All rights reserved.                             ***
 **********************************************************************
 **********************************************************************
 **********************************************************************/

package java.awt.image;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;

/**
 * This class performs a pixel-by-pixel color conversion of the data in
 * the source image.  The resulting color values are scaled to the precision
 * of the destination image.  Color conversion can be specified
 * via an array of ColorSpace objects or an array of ICC_Profile objects.
 * <p>
 * If the source is a BufferedImage with premultiplied alpha, the
 * color components are divided by the alpha component before color conversion.
 * If the destination is a BufferedImage with premultiplied alpha, the
 * color components are multiplied by the alpha component after conversion.
 * Rasters are treated as having no alpha channel, i.e. all bands are
 * color bands.
 * <p>
 * If a RenderingHints object is specified in the constructor, the
 * color rendering hint and the dithering hint may be used to control
 * color conversion.
 * <p>
 * Note that Source and Destination may be the same object.
 * @see java.awt.RenderingHints#KEY_COLOR_RENDERING
 * @see java.awt.RenderingHints#KEY_DITHERING
 */
public class ColorConvertOp implements BufferedImageOp, RasterOp {
    ICC_Profile[]    profileList;
    ColorSpace[]     CSList;
    ColorTransform    thisTransform, thisRasterTransform;
    ICC_Profile      thisSrcProfile, thisDestProfile;
    RenderingHints   hints;
    boolean          gotProfiles;
    float[]          srcMinVals, srcMaxVals, dstMinVals, dstMaxVals;

    /**
     * Constructs a new ColorConvertOp which will convert
     * from a source color space to a destination color space.
     * The RenderingHints argument may be null.
     * This Op can be used only with BufferedImages, and will convert
     * directly from the ColorSpace of the source image to that of the
     * destination.  The destination argument of the filter method
     * cannot be specified as null.
     * @param hints the {@code RenderingHints} object used to control
     *        the color conversion, or {@code null}
     */
    public ColorConvertOp (RenderingHints hints)
    {
        profileList = new ICC_Profile [0];    /* 0 length list */
        this.hints  = hints;
    }

    /**
     * Constructs a new ColorConvertOp from a ColorSpace object.
     * The RenderingHints argument may be null.  This
     * Op can be used only with BufferedImages, and is primarily useful
     * when the {@link #filter(BufferedImage, BufferedImage) filter}
     * method is invoked with a destination argument of null.
     * In that case, the ColorSpace defines the destination color space
     * for the destination created by the filter method.  Otherwise, the
     * ColorSpace defines an intermediate space to which the source is
     * converted before being converted to the destination space.
     * @param cspace defines the destination {@code ColorSpace} or an
     *        intermediate {@code ColorSpace}
     * @param hints the {@code RenderingHints} object used to control
     *        the color conversion, or {@code null}
     * @throws NullPointerException if cspace is null
     */
    public ColorConvertOp (ColorSpace cspace, RenderingHints hints)
    {
        if (cspace == null) {
            throw new NullPointerException("ColorSpace cannot be null");
        }
        if (cspace instanceof ICC_ColorSpace) {
            profileList = new ICC_Profile [1];    /* 1 profile in the list */

            profileList [0] = ((ICC_ColorSpace) cspace).getProfile();
        }
        else {
            CSList = new ColorSpace[1]; /* non-ICC case: 1 ColorSpace in list */
            CSList[0] = cspace;
        }
        this.hints  = hints;
    }


    /**
     * Constructs a new ColorConvertOp from two ColorSpace objects.
     * The RenderingHints argument may be null.
     * This Op is primarily useful for calling the filter method on
     * Rasters, in which case the two ColorSpaces define the operation
     * to be performed on the Rasters.  In that case, the number of bands
     * in the source Raster must match the number of components in
     * srcCspace, and the number of bands in the destination Raster
     * must match the number of components in dstCspace.  For BufferedImages,
     * the two ColorSpaces define intermediate spaces through which the
     * source is converted before being converted to the destination space.
     * @param srcCspace the source {@code ColorSpace}
     * @param dstCspace the destination {@code ColorSpace}
     * @param hints the {@code RenderingHints} object used to control
     *        the color conversion, or {@code null}
     * @throws NullPointerException if either srcCspace or dstCspace is null
     */
    public ColorConvertOp(ColorSpace srcCspace, ColorSpace dstCspace,
                           RenderingHints hints)
    {
        if ((srcCspace == null) || (dstCspace == null)) {
            throw new NullPointerException("ColorSpaces cannot be null");
        }
        if ((srcCspace instanceof ICC_ColorSpace) &&
            (dstCspace instanceof ICC_ColorSpace)) {
            profileList = new ICC_Profile [2];    /* 2 profiles in the list */

            profileList [0] = ((ICC_ColorSpace) srcCspace).getProfile();
            profileList [1] = ((ICC_ColorSpace) dstCspace).getProfile();

            getMinMaxValsFromColorSpaces(srcCspace, dstCspace);
        } else {
            /* non-ICC case: 2 ColorSpaces in list */
            CSList = new ColorSpace[2];
            CSList[0] = srcCspace;
            CSList[1] = dstCspace;
        }
        this.hints  = hints;
    }


     /**
     * Constructs a new ColorConvertOp from an array of ICC_Profiles.
     * The RenderingHints argument may be null.
     * The sequence of profiles may include profiles that represent color
     * spaces, profiles that represent effects, etc.  If the whole sequence
     * does not represent a well-defined color conversion, an exception is
     * thrown.
     * <p>For BufferedImages, if the ColorSpace
     * of the source BufferedImage does not match the requirements of the
     * first profile in the array,
     * the first conversion is to an appropriate ColorSpace.
     * If the requirements of the last profile in the array are not met
     * by the ColorSpace of the destination BufferedImage,
     * the last conversion is to the destination's ColorSpace.
     * <p>For Rasters, the number of bands in the source Raster must match
     * the requirements of the first profile in the array, and the
     * number of bands in the destination Raster must match the requirements
     * of the last profile in the array.  The array must have at least two
     * elements or calling the filter method for Rasters will throw an
     * IllegalArgumentException.
     * @param profiles the array of {@code ICC_Profile} objects
     * @param hints the {@code RenderingHints} object used to control
     *        the color conversion, or {@code null}
     * @exception IllegalArgumentException when the profile sequence does not
     *             specify a well-defined color conversion
     * @exception NullPointerException if profiles is null
     */
    public ColorConvertOp (ICC_Profile[] profiles, RenderingHints hints)
    {
        if (profiles == null) {
            throw new NullPointerException("Profiles cannot be null");
        }
        gotProfiles = true;
        profileList = new ICC_Profile[profiles.length];
        for (int i1 = 0; i1 < profiles.length; i1++) {
            profileList[i1] = profiles[i1];
        }
        this.hints  = hints;
    }


    /**
     * Returns the array of ICC_Profiles used to construct this ColorConvertOp.
     * Returns null if the ColorConvertOp was not constructed from such an
     * array.
     * @return the array of {@code ICC_Profile} objects of this
     *         {@code ColorConvertOp}, or {@code null} if this
     *         {@code ColorConvertOp} was not constructed with an
     *         array of {@code ICC_Profile} objects.
     */
    public final ICC_Profile[] getICC_Profiles() {
        if (gotProfiles) {
            ICC_Profile[] profiles = new ICC_Profile[profileList.length];
            for (int i1 = 0; i1 < profileList.length; i1++) {
                profiles[i1] = profileList[i1];
            }
            return profiles;
        }
        return null;
    }

    /**
     * ColorConverts the source BufferedImage.
     * If the destination image is null,
     * a BufferedImage will be created with an appropriate ColorModel.
     * @param src the source {@code BufferedImage} to be converted
     * @param dest the destination {@code BufferedImage},
     *        or {@code null}
     * @return {@code dest} color converted from {@code src}
     *         or a new, converted {@code BufferedImage}
     *         if {@code dest} is {@code null}
     * @exception IllegalArgumentException if dest is null and this op was
     *             constructed using the constructor which takes only a
     *             RenderingHints argument, since the operation is ill defined.
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dest) {
        ColorSpace srcColorSpace, destColorSpace;
        BufferedImage savdest = null;

        if (src.getColorModel() instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) src.getColorModel();
            src = icm.convertToIntDiscrete(src.getRaster(), true);
        }
        srcColorSpace = src.getColorModel().getColorSpace();
        if (dest != null) {
            if (dest.getColorModel() instanceof IndexColorModel) {
                savdest = dest;
                dest = null;
                destColorSpace = null;
            } else {
                destColorSpace = dest.getColorModel().getColorSpace();
            }
        } else {
            destColorSpace = null;
        }

        if ((CSList != null) ||
            (!(srcColorSpace instanceof ICC_ColorSpace)) ||
            ((dest != null) &&
             (!(destColorSpace instanceof ICC_ColorSpace)))) {
            /* non-ICC case */
            dest = nonICCBIFilter(src, srcColorSpace, dest, destColorSpace);
        } else {
            dest = ICCBIFilter(src, srcColorSpace, dest, destColorSpace);
        }

        if (savdest != null) {
            Graphics2D big = savdest.createGraphics();
            try {
                big.drawImage(dest, 0, 0, null);
            } finally {
                big.dispose();
            }
            return savdest;
        } else {
            return dest;
        }
    }

    private BufferedImage ICCBIFilter(BufferedImage src,
                                            ColorSpace srcColorSpace,
                                            BufferedImage dest,
                                            ColorSpace destColorSpace) {
    int              nProfiles = profileList.length;
    ICC_Profile      srcProfile = null, destProfile = null;

        srcProfile = ((ICC_ColorSpace) srcColorSpace).getProfile();

        if (dest == null) {        /* last profile in the list defines
                                      the output color space */
            if (nProfiles == 0) {
                throw new IllegalArgumentException(
                    "Destination ColorSpace is undefined");
            }
            destProfile = profileList [nProfiles - 1];
            dest = createCompatibleDestImage(src, null);
        }
        else {
            if (src.getHeight() != dest.getHeight() ||
                src.getWidth() != dest.getWidth()) {
                throw new IllegalArgumentException(
                    "Width or height of BufferedImages do not match");
            }
            destProfile = ((ICC_ColorSpace) destColorSpace).getProfile();
        }

        /* Checking if all profiles in the transform sequence are the same.
         * If so, performing just copying the data.
         */
        if (srcProfile == destProfile) {
            boolean noTrans = true;
            for (int i = 0; i < nProfiles; i++) {
                if (srcProfile != profileList[i]) {
                    noTrans = false;
                    break;
                }
            }
            if (noTrans) {
                Graphics2D g = dest.createGraphics();
                try {
                    g.drawImage(src, 0, 0, null);
                } finally {
                    g.dispose();
                }

                return dest;
            }
        }

        /* make a new transform if needed */
        if ((thisTransform == null) || (thisSrcProfile != srcProfile) ||
            (thisDestProfile != destProfile) ) {
            updateBITransform(srcProfile, destProfile);
        }

        /* color convert the image */
        thisTransform.colorConvert(src, dest);

        return dest;
    }

    private void updateBITransform(ICC_Profile srcProfile,
                                   ICC_Profile destProfile) {
        ICC_Profile[]    theProfiles;
        int              i1, nProfiles, nTransforms, whichTrans, renderState;
        ColorTransform[]  theTransforms;
        boolean          useSrc = false, useDest = false;

        nProfiles = profileList.length;
        nTransforms = nProfiles;
        if ((nProfiles == 0) || (srcProfile != profileList[0])) {
            nTransforms += 1;
            useSrc = true;
        }
        if ((nProfiles == 0) || (destProfile != profileList[nProfiles - 1]) ||
            (nTransforms < 2)) {
            nTransforms += 1;
            useDest = true;
        }

        /* make the profile list */
        theProfiles = new ICC_Profile[nTransforms]; /* the list of profiles
                                                       for this Op */

        int idx = 0;
        if (useSrc) {
            /* insert source as first profile */
            theProfiles[idx++] = srcProfile;
        }

        for (i1 = 0; i1 < nProfiles; i1++) {
                                   /* insert profiles defined in this Op */
            theProfiles[idx++] = profileList [i1];
        }

        if (useDest) {
            /* insert dest as last profile */
            theProfiles[idx] = destProfile;
        }

        /* make the transform list */
        theTransforms = new ColorTransform [nTransforms];

        /* initialize transform get loop */
        if (theProfiles[0].getProfileClass() == ICC_Profile.CLASS_OUTPUT) {
                                        /* if first profile is a printer
                                           render as colorimetric */
            renderState = ICC_Profile.icRelativeColorimetric;
        }
        else {
            renderState = ICC_Profile.icPerceptual; /* render any other
                                                       class perceptually */
        }

        whichTrans = ColorTransform.In;

        PCMM mdl = CMSManager.getModule();

        /* get the transforms from each profile */
        for (i1 = 0; i1 < nTransforms; i1++) {
            if (i1 == nTransforms -1) {         /* last profile? */
                whichTrans = ColorTransform.Out; /* get output transform */
            }
            else {      /* check for abstract profile */
                if ((whichTrans == ColorTransform.Simulation) &&
                    (theProfiles[i1].getProfileClass () ==
                     ICC_Profile.CLASS_ABSTRACT)) {
                renderState = ICC_Profile.icPerceptual;
                    whichTrans = ColorTransform.In;
                }
            }

            theTransforms[i1] = mdl.createTransform (
                theProfiles[i1], renderState, whichTrans);

            /* get this profile's rendering intent to select transform
               from next profile */
            renderState = getRenderingIntent(theProfiles[i1]);

            /* "middle" profiles use simulation transform */
            whichTrans = ColorTransform.Simulation;
        }

        /* make the net transform */
        thisTransform = mdl.createTransform(theTransforms);

        /* update corresponding source and dest profiles */
        thisSrcProfile = srcProfile;
        thisDestProfile = destProfile;
    }

    /**
     * ColorConverts the image data in the source Raster.
     * If the destination Raster is null, a new Raster will be created.
     * The number of bands in the source and destination Rasters must
     * meet the requirements explained above.  The constructor used to
     * create this ColorConvertOp must have provided enough information
     * to define both source and destination color spaces.  See above.
     * Otherwise, an exception is thrown.
     * @param src the source {@code Raster} to be converted
     * @param dest the destination {@code WritableRaster},
     *        or {@code null}
     * @return {@code dest} color converted from {@code src}
     *         or a new, converted {@code WritableRaster}
     *         if {@code dest} is {@code null}
     * @exception IllegalArgumentException if the number of source or
     *             destination bands is incorrect, the source or destination
     *             color spaces are undefined, or this op was constructed
     *             with one of the constructors that applies only to
     *             operations on BufferedImages.
     */
    public final WritableRaster filter (Raster src, WritableRaster dest)  {

        if (CSList != null) {
            /* non-ICC case */
            return nonICCRasterFilter(src, dest);
        }
        int nProfiles = profileList.length;
        if (nProfiles < 2) {
            throw new IllegalArgumentException(
                "Source or Destination ColorSpace is undefined");
        }
        if (src.getNumBands() != profileList[0].getNumComponents()) {
            throw new IllegalArgumentException(
                "Numbers of source Raster bands and source color space " +
                "components do not match");
        }
        if (dest == null) {
            dest = createCompatibleDestRaster(src);
        }
        else {
            if (src.getHeight() != dest.getHeight() ||
                src.getWidth() != dest.getWidth()) {
                throw new IllegalArgumentException(
                    "Width or height of Rasters do not match");
            }
            if (dest.getNumBands() !=
                profileList[nProfiles-1].getNumComponents()) {
                throw new IllegalArgumentException(
                    "Numbers of destination Raster bands and destination " +
                    "color space components do not match");
            }
        }

        /* make a new transform if needed */
        if (thisRasterTransform == null) {
            int              i1, whichTrans, renderState;
            ColorTransform[]  theTransforms;

            /* make the transform list */
            theTransforms = new ColorTransform [nProfiles];

            /* initialize transform get loop */
            if (profileList[0].getProfileClass() == ICC_Profile.CLASS_OUTPUT) {
                                            /* if first profile is a printer
                                               render as colorimetric */
                renderState = ICC_Profile.icRelativeColorimetric;
            }
            else {
                renderState = ICC_Profile.icPerceptual; /* render any other
                                                           class perceptually */
            }

            whichTrans = ColorTransform.In;

            PCMM mdl = CMSManager.getModule();

            /* get the transforms from each profile */
            for (i1 = 0; i1 < nProfiles; i1++) {
                if (i1 == nProfiles -1) {         /* last profile? */
                    whichTrans = ColorTransform.Out; /* get output transform */
                }
                else {  /* check for abstract profile */
                    if ((whichTrans == ColorTransform.Simulation) &&
                        (profileList[i1].getProfileClass () ==
                         ICC_Profile.CLASS_ABSTRACT)) {
                        renderState = ICC_Profile.icPerceptual;
                        whichTrans = ColorTransform.In;
                    }
                }

                theTransforms[i1] = mdl.createTransform (
                    profileList[i1], renderState, whichTrans);

                /* get this profile's rendering intent to select transform
                   from next profile */
                renderState = getRenderingIntent(profileList[i1]);

                /* "middle" profiles use simulation transform */
                whichTrans = ColorTransform.Simulation;
            }

            /* make the net transform */
            thisRasterTransform = mdl.createTransform(theTransforms);
        }

        int srcTransferType = src.getTransferType();
        int dstTransferType = dest.getTransferType();
        if ((srcTransferType == DataBuffer.TYPE_FLOAT) ||
            (srcTransferType == DataBuffer.TYPE_DOUBLE) ||
            (dstTransferType == DataBuffer.TYPE_FLOAT) ||
            (dstTransferType == DataBuffer.TYPE_DOUBLE)) {
            if (srcMinVals == null) {
                getMinMaxValsFromProfiles(profileList[0],
                                          profileList[nProfiles-1]);
            }
            /* color convert the raster */
            thisRasterTransform.colorConvert(src, dest,
                                             srcMinVals, srcMaxVals,
                                             dstMinVals, dstMaxVals);
        } else {
            /* color convert the raster */
            thisRasterTransform.colorConvert(src, dest);
        }


        return dest;
    }

    /**
     * Returns the bounding box of the destination, given this source.
     * Note that this will be the same as the bounding box of the
     * source.
     * @param src the source {@code BufferedImage}
     * @return a {@code Rectangle2D} that is the bounding box
     *         of the destination, given the specified {@code src}
     */
    public final Rectangle2D getBounds2D (BufferedImage src) {
        return getBounds2D(src.getRaster());
    }

    /**
     * Returns the bounding box of the destination, given this source.
     * Note that this will be the same as the bounding box of the
     * source.
     * @param src the source {@code Raster}
     * @return a {@code Rectangle2D} that is the bounding box
     *         of the destination, given the specified {@code src}
     */
    public final Rectangle2D getBounds2D (Raster src) {
        /*        return new Rectangle (src.getXOffset(),
                              src.getYOffset(),
                              src.getWidth(), src.getHeight()); */
        return src.getBounds();
    }

    /**
     * Creates a zeroed destination image with the correct size and number of
     * bands, given this source.
     * @param src       Source image for the filter operation.
     * @param destCM    ColorModel of the destination.  If null, an
     *                  appropriate ColorModel will be used.
     * @return a {@code BufferedImage} with the correct size and
     * number of bands from the specified {@code src}.
     * @throws IllegalArgumentException if {@code destCM} is
     *         {@code null} and this {@code ColorConvertOp} was
     *         created without any {@code ICC_Profile} or
     *         {@code ColorSpace} defined for the destination
     */
    public BufferedImage createCompatibleDestImage (BufferedImage src,
                                                    ColorModel destCM) {
        ColorSpace cs = null;;
        if (destCM == null) {
            if (CSList == null) {
                /* ICC case */
                int nProfiles = profileList.length;
                if (nProfiles == 0) {
                    throw new IllegalArgumentException(
                        "Destination ColorSpace is undefined");
                }
                ICC_Profile destProfile = profileList[nProfiles - 1];
                cs = new ICC_ColorSpace(destProfile);
            } else {
                /* non-ICC case */
                int nSpaces = CSList.length;
                cs = CSList[nSpaces - 1];
            }
        }
        return createCompatibleDestImage(src, destCM, cs);
    }

    private BufferedImage createCompatibleDestImage(BufferedImage src,
                                                    ColorModel destCM,
                                                    ColorSpace destCS) {
        BufferedImage image;
        if (destCM == null) {
            ColorModel srcCM = src.getColorModel();
            int nbands = destCS.getNumComponents();
            boolean hasAlpha = srcCM.hasAlpha();
            if (hasAlpha) {
               nbands += 1;
            }
            int[] nbits = new int[nbands];
            for (int i = 0; i < nbands; i++) {
                nbits[i] = 8;
            }
            destCM = new ComponentColorModel(destCS, nbits, hasAlpha,
                                             srcCM.isAlphaPremultiplied(),
                                             srcCM.getTransparency(),
                                             DataBuffer.TYPE_BYTE);
        }
        int w = src.getWidth();
        int h = src.getHeight();
        image = new BufferedImage(destCM,
                                  destCM.createCompatibleWritableRaster(w, h),
                                  destCM.isAlphaPremultiplied(), null);
        return image;
    }


    /**
     * Creates a zeroed destination Raster with the correct size and number of
     * bands, given this source.
     * @param src the specified {@code Raster}
     * @return a {@code WritableRaster} with the correct size and number
     *         of bands from the specified {@code src}
     * @throws IllegalArgumentException if this {@code ColorConvertOp}
     *         was created without sufficient information to define the
     *         {@code dst} and {@code src} color spaces
     */
    public WritableRaster createCompatibleDestRaster (Raster src) {
        int ncomponents;

        if (CSList != null) {
            /* non-ICC case */
            if (CSList.length != 2) {
                throw new IllegalArgumentException(
                    "Destination ColorSpace is undefined");
            }
            ncomponents = CSList[1].getNumComponents();
        } else {
            /* ICC case */
            int nProfiles = profileList.length;
            if (nProfiles < 2) {
                throw new IllegalArgumentException(
                    "Destination ColorSpace is undefined");
            }
            ncomponents = profileList[nProfiles-1].getNumComponents();
        }

        WritableRaster dest =
            Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                  src.getWidth(),
                                  src.getHeight(),
                                  ncomponents,
                                  new Point(src.getMinX(), src.getMinY()));
        return dest;
    }

    /**
     * Returns the location of the destination point given a
     * point in the source.  If {@code dstPt} is non-null,
     * it will be used to hold the return value.  Note that
     * for this class, the destination point will be the same
     * as the source point.
     * @param srcPt the specified source {@code Point2D}
     * @param dstPt the destination {@code Point2D}
     * @return {@code dstPt} after setting its location to be
     *         the same as {@code srcPt}
     */
    public final Point2D getPoint2D (Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Float();
        }
        dstPt.setLocation(srcPt.getX(), srcPt.getY());

        return dstPt;
    }


    /**
     * Returns the RenderingIntent from the specified ICC Profile.
     */
    private int getRenderingIntent (ICC_Profile profile) {
        byte[] header = profile.getData(ICC_Profile.icSigHead);
        int index = ICC_Profile.icHdrRenderingIntent;

        /* According to ICC spec, only the least-significant 16 bits shall be
         * used to encode the rendering intent. The most significant 16 bits
         * shall be set to zero. Thus, we are ignoring two most significant
         * bytes here.
         *
         *  See http://www.color.org/ICC1v42_2006-05.pdf, section 7.2.15.
         */
        return ((header[index+2] & 0xff) <<  8) |
                (header[index+3] & 0xff);
    }

    /**
     * Returns the rendering hints used by this op.
     * @return the {@code RenderingHints} object of this
     *         {@code ColorConvertOp}
     */
    public final RenderingHints getRenderingHints() {
        return hints;
    }

    private BufferedImage nonICCBIFilter(BufferedImage src,
                                               ColorSpace srcColorSpace,
                                               BufferedImage dst,
                                               ColorSpace dstColorSpace) {

        int w = src.getWidth();
        int h = src.getHeight();
        ICC_ColorSpace ciespace =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
            dstColorSpace = dst.getColorModel().getColorSpace();
        } else {
            if ((h != dst.getHeight()) || (w != dst.getWidth())) {
                throw new IllegalArgumentException(
                    "Width or height of BufferedImages do not match");
            }
        }
        Raster srcRas = src.getRaster();
        WritableRaster dstRas = dst.getRaster();
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM = dst.getColorModel();
        int srcNumComp = srcCM.getNumColorComponents();
        int dstNumComp = dstCM.getNumColorComponents();
        boolean dstHasAlpha = dstCM.hasAlpha();
        boolean needSrcAlpha = srcCM.hasAlpha() && dstHasAlpha;
        ColorSpace[] list;
        if ((CSList == null) && (profileList.length != 0)) {
            /* possible non-ICC src, some profiles, possible non-ICC dst */
            boolean nonICCSrc, nonICCDst;
            ICC_Profile srcProfile, dstProfile;
            if (!(srcColorSpace instanceof ICC_ColorSpace)) {
                nonICCSrc = true;
                srcProfile = ciespace.getProfile();
            } else {
                nonICCSrc = false;
                srcProfile = ((ICC_ColorSpace) srcColorSpace).getProfile();
            }
            if (!(dstColorSpace instanceof ICC_ColorSpace)) {
                nonICCDst = true;
                dstProfile = ciespace.getProfile();
            } else {
                nonICCDst = false;
                dstProfile = ((ICC_ColorSpace) dstColorSpace).getProfile();
            }
            /* make a new transform if needed */
            if ((thisTransform == null) || (thisSrcProfile != srcProfile) ||
                (thisDestProfile != dstProfile) ) {
                updateBITransform(srcProfile, dstProfile);
            }
            // process per scanline
            float maxNum = 65535.0f; // use 16-bit precision in CMM
            ColorSpace cs;
            int iccSrcNumComp;
            if (nonICCSrc) {
                cs = ciespace;
                iccSrcNumComp = 3;
            } else {
                cs = srcColorSpace;
                iccSrcNumComp = srcNumComp;
            }
            float[] srcMinVal = new float[iccSrcNumComp];
            float[] srcInvDiffMinMax = new float[iccSrcNumComp];
            for (int i = 0; i < srcNumComp; i++) {
                srcMinVal[i] = cs.getMinValue(i);
                srcInvDiffMinMax[i] = maxNum / (cs.getMaxValue(i) - srcMinVal[i]);
            }
            int iccDstNumComp;
            if (nonICCDst) {
                cs = ciespace;
                iccDstNumComp = 3;
            } else {
                cs = dstColorSpace;
                iccDstNumComp = dstNumComp;
            }
            float[] dstMinVal = new float[iccDstNumComp];
            float[] dstDiffMinMax = new float[iccDstNumComp];
            for (int i = 0; i < dstNumComp; i++) {
                dstMinVal[i] = cs.getMinValue(i);
                dstDiffMinMax[i] = (cs.getMaxValue(i) - dstMinVal[i]) / maxNum;
            }
            float[] dstColor;
            if (dstHasAlpha) {
                int size = ((dstNumComp + 1) > 3) ? (dstNumComp + 1) : 3;
                dstColor = new float[size];
            } else {
                int size = (dstNumComp  > 3) ? dstNumComp : 3;
                dstColor = new float[size];
            }
            short[] srcLine = new short[w * iccSrcNumComp];
            short[] dstLine = new short[w * iccDstNumComp];
            Object pixel;
            float[] color;
            float[] alpha = null;
            if (needSrcAlpha) {
                alpha = new float[w];
            }
            int idx;
            // process each scanline
            for (int y = 0; y < h; y++) {
                // convert src scanline
                pixel = null;
                color = null;
                idx = 0;
                for (int x = 0; x < w; x++) {
                    pixel = srcRas.getDataElements(x, y, pixel);
                    color = srcCM.getNormalizedComponents(pixel, color, 0);
                    if (needSrcAlpha) {
                        alpha[x] = color[srcNumComp];
                    }
                    if (nonICCSrc) {
                        color = srcColorSpace.toCIEXYZ(color);
                    }
                    for (int i = 0; i < iccSrcNumComp; i++) {
                        srcLine[idx++] = (short)
                            ((color[i] - srcMinVal[i]) * srcInvDiffMinMax[i] +
                             0.5f);
                    }
                }
                // color convert srcLine to dstLine
                thisTransform.colorConvert(srcLine, dstLine);
                // convert dst scanline
                pixel = null;
                idx = 0;
                for (int x = 0; x < w; x++) {
                    for (int i = 0; i < iccDstNumComp; i++) {
                        dstColor[i] = ((float) (dstLine[idx++] & 0xffff)) *
                                      dstDiffMinMax[i] + dstMinVal[i];
                    }
                    if (nonICCDst) {
                        color = srcColorSpace.fromCIEXYZ(dstColor);
                        for (int i = 0; i < dstNumComp; i++) {
                            dstColor[i] = color[i];
                        }
                    }
                    if (needSrcAlpha) {
                        dstColor[dstNumComp] = alpha[x];
                    } else if (dstHasAlpha) {
                        dstColor[dstNumComp] = 1.0f;
                    }
                    pixel = dstCM.getDataElements(dstColor, 0, pixel);
                    dstRas.setDataElements(x, y, pixel);
                }
            }
        } else {
            /* possible non-ICC src, possible CSList, possible non-ICC dst */
            // process per pixel
            int numCS;
            if (CSList == null) {
                numCS = 0;
            } else {
                numCS = CSList.length;
            }
            float[] dstColor;
            if (dstHasAlpha) {
                dstColor = new float[dstNumComp + 1];
            } else {
                dstColor = new float[dstNumComp];
            }
            Object spixel = null;
            Object dpixel = null;
            float[] color = null;
            float[] tmpColor;
            // process each pixel
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    spixel = srcRas.getDataElements(x, y, spixel);
                    color = srcCM.getNormalizedComponents(spixel, color, 0);
                    tmpColor = srcColorSpace.toCIEXYZ(color);
                    for (int i = 0; i < numCS; i++) {
                        tmpColor = CSList[i].fromCIEXYZ(tmpColor);
                        tmpColor = CSList[i].toCIEXYZ(tmpColor);
                    }
                    tmpColor = dstColorSpace.fromCIEXYZ(tmpColor);
                    for (int i = 0; i < dstNumComp; i++) {
                        dstColor[i] = tmpColor[i];
                    }
                    if (needSrcAlpha) {
                        dstColor[dstNumComp] = color[srcNumComp];
                    } else if (dstHasAlpha) {
                        dstColor[dstNumComp] = 1.0f;
                    }
                    dpixel = dstCM.getDataElements(dstColor, 0, dpixel);
                    dstRas.setDataElements(x, y, dpixel);

                }
            }
        }

        return dst;
    }

    /* color convert a Raster - handles byte, ushort, int, short, float,
       or double transferTypes */
    private WritableRaster nonICCRasterFilter(Raster src,
                                                    WritableRaster dst)  {

        if (CSList.length != 2) {
            throw new IllegalArgumentException(
                "Destination ColorSpace is undefined");
        }
        if (src.getNumBands() != CSList[0].getNumComponents()) {
            throw new IllegalArgumentException(
                "Numbers of source Raster bands and source color space " +
                "components do not match");
        }
        if (dst == null) {
            dst = createCompatibleDestRaster(src);
        } else {
            if (src.getHeight() != dst.getHeight() ||
                src.getWidth() != dst.getWidth()) {
                throw new IllegalArgumentException(
                    "Width or height of Rasters do not match");
            }
            if (dst.getNumBands() != CSList[1].getNumComponents()) {
                throw new IllegalArgumentException(
                    "Numbers of destination Raster bands and destination " +
                    "color space components do not match");
            }
        }

        if (srcMinVals == null) {
            getMinMaxValsFromColorSpaces(CSList[0], CSList[1]);
        }

        SampleModel srcSM = src.getSampleModel();
        SampleModel dstSM = dst.getSampleModel();
        boolean srcIsFloat, dstIsFloat;
        int srcTransferType = src.getTransferType();
        int dstTransferType = dst.getTransferType();
        if ((srcTransferType == DataBuffer.TYPE_FLOAT) ||
            (srcTransferType == DataBuffer.TYPE_DOUBLE)) {
            srcIsFloat = true;
        } else {
            srcIsFloat = false;
        }
        if ((dstTransferType == DataBuffer.TYPE_FLOAT) ||
            (dstTransferType == DataBuffer.TYPE_DOUBLE)) {
            dstIsFloat = true;
        } else {
            dstIsFloat = false;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int srcNumBands = src.getNumBands();
        int dstNumBands = dst.getNumBands();
        float[] srcScaleFactor = null;
        float[] dstScaleFactor = null;
        if (!srcIsFloat) {
            srcScaleFactor = new float[srcNumBands];
            for (int i = 0; i < srcNumBands; i++) {
                if (srcTransferType == DataBuffer.TYPE_SHORT) {
                    srcScaleFactor[i] = (srcMaxVals[i] - srcMinVals[i]) /
                                        32767.0f;
                } else {
                    srcScaleFactor[i] = (srcMaxVals[i] - srcMinVals[i]) /
                        ((float) ((1 << srcSM.getSampleSize(i)) - 1));
                }
            }
        }
        if (!dstIsFloat) {
            dstScaleFactor = new float[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                if (dstTransferType == DataBuffer.TYPE_SHORT) {
                    dstScaleFactor[i] = 32767.0f /
                                        (dstMaxVals[i] - dstMinVals[i]);
                } else {
                    dstScaleFactor[i] =
                        ((float) ((1 << dstSM.getSampleSize(i)) - 1)) /
                        (dstMaxVals[i] - dstMinVals[i]);
                }
            }
        }
        int ys = src.getMinY();
        int yd = dst.getMinY();
        int xs, xd;
        float sample;
        float[] color = new float[srcNumBands];
        float[] tmpColor;
        ColorSpace srcColorSpace = CSList[0];
        ColorSpace dstColorSpace = CSList[1];
        // process each pixel
        for (int y = 0; y < h; y++, ys++, yd++) {
            // get src scanline
            xs = src.getMinX();
            xd = dst.getMinX();
            for (int x = 0; x < w; x++, xs++, xd++) {
                for (int i = 0; i < srcNumBands; i++) {
                    sample = src.getSampleFloat(xs, ys, i);
                    if (!srcIsFloat) {
                        sample = sample * srcScaleFactor[i] + srcMinVals[i];
                    }
                    color[i] = sample;
                }
                tmpColor = srcColorSpace.toCIEXYZ(color);
                tmpColor = dstColorSpace.fromCIEXYZ(tmpColor);
                for (int i = 0; i < dstNumBands; i++) {
                    sample = tmpColor[i];
                    if (!dstIsFloat) {
                        sample = (sample - dstMinVals[i]) * dstScaleFactor[i];
                    }
                    dst.setSample(xd, yd, i, sample);
                }
            }
        }
        return dst;
    }

    private void getMinMaxValsFromProfiles(ICC_Profile srcProfile,
                                           ICC_Profile dstProfile) {
        int type = srcProfile.getColorSpaceType();
        int nc = srcProfile.getNumComponents();
        srcMinVals = new float[nc];
        srcMaxVals = new float[nc];
        setMinMax(type, nc, srcMinVals, srcMaxVals);
        type = dstProfile.getColorSpaceType();
        nc = dstProfile.getNumComponents();
        dstMinVals = new float[nc];
        dstMaxVals = new float[nc];
        setMinMax(type, nc, dstMinVals, dstMaxVals);
    }

    private void setMinMax(int type, int nc, float[] minVals, float[] maxVals) {
        if (type == ColorSpace.TYPE_Lab) {
            minVals[0] = 0.0f;    // L
            maxVals[0] = 100.0f;
            minVals[1] = -128.0f; // a
            maxVals[1] = 127.0f;
            minVals[2] = -128.0f; // b
            maxVals[2] = 127.0f;
        } else if (type == ColorSpace.TYPE_XYZ) {
            minVals[0] = minVals[1] = minVals[2] = 0.0f; // X, Y, Z
            maxVals[0] = maxVals[1] = maxVals[2] = 1.0f + (32767.0f/ 32768.0f);
        } else {
            for (int i = 0; i < nc; i++) {
                minVals[i] = 0.0f;
                maxVals[i] = 1.0f;
            }
        }
    }

    private void getMinMaxValsFromColorSpaces(ColorSpace srcCspace,
                                              ColorSpace dstCspace) {
        int nc = srcCspace.getNumComponents();
        srcMinVals = new float[nc];
        srcMaxVals = new float[nc];
        for (int i = 0; i < nc; i++) {
            srcMinVals[i] = srcCspace.getMinValue(i);
            srcMaxVals[i] = srcCspace.getMaxValue(i);
        }
        nc = dstCspace.getNumComponents();
        dstMinVals = new float[nc];
        dstMaxVals = new float[nc];
        for (int i = 0; i < nc; i++) {
            dstMinVals[i] = dstCspace.getMinValue(i);
            dstMaxVals[i] = dstCspace.getMaxValue(i);
        }
    }

}
