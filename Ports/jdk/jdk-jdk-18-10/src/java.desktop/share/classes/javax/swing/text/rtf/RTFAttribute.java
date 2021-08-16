/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.rtf;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import java.io.IOException;

/**
 * This interface describes a class which defines a 1-1 mapping between
 * an RTF keyword and a SwingText attribute.
 */
interface RTFAttribute
{
    static final int D_CHARACTER = 0;
    static final int D_PARAGRAPH = 1;
    static final int D_SECTION = 2;
    static final int D_DOCUMENT = 3;
    static final int D_META = 4;

    /* These next three should really be public variables,
       but you can't declare public variables in an interface... */
    /* int domain; */
    public int domain();
    /* String swingName; */
    public Object swingName();
    /* String rtfName; */
    public String rtfName();

    public boolean set(MutableAttributeSet target);
    public boolean set(MutableAttributeSet target, int parameter);

    public boolean setDefault(MutableAttributeSet target);

    /* TODO: This method is poorly thought out */
    public boolean write(AttributeSet source,
                         RTFGenerator target,
                         boolean force)
        throws IOException;

    public boolean writeValue(Object value,
                              RTFGenerator target,
                              boolean force)
        throws IOException;
}
