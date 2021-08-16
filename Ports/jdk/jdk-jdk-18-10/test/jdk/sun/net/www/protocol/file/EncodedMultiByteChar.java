/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6521166
 * @summary Exception when opening file URLConnection with percent encoded 4 byte UTF8 char
 */

import java.net.*;
import java.io.*;

/* java.lang.IllegalArgumentException if fails */

public class EncodedMultiByteChar
{
    static String filename = "EncodedMultiByteChar" + new String(Character.toChars(0x2123D));
    static String urlStr;
    static String message = "This is a message";

    static {
        try {
            urlStr = "file://" + System.getProperty("java.io.tmpdir") + "/EncodedMultiByteChar" +
                      URLEncoder.encode(new String(Character.toChars(0x2123D)), "UTF-8") + ".txt";
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
    }

    public static void main(String[] args) {
        File file = null;
        try {
            //Create a file with a 4 byte UTF8 character in its name.
            file = new File(System.getProperty("java.io.tmpdir") + File.separator + filename + ".txt");
            file.createNewFile();
            file.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(message.getBytes("UTF-8"));
            fos.close();
        } catch (IOException e) {
            System.out.println("Failed to create test file ");
            e.printStackTrace();
            return;
        }

        System.out.println("file = " + file);

        try {
            System.out.println("URL = " + urlStr);
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(message)) {
                    throw new RuntimeException("Failed: read \"" + line + "\" from file");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
