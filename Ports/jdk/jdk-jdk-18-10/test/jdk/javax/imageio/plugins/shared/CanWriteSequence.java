/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * @test
 * @bug     4958064 8183349
 * @summary Test verifies that when we try to forcefully run
 *          prepareWriteSequence() where it is not supported
 *          will ImageIO throws an UnsupportedOperationException
 *          or not.
 * @run     main CanWriteSequence
 */
public final class CanWriteSequence {

    private static File file;
    private static FileOutputStream fos;
    private static ImageOutputStream ios;

    public static void main(final String[] args) throws Exception {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        final Iterator<ImageWriterSpi> iter =
                registry.getServiceProviders(ImageWriterSpi.class,
                        provider -> true, true);
        // Validates all supported ImageWriters
        while (iter.hasNext()) {
            final ImageWriter writer = iter.next().createWriterInstance();
            System.out.println("ImageWriter = " + writer);
            test(writer);
        }
        System.out.println("Test passed");
    }

    private static void test(final ImageWriter writer) throws Exception {
        try {
            file = File.createTempFile("temp", ".img");
            fos = new FileOutputStream(file);
            ios = ImageIO.createImageOutputStream(fos);
            writer.setOutput(ios);
            final IIOMetadata data = writer.getDefaultStreamMetadata(null);

            if (writer.canWriteSequence()) {
                writer.prepareWriteSequence(data);
            } else {
                try {
                    writer.prepareWriteSequence(data);
                    throw new RuntimeException(
                            "UnsupportedOperationException was not thrown");
                } catch (final UnsupportedOperationException ignored) {
                // expected
                }
            }
        } finally {
            writer.dispose();
            if (file != null) {
                if (ios != null) {
                    ios.close();
                }
                if (fos != null) {
                    fos.close();
                }
                Files.delete(file.toPath());
            }
        }
    }
}