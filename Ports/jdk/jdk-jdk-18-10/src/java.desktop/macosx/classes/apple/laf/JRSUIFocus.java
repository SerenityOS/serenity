/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package apple.laf;

public class JRSUIFocus {
    private static final int SUCCESS = 0;
    private static final int NULL_PTR = -1;
    private static final int NULL_CG_REF = -2;

    // from HITheme.h
    public static final int RING_ONLY = 0;
    public static final int RING_ABOVE = 1;
    public static final int RING_BELOW = 2;

    private static native int beginNativeFocus(final long cgContext, final int ringStyle);
    private static native int endNativeFocus(final long cgContext);

    final long cgContext;
    public JRSUIFocus(final long cgContext) {
        this.cgContext = cgContext;
    }

    public void beginFocus(final int ringStyle) {
        testForFailure(beginNativeFocus(cgContext, ringStyle));
    }

    public void endFocus() {
        testForFailure(endNativeFocus(cgContext));
    }

    static void testForFailure(final int status) {
        if (status == SUCCESS) return;

        switch(status) {
            case NULL_PTR: throw new RuntimeException("Null pointer exception in native JRSUI");
            case NULL_CG_REF: throw new RuntimeException("Null CG reference in native JRSUI");
            default: throw new RuntimeException("JRSUI draw focus problem: " + status);
        }
    }
}
