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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * @test
 * @bug 8261200
 * @summary Checks that we can write/read the icc profile to/from file
 */
public final class WriteProfileToFile {

    public static void main(String[] args) throws Exception {
        byte[] gold = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();

        testViaDataArray(gold);
        testViaFile(gold);
        testViaStream(gold);
    }

    private static void testViaDataArray(byte[] gold) {
        ICC_Profile profile = ICC_Profile.getInstance(gold);
        compare(gold, profile.getData());
    }

    private static void testViaFile(byte[] gold) throws Exception {
        ICC_Profile profile = ICC_Profile.getInstance(gold);
        profile.write("fileName.icc");
        try {
            profile = ICC_Profile.getInstance("fileName.icc");
            compare(gold, profile.getData());
        } finally {
            Files.delete(new File("fileName.icc").toPath());
        }
    }

    private static void testViaStream(byte[] gold) throws Exception {
        ICC_Profile profile = ICC_Profile.getInstance(gold);
        File file = new File("fileName.icc");
        try (OutputStream outputStream = new FileOutputStream(file)) {
            profile.write(outputStream);
            profile = ICC_Profile.getInstance("fileName.icc");
            compare(gold, profile.getData());
        } finally {
            Files.delete(file.toPath());
        }
    }

    private static void compare(byte[] data1, byte[] data2) {
        if (!Arrays.equals(data1, data2)) {
            throw new RuntimeException("Data mismatch");
        }
    }
}
