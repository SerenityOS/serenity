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
 * @bug 6522294
 * @summary If URI scheme is file and URL is not UTF-8 encoded, the ParseUtil.decode throws an Exception
 */

import java.net.*;
import java.io.*;

public class NonUTF8EncodedChar
{
    public static void main(String[] args) {
        try {
            String s = "file:///c:/temp//m%FCnchen.txt";
            System.out.println("URL = "+s);
            URL url = new URL(s);
            URLConnection urlCon = url.openConnection();
            System.out.println("succeeded");

            urlCon.getInputStream();
             System.out.println("succeeded");

        } catch (IOException ioe) {
            // Ignore this is ok. The file may not exist.
            ioe.printStackTrace();
        } catch (IllegalArgumentException iae) {
            String message = iae.getMessage();
            if (message == null || message.equals("")) {
                System.out.println("No message");
                throw new RuntimeException("Failed: No message in Exception");
            }
        }
    }
}
