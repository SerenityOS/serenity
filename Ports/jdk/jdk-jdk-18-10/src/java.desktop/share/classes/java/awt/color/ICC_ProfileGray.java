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

import java.io.Serial;

import sun.java2d.cmm.Profile;
import sun.java2d.cmm.ProfileDeferralInfo;

/**
 * The {@code ICC_ProfileGray} class is a subclass of the {@code ICC_Profile}
 * class that represents profiles which meet the following criteria: the color
 * space type of the profile is {@code TYPE_GRAY} and the profile includes the
 * {@code grayTRCTag} and {@code mediaWhitePointTag} tags. The
 * {@code getInstance} methods in the {@code ICC_Profile} class will return an
 * {@code ICC_ProfileGray} object when the above conditions are met. Examples of
 * this kind of profile are monochrome input profiles, monochrome display
 * profiles, and monochrome output profiles.
 * <p>
 * The advantage of this class is that it provides a lookup table that Java or
 * native methods can use directly to optimize color conversion in some cases.
 * <p>
 * To transform from a GRAY device profile color space to the CIEXYZ Profile
 * Connection Space, the device gray component is transformed by a lookup
 * through the tone reproduction curve (TRC). The result is treated as the
 * achromatic component of the PCS.
 * <pre>
 *
 * &nbsp;               PCSY = grayTRC[deviceGray]
 *
 * </pre>
 * The inverse transform is done by converting the PCS Y components to device
 * Gray via the inverse of the grayTRC.
 */
public class ICC_ProfileGray extends ICC_Profile {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1124721290732002649L;

    /**
     * Constructs a new {@code ICC_ProfileGray} from a CMM ID.
     */
    ICC_ProfileGray(Profile p) {
        super(p);
    }

    /**
     * Constructs a new {@code ICC_ProfileGray} from a
     * {@code ProfileDeferralInfo} object.
     */
    ICC_ProfileGray(ProfileDeferralInfo pdi) {
        super(pdi);
    }

    /**
     * Returns a float array of length 3 containing the X, Y, and Z components
     * of the mediaWhitePointTag in the ICC profile.
     *
     * @return an array containing the components of the mediaWhitePointTag in
     *         the ICC profile
     */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * Returns a gamma value representing the tone reproduction curve (TRC). If
     * the profile represents the TRC as a table rather than a single gamma
     * value, then an exception is thrown. In this case the actual table can be
     * obtained via {@code getTRC()}. When using a gamma value, the PCS Y
     * component is computed as follows:
     * <pre>
     *
     * &nbsp;                         gamma
     * &nbsp;        PCSY = deviceGray
     *
     * </pre>
     *
     * @return the gamma value as a float
     * @throws ProfileDataException if the profile does not specify the TRC as a
     *         single gamma value
     */
    public float getGamma() {
        return getGamma(ICC_Profile.icSigGrayTRCTag);
    }

    /**
     * Returns the TRC as an array of shorts. If the profile has specified the
     * TRC as linear (gamma = 1.0) or as a simple gamma value, this method
     * throws an exception, and the {@code getGamma()} method should be used to
     * get the gamma value. Otherwise the short array returned here represents a
     * lookup table where the input Gray value is conceptually in the range
     * [0.0, 1.0]. Value 0.0 maps to array index 0 and value 1.0 maps to array
     * index {@code length-1}. Interpolation may be used to generate output
     * values for input values which do not map exactly to an index in the
     * array. Output values also map linearly to the range [0.0, 1.0]. Value 0.0
     * is represented by an array value of 0x0000 and value 1.0 by 0xFFFF, i.e.
     * the values are really unsigned short values, although they are returned
     * in a short array.
     *
     * @return a short array representing the TRC
     * @throws ProfileDataException if the profile does not specify the TRC as a
     *         table
     */
    public short[] getTRC() {
        return getTRC(ICC_Profile.icSigGrayTRCTag);
    }
}
