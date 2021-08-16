/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.swing;

import java.awt.Color;
import java.io.Serial;

import javax.swing.plaf.ColorUIResource;

/**
 * A subclass of ColorUIResource that wraps an alternate color
 * for use during printing. Useful to replace color values that
 * may look poor in printed output.
 *
 * @author Shannon Hickey
 *
 */
@SuppressWarnings("serial") // JDK-implementation class
public class PrintColorUIResource extends ColorUIResource {

    /** The color to use during printing */
    private Color printColor;

    /**
     * Construct an instance for the given RGB value and
     * alternate color to use during printing.
     *
     * @param rgb the color rgb value
     * @param printColor the alternate color for printing
     */
    public PrintColorUIResource(int rgb, Color printColor) {
        super(rgb);
        this.printColor = printColor;
    }

    /**
     * Return the color to use during printing. If no alternate
     * color was specified on construction, this method will
     * return <code>this</code>.
     *
     * @return the color to use during printing
     */
    public Color getPrintColor() {
        return ((printColor != null) ? printColor : this);
    }

    /**
     * Replaces this object with a plain {@code ColorUIResource} during
     * serialization. Since {@code PrintColorUIResource} resides in the
     * sun.swing package, access can be disallowed to it by a security
     * manager. When access is disallowed, deserialization of any object
     * with reference to a {@code PrintColorUIResource} fails.
     * <p>
     * Since {@code PrintColorUIResource} is used only by Swing's look
     * and feels, and we know that UI supplied colors are replaced after
     * deserialization when the UI is re-installed, the only important
     * aspect of the {@code PrintColorUIResource} that needs to be
     * persisted is the fact that it is a {@code ColorUIResource}. As
     * such, we can avoid the problem outlined above by replacing
     * the problematic {@code PrintColorUIResource} with a plain
     * {@code ColorUIResource}.
     * <p>
     * Note: As a result of this method, it is not possible to write
     * a {@code PrintColorUIResource} to a stream and then read
     * back a {@code PrintColorUIResource}. This is acceptable since we
     * don't have a requirement for that in Swing.
     */
    @Serial
    private Object writeReplace() {
        return new ColorUIResource(this);
    }
}
