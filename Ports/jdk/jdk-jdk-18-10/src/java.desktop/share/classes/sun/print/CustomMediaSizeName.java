/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serial;
import java.util.ArrayList;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

class CustomMediaSizeName extends MediaSizeName {
    private static ArrayList<String> customStringTable = new ArrayList<>();
    private static ArrayList<MediaSizeName> customEnumTable = new ArrayList<>();
    private String choiceName;
    private MediaSizeName mediaName;

    private CustomMediaSizeName(int x) {
        super(x);

    }

    private static synchronized int nextValue(String name) {
      customStringTable.add(name);

      return (customStringTable.size()-1);
    }

    public CustomMediaSizeName(String name) {
        super(nextValue(name));
        customEnumTable.add(this);
        choiceName = null;
        mediaName = null;
    }

    public CustomMediaSizeName(String name, String choice,
                               float width, float length) {
        super(nextValue(name));
        choiceName = choice;
        customEnumTable.add(this);
        mediaName = null;
        try {
            mediaName = MediaSize.findMedia(width, length,
                                            MediaSize.INCH);
        } catch (IllegalArgumentException iae) {
        }
        // The public API method finds a closest match even if it not
        // all that close. Here we want to be sure its *really* close.
        if (mediaName != null) {
            MediaSize sz = MediaSize.getMediaSizeForName(mediaName);
            if (sz == null) {
                mediaName = null;
            } else {
                float w = sz.getX(MediaSize.INCH);
                float h = sz.getY(MediaSize.INCH);
                float dw = Math.abs(w - width);
                float dh = Math.abs(h - length);
                if (dw > 0.1 || dh > 0.1) {
                    mediaName = null;
                }
            }
        }
    }

    /**
     * Use serialVersionUID from JDK 1.5 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 7412807582228043717L;

    /**
     * Returns the command string for this media.
     */
    public String getChoiceName() {
        return choiceName;
    }


    /**
     * Returns matching standard MediaSizeName.
     */
    public MediaSizeName getStandardMedia() {
        return mediaName;
    }


    // moved from RasterPrinterJob
    /**
     * Returns closest matching MediaSizeName among given array of Media
     */
    public static MediaSizeName findMedia(Media[] media, float x, float y,
                                          int units) {


        if (x <= 0.0f || y <= 0.0f || units < 1) {
            throw new IllegalArgumentException("args must be +ve values");
        }

        if (media == null || media.length == 0) {
            throw new IllegalArgumentException("args must have valid array of media");
        }

        int size =0;
        MediaSizeName[] msn = new MediaSizeName[media.length];
        for (int i=0; i<media.length; i++) {
            if (media[i] instanceof MediaSizeName) {
                msn[size++] = (MediaSizeName)media[i];
            }
        }

        if (size == 0) {
            return null;
        }

        int match = 0;

        double ls = x * x + y * y;
        double tmp_ls;
        float []dim;
        float diffx = x;
        float diffy = y;

        for (int i=0; i < size ; i++) {
            MediaSize mediaSize = MediaSize.getMediaSizeForName(msn[i]);
            if (mediaSize == null) {
                continue;
            }
            dim = mediaSize.getSize(units);
            if (x == dim[0] && y == dim[1]) {
                match = i;
                break;
            } else {
                diffx = x - dim[0];
                diffy = y - dim[1];
                tmp_ls = diffx * diffx + diffy * diffy;
                if (tmp_ls < ls) {
                    ls = tmp_ls;
                    match = i;
                }
            }
        }

        return msn[match];
    }

    /**
     * Returns the string table for super class MediaSizeName.
     */
    public  Media[] getSuperEnumTable() {
        return (Media[])super.getEnumValueTable();
    }


    /**
     * Returns the string table for class CustomMediaSizeName.
     */
    protected String[] getStringTable() {
      String[] nameTable = new String[customStringTable.size()];
      return customStringTable.toArray(nameTable);
    }

    /**
     * Returns the enumeration value table for class CustomMediaSizeName.
     */
    protected EnumSyntax[] getEnumValueTable() {
      MediaSizeName[] enumTable = new MediaSizeName[customEnumTable.size()];
      return customEnumTable.toArray(enumTable);
    }

}
