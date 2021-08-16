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

/* ********************************************************************
 **********************************************************************
 **********************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997                      ***
 *** As  an unpublished  work pursuant to Title 17 of the United    ***
 *** States Code.  All rights reserved.                             ***
 **********************************************************************
 **********************************************************************
 **********************************************************************/

package java.awt.color;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.StringTokenizer;

import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.PCMM;
import sun.java2d.cmm.Profile;
import sun.java2d.cmm.ProfileDataVerifier;
import sun.java2d.cmm.ProfileDeferralInfo;

/**
 * A representation of color profile data for device independent and device
 * dependent color spaces based on the International Color Consortium
 * Specification ICC.1:2001-12, File Format for Color Profiles, (see
 * <a href="http://www.color.org"> http://www.color.org</a>).
 * <p>
 * An {@code ICC_ColorSpace} object can be constructed from an appropriate
 * {@code ICC_Profile}. Typically, an {@code ICC_ColorSpace} would be associated
 * with an ICC Profile which is either an input, display, or output profile (see
 * the ICC specification). There are also device link, abstract, color space
 * conversion, and named color profiles. These are less useful for tagging a
 * color or image, but are useful for other purposes (in particular device link
 * profiles can provide improved performance for converting from one device's
 * color space to another's).
 * <p>
 * ICC Profiles represent transformations from the color space of the profile
 * (e.g. a monitor) to a Profile Connection Space (PCS). Profiles of interest
 * for tagging images or colors have a PCS which is one of the two specific
 * device independent spaces (one CIEXYZ space and one CIELab space) defined in
 * the ICC Profile Format Specification. Most profiles of interest either have
 * invertible transformations or explicitly specify transformations going both
 * directions.
 *
 * @see ICC_ColorSpace
 */
public class ICC_Profile implements Serializable {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3938515861990936766L;

    /**
     * The implementation specific CMM profile, {@code null} if this
     * {@code ICC_Profile} is not activated by the {@link #cmmProfile()} method.
     * This field must not be used directly and only via {@link #cmmProfile()}.
     */
    private transient volatile Profile cmmProfile;

    /**
     * Stores some information about {@code ICC_Profile} without causing a
     * deferred profile to be loaded. Note that we can defer the loading of
     * standard profiles only. If this field is null, then {@link #cmmProfile}
     * should be used to access profile information.
     */
    private transient volatile ProfileDeferralInfo deferralInfo;

    /**
     * The lazy registry of singleton profile objects for specific built-in
     * color spaces defined in the ColorSpace class (e.g. CS_sRGB),
     * see getInstance(int cspace) factory method.
     */
    private interface BuiltInProfile {
        /*
         * Deferral is only used for standard profiles. Enabling the appropriate
         * access privileges is handled at a lower level.
         */
        ICC_Profile SRGB = new ICC_ProfileRGB(new ProfileDeferralInfo(
               "sRGB.pf", ColorSpace.TYPE_RGB, 3, CLASS_DISPLAY));

        ICC_Profile LRGB = new ICC_ProfileRGB(new ProfileDeferralInfo(
               "LINEAR_RGB.pf", ColorSpace.TYPE_RGB, 3, CLASS_DISPLAY));

        ICC_Profile XYZ = new ICC_Profile(new ProfileDeferralInfo(
               "CIEXYZ.pf", ColorSpace.TYPE_XYZ, 3, CLASS_ABSTRACT));

        ICC_Profile PYCC = new ICC_Profile(new ProfileDeferralInfo(
               "PYCC.pf", ColorSpace.TYPE_3CLR, 3, CLASS_COLORSPACECONVERSION));

        ICC_Profile GRAY = new ICC_ProfileGray(new ProfileDeferralInfo(
               "GRAY.pf", ColorSpace.TYPE_GRAY, 1, CLASS_DISPLAY));
    }

    /**
     * Profile class is input.
     */
    public static final int CLASS_INPUT = 0;

    /**
     * Profile class is display.
     */
    public static final int CLASS_DISPLAY = 1;

    /**
     * Profile class is output.
     */
    public static final int CLASS_OUTPUT = 2;

    /**
     * Profile class is device link.
     */
    public static final int CLASS_DEVICELINK = 3;

    /**
     * Profile class is color space conversion.
     */
    public static final int CLASS_COLORSPACECONVERSION = 4;

    /**
     * Profile class is abstract.
     */
    public static final int CLASS_ABSTRACT = 5;

    /**
     * Profile class is named color.
     */
    public static final int CLASS_NAMEDCOLOR = 6;


    /**
     * ICC Profile Color Space Type Signature: 'XYZ '.
     */
    public static final int icSigXYZData = 0x58595A20;

    /**
     * ICC Profile Color Space Type Signature: 'Lab '.
     */
    public static final int icSigLabData = 0x4C616220;

    /**
     * ICC Profile Color Space Type Signature: 'Luv '.
     */
    public static final int icSigLuvData = 0x4C757620;

    /**
     * ICC Profile Color Space Type Signature: 'YCbr'.
     */
    public static final int icSigYCbCrData = 0x59436272;

    /**
     * ICC Profile Color Space Type Signature: 'Yxy '.
     */
    public static final int icSigYxyData = 0x59787920;

    /**
     * ICC Profile Color Space Type Signature: 'RGB '.
     */
    public static final int icSigRgbData = 0x52474220;

    /**
     * ICC Profile Color Space Type Signature: 'GRAY'.
     */
    public static final int icSigGrayData = 0x47524159;

    /**
     * ICC Profile Color Space Type Signature: 'HSV'.
     */
    public static final int icSigHsvData = 0x48535620;

    /**
     * ICC Profile Color Space Type Signature: 'HLS'.
     */
    public static final int icSigHlsData = 0x484C5320;

    /**
     * ICC Profile Color Space Type Signature: 'CMYK'.
     */
    public static final int icSigCmykData = 0x434D594B;

    /**
     * ICC Profile Color Space Type Signature: 'CMY '.
     */
    public static final int icSigCmyData = 0x434D5920;

    /**
     * ICC Profile Color Space Type Signature: '2CLR'.
     */
    public static final int icSigSpace2CLR = 0x32434C52;

    /**
     * ICC Profile Color Space Type Signature: '3CLR'.
     */
    public static final int icSigSpace3CLR = 0x33434C52;

    /**
     * ICC Profile Color Space Type Signature: '4CLR'.
     */
    public static final int icSigSpace4CLR = 0x34434C52;

    /**
     * ICC Profile Color Space Type Signature: '5CLR'.
     */
    public static final int icSigSpace5CLR = 0x35434C52;

    /**
     * ICC Profile Color Space Type Signature: '6CLR'.
     */
    public static final int icSigSpace6CLR = 0x36434C52;

    /**
     * ICC Profile Color Space Type Signature: '7CLR'.
     */
    public static final int icSigSpace7CLR = 0x37434C52;

    /**
     * ICC Profile Color Space Type Signature: '8CLR'.
     */
    public static final int icSigSpace8CLR = 0x38434C52;

    /**
     * ICC Profile Color Space Type Signature: '9CLR'.
     */
    public static final int icSigSpace9CLR = 0x39434C52;

    /**
     * ICC Profile Color Space Type Signature: 'ACLR'.
     */
    public static final int icSigSpaceACLR = 0x41434C52;

    /**
     * ICC Profile Color Space Type Signature: 'BCLR'.
     */
    public static final int icSigSpaceBCLR = 0x42434C52;

    /**
     * ICC Profile Color Space Type Signature: 'CCLR'.
     */
    public static final int icSigSpaceCCLR = 0x43434C52;

    /**
     * ICC Profile Color Space Type Signature: 'DCLR'.
     */
    public static final int icSigSpaceDCLR = 0x44434C52;

    /**
     * ICC Profile Color Space Type Signature: 'ECLR'.
     */
    public static final int icSigSpaceECLR = 0x45434C52;

    /**
     * ICC Profile Color Space Type Signature: 'FCLR'.
     */
    public static final int icSigSpaceFCLR = 0x46434C52;


    /**
     * ICC Profile Class Signature: 'scnr'.
     */
    public static final int icSigInputClass = 0x73636E72;

    /**
     * ICC Profile Class Signature: 'mntr'.
     */
    public static final int icSigDisplayClass = 0x6D6E7472;

    /**
     * ICC Profile Class Signature: 'prtr'.
     */
    public static final int icSigOutputClass = 0x70727472;

    /**
     * ICC Profile Class Signature: 'link'.
     */
    public static final int icSigLinkClass = 0x6C696E6B;

    /**
     * ICC Profile Class Signature: 'abst'.
     */
    public static final int icSigAbstractClass = 0x61627374;

    /**
     * ICC Profile Class Signature: 'spac'.
     */
    public static final int icSigColorSpaceClass = 0x73706163;

    /**
     * ICC Profile Class Signature: 'nmcl'.
     */
    public static final int icSigNamedColorClass = 0x6e6d636c;


    /**
     * ICC Profile Rendering Intent: Perceptual.
     */
    public static final int icPerceptual = 0;

    /**
     * ICC Profile Rendering Intent: RelativeColorimetric.
     */
    public static final int icRelativeColorimetric = 1;

    /**
     * ICC Profile Rendering Intent: Media-RelativeColorimetric.
     *
     * @since 1.5
     */
    public static final int icMediaRelativeColorimetric = 1;

    /**
     * ICC Profile Rendering Intent: Saturation.
     */
    public static final int icSaturation = 2;

    /**
     * ICC Profile Rendering Intent: AbsoluteColorimetric.
     */
    public static final int icAbsoluteColorimetric = 3;

    /**
     * ICC Profile Rendering Intent: ICC-AbsoluteColorimetric.
     *
     * @since 1.5
     */
    public static final int icICCAbsoluteColorimetric = 3;


    /**
     * ICC Profile Tag Signature: 'head' - special.
     */
    public static final int icSigHead = 0x68656164;

    /**
     * ICC Profile Tag Signature: 'A2B0'.
     */
    public static final int icSigAToB0Tag = 0x41324230;

    /**
     * ICC Profile Tag Signature: 'A2B1'.
     */
    public static final int icSigAToB1Tag = 0x41324231;

    /**
     * ICC Profile Tag Signature: 'A2B2'.
     */
    public static final int icSigAToB2Tag = 0x41324232;

    /**
     * ICC Profile Tag Signature: 'bXYZ'.
     */
    public static final int icSigBlueColorantTag = 0x6258595A;

    /**
     * ICC Profile Tag Signature: 'bXYZ'.
     *
     * @since 1.5
     */
    public static final int icSigBlueMatrixColumnTag = 0x6258595A;

    /**
     * ICC Profile Tag Signature: 'bTRC'.
     */
    public static final int icSigBlueTRCTag = 0x62545243;

    /**
     * ICC Profile Tag Signature: 'B2A0'.
     */
    public static final int icSigBToA0Tag = 0x42324130;

    /**
     * ICC Profile Tag Signature: 'B2A1'.
     */
    public static final int icSigBToA1Tag = 0x42324131;

    /**
     * ICC Profile Tag Signature: 'B2A2'.
     */
    public static final int icSigBToA2Tag = 0x42324132;

    /**
     * ICC Profile Tag Signature: 'calt'.
     */
    public static final int icSigCalibrationDateTimeTag = 0x63616C74;

    /**
     * ICC Profile Tag Signature: 'targ'.
     */
    public static final int icSigCharTargetTag = 0x74617267;

    /**
     * ICC Profile Tag Signature: 'cprt'.
     */
    public static final int icSigCopyrightTag = 0x63707274;

    /**
     * ICC Profile Tag Signature: 'crdi'.
     */
    public static final int icSigCrdInfoTag = 0x63726469;

    /**
     * ICC Profile Tag Signature: 'dmnd'.
     */
    public static final int icSigDeviceMfgDescTag = 0x646D6E64;

    /**
     * ICC Profile Tag Signature: 'dmdd'.
     */
    public static final int icSigDeviceModelDescTag = 0x646D6464;

    /**
     * ICC Profile Tag Signature: 'devs'.
     */
    public static final int icSigDeviceSettingsTag = 0x64657673;

    /**
     * ICC Profile Tag Signature: 'gamt'.
     */
    public static final int icSigGamutTag = 0x67616D74;

    /**
     * ICC Profile Tag Signature: 'kTRC'.
     */
    public static final int icSigGrayTRCTag = 0x6b545243;

    /**
     * ICC Profile Tag Signature: 'gXYZ'.
     */
    public static final int icSigGreenColorantTag = 0x6758595A;

    /**
     * ICC Profile Tag Signature: 'gXYZ'.
     *
     * @since 1.5
     */
    public static final int icSigGreenMatrixColumnTag = 0x6758595A;

    /**
     * ICC Profile Tag Signature: 'gTRC'.
     */
    public static final int icSigGreenTRCTag = 0x67545243;

    /**
     * ICC Profile Tag Signature: 'lumi'.
     */
    public static final int icSigLuminanceTag = 0x6C756d69;

    /**
     * ICC Profile Tag Signature: 'meas'.
     */
    public static final int icSigMeasurementTag = 0x6D656173;

    /**
     * ICC Profile Tag Signature: 'bkpt'.
     */
    public static final int icSigMediaBlackPointTag = 0x626B7074;

    /**
     * ICC Profile Tag Signature: 'wtpt'.
     */
    public static final int icSigMediaWhitePointTag = 0x77747074;

    /**
     * ICC Profile Tag Signature: 'ncl2'.
     */
    public static final int icSigNamedColor2Tag = 0x6E636C32;

    /**
     * ICC Profile Tag Signature: 'resp'.
     */
    public static final int icSigOutputResponseTag = 0x72657370;

    /**
     * ICC Profile Tag Signature: 'pre0'.
     */
    public static final int icSigPreview0Tag = 0x70726530;

    /**
     * ICC Profile Tag Signature: 'pre1'.
     */
    public static final int icSigPreview1Tag = 0x70726531;

    /**
     * ICC Profile Tag Signature: 'pre2'.
     */
    public static final int icSigPreview2Tag = 0x70726532;

    /**
     * ICC Profile Tag Signature: 'desc'.
     */
    public static final int icSigProfileDescriptionTag = 0x64657363;

    /**
     * ICC Profile Tag Signature: 'pseq'.
     */
    public static final int icSigProfileSequenceDescTag = 0x70736571;

    /**
     * ICC Profile Tag Signature: 'psd0'.
     */
    public static final int icSigPs2CRD0Tag = 0x70736430;

    /**
     * ICC Profile Tag Signature: 'psd1'.
     */
    public static final int icSigPs2CRD1Tag = 0x70736431;

    /**
     * ICC Profile Tag Signature: 'psd2'.
     */
    public static final int icSigPs2CRD2Tag = 0x70736432;

    /**
     * ICC Profile Tag Signature: 'psd3'.
     */
    public static final int icSigPs2CRD3Tag = 0x70736433;

    /**
     * ICC Profile Tag Signature: 'ps2s'.
     */
    public static final int icSigPs2CSATag = 0x70733273;

    /**
     * ICC Profile Tag Signature: 'ps2i'.
     */
    public static final int icSigPs2RenderingIntentTag = 0x70733269;

    /**
     * ICC Profile Tag Signature: 'rXYZ'.
     */
    public static final int icSigRedColorantTag = 0x7258595A;

    /**
     * ICC Profile Tag Signature: 'rXYZ'.
     *
     * @since 1.5
     */
    public static final int icSigRedMatrixColumnTag = 0x7258595A;

    /**
     * ICC Profile Tag Signature: 'rTRC'.
     */
    public static final int icSigRedTRCTag = 0x72545243;

    /**
     * ICC Profile Tag Signature: 'scrd'.
     */
    public static final int icSigScreeningDescTag = 0x73637264;

    /**
     * ICC Profile Tag Signature: 'scrn'.
     */
    public static final int icSigScreeningTag = 0x7363726E;

    /**
     * ICC Profile Tag Signature: 'tech'.
     */
    public static final int icSigTechnologyTag = 0x74656368;

    /**
     * ICC Profile Tag Signature: 'bfd '.
     */
    public static final int icSigUcrBgTag = 0x62666420;

    /**
     * ICC Profile Tag Signature: 'vued'.
     */
    public static final int icSigViewingCondDescTag = 0x76756564;

    /**
     * ICC Profile Tag Signature: 'view'.
     */
    public static final int icSigViewingConditionsTag = 0x76696577;

    /**
     * ICC Profile Tag Signature: 'chrm'.
     */
    public static final int icSigChromaticityTag = 0x6368726d;

    /**
     * ICC Profile Tag Signature: 'chad'.
     *
     * @since 1.5
     */
    public static final int icSigChromaticAdaptationTag = 0x63686164;

    /**
     * ICC Profile Tag Signature: 'clro'.
     *
     * @since 1.5
     */
    public static final int icSigColorantOrderTag = 0x636C726F;

    /**
     * ICC Profile Tag Signature: 'clrt'.
     *
     * @since 1.5
     */
    public static final int icSigColorantTableTag = 0x636C7274;


    /**
     * ICC Profile Header Location: profile size in bytes.
     */
    public static final int icHdrSize = 0;

    /**
     * ICC Profile Header Location: CMM for this profile.
     */
    public static final int icHdrCmmId = 4;

    /**
     * ICC Profile Header Location: format version number.
     */
    public static final int icHdrVersion = 8;

    /**
     * ICC Profile Header Location: type of profile.
     */
    public static final int icHdrDeviceClass = 12;

    /**
     * ICC Profile Header Location: color space of data.
     */
    public static final int icHdrColorSpace = 16;

    /**
     * ICC Profile Header Location: PCS - XYZ or Lab only.
     */
    public static final int icHdrPcs = 20;

    /**
     * ICC Profile Header Location: date profile was created.
     */
    public static final int icHdrDate = 24;

    /**
     * ICC Profile Header Location: icMagicNumber.
     */
    public static final int icHdrMagic = 36;

    /**
     * ICC Profile Header Location: primary platform.
     */
    public static final int icHdrPlatform = 40;

    /**
     * ICC Profile Header Location: various bit settings.
     */
    public static final int icHdrFlags = 44;

    /**
     * ICC Profile Header Location: device manufacturer.
     */
    public static final int icHdrManufacturer = 48;

    /**
     * ICC Profile Header Location: device model number.
     */
    public static final int icHdrModel = 52;

    /**
     * ICC Profile Header Location: device attributes.
     */
    public static final int icHdrAttributes = 56;

    /**
     * ICC Profile Header Location: rendering intent.
     */
    public static final int icHdrRenderingIntent = 64;

    /**
     * ICC Profile Header Location: profile illuminant.
     */
    public static final int icHdrIlluminant = 68;

    /**
     * ICC Profile Header Location: profile creator.
     */
    public static final int icHdrCreator = 80;

    /**
     * ICC Profile Header Location: profile's ID.
     *
     * @since 1.5
     */
    public static final int icHdrProfileID = 84;


    /**
     * ICC Profile Constant: tag type signature.
     */
    public static final int icTagType = 0;

    /**
     * ICC Profile Constant: reserved.
     */
    public static final int icTagReserved = 4;

    /**
     * ICC Profile Constant: curveType count.
     */
    public static final int icCurveCount = 8;

    /**
     * ICC Profile Constant: curveType data.
     */
    public static final int icCurveData = 12;

    /**
     * ICC Profile Constant: XYZNumber X.
     */
    public static final int icXYZNumberX = 8;


    /**
     * Constructs an {@code ICC_Profile} object with a given ID.
     */
    ICC_Profile(Profile p) {
        cmmProfile = p;
    }

    /**
     * Constructs an {@code ICC_Profile} object whose loading will be deferred.
     * The ID will be 0 until the profile is loaded.
     */
    ICC_Profile(ProfileDeferralInfo pdi) {
        deferralInfo = pdi;
    }

    /**
     * Frees the resources associated with an {@code ICC_Profile} object.
     *
     * @deprecated The {@code finalize} method has been deprecated. Subclasses
     *         that override {@code finalize} in order to perform cleanup should
     *         be modified to use alternative cleanup mechanisms and to remove
     *         the overriding {@code finalize} method. When overriding the
     *         {@code finalize} method, its implementation must explicitly
     *         ensure that {@code super.finalize()} is invoked as described in
     *         {@link Object#finalize}. See the specification for {@link
     *         Object#finalize()} for further information about migration
     *         options.
     */
    @Deprecated(since = "9", forRemoval = true)
    @SuppressWarnings("removal")
    protected void finalize() {
    }

    /**
     * Constructs an {@code ICC_Profile} object corresponding to the data in a
     * byte array.
     *
     * @param  data the specified ICC Profile data
     * @return an {@code ICC_Profile} object corresponding to the data in the
     *         specified {@code data} array
     * @throws IllegalArgumentException If the byte array does not contain valid
     *         ICC Profile data
     */
    public static ICC_Profile getInstance(byte[] data) {
        ProfileDataVerifier.verify(data);
        Profile p;
        try {
            p = CMSManager.getModule().loadProfile(data);
        } catch (CMMException c) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        try {
            if (getColorSpaceType(p) == ColorSpace.TYPE_GRAY
                    && getData(p, icSigMediaWhitePointTag) != null
                    && getData(p, icSigGrayTRCTag) != null) {
                return new ICC_ProfileGray(p);
            }
            if (getColorSpaceType(p) == ColorSpace.TYPE_RGB
                    && getData(p, icSigMediaWhitePointTag) != null
                    && getData(p, icSigRedColorantTag) != null
                    && getData(p, icSigGreenColorantTag) != null
                    && getData(p, icSigBlueColorantTag) != null
                    && getData(p, icSigRedTRCTag) != null
                    && getData(p, icSigGreenTRCTag) != null
                    && getData(p, icSigBlueTRCTag) != null) {
                return new ICC_ProfileRGB(p);
            }
        } catch (CMMException c) {
            // will try to create a general icc profile
        }
        return new ICC_Profile(p);
    }

    /**
     * Constructs an {@code ICC_Profile} corresponding to one of the specific
     * color spaces defined by the {@code ColorSpace} class (for example
     * {@code CS_sRGB}). Throws an {@code IllegalArgumentException} if cspace is
     * not one of the defined color spaces.
     *
     * @param  cspace the type of color space to create a profile for. The
     *         specified type is one of the color space constants defined in the
     *         {@code ColorSpace} class.
     * @return an {@code ICC_Profile} object corresponding to the specified
     *         {@code ColorSpace} type
     * @throws IllegalArgumentException If {@code cspace} is not one of the
     *         predefined color space types
     */
    public static ICC_Profile getInstance(int cspace) {
        return switch (cspace) {
            case ColorSpace.CS_sRGB -> BuiltInProfile.SRGB;
            case ColorSpace.CS_LINEAR_RGB -> BuiltInProfile.LRGB;
            case ColorSpace.CS_CIEXYZ -> BuiltInProfile.XYZ;
            case ColorSpace.CS_PYCC -> BuiltInProfile.PYCC;
            case ColorSpace.CS_GRAY -> BuiltInProfile.GRAY;
            default -> {
                throw new IllegalArgumentException("Unknown color space");
            }
        };
    }

    /**
     * Constructs an {@code ICC_Profile} corresponding to the data in a file.
     * {@code fileName} may be an absolute or a relative file specification.
     * Relative file names are looked for in several places: first, relative to
     * any directories specified by the {@code java.iccprofile.path} property;
     * second, relative to any directories specified by the
     * {@code java.class.path} property; finally, in a directory used to store
     * profiles always available, such as the profile for sRGB. Built-in
     * profiles use {@code .pf} as the file name extension for profiles, e.g.
     * {@code sRGB.pf}. This method throws an {@code IOException} if the
     * specified file cannot be opened or if an I/O error occurs while reading
     * the file. It throws an {@code IllegalArgumentException} if the file does
     * not contain valid ICC Profile data.
     *
     * @param  fileName the file that contains the data for the profile
     * @return an {@code ICC_Profile} object corresponding to the data in the
     *         specified file
     * @throws IOException If the specified file cannot be opened or an I/O
     *         error occurs while reading the file
     * @throws IllegalArgumentException If the file does not contain valid ICC
     *         Profile data
     * @throws SecurityException If a security manager is installed and it does
     *         not permit read access to the given file
     */
    public static ICC_Profile getInstance(String fileName) throws IOException {
        InputStream is;
        File f = getProfileFile(fileName);
        if (f != null) {
            is = new FileInputStream(f);
        } else {
            is = getStandardProfileInputStream(fileName);
        }
        if (is == null) {
            throw new IOException("Cannot open file " + fileName);
        }
        try (is) {
            return getInstance(is);
        }
    }

    /**
     * Constructs an {@code ICC_Profile} corresponding to the data in an
     * {@code InputStream}. This method throws an
     * {@code IllegalArgumentException} if the stream does not contain valid ICC
     * Profile data. It throws an {@code IOException} if an I/O error occurs
     * while reading the stream.
     *
     * @param  s the input stream from which to read the profile data
     * @return an {@code ICC_Profile} object corresponding to the data in the
     *         specified {@code InputStream}
     * @throws IOException If an I/O error occurs while reading the stream
     * @throws IllegalArgumentException If the stream does not contain valid ICC
     *         Profile data
     */
    public static ICC_Profile getInstance(InputStream s) throws IOException {
        return getInstance(getProfileDataFromStream(s));
    }

    static byte[] getProfileDataFromStream(InputStream s) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(s);
        bis.mark(128); // 128 is the length of the ICC profile header

        byte[] header = bis.readNBytes(128);
        if (header.length < 128 || header[36] != 0x61 || header[37] != 0x63 ||
            header[38] != 0x73 || header[39] != 0x70) {
            return null;   /* not a valid profile */
        }
        int profileSize = intFromBigEndian(header, 0);
        bis.reset();
        try {
            return bis.readNBytes(profileSize);
        } catch (OutOfMemoryError e) {
            throw new IOException("Color profile is too big");
        }
    }

    /**
     * Activates and returns the deferred standard profiles. Implementation of
     * this method mimics the old behaviour when the {@code CMMException} and
     * {@code IOException} were wrapped by the {@code ProfileDataException}, and
     * the {@code ProfileDataException} itself was ignored during activation.
     *
     * @return the implementation specific CMM profile, or {@code null}
     */
    private Profile cmmProfile() {
        Profile p = cmmProfile;
        if (p != null) {
            return p; // one volatile read on common path
        }
        synchronized (this) {
            if (cmmProfile != null) {
                return cmmProfile;
            }
            var is = getStandardProfileInputStream(deferralInfo.filename);
            if (is == null) {
                return null;
            }
            try (is) {
                byte[] data = getProfileDataFromStream(is);
                if (data != null) {
                    p = cmmProfile = CMSManager.getModule().loadProfile(data);
                    // from now we cannot use the deferred value, drop it
                    deferralInfo = null;
                }
            } catch (CMMException | IOException ignore) {
            }
        }
        return p;
    }

    /**
     * Returns profile major version.
     *
     * @return the major version of the profile
     */
    public int getMajorVersion() {
        return getData(icSigHead)[8];
    }

    /**
     * Returns profile minor version.
     *
     * @return the minor version of the profile
     */
    public int getMinorVersion() {
        return getData(icSigHead)[9];
    }

    /**
     * Returns the profile class.
     *
     * @return one of the predefined profile class constants
     */
    public int getProfileClass() {
        ProfileDeferralInfo info = deferralInfo;
        if (info != null) {
            return info.profileClass;
        }
        byte[] theHeader = getData(icSigHead);
        int theClassSig = intFromBigEndian(theHeader, icHdrDeviceClass);
        return switch (theClassSig) {
            case icSigInputClass -> CLASS_INPUT;
            case icSigDisplayClass -> CLASS_DISPLAY;
            case icSigOutputClass -> CLASS_OUTPUT;
            case icSigLinkClass -> CLASS_DEVICELINK;
            case icSigColorSpaceClass -> CLASS_COLORSPACECONVERSION;
            case icSigAbstractClass -> CLASS_ABSTRACT;
            case icSigNamedColorClass -> CLASS_NAMEDCOLOR;
            default -> {
                throw new IllegalArgumentException("Unknown profile class");
            }
        };
    }

    /**
     * Returns the color space type. Returns one of the color space type
     * constants defined by the {@code ColorSpace} class. This is the "input"
     * color space of the profile. The type defines the number of components of
     * the color space and the interpretation, e.g. {@code TYPE_RGB} identifies
     * a color space with three components - red, green, and blue. It does not
     * define the particular color characteristics of the space, e.g. the
     * chromaticities of the primaries.
     *
     * @return one of the color space type constants defined in the
     *         {@code ColorSpace} class
     */
    public int getColorSpaceType() {
        ProfileDeferralInfo info = deferralInfo;
        if (info != null) {
            return info.colorSpaceType;
        }
        return getColorSpaceType(cmmProfile());
    }

    private static int getColorSpaceType(Profile p) {
        byte[] theHeader = getData(p, icSigHead);
        int theColorSpaceSig = intFromBigEndian(theHeader, icHdrColorSpace);
        return iccCStoJCS(theColorSpaceSig);
    }

    /**
     * Returns the color space type of the Profile Connection Space (PCS).
     * Returns one of the color space type constants defined by the ColorSpace
     * class. This is the "output" color space of the profile. For an input,
     * display, or output profile useful for tagging colors or images, this will
     * be either {@code TYPE_XYZ} or {@code TYPE_Lab} and should be interpreted
     * as the corresponding specific color space defined in the ICC
     * specification. For a device link profile, this could be any of the color
     * space type constants.
     *
     * @return one of the color space type constants defined in the
     *         {@code ColorSpace} class
     */
    public int getPCSType() {
        byte[] theHeader = getData(icSigHead);
        int thePCSSig = intFromBigEndian(theHeader, icHdrPcs);
        return iccCStoJCS(thePCSSig);
    }

    /**
     * Write this {@code ICC_Profile} to a file.
     *
     * @param  fileName the file to write the profile data to
     * @throws IOException If the file cannot be opened for writing or an I/O
     *         error occurs while writing to the file
     */
    public void write(String fileName) throws IOException {
        try (OutputStream out = new FileOutputStream(fileName)) {
            write(out);
        }
    }

    /**
     * Write this {@code ICC_Profile} to an {@code OutputStream}.
     *
     * @param  s the stream to write the profile data to
     * @throws IOException If an I/O error occurs while writing to the stream
     */
    public void write(OutputStream s) throws IOException {
        s.write(getData());
    }

    /**
     * Returns a byte array corresponding to the data of this
     * {@code ICC_Profile}.
     *
     * @return a byte array that contains the profile data
     * @see #setData(int, byte[])
     */
    public byte[] getData() {
        return CMSManager.getModule().getProfileData(cmmProfile());
    }

    /**
     * Returns a particular tagged data element from the profile as a byte
     * array. Elements are identified by signatures as defined in the ICC
     * specification. The signature icSigHead can be used to get the header.
     * This method is useful for advanced applets or applications which need to
     * access profile data directly.
     *
     * @param  tagSignature the ICC tag signature for the data element you want
     *         to get
     * @return a byte array that contains the tagged data element. Returns
     *         {@code null} if the specified tag doesn't exist.
     * @see #setData(int, byte[])
     */
    public byte[] getData(int tagSignature) {
        byte[] t = getData(cmmProfile(), tagSignature);
        return t != null ? t.clone() : null;
    }

    private static byte[] getData(Profile p, int tagSignature) {
        try {
            return CMSManager.getModule().getTagData(p, tagSignature);
        } catch (CMMException c) {
            return null;
        }
    }

    /**
     * Sets a particular tagged data element in the profile from a byte array.
     * The array should contain data in a format, corresponded to the
     * {@code tagSignature} as defined in the ICC specification, section 10.
     * This method is useful for advanced applets or applications which need to
     * access profile data directly.
     *
     * @param  tagSignature the ICC tag signature for the data element you want
     *         to set
     * @param  tagData the data to set for the specified tag signature
     * @throws IllegalArgumentException if {@code tagSignature} is not a
     *         signature as defined in the ICC specification.
     * @throws IllegalArgumentException if a content of the {@code tagData}
     *         array can not be interpreted as valid tag data, corresponding to
     *         the {@code tagSignature}
     * @see #getData
     */
    public void setData(int tagSignature, byte[] tagData) {
        CMSManager.getModule().setTagData(cmmProfile(), tagSignature, tagData);
    }

    /**
     * Returns the number of color components in the "input" color space of this
     * profile. For example if the color space type of this profile is
     * {@code TYPE_RGB}, then this method will return 3.
     *
     * @return the number of color components in the profile's input color space
     * @throws ProfileDataException if color space is in the profile is invalid
     */
    public int getNumComponents() {
        ProfileDeferralInfo info = deferralInfo;
        if (info != null) {
            return info.numComponents;
        }
        byte[] theHeader = getData(icSigHead);
        int theColorSpaceSig = intFromBigEndian(theHeader, icHdrColorSpace);
        return switch (theColorSpaceSig) {
            case icSigGrayData -> 1;
            case icSigSpace2CLR -> 2;
            case icSigXYZData, icSigLabData, icSigLuvData, icSigYCbCrData,
                    icSigYxyData, icSigRgbData, icSigHsvData, icSigHlsData,
                    icSigCmyData, icSigSpace3CLR -> 3;
            case icSigCmykData, icSigSpace4CLR -> 4;
            case icSigSpace5CLR -> 5;
            case icSigSpace6CLR -> 6;
            case icSigSpace7CLR -> 7;
            case icSigSpace8CLR -> 8;
            case icSigSpace9CLR -> 9;
            case icSigSpaceACLR -> 10;
            case icSigSpaceBCLR -> 11;
            case icSigSpaceCCLR -> 12;
            case icSigSpaceDCLR -> 13;
            case icSigSpaceECLR -> 14;
            case icSigSpaceFCLR -> 15;
            default -> {
                throw new ProfileDataException("invalid ICC color space");
            }
        };
    }

    /**
     * Returns a float array of length 3 containing the X, Y, and Z components
     * of the mediaWhitePointTag in the ICC profile.
     */
    float[] getMediaWhitePoint() {
        return getXYZTag(icSigMediaWhitePointTag);
    }

    /**
     * Returns a float array of length 3 containing the X, Y, and Z components
     * encoded in an XYZType tag.
     */
    final float[] getXYZTag(int tagSignature) {
        byte[] theData = getData(tagSignature);
        float[] theXYZNumber = new float[3]; /* array to return */

        /* convert s15Fixed16Number to float */
        for (int i1 = 0, i2 = icXYZNumberX; i1 < 3; i1++, i2 += 4) {
            int theS15Fixed16 = intFromBigEndian(theData, i2);
            theXYZNumber[i1] = ((float) theS15Fixed16) / 65536.0f;
        }
        return theXYZNumber;
    }

    /**
     * Returns a gamma value representing a tone reproduction curve (TRC). If
     * the profile represents the TRC as a table rather than a single gamma
     * value, then an exception is thrown. In this case the actual table can be
     * obtained via {@link #getTRC}. {@code tagSignature} should be one of
     * {@code icSigGrayTRCTag}, {@code icSigRedTRCTag},
     * {@code icSigGreenTRCTag}, or {@code icSigBlueTRCTag}.
     *
     * @return the gamma value as a float
     * @throws ProfileDataException if the profile does not specify the TRC as a
     *         single gamma value
     */
    float getGamma(int tagSignature) {
        byte[] theTRCData = getData(tagSignature);
        if (intFromBigEndian(theTRCData, icCurveCount) != 1) {
            throw new ProfileDataException("TRC is not a gamma");
        }

        /* convert u8Fixed8 to float */
        int theU8Fixed8 = shortFromBigEndian(theTRCData, icCurveData) & 0xffff;
        return theU8Fixed8 / 256.0f;
    }

    /**
     * Returns the TRC as an array of shorts. If the profile has specified the
     * TRC as linear (gamma = 1.0) or as a simple gamma value, this method
     * throws an exception, and the {@link #getGamma} method should be used to
     * get the gamma value. Otherwise the short array returned here represents a
     * lookup table where the input Gray value is conceptually in the range
     * [0.0, 1.0]. Value 0.0 maps to array index 0 and value 1.0 maps to array
     * index length-1. Interpolation may be used to generate output values for
     * input values which do not map exactly to an index in the array. Output
     * values also map linearly to the range [0.0, 1.0]. Value 0.0 is
     * represented by an array value of 0x0000 and value 1.0 by 0xFFFF, i.e. the
     * values are really unsigned short values, although they are returned in a
     * short array. {@code tagSignature} should be one of
     * {@code icSigGrayTRCTag}, {@code icSigRedTRCTag},
     * {@code icSigGreenTRCTag}, or {@code icSigBlueTRCTag}.
     *
     * @return a short array representing the TRC
     * @throws ProfileDataException if the profile does not specify the TRC as a
     *         table
     */
    short[] getTRC(int tagSignature) {
        byte[] theTRCData = getData(tagSignature);
        int nElements = intFromBigEndian(theTRCData, icCurveCount);
        if (nElements == 1) {
            throw new ProfileDataException("TRC is not a table");
        }

        short[] theTRC = new short[nElements]; /* array to return */
        for (int i1 = 0, i2 = icCurveData; i1 < nElements; i1++, i2 += 2) {
            theTRC[i1] = shortFromBigEndian(theTRCData, i2);
        }
        return theTRC;
    }

    /**
     * Convert an ICC color space signature into a Java color space type.
     */
    private static int iccCStoJCS(int theColorSpaceSig) {
        return switch (theColorSpaceSig) {
            case icSigXYZData -> ColorSpace.TYPE_XYZ;
            case icSigLabData -> ColorSpace.TYPE_Lab;
            case icSigLuvData -> ColorSpace.TYPE_Luv;
            case icSigYCbCrData -> ColorSpace.TYPE_YCbCr;
            case icSigYxyData -> ColorSpace.TYPE_Yxy;
            case icSigRgbData -> ColorSpace.TYPE_RGB;
            case icSigGrayData -> ColorSpace.TYPE_GRAY;
            case icSigHsvData -> ColorSpace.TYPE_HSV;
            case icSigHlsData -> ColorSpace.TYPE_HLS;
            case icSigCmykData -> ColorSpace.TYPE_CMYK;
            case icSigCmyData -> ColorSpace.TYPE_CMY;
            case icSigSpace2CLR -> ColorSpace.TYPE_2CLR;
            case icSigSpace3CLR -> ColorSpace.TYPE_3CLR;
            case icSigSpace4CLR -> ColorSpace.TYPE_4CLR;
            case icSigSpace5CLR -> ColorSpace.TYPE_5CLR;
            case icSigSpace6CLR -> ColorSpace.TYPE_6CLR;
            case icSigSpace7CLR -> ColorSpace.TYPE_7CLR;
            case icSigSpace8CLR -> ColorSpace.TYPE_8CLR;
            case icSigSpace9CLR -> ColorSpace.TYPE_9CLR;
            case icSigSpaceACLR -> ColorSpace.TYPE_ACLR;
            case icSigSpaceBCLR -> ColorSpace.TYPE_BCLR;
            case icSigSpaceCCLR -> ColorSpace.TYPE_CCLR;
            case icSigSpaceDCLR -> ColorSpace.TYPE_DCLR;
            case icSigSpaceECLR -> ColorSpace.TYPE_ECLR;
            case icSigSpaceFCLR -> ColorSpace.TYPE_FCLR;
            default -> {
                throw new IllegalArgumentException("Unknown color space");
            }
        };
    }

    private static int intFromBigEndian(byte[] array, int index) {
        return (((array[index]   & 0xff) << 24) |
                ((array[index+1] & 0xff) << 16) |
                ((array[index+2] & 0xff) <<  8) |
                 (array[index+3] & 0xff));
    }

    private static short shortFromBigEndian(byte[] array, int index) {
        return (short) (((array[index]   & 0xff) << 8) |
                         (array[index+1] & 0xff));
    }

    /**
     * {@code fileName} may be an absolute or a relative file specification.
     * Relative file names are looked for in several places: first, relative to
     * any directories specified by the {@code java.iccprofile.path} property;
     * second, relative to any directories specified by the
     * {@code java.class.path}. The built-in profile files are now loaded as
     * resources, since they may not be individual disk files, and so this
     * method will not find these and on a {@code null} return, the caller needs
     * to try as resources. Built-in profiles use {@code .pf} as the file name
     * extension for profiles, e.g. {@code sRGB.pf}.
     */
    private static File getProfileFile(String fileName) {
        File f = new File(fileName); /* try absolute file name */
        if (f.isAbsolute()) {
            /* Rest of code has little sense for an absolute pathname,
               so return here. */
            return f.isFile() ? f : null;
        }
        String path, dir, fullPath;
        if (!f.isFile() &&
                (path = System.getProperty("java.iccprofile.path")) != null) {
            /* try relative to java.iccprofile.path */
            StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
            while (st.hasMoreTokens() && ((f == null) || (!f.isFile()))) {
                dir = st.nextToken();
                fullPath = dir + File.separatorChar + fileName;
                f = new File(fullPath);
                if (!isChildOf(f, dir)) {
                    f = null;
                }
            }
        }

        if ((f == null || !f.isFile())
                && (path = System.getProperty("java.class.path")) != null) {
            /* try relative to java.class.path */
            StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
            while (st.hasMoreTokens() && ((f == null) || (!f.isFile()))) {
                dir = st.nextToken();
                fullPath = dir + File.separatorChar + fileName;
                f = new File(fullPath);
            }
        }

        if (f != null && !f.isFile()) {
            f = null;
        }
        return f;
    }

    /**
     * Returns a stream corresponding to a built-in profile specified by
     * fileName. If there is no built-in profile with such name, then the method
     * returns {@code null}.
     */
    @SuppressWarnings("removal")
    private static InputStream getStandardProfileInputStream(String fileName) {
        return AccessController.doPrivileged(
            (PrivilegedAction<InputStream>) () -> {
                return PCMM.class.getResourceAsStream("profiles/" + fileName);
            }, null, new FilePermission("<<ALL FILES>>", "read"),
                     new RuntimePermission("accessSystemModules"));
    }

    /**
     * Checks whether given file resides inside give directory.
     */
    private static boolean isChildOf(File f, String dirName) {
        try {
            File dir = new File(dirName);
            String canonicalDirName = dir.getCanonicalPath();
            if (!canonicalDirName.endsWith(File.separator)) {
                canonicalDirName += File.separator;
            }
            String canonicalFileName = f.getCanonicalPath();
            return canonicalFileName.startsWith(canonicalDirName);
        } catch (IOException e) {
            /* we do not expect the IOException here, because invocation
             * of this function is always preceded by isFile() call.
             */
            return false;
        }
    }

    /*
     * Serialization support.
     *
     * Directly deserialized profiles are useless since they are not registered
     * with CMM. We don't allow constructor to be called directly and instead
     * have clients to call one of getInstance factory methods that will
     * register the profile with CMM. For deserialization we implement
     * readResolve method that will resolve the bogus deserialized profile
     * object with one obtained with getInstance as well.
     *
     * There are two primary factory methods for construction of ICC profiles:
     * getInstance(int cspace) and getInstance(byte[] data). This implementation
     * of ICC_Profile uses the former to return a cached singleton profile
     * object, other implementations will likely use this technique too. To
     * preserve the singleton pattern across serialization we serialize cached
     * singleton profiles in such a way that deserializing VM could call
     * getInstance(int cspace) method that will resolve deserialized object into
     * the corresponding singleton as well.
     *
     * Since the singletons are private to ICC_Profile the readResolve method
     * have to be `protected' instead of `private' so that singletons that are
     * instances of subclasses of ICC_Profile could be correctly deserialized.
     */

    /**
     * Version of the format of additional serialized data in the stream.
     * Version&nbsp;{@code 1} corresponds to Java&nbsp;2 Platform,&nbsp;v1.3.
     *
     * @serial
     * @since 1.3
     */
    private int iccProfileSerializedDataVersion = 1;

    /**
     * Writes default serializable fields to the stream. Writes a string and an
     * array of bytes to the stream as additional data.
     *
     * @param  s stream used for serialization
     * @throws IOException thrown by {@code ObjectInputStream}
     * @serialData the {@code String} is the name of one of
     *         <code>CS_<var>*</var></code> constants defined in the
     *         {@link ColorSpace} class if the profile object is a profile for a
     *         predefined color space (for example {@code "CS_sRGB"}). The
     *         string is {@code null} otherwise.
     *         <p>
     *         The {@code byte[]} array is the profile data for the profile. For
     *         predefined color spaces {@code null} is written instead of the
     *         profile data. If in the future versions of Java API new
     *         predefined color spaces will be added, future versions of this
     *         class may choose to write for new predefined color spaces not
     *         only the color space name, but the profile data as well so that
     *         older versions could still deserialize the object.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        String csName = null;
        if (this == BuiltInProfile.SRGB) {
            csName = "CS_sRGB";
        } else if (this == BuiltInProfile.XYZ) {
            csName = "CS_CIEXYZ";
        } else if (this == BuiltInProfile.PYCC) {
            csName = "CS_PYCC";
        } else if (this == BuiltInProfile.GRAY) {
            csName = "CS_GRAY";
        } else if (this == BuiltInProfile.LRGB) {
            csName = "CS_LINEAR_RGB";
        }

        // Future versions may choose to write profile data for new
        // predefined color spaces as well, if any will be introduced,
        // so that old versions that don't recognize the new CS name
        // may fall back to constructing profile from the data.
        byte[] data = null;
        if (csName == null) {
            // getData will activate deferred profile if necessary
            data = getData();
        }

        s.writeObject(csName);
        s.writeObject(data);
    }

    // Temporary storage used by readObject to store resolved profile
    // (obtained with getInstance) for readResolve to return.
    private transient ICC_Profile resolvedDeserializedProfile;

    /**
     * Reads default serializable fields from the stream. Reads from the stream
     * a string and an array of bytes as additional data.
     *
     * @param  s stream used for deserialization
     * @throws IOException thrown by {@code ObjectInputStream}
     * @throws ClassNotFoundException thrown by {@code
     *         ObjectInputStream}
     * @serialData the {@code String} is the name of one of
     *         <code>CS_<var>*</var></code> constants defined in the
     *         {@link ColorSpace} class if the profile object is a profile for a
     *         predefined color space (for example {@code "CS_sRGB"}). The
     *         string is {@code null} otherwise.
     *         <p>
     *         The {@code byte[]} array is the profile data for the profile. It
     *         will usually be {@code null} for the predefined profiles.
     *         <p>
     *         If the string is recognized as a constant name for predefined
     *         color space the object will be resolved into profile obtained
     *         with
     *         <code>getInstance(int&nbsp;cspace)</code> and the profile data
     *         are ignored. Otherwise the object will be resolved into profile
     *         obtained with
     *         <code>getInstance(byte[]&nbsp;data)</code>.
     * @see #readResolve()
     * @see #getInstance(int)
     * @see #getInstance(byte[])
     */
    @Serial
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        String csName = (String) s.readObject();
        byte[] data = (byte[]) s.readObject();

        int cspace = 0;         // ColorSpace.CS_* constant if known
        boolean isKnownPredefinedCS = false;
        if (csName != null) {
            isKnownPredefinedCS = true;
            if (csName.equals("CS_sRGB")) {
                cspace = ColorSpace.CS_sRGB;
            } else if (csName.equals("CS_CIEXYZ")) {
                cspace = ColorSpace.CS_CIEXYZ;
            } else if (csName.equals("CS_PYCC")) {
                cspace = ColorSpace.CS_PYCC;
            } else if (csName.equals("CS_GRAY")) {
                cspace = ColorSpace.CS_GRAY;
            } else if (csName.equals("CS_LINEAR_RGB")) {
                cspace = ColorSpace.CS_LINEAR_RGB;
            } else {
                isKnownPredefinedCS = false;
            }
        }

        if (isKnownPredefinedCS) {
            resolvedDeserializedProfile = getInstance(cspace);
        } else {
            resolvedDeserializedProfile = getInstance(data);
        }
    }

    /**
     * Resolves instances being deserialized into instances registered with CMM.
     *
     * @return ICC_Profile object for profile registered with CMM
     * @throws ObjectStreamException never thrown, but mandated by the
     *         serialization spec
     * @since 1.3
     */
    @Serial
    protected Object readResolve() throws ObjectStreamException {
        return resolvedDeserializedProfile;
    }
}
