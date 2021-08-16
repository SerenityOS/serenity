/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6294683
 * @summary Test verifies that GIF ImageWriteParam behaves according to spec
 */

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

public class DisableCompressionTest {

    public static void main(String[] args) {
        testFormat("GIF");
    }

    protected static void testFormat(String format) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        if (writer == null) {
            throw new RuntimeException("No writer for " + format);
        }

        ImageWriteParam param = writer.getDefaultWriteParam();
        int[] supported_modes = new int[] {
            ImageWriteParam.MODE_COPY_FROM_METADATA,
                    ImageWriteParam.MODE_DEFAULT,
                    ImageWriteParam.MODE_EXPLICIT };

        for (int mode : supported_modes) {
            String mode_name = getModeName(mode);
            System.out.println("Test mode " + mode_name + "...");
            // we know that GIF image writer supports compression
            // and supports any compression mode form supportd_modes
            // If exception would be thrown here then test failed.
            param.setCompressionMode(mode);

            // now we are trying to disable compression.
            // This operation is not supported because GIF image writer
            // does not provide uncompressed output.
            // The expected behaviour is that UnsupportedOperationException
            // will be thrown here and current compression mode will not be
            // changed.
            boolean gotException = false;
            try {
                param.setCompressionMode(ImageWriteParam.MODE_DISABLED);
            } catch (UnsupportedOperationException e) {
                gotException = true;
            } catch (Throwable e) {
                throw new RuntimeException("Test failed due to unexpected exception", e);
            }

            if (!gotException) {
                throw new RuntimeException("Test failed.");
            }

            if (param.getCompressionMode() != mode) {
                throw new RuntimeException("Param state was changed.");
            }
            System.out.println("Test passed.");
        }
    }

    private static String getModeName(int mode) {
        switch(mode) {
            case ImageWriteParam.MODE_COPY_FROM_METADATA:
                return "MODE_COPY_FROM_METADATA";
            case ImageWriteParam.MODE_DEFAULT:
                return "MODE_DEFAULT";
            case ImageWriteParam.MODE_DISABLED:
                return "MODE_DISABLED";
            case ImageWriteParam.MODE_EXPLICIT:
                return "MODE_EXPLICIT";
            default:
                throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }
}
