/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public final class Utils {
    private static final int BUFFER_LENGTH = 1024;

    public static String prependPrefix(String prefix, String name) {
        return  (prefix == null || prefix.isEmpty())
                ? name
                : (name == null || name.isEmpty())
                  ? prefix
                  : String.format("%s.%s", prefix, name);
    }

    public static void copyStream(InputStream in, OutputStream out)
            throws IOException {
        int n;
        byte[] buffer = new byte[BUFFER_LENGTH];
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    public static void copyStream(Reader in, Writer out)
            throws IOException {
        int n;
        char[] buffer = new char[BUFFER_LENGTH];
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    public static Properties getProperties(String name) {
        Properties properties = new Properties();
        String resourceName = String.format(
                "/%s.%s", name.toLowerCase(), "properties");
        InputStream stream = Utils.class.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IllegalStateException(String.format(
                    "resource '%s' doesn't exist%n", resourceName));
        }
        try {
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                    "can't read resource '%s' : %s%n",
                    resourceName, e.getMessage()), e);
        }
        return properties;
    }

    private Utils() { }
}
