/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4504473
 * @summary File.lastModified() & URLConnection.getLastModified() are inconsistent
 */
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class GetLastModified {
    public static void main(String[] args) {
        try {
            File f = File.createTempFile("test", null);
            f.deleteOnExit();
            String s = f.getAbsolutePath();
            s = s.startsWith("/") ? s : "/" + s;
            URL url = new URL("file://localhost"+s);
            URLConnection conn = null;
            conn = url.openConnection();
            conn.connect();
            if (f.lastModified() != conn.getLastModified())
                throw new RuntimeException("file.lastModified() & FileURLConnection.getLastModified() should be equal");
            f.delete();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
