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
 * The {@code ICC_ProfileRGB} class is a subclass of the {@code ICC_Profile}
 * class that represents profiles which meet the following criteria: the
 * profile's color space type is RGB, and the profile includes the
 * {@code redColorantTag}, {@code greenColorantTag}, {@code blueColorantTag},
 * {@code redTRCTag}, {@code greenTRCTag}, {@code blueTRCTag},
 * {@code mediaWhitePointTag} tags. The {@code getInstance} methods in the
 * {@code ICC_Profile} class will return an {@code ICC_ProfileRGB} object when
 * the above conditions are met. Three-component, matrix-based input profiles
 * and RGB display profiles are examples of this type of profile.
 * <p>
 * The advantage of this class is that it provides color transform matrices and
 * lookup tables that Java or native methods can use directly to optimize color
 * conversion in some cases.
 * <p>
 * To transform from a device profile color space to the CIEXYZ Profile
 * Connection Space, each device color component is first linearized by a lookup
 * through the corresponding tone reproduction curve (TRC). The resulting linear
 * RGB components are converted to the CIEXYZ PCS using a a 3x3 matrix
 * constructed from the RGB colorants.
 * <pre>
 *
 * &nbsp;               linearR = redTRC[deviceR]
 *
 * &nbsp;               linearG = greenTRC[deviceG]
 *
 * &nbsp;               linearB = blueTRC[deviceB]
 *
 * &nbsp; _      _       _                                             _   _         _
 * &nbsp;[  PCSX  ]     [  redColorantX  greenColorantX  blueColorantX  ] [  linearR  ]
 * &nbsp;[        ]     [                                               ] [           ]
 * &nbsp;[  PCSY  ]  =  [  redColorantY  greenColorantY  blueColorantY  ] [  linearG  ]
 * &nbsp;[        ]     [                                               ] [           ]
 * &nbsp;[_ PCSZ _]     [_ redColorantZ  greenColorantZ  blueColorantZ _] [_ linearB _]
 *
 * </pre>
 * The inverse transform is performed by converting PCS XYZ components to linear
 * RGB components through the inverse of the above 3x3 matrix, and then
 * converting linear RGB to device RGB through inverses of the TRCs.
 */
public class ICC_ProfileRGB extends ICC_Profile {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 8505067385152579334L;

    /**
     * Used to get a gamma value or TRC for the red component.
     */
    public static final int REDCOMPONENT = 0;

    /**
     * Used to get a gamma value or TRC for the green component.
     */
    public static final int GREENCOMPONENT = 1;

    /**
     * Used to get a gamma value or TRC for the blue component.
     */
    public static final int BLUECOMPONENT = 2;

    /**
     * Constructs an new {@code ICC_ProfileRGB} from a CMM ID.
     *
     * @param  p the CMM ID for the profile.
     */
    ICC_ProfileRGB(Profile p) {
        super(p);
    }

    /**
     * Constructs a new {@code ICC_ProfileRGB} from a
     * {@code ProfileDeferralInfo} object.
     *
     * @param  pdi
     */
    ICC_ProfileRGB(ProfileDeferralInfo pdi) {
        super(pdi);
    }

    /**
     * Returns an array that contains the components of the profile's
     * {@code mediaWhitePointTag}.
     *
     * @return a 3-element {@code float} array containing the x, y, and z
     *         components of the profile's {@code mediaWhitePointTag}
     */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * Returns a 3x3 {@code float} matrix constructed from the X, Y, and Z
     * components of the profile's {@code redColorantTag},
     * {@code greenColorantTag}, and {@code blueColorantTag}.
     * <p>
     * This matrix can be used for color transforms in the forward direction of
     * the profile--from the profile color space to the CIEXYZ PCS.
     *
     * @return a 3x3 {@code float} array that contains the x, y, and z
     *         components of the profile's {@code redColorantTag},
     *         {@code greenColorantTag}, and {@code blueColorantTag}
     */
    public float[][] getMatrix() {
        float[] red = getXYZTag(ICC_Profile.icSigRedColorantTag);
        float[] green = getXYZTag(ICC_Profile.icSigGreenColorantTag);
        float[] blue = getXYZTag(ICC_Profile.icSigBlueColorantTag);
        return new float[][]{{red[0], green[0], blue[0]},
                             {red[1], green[1], blue[1]},
                             {red[2], green[2], blue[2]}};
    }

    /**
     * Returns a gamma value representing the tone reproduction curve (TRC) for
     * a particular component. The component parameter must be one of
     * {@code REDCOMPONENT}, {@code GREENCOMPONENT}, or {@code BLUECOMPONENT}.
     * <p>
     * If the profile represents the TRC for the corresponding component as a
     * table rather than a single gamma value, an exception is thrown. In this
     * case the actual table can be obtained through the {@link #getTRC(int)}
     * method. When using a gamma value, the linear component (R, G, or B) is
     * computed as follows:
     * <pre>
     *
     * &nbsp;                                         gamma
     * &nbsp;        linearComponent = deviceComponent
     *
     * </pre>
     *
     * @param  component the {@code ICC_ProfileRGB} constant that represents the
     *         component whose TRC you want to retrieve
     * @return the gamma value as a float
     * @throws IllegalArgumentException if the component is not
     *         {@code REDCOMPONENT}, {@code GREENCOMPONENT}, or
     *         {@code BLUECOMPONENT}
     * @throws ProfileDataException if the profile does not specify the
     *         corresponding TRC as a single gamma value
     */
    public float getGamma(int component) {
        return super.getGamma(toTag(component));
    }

    /**
     * Returns the TRC for a particular component as an array. Component must be
     * {@code REDCOMPONENT}, {@code GREENCOMPONENT}, or {@code BLUECOMPONENT}.
     * Otherwise the returned array represents a lookup table where the input
     * component value is conceptually in the range [0.0, 1.0]. Value 0.0 maps
     * to array index 0 and value 1.0 maps to array index {@code length-1}.
     * Interpolation might be used to generate output values for input values
     * that do not map exactly to an index in the array. Output values also map
     * linearly to the range [0.0, 1.0]. Value 0.0 is represented by an array
     * value of 0x0000 and value 1.0 by 0xFFFF. In other words, the values are
     * really unsigned {@code short} values even though they are returned in a
     * {@code short} array.
     * <p>
     * If the profile has specified the corresponding TRC as linear (gamma =
     * 1.0) or as a simple gamma value, this method throws an exception. In this
     * case, the {@link #getGamma(int)} method should be used to get the gamma
     * value.
     *
     * @param  component the {@code ICC_ProfileRGB} constant that represents the
     *         component whose TRC you want to retrieve: {@code REDCOMPONENT},
     *         {@code GREENCOMPONENT}, or {@code BLUECOMPONENT}
     * @return a short array representing the TRC
     * @throws IllegalArgumentException if the component is not
     *         {@code REDCOMPONENT}, {@code GREENCOMPONENT}, or
     *         {@code BLUECOMPONENT}
     * @throws ProfileDataException if the profile does not specify the
     *         corresponding TRC as a table
     */
    public short[] getTRC(int component) {
        return super.getTRC(toTag(component));
    }

    /**
     * Converts the {@code ICC_ProfileRGB} constant to the appropriate tag.
     *
     * @param  component the {@code ICC_ProfileRGB} constant
     * @return the tag signature
     * @throws IllegalArgumentException if the component is not
     *         {@code REDCOMPONENT}, {@code GREENCOMPONENT}, or
     *         {@code BLUECOMPONENT}
     */
    private static int toTag(int component) {
        return switch (component) {
            case REDCOMPONENT -> ICC_Profile.icSigRedTRCTag;
            case GREENCOMPONENT -> ICC_Profile.icSigGreenTRCTag;
            case BLUECOMPONENT -> ICC_Profile.icSigBlueTRCTag;
            default -> throw new IllegalArgumentException(
                    "Must be Red, Green, or Blue");
        };
    }
}
