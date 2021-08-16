/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.www.content.image;

import java.net.*;
import sun.awt.image.*;
import java.awt.Image;
import java.awt.Toolkit;

public class x_xpixmap extends ContentHandler {
    public Object getContent(URLConnection urlc) throws java.io.IOException {
        return new URLImageSource(urlc);
    }

    @SuppressWarnings("rawtypes")
    public Object getContent(URLConnection urlc, Class[] classes) throws java.io.IOException {
        Class<?>[] cls = classes;
        for (int i = 0; i < cls.length; i++) {
            if (cls[i].isAssignableFrom(URLImageSource.class)) {
                return new URLImageSource(urlc);
            }
            if (cls[i].isAssignableFrom(Image.class)) {
                Toolkit tk = Toolkit.getDefaultToolkit();
                return tk.createImage(new URLImageSource(urlc));
            }
        }
        return null;
    }
}
