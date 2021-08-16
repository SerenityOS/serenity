/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.loader;

import java.net.URL;
import java.io.File;
import sun.net.www.ParseUtil;

/**
 * (Solaris) platform specific handling for file: URLs .
 * urls must not contain a hostname in the authority field
 * other than "localhost".
 *
 * This implementation could be updated to map such URLs
 * on to /net/host/...
 *
 * @author      Michael McMahon
 */

public class FileURLMapper {

    URL url;
    String path;

    public FileURLMapper (URL url) {
        this.url = url;
    }

    /**
     * @return the platform specific path corresponding to the URL
     *  so long as the URL does not contain a hostname in the authority field.
     */

    public String getPath () {
        if (path != null) {
            return path;
        }
        String host = url.getHost();
        if (host == null || host.isEmpty() || "localhost".equalsIgnoreCase(host)) {
            path = url.getFile();
            path = ParseUtil.decode(path);
        }
        return path;
    }

    /**
     * Checks whether the file identified by the URL exists.
     */
    public boolean exists () {
        String s = getPath ();
        if (s == null) {
            return false;
        } else {
            File f = new File (s);
            return f.exists();
        }
    }
}
