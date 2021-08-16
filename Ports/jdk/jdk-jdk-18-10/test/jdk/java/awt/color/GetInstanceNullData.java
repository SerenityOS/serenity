/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.color.ICC_ProfileRGB;

/**
 * @test
 * @bug 4176618 7042594 6211198
 * @summary This interactive test verifies that passing null to
 *          ICC_ProfileRGB.getInstance() does not crash the VM.
 *          An IllegalArgumentException: Invalid ICC Profile Data should be
 *          generated.
 */
public final class GetInstanceNullData {

    public static void main(String[] argv) {
        byte b[] = null;
        try {
            ICC_ProfileRGB p = (ICC_ProfileRGB) ICC_ProfileRGB.getInstance(b);
            throw new RuntimeException("IllegalArgumentException is expected");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            ICC_ProfileGray p = (ICC_ProfileGray) ICC_ProfileGray.getInstance(b);
            throw new RuntimeException("IllegalArgumentException is expected");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            ICC_Profile p = ICC_Profile.getInstance(b);
            throw new RuntimeException("IllegalArgumentException is expected");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }
}
