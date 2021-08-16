/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8032370
 *
 * @summary Test verifies that Image I/O jpeg reader correctly handles
 *          and warns of a truncated image stream.
 *
 * @run     main TruncatedImageWarningTest
 */

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;

public class TruncatedImageWarningTest implements IIOReadWarningListener {

    private static String fileName = "truncated.jpg";
    boolean receivedWarning = false;

    public static void main(String[] args) throws IOException {

        String sep = System.getProperty("file.separator");
        String dir = System.getProperty("test.src", ".");
        String filePath = dir+sep+fileName;
        System.out.println("Test file: " + filePath);
        File f = new File(filePath);
        ImageInputStream in = ImageIO.createImageInputStream(f);
        ImageReader reader = ImageIO.getImageReaders(in).next();
        TruncatedImageWarningTest twt = new TruncatedImageWarningTest();
        reader.addIIOReadWarningListener(twt);
        reader.setInput(in);
        reader.read(0);
        if (!twt.receivedWarning) {
            throw new RuntimeException("No expected warning");
        }
    }

    public void warningOccurred(ImageReader source, String warning) {
        System.out.println("Expected warning: " + warning);
        receivedWarning = true;
    }
}
