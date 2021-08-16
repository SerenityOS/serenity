/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.standard.Media;

/*
 * An implementation class used by services which can distinguish media
 * by size and media by source. Values are expected to be MediaTray
 * instances, but this is not enforced by API.
 */
public class SunAlternateMedia implements PrintRequestAttribute {

    /**
     * Use serialVersionUID from JDK 1.5 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8878868345472850201L;

    private Media media;

    public SunAlternateMedia(Media altMedia) {
        media = altMedia;
    }

    public Media getMedia() {
        return media;
    }

    public final Class<? extends Attribute> getCategory() {
        return SunAlternateMedia.class;
    }

    public final String getName() {
        return "sun-alternate-media";
    }

    public String toString() {
       return "alternate-media: " + media.toString();
    }

    /**
     * Returns a hash code value for this enumeration value. The hash code is
     * just this enumeration value's integer value.
     */
    public int hashCode() {
        return media.hashCode();
    }
}
