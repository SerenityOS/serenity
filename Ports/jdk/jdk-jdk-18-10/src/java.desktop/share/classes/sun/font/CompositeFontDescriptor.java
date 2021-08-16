/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

/**
 * Encapsulates the information that 2D needs to create a composite font,
 * the runtime representation of a logical font.
 */
public class CompositeFontDescriptor {

    private String faceName;
    private int coreComponentCount;
    private String[] componentFaceNames;
    private String[] componentFileNames;
    private int[] exclusionRanges;
    private int[] exclusionRangeLimits;

    /**
     * Constructs a composite font descriptor.
     * @param faceName the font face name, i.e., the family name suffixed
     *                 with ".plain", ".bold", ".italic", ".bolditalic".
     * @param coreComponentCount the number of core fonts, i.e., the ones
     *                 derived from a non-fallback sequence.
     * @param componentFaceNames the face names for the component fonts
     * @param componentFileNames the file names for the component fonts
     * @param exclusionRanges an array holding lower and upper boundaries
     *                 for all exclusion ranges for all component fonts
     * @param exclusionRangeLimits an array holding the limits of the
     *                 sections for each component font within the previous
     *                 array
     */
    public CompositeFontDescriptor(String faceName,
            int coreComponentCount,
            String[] componentFaceNames,
            String[] componentFileNames,
            int[] exclusionRanges,
            int[] exclusionRangeLimits) {
        this.faceName = faceName;
        this.coreComponentCount = coreComponentCount;
        this.componentFaceNames = componentFaceNames;
        this.componentFileNames = componentFileNames;
        this.exclusionRanges = exclusionRanges;
        this.exclusionRangeLimits = exclusionRangeLimits;
    }

    public String getFaceName() {
        return faceName;
    }

    public int getCoreComponentCount() {
        return coreComponentCount;
    }

    public String[] getComponentFaceNames() {
        return componentFaceNames;
    }

    public String[] getComponentFileNames() {
        return componentFileNames;
    }

    public int[] getExclusionRanges() {
        return exclusionRanges;
    }

    public int[] getExclusionRangeLimits() {
        return exclusionRangeLimits;
    }
}
