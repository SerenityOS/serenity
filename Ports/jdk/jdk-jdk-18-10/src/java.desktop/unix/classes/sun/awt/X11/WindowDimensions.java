/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

class WindowDimensions {
    private Point loc;
    private Dimension size;
    private Insets insets;
    private boolean isClientSizeSet;

    /**
     * If isClient is true, the bounds represent the client window area.
     * Otherwise, they represent the entire window area, with the insets included
     */
    public WindowDimensions(int x, int y, int width, int height, boolean isClient) {
        this(new Rectangle(x, y, width, height), null, isClient);
    }

    /**
     * If isClient is true, the bounds represent the client window area.
     * Otherwise, they represent the entire window area, with the insets included
     */
    public WindowDimensions(Rectangle rec, Insets ins, boolean isClient) {
        if (rec == null) {
            throw new IllegalArgumentException("Client bounds can't be null");
        }
        isClientSizeSet = isClient;
        this.loc = rec.getLocation();
        this.size = rec.getSize();
        setInsets(ins);
    }

    /**
     * If isClient is true, the bounds represent the client window area.
     * Otherwise, they represent the entire window area, with the insets included
     */
    public WindowDimensions(Point loc, Dimension size, Insets in, boolean isClient) {
        this(new Rectangle(loc, size), in, isClient);
    }

    /**
     * If isClient is true, the bounds represent the client window area.
     * Otherwise, they represent the entire window area, with the insets included
     */
    public WindowDimensions(Rectangle bounds, boolean isClient) {
        this(bounds, null, isClient);
    }

    public WindowDimensions(final WindowDimensions dims) {
        this.loc = new Point(dims.loc);
        this.size = new Dimension(dims.size);
        this.insets = (dims.insets != null)?((Insets)dims.insets.clone()):new Insets(0, 0, 0, 0);
        this.isClientSizeSet = dims.isClientSizeSet;
    }

    public Rectangle getClientRect() {
        if (isClientSizeSet) {
            return new Rectangle(loc, size);
        } else {
            // Calculate client bounds
            if (insets != null) {
                return new Rectangle(loc.x, loc.y,
                                     size.width-(insets.left+insets.right),
                                     size.height-(insets.top+insets.bottom));
            } else {
                return new Rectangle(loc, size);
            }
        }
    }

    public void setClientSize(Dimension size) {
        this.size = new Dimension(size);
        isClientSizeSet = true;
    }

    public void setClientSize(int width, int height) {
        size = new Dimension(width, height);
        isClientSizeSet = true;
    }

    public Dimension getClientSize() {
        return getClientRect().getSize();
    }

    public void setSize(int width, int height) {
        size = new Dimension(width, height);
        isClientSizeSet = false;
    }

    public Dimension getSize() {
        return getBounds().getSize();
    }

    public Insets getInsets() {
        return (Insets)insets.clone();
    }
    public Rectangle getBounds() {
        if (isClientSizeSet) {
            Rectangle res = new Rectangle(loc, size);
            res.width += (insets.left + insets.right);
            res.height += (insets.top + insets.bottom);
            return res;
        } else {
            return new Rectangle(loc, size);
        }
    }

    public Point getLocation() {
        return new Point(loc);
    }
    public void setLocation(int x, int y) {
        loc = new Point(x, y);
    }

    public Rectangle getScreenBounds() {
        Dimension size = getClientSize();
        Point location = getLocation(); // this is Java location
        location.x += insets.left;
        location.y += insets.top;
        return new Rectangle(location, size);
    }

    public void setInsets(Insets in) {
        insets = (in != null)?((Insets)in.clone()):new Insets(0, 0, 0, 0);
        if (!isClientSizeSet) {
            if (size.width < (insets.left+insets.right)) {
                size.width = (insets.left+insets.right);
            }
            if (size.height < (insets.top+insets.bottom)) {
                size.height = (insets.top+insets.bottom);
            }
        }
    }

    public boolean isClientSizeSet() {
        return isClientSizeSet;
    }

    public String toString() {
        return "[" + loc + ", " + size + "(" +(isClientSizeSet?"client":"bounds") + ")+" + insets + "]";
    }

    public boolean equals(Object o) {
        if (!(o instanceof WindowDimensions)) {
            return false;
        }
        WindowDimensions dims = (WindowDimensions)o;
        return ((dims.insets.equals(insets)))
            && (getClientRect().equals(dims.getClientRect()))
            && (getBounds().equals(dims.getBounds()));
    }

    public int hashCode() {
        return ((insets == null)? (0):(insets.hashCode())) ^ getClientRect().hashCode() ^ getBounds().hashCode();
    }
}
