/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;

public class TIFFImageMetadataFormat extends TIFFMetadataFormat {

    private static TIFFImageMetadataFormat theInstance = null;

    static {
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
        return false;
    }

    private TIFFImageMetadataFormat() {
        this.resourceBaseName =
     "javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources";
        this.rootName = TIFFImageMetadata.NATIVE_METADATA_FORMAT_NAME;

        TIFFElementInfo einfo;
        TIFFAttrInfo ainfo;
        String[] empty = new String[0];
        String[] childNames;
        String[] attrNames;

        childNames = new String[] { "TIFFIFD" };
        einfo = new TIFFElementInfo(childNames, empty, CHILD_POLICY_SEQUENCE);

        elementInfoMap.put(TIFFImageMetadata.NATIVE_METADATA_FORMAT_NAME,
                           einfo);

        childNames = new String[] { "TIFFField", "TIFFIFD" };
        attrNames =
            new String[] { "tagSets", "parentTagNumber", "parentTagName" };
        einfo = new TIFFElementInfo(childNames, attrNames, CHILD_POLICY_SEQUENCE);
        elementInfoMap.put("TIFFIFD", einfo);

        ainfo = new TIFFAttrInfo();
        ainfo.dataType = DATATYPE_STRING;
        ainfo.isRequired = true;
        attrInfoMap.put("TIFFIFD/tagSets", ainfo);

        ainfo = new TIFFAttrInfo();
        ainfo.dataType = DATATYPE_INTEGER;
        ainfo.isRequired = false;
        attrInfoMap.put("TIFFIFD/parentTagNumber", ainfo);

        ainfo = new TIFFAttrInfo();
        ainfo.dataType = DATATYPE_STRING;
        ainfo.isRequired = false;
        attrInfoMap.put("TIFFIFD/parentTagName", ainfo);

        String[] types = {
            "TIFFByte",
            "TIFFAscii",
            "TIFFShort",
            "TIFFSShort",
            "TIFFLong",
            "TIFFSLong",
            "TIFFRational",
            "TIFFSRational",
            "TIFFFloat",
            "TIFFDouble",
            "TIFFUndefined"
        };

        attrNames = new String[] { "value", "description" };
        String[] attrNamesValueOnly = new String[] { "value" };
        TIFFAttrInfo ainfoValue = new TIFFAttrInfo();
        TIFFAttrInfo ainfoDescription = new TIFFAttrInfo();

        for (int i = 0; i < types.length; i++) {
            if (!types[i].equals("TIFFUndefined")) {
                childNames = new String[1];
                childNames[0] = types[i];
                einfo =
                    new TIFFElementInfo(childNames, empty, CHILD_POLICY_SEQUENCE);
                elementInfoMap.put(types[i] + "s", einfo);
            }

            boolean hasDescription =
                !types[i].equals("TIFFUndefined") &&
                !types[i].equals("TIFFAscii") &&
                !types[i].equals("TIFFRational") &&
                !types[i].equals("TIFFSRational") &&
                !types[i].equals("TIFFFloat") &&
                !types[i].equals("TIFFDouble");

            String[] anames = hasDescription ? attrNames : attrNamesValueOnly;
            einfo = new TIFFElementInfo(empty, anames, CHILD_POLICY_EMPTY);
            elementInfoMap.put(types[i], einfo);

            attrInfoMap.put(types[i] + "/value", ainfoValue);
            if (hasDescription) {
                attrInfoMap.put(types[i] + "/description", ainfoDescription);
            }
        }

        childNames = new String[2*types.length - 1];
        for (int i = 0; i < types.length; i++) {
            childNames[2*i] = types[i];
            if (!types[i].equals("TIFFUndefined")) {
                childNames[2*i + 1] = types[i] + "s";
            }
        }
        attrNames = new String[] { "number", "name" };
        einfo = new TIFFElementInfo(childNames, attrNames, CHILD_POLICY_CHOICE);
        elementInfoMap.put("TIFFField", einfo);

        ainfo = new TIFFAttrInfo();
        ainfo.isRequired = true;
        attrInfoMap.put("TIFFField/number", ainfo);

        ainfo = new TIFFAttrInfo();
        attrInfoMap.put("TIFFField/name", ainfo);
    }

    public static synchronized IIOMetadataFormat getInstance() {
        if (theInstance == null) {
            theInstance = new TIFFImageMetadataFormat();
        }
        return theInstance;
    }
}
