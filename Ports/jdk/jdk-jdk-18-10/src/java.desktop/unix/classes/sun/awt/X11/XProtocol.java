/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import sun.util.logging.PlatformLogger;

import java.util.*;

class XProtocol {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XProtocol");

    private Map<XAtom, XAtomList> atomToList = new HashMap<XAtom, XAtomList>();
    private Map<XAtom, Long> atomToAnchor = new HashMap<XAtom, Long>();

    volatile boolean firstCheck = true;
    /*
     * Check that that the list of protocols specified by WM in property
     * named LIST_NAME on the root window contains protocol PROTO.
     */
    boolean checkProtocol(XAtom listName, XAtom protocol) {
        XAtomList protocols = atomToList.get(listName);

        if (protocols != null) {
            return protocols.contains(protocol);
        }

        protocols = listName.getAtomListPropertyList(XToolkit.getDefaultRootWindow());
        atomToList.put(listName, protocols);
        try {
            return protocols.contains(protocol);
        } finally {
            if (firstCheck) {
                firstCheck = false;
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("{0}:{1} supports {2}", this, listName, protocols);
                }
            }
        }
    }

    /*
     * Check for anchor_prop(anchor_type) on root, take the value as the
     * window id and check if that window exists and has anchor_prop(anchor_type)
     * with the same value (i.e. pointing back to self).
     *
     * Returns the anchor window, as some WM may put interesting stuff in
     * its properties (e.g. sawfish).
     */
    long checkAnchorImpl(XAtom anchorProp, long anchorType) {
        long root_xref, self_xref;

        XToolkit.awtLock();
        try {
            root_xref = anchorProp.get32Property(XToolkit.getDefaultRootWindow(),
                                                 anchorType);
        } finally {
            XToolkit.awtUnlock();
        }
        if (root_xref == 0) {
            return 0;
        }
        self_xref = anchorProp.get32Property(root_xref, anchorType);
        if (self_xref != root_xref) {
            return 0;
        }
        return self_xref;
    }
    public long checkAnchor(XAtom anchorProp, long anchorType) {
        Long val = atomToAnchor.get(anchorProp);
        if (val != null) {
            return val.longValue();
        }
        long res = checkAnchorImpl(anchorProp, anchorType);
        atomToAnchor.put(anchorProp, res);
        return res;
    }
    public long checkAnchor(XAtom anchorProp, XAtom anchorType) {
        return checkAnchor(anchorProp, anchorType.getAtom());
    }

}
