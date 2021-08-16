/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4112524
 * @summary check for correct implementation of getContentType for JAR urls.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class GetContentType {

    static final String JAR_MIME_TYPE = "x-java/jar";
    static final String GIF_MIME_TYPE = "image/gif";

    public static void main(String[] args) throws Exception {
        /* test JAR URLs -- NOTE: we musn't connect! */
        URL url = new URL(getSpec());
        URLConnection connection = url.openConnection();
        String contentType = connection.getContentType();
        System.out.println(url + " jar content type: " + contentType);
        if (!contentType.equals(JAR_MIME_TYPE)) {
            throw new RuntimeException("invalid MIME type for JAR archive");
        }
        url = new URL(url, "image.gif");
        connection = url.openConnection();
        contentType = connection.getContentType();
        System.out.println(url + " img content type: " + contentType);
        if (!contentType.equals(GIF_MIME_TYPE)) {
            throw new RuntimeException("invalid MIME type for JAR entry");
        }
    }

    static String getSpec() throws IOException {
        File file = new File(".");
        return "jar:file:" + file.getCanonicalPath() +
            File.separator + "jars" + File.separator + "test.jar!/";
    }

}
