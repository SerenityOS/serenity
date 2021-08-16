/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.AlphaComposite;
import java.util.HashMap;

/**
 * A CompositeType object provides a chained description of a type of
 * algorithm for color compositing.  The object will provide a single
 * String constant descriptor which is one way of describing a particular
 * compositing algorithm as well as a pointer to another CompositeType
 * which describes a more general algorithm for achieving the same result.
 * <p>
 * A description of a more specific algorithm is considered a "subtype"
 * and a description of a more general algorithm is considered a "supertype".
 * Thus, the deriveSubType method provides a way to create a new CompositeType
 * that is related to but more specific than an existing CompositeType and
 * the getSuperType method provides a way to ask a given CompositeType
 * for a more general algorithm to achieve the same result.
 * <p>
 * Note that you cannot construct a brand new root for a chain since
 * the constructor is private.  Every chain of types must at some point
 * derive from the Any node provided here using the deriveSubType()
 * method.  The presence of this common Any node on every chain
 * ensures that all chains end with the DESC_ANY descriptor so that
 * a suitable General GraphicsPrimitive object can be obtained for
 * the indicated algorithm if all of the more specific searches fail.
 */
public final class CompositeType {

    private static int unusedUID = 1;
    private static final HashMap<String,Integer> compositeUIDMap =
        new HashMap<String,Integer>(100);

    /*
     * CONSTANTS USED BY ALL PRIMITIVES TO DESCRIBE THE COMPOSITING
     * ALGORITHMS THEY CAN PERFORM
     */

    /**
     * algorithm is a general algorithm that uses a CompositeContext
     * to do the rendering.
     */
    public static final String DESC_ANY      = "Any CompositeContext";

    /**
     * constant used to describe the Graphics.setXORMode() algorithm
     */
    public static final String DESC_XOR      = "XOR mode";

    /**
     * constants used to describe the various AlphaComposite
     * algorithms.
     */
    public static final String DESC_CLEAR     = "Porter-Duff Clear";
    public static final String DESC_SRC       = "Porter-Duff Src";
    public static final String DESC_DST       = "Porter-Duff Dst";
    public static final String DESC_SRC_OVER  = "Porter-Duff Src Over Dst";
    public static final String DESC_DST_OVER  = "Porter-Duff Dst Over Src";
    public static final String DESC_SRC_IN    = "Porter-Duff Src In Dst";
    public static final String DESC_DST_IN    = "Porter-Duff Dst In Src";
    public static final String DESC_SRC_OUT   = "Porter-Duff Src HeldOutBy Dst";
    public static final String DESC_DST_OUT   = "Porter-Duff Dst HeldOutBy Src";
    public static final String DESC_SRC_ATOP  = "Porter-Duff Src Atop Dst";
    public static final String DESC_DST_ATOP  = "Porter-Duff Dst Atop Src";
    public static final String DESC_ALPHA_XOR = "Porter-Duff Xor";

    /**
     * constants used to describe the two common cases of
     * AlphaComposite algorithms that are simpler if there
     * is not extraAlpha.
     */
    public static final String
        DESC_SRC_NO_EA      = "Porter-Duff Src, No Extra Alpha";
    public static final String
        DESC_SRC_OVER_NO_EA = "Porter-Duff SrcOverDst, No Extra Alpha";

    /**
     * constant used to describe an algorithm that implements all 8 of
     * the Porter-Duff rules in one Primitive.
     */
    public static final String DESC_ANY_ALPHA = "Any AlphaComposite Rule";

    /*
     * END OF COMPOSITE ALGORITHM TYPE CONSTANTS
     */

    /**
     * The root CompositeType object for all chains of algorithm descriptions.
     */
    public static final CompositeType
        Any           = new CompositeType(null, DESC_ANY);

    /*
     * START OF CompositeeType OBJECTS FOR THE VARIOUS CONSTANTS
     */

    public static final CompositeType
        General       = Any;

    public static final CompositeType
        AnyAlpha      = General.deriveSubType(DESC_ANY_ALPHA);
    public static final CompositeType
        Xor           = General.deriveSubType(DESC_XOR);

    public static final CompositeType
        Clear         = AnyAlpha.deriveSubType(DESC_CLEAR);
    public static final CompositeType
        Src           = AnyAlpha.deriveSubType(DESC_SRC);
    public static final CompositeType
        Dst           = AnyAlpha.deriveSubType(DESC_DST);
    public static final CompositeType
        SrcOver       = AnyAlpha.deriveSubType(DESC_SRC_OVER);
    public static final CompositeType
        DstOver       = AnyAlpha.deriveSubType(DESC_DST_OVER);
    public static final CompositeType
        SrcIn         = AnyAlpha.deriveSubType(DESC_SRC_IN);
    public static final CompositeType
        DstIn         = AnyAlpha.deriveSubType(DESC_DST_IN);
    public static final CompositeType
        SrcOut        = AnyAlpha.deriveSubType(DESC_SRC_OUT);
    public static final CompositeType
        DstOut        = AnyAlpha.deriveSubType(DESC_DST_OUT);
    public static final CompositeType
        SrcAtop       = AnyAlpha.deriveSubType(DESC_SRC_ATOP);
    public static final CompositeType
        DstAtop       = AnyAlpha.deriveSubType(DESC_DST_ATOP);
    public static final CompositeType
        AlphaXor      = AnyAlpha.deriveSubType(DESC_ALPHA_XOR);

    public static final CompositeType
        SrcNoEa       = Src.deriveSubType(DESC_SRC_NO_EA);
    public static final CompositeType
        SrcOverNoEa   = SrcOver.deriveSubType(DESC_SRC_OVER_NO_EA);

    /*
     * A special CompositeType for the case where we are filling in
     * SrcOverNoEa mode with an opaque color.  In that case then the
     * best loop for us to use would be a SrcNoEa loop, but what if
     * there is no such loop?  In that case then we would end up
     * backing off to a Src loop (which should still be fine) or an
     * AnyAlpha loop which would be slower than a SrcOver loop in
     * most cases.
     * The fix is to use the following chain which looks for loops
     * in the following order:
     *    SrcNoEa, Src, SrcOverNoEa, SrcOver, AnyAlpha
     */
    public static final CompositeType
        OpaqueSrcOverNoEa = SrcOverNoEa.deriveSubType(DESC_SRC)
                                       .deriveSubType(DESC_SRC_NO_EA);

    /*
     * END OF CompositeType OBJECTS FOR THE VARIOUS CONSTANTS
     */

    /**
     * Return a new CompositeType object which uses this object as its
     * more general "supertype" descriptor.  If no operation can be
     * found that implements the algorithm described more exactly
     * by desc, then this object will define the more general
     * compositing algorithm that can be used instead.
     */
    public CompositeType deriveSubType(String desc) {
        return new CompositeType(this, desc);
    }

    /**
     * Return a CompositeType object for the specified AlphaComposite
     * rule.
     */
    public static CompositeType forAlphaComposite(AlphaComposite ac) {
        switch (ac.getRule()) {
        case AlphaComposite.CLEAR:
            return Clear;
        case AlphaComposite.SRC:
            if (ac.getAlpha() >= 1.0f) {
                return SrcNoEa;
            } else {
                return Src;
            }
        case AlphaComposite.DST:
            return Dst;
        case AlphaComposite.SRC_OVER:
            if (ac.getAlpha() >= 1.0f) {
                return SrcOverNoEa;
            } else {
                return SrcOver;
            }
        case AlphaComposite.DST_OVER:
            return DstOver;
        case AlphaComposite.SRC_IN:
            return SrcIn;
        case AlphaComposite.DST_IN:
            return DstIn;
        case AlphaComposite.SRC_OUT:
            return SrcOut;
        case AlphaComposite.DST_OUT:
            return DstOut;
        case AlphaComposite.SRC_ATOP:
            return SrcAtop;
        case AlphaComposite.DST_ATOP:
            return DstAtop;
        case AlphaComposite.XOR:
            return AlphaXor;
        default:
            throw new InternalError("Unrecognized alpha rule");
        }
    }

    private int uniqueID;
    private String desc;
    private CompositeType next;

    private CompositeType(CompositeType parent, String desc) {
        next = parent;
        this.desc = desc;
        this.uniqueID = makeUniqueID(desc);
    }

    public static synchronized int makeUniqueID(String desc) {
        Integer i = compositeUIDMap.get(desc);

        if (i == null) {
            if (unusedUID > 255) {
                throw new InternalError("composite type id overflow");
            }
            i = unusedUID++;
            compositeUIDMap.put(desc, i);
        }
        return i;
    }

    public int getUniqueID() {
        return uniqueID;
    }

    public String getDescriptor() {
        return desc;
    }

    public CompositeType getSuperType() {
        return next;
    }

    public int hashCode() {
        return desc.hashCode();
    }

    public boolean isDerivedFrom(CompositeType other) {
        CompositeType comptype = this;
        do {
            if (comptype.desc == other.desc) {
                return true;
            }
            comptype = comptype.next;
        } while (comptype != null);
        return false;
    }

    public boolean equals(Object o) {
        if (o instanceof CompositeType) {
            return (((CompositeType) o).uniqueID == this.uniqueID);
        }
        return false;
    }

    public String toString() {
        return desc;
    }
}
