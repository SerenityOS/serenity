/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.peer.*;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.SunGraphicsCallback;

public class XPanelPeer extends XCanvasPeer implements PanelPeer {

    XEmbeddingContainer embedder = null; //new XEmbeddingContainer();
    /**
     * Embeds the given window into container using XEmbed protocol
     */
    public void xembed(long window) {
        if (embedder != null) {
            embedder.add(window);
        }
    }
    XPanelPeer() {}

    XPanelPeer(XCreateWindowParams params) {
        super(params);
    }

    XPanelPeer(Component target) {
        super(target);
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        if (embedder != null) {
            embedder.install(this);
        }
    }

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }
    public void paint(Graphics g) {
        super.paint(g);
        SunGraphicsCallback.PaintHeavyweightComponentsCallback.getInstance().
            runComponents(((Container)target).getComponents(), g,
                          SunGraphicsCallback.LIGHTWEIGHTS |
                          SunGraphicsCallback.HEAVYWEIGHTS);
    }
    public void print(Graphics g) {
        super.print(g);
        SunGraphicsCallback.PrintHeavyweightComponentsCallback.getInstance().
            runComponents(((Container)target).getComponents(), g,
                          SunGraphicsCallback.LIGHTWEIGHTS |
                          SunGraphicsCallback.HEAVYWEIGHTS);

    }

    public void setBackground(Color c) {
        Component comp;
        int i;

        Container cont = (Container) target;
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        synchronized(target.getTreeLock()) {
            int n = cont.getComponentCount();
            for(i=0; i < n; i++) {
                comp = cont.getComponent(i);
                ComponentPeer peer = acc.getPeer(comp);
                if (peer != null) {
                    Color color = comp.getBackground();
                    if (color == null || color.equals(c)) {
                        peer.setBackground(c);
                    }
                }
            }
        }
        super.setBackground(c);
    }

    public void setForeground(Color c) {
        setForegroundForHierarchy((Container) target, c);
    }

    private void setForegroundForHierarchy(Container cont, Color c) {
        synchronized(target.getTreeLock()) {
            final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
            int n = cont.getComponentCount();
            for(int i=0; i < n; i++) {
                Component comp = cont.getComponent(i);
                Color color = comp.getForeground();
                if (color == null || color.equals(c)) {
                    ComponentPeer cpeer = acc.getPeer(comp);
                    if (cpeer != null) {
                        cpeer.setForeground(c);
                    }
                    if (cpeer instanceof LightweightPeer
                        && comp instanceof Container)
                    {
                        setForegroundForHierarchy((Container) comp, c);
                    }
                }
            }
        }
    }

    public void dispose() {
        if (embedder != null) {
            embedder.deinstall();
        }
        super.dispose();
    }

    protected boolean shouldFocusOnClick() {
        // Return false if this container has children so in that case it won't
        // be focused. Return true otherwise.
        return ((Container)target).getComponentCount() == 0;
    }
}
