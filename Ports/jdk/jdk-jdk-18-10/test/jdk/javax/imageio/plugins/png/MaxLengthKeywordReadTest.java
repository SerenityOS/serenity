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

/*
 * @test
 * @bug     8195841
 * @summary Test verifies that PNGImageReader doesn't allow creation of
 *          non null terminated keywords consuming full length where they
 *          should be null terminated
 * @run     main MaxLengthKeywordReadTest
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class MaxLengthKeywordReadTest {
    // PNG image stream having non null terminated keyword of length 80
    // in tEXt chunk
    private static String inputImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAA" +
            "ABCAAAAAA6fptVAAAAUXRFWHRBdXRob3JlZEF1dGhvcmVkQXV0aG9yZWRBdXRob" +
            "3JlZEF1dGhvcmVkQXV0aG9yZWRBdXRob3JlZEF1dGhvcmVkQXV0aG9yZWRBdXRo" +
            "b3JlZAAhn0sLAAAACklEQVR4XmP4DwABAQEAJwnd2gAAAABJRU5ErkJggg==";

    public static void main(String[] args) throws IOException {
        byte[] inputBytes = Base64.getDecoder().decode(inputImageBase64);
        ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
        ImageInputStream input = ImageIO.createImageInputStream(bais);
        Iterator iter = ImageIO.getImageReaders(input);
        ImageReader reader = (ImageReader) iter.next();
        reader.setInput(input, false, false);
        try {
            reader.read(0, reader.getDefaultReadParam());
        } catch (IIOException e) {
            // we expect it to throw IIOException
            if (e.getCause().getMessage() !=
                "Found non null terminated string") {
                throw new RuntimeException("Test failed. Did not get " +
                        "expected IIOException");
            }
        }
    }
}
