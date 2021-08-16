/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import static java.awt.color.ColorSpace.CS_CIEXYZ;
import static java.awt.color.ColorSpace.CS_GRAY;
import static java.awt.color.ColorSpace.CS_LINEAR_RGB;
import static java.awt.color.ColorSpace.CS_PYCC;
import static java.awt.color.ColorSpace.CS_sRGB;

/**
 * @test
 * @bug 8261282
 * @summary Checks static locks in the ColorSpace/ICC_Profile classes.
 */
public final class HotStaticLocks {

    public static void main(String[] args) throws Exception {
        testICCProfile();
        testColorSpace();
    }

    private static void testICCProfile() throws Exception {
        int[] spaces = {CS_sRGB, CS_LINEAR_RGB, CS_CIEXYZ, CS_PYCC, CS_GRAY};
        for (int cs : spaces) {
            synchronized (ICC_Profile.class) {
                Thread t = new Thread(() -> ICC_Profile.getInstance(cs));
                t.start();
                t.join();
            }
        }
    }

    private static void testColorSpace() throws Exception {
        int[] spaces = {CS_sRGB, CS_LINEAR_RGB, CS_CIEXYZ, CS_PYCC, CS_GRAY};
        for (int cs : spaces) {
            synchronized (ColorSpace.class) {
                Thread t = new Thread(() -> ColorSpace.getInstance(cs));
                t.start();
                t.join();
            }
        }
    }
}
