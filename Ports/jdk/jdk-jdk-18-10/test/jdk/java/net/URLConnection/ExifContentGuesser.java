/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4639576
 * @summary java.net.URLConnection.guessContentTypeFromStream cannot
 * handle Exif format
 */

import java.net.URLConnection;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * This test makes sure that URLConnection.guessContentTypeFromStream
 * recognizes Exif file format. This test uses the file:
 * olympus.jpg for testing the same.
 */

public class ExifContentGuesser {

    public static void main(String args[]) throws Exception {
        String filename = System.getProperty("test.src", ".") +
                          "/" + "olympus.jpg";
        System.out.println("filename: " + filename);
        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(filename));

            String content_type = URLConnection.guessContentTypeFromStream(in);
            if (content_type == null) {
                throw new Exception("Test failed: Could not recognise " +
                                "Exif image");
            } else {
                System.out.println("content-type: " + content_type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
