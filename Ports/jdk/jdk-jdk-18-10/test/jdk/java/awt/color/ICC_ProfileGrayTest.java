/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.color.ProfileDataException;

/**
 * @test
 * @bug 8254370
 * @summary Tests basic ICC_ProfileGray functionality
 */
public final class ICC_ProfileGrayTest {

    public static void main(String[] args) throws Exception {
        ICC_Profile csProfile = ICC_Profile.getInstance(ColorSpace.CS_GRAY);
        ICC_Profile dataProfile = ICC_Profile.getInstance(csProfile.getData());
        ICC_Profile stringProfile = ICC_Profile.getInstance("GRAY.pf");
        test(csProfile);
        test(dataProfile);
        test(stringProfile);
    }

    private static void test(ICC_Profile profile) {
        // Gray profile should be implemented as ICC_ProfileGray and includes
        // the mediaWhitePointTag and grayTRCTag tags
        if (!(profile instanceof ICC_ProfileGray)
                || profile.getData(ICC_Profile.icSigMediaWhitePointTag) == null
                || profile.getData(ICC_Profile.icSigGrayTRCTag) == null) {
            throw new RuntimeException("Wrong profile: " + profile);
        }

        ICC_ProfileGray gray = (ICC_ProfileGray) profile;

        int length = gray.getMediaWhitePoint().length;
        if (length != 3) {
            throw new RuntimeException("Wrong data length: " + length);
        }

        // if getTRC() throws an exception then getGamma() should work
        boolean trc = false;
        try {
            gray.getTRC();
            trc = true;
            System.out.println("getTRC() works fine");
        } catch (ProfileDataException ignore) {
            gray.getGamma();
        }
        // if getGamma() throws an exception then getTRC() should work
        boolean gamma = false;
        try {
            gray.getGamma();
            gamma = true;
            System.out.println("getGamma() works fine");
        } catch (ProfileDataException ignore) {
            gray.getTRC();
        }

        if (gamma == trc) {
            // only one should work
            throw new RuntimeException("Only one operation should work");
        }
    }
}
