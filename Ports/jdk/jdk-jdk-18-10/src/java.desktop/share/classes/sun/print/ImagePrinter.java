/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.net.URL;
import java.io.InputStream;
import javax.imageio.ImageIO;

class ImagePrinter implements Printable {

    BufferedImage image;

    ImagePrinter(InputStream stream) {
        try {
            image = ImageIO.read(stream);
        } catch (Exception e) {
        }
    }

    ImagePrinter(URL url) {
        try {
            image = ImageIO.read(url);
        } catch (Exception e) {
        }
    }

    public int print(Graphics g, PageFormat pf, int index) {

        if (index > 0 || image == null) {
            return Printable.NO_SUCH_PAGE;
        }

        ((Graphics2D)g).translate(pf.getImageableX(), pf.getImageableY());
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        int iw = (int)pf.getImageableWidth();
        int ih = (int)pf.getImageableHeight();

        // ensure image will fit
        int dw = w;
        int dh = h;
        if (dw > iw) {
            dh = (int)(dh * ( (float) iw / (float) dw)) ;
            dw = iw;
        }
        if (dh > ih) {
            dw = (int)(dw * ( (float) ih / (float) dh)) ;
            dh = ih;
        }
        // centre on page
        int dx = (iw - dw) / 2;
        int dy = (ih - dh) / 2;

        g.drawImage(image, dx, dy, dx+dw, dy+dh, 0, 0, w, h, null);
        return Printable.PAGE_EXISTS;
    }
}
