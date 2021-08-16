/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;

public class CompactLayout implements LayoutManager {
    boolean horizontal;

    public CompactLayout(boolean horizontal) {
        this.horizontal = horizontal;
    }

    /**
     * If the layout manager uses a per-component string,
     * adds the component <code>comp</code> to the layout,
     * associating it
     * with the string specified by <code>name</code>.
     *
     * @param name the string to be associated with the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * @param comp the component to be removed
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Calculates the preferred size dimensions for the specified
     * container, given the components it contains.
     * @param parent the container to be laid out
     *
     * @see #minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container parent) {
        return getSize(parent, false);
    }

    /**
     * Calculates the minimum size dimensions for the specified
     * container, given the components it contains.
     * @param parent the component to be laid out
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container parent) {
        return getSize(parent, true);
    }

    public Dimension getSize(Container parent, boolean minimum) {
        int n = parent.getComponentCount();
        Insets insets = parent.getInsets();
        Dimension d = new Dimension();
        for (int i = 0; i < n; i++) {
            Component comp = parent.getComponent(i);
            if (comp instanceof EnableButton) {
                continue;
            }
            Dimension p = (minimum
                           ? comp.getMinimumSize()
                           : comp.getPreferredSize());
            if (horizontal) {
                d.width += p.width;
                if (d.height < p.height) {
                    d.height = p.height;
                }
            } else {
                if (d.width < p.width) {
                    d.width = p.width;
                }
                d.height += p.height;
            }
        }
        d.width += (insets.left + insets.right);
        d.height += (insets.top + insets.bottom);
        return d;
    }

    /**
     * Lays out the specified container.
     * @param parent the container to be laid out
     */
    public void layoutContainer(Container parent) {
        int n = parent.getComponentCount();
        Insets insets = parent.getInsets();
        Dimension size = parent.getSize();
        int c = horizontal ? insets.left : insets.top;
        int x, y;
        int ebx = size.width - insets.right;
        size.width -= (insets.left + insets.right);
        size.height -= (insets.top + insets.bottom);
        for (int i = 0; i < n; i++) {
            Component comp = parent.getComponent(i);
            Dimension pref = comp.getPreferredSize();
            if (comp instanceof EnableButton) {
                ebx -= 4;
                ebx -= pref.width;
                x = ebx;
                y = (insets.top - pref.height) / 2;
            } else if (horizontal) {
                x = c;
                c += pref.width;
                y = insets.top;
                pref.height = size.height;
            } else {
                x = insets.left;
                pref.width = size.width;
                y = c;
                c += pref.height;
            }
            comp.setBounds(x, y, pref.width, pref.height);
        }
    }
}
