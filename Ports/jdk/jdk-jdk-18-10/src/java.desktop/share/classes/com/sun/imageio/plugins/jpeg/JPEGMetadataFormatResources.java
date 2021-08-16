/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.jpeg;

import java.util.ListResourceBundle;

abstract class JPEGMetadataFormatResources
        extends ListResourceBundle {

    static final Object[][] commonContents = {
        // Node name, followed by description
        { "dqt", "A Define Quantization Table(s) marker segment" },
        { "dqtable", "A single quantization table" },
        { "dht", "A Define Huffman Table(s) marker segment" },
        { "dhtable", "A single Huffman table" },
        { "dri", "A Define Restart Interval marker segment" },
        { "com", "A Comment marker segment.  The user object contains "
          + "the actual bytes."},
        { "unknown", "An unrecognized marker segment.  The user object "
          + "contains the data not including length." },

        // Node name + "/" + AttributeName, followed by description
        { "dqtable/elementPrecision",
          "The number of bits in each table element (0 = 8, 1 = 16)" },
        { "dgtable/qtableId",
          "The table id" },
        { "dhtable/class",
          "Indicates whether this is a DC (0) or an AC (1) table" },
        { "dhtable/htableId",
          "The table id" },
        { "dri/interval",
          "The restart interval in MCUs" },
        { "com/comment",
          "The comment as a string (used only if user object is null)" },
        { "unknown/MarkerTag",
          "The tag identifying this marker segment" }
    };
}
