/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

public class XRSolidSrcPict {
    XRBackend con;

    XRSurfaceData srcPict;
    XRColor xrCol;
    int curPixVal;

    public XRSolidSrcPict(XRBackend con, int parentXid) {
        this.con = con;

        xrCol = new XRColor();
        curPixVal = 0xFF000000;

        int solidPixmap = con.createPixmap(parentXid, 32, 1, 1);
        int solidSrcPictXID = con.createPicture(solidPixmap, XRUtils.PictStandardARGB32);
        con.setPictureRepeat(solidSrcPictXID, XRUtils.RepeatNormal);
        con.renderRectangle(solidSrcPictXID, XRUtils.PictOpSrc, XRColor.FULL_ALPHA, 0, 0, 1, 1);
        srcPict = new XRSurfaceData.XRInternalSurfaceData(con, solidSrcPictXID);
    }

    public XRSurfaceData prepareSrcPict(int pixelVal) {
        if(pixelVal != curPixVal) {
            xrCol.setColorValues(pixelVal);
            con.renderRectangle(srcPict.picture, XRUtils.PictOpSrc, xrCol, 0, 0, 1, 1);
            this.curPixVal = pixelVal;
        }

        return srcPict;
    }

}
