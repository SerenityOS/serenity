/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.Autoscroll;

public class JListWithScroll<E> extends JList<E> implements Autoscroll {
    private Insets scrollInsets;

    public JListWithScroll(E[] listData) {
        super(listData);
        scrollInsets = new Insets(50, 50, 50, 50);
    }

    @Override
    public Insets getAutoscrollInsets() {
        return scrollInsets;
    }

    @Override
    public void autoscroll(Point cursorLoc) {
        JViewport viewport = getViewport();

        if (viewport == null) {
            return;
        }

        Point viewPos = viewport.getViewPosition();
        int viewHeight = viewport.getExtentSize().height;
        int viewWidth = viewport.getExtentSize().width;

        if ((cursorLoc.y - viewPos.y) < scrollInsets.top) {
            viewport.setViewPosition(new Point(viewPos.x, Math.max(viewPos.y - scrollInsets.top, 0)));
        } else if (((viewPos.y + viewHeight) - cursorLoc.y) < scrollInsets.bottom) {
            viewport.setViewPosition(
                    new Point(viewPos.x, Math.min(viewPos.y + scrollInsets.bottom, this.getHeight() - viewHeight))
            );
        } else if ((cursorLoc.x - viewPos.x) < scrollInsets.left) {
            viewport.setViewPosition(new Point(Math.max(viewPos.x - scrollInsets.left, 0), viewPos.y));
        } else if (((viewPos.x + viewWidth) - cursorLoc.x) < scrollInsets.right) {
            viewport.setViewPosition(
                    new Point(Math.min(viewPos.x + scrollInsets.right, this.getWidth() - viewWidth), viewPos.y)
            );
        }

    }

    public JViewport getViewport() {
        Component curComp = this;

        while (!(curComp instanceof JViewport) && (curComp != null)) {
            curComp = curComp.getParent();
        }
        if(curComp instanceof JViewport) {
            return (JViewport) curComp;
        } else {
            return null;
        }
    }
}
