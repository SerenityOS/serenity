/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

class XLabelPeer extends XComponentPeer implements LabelPeer {
    /**
     * Create the label
     */

    static final int            TEXT_XPAD = 8;
    static final int            TEXT_YPAD = 6;
    String label;
    int alignment;

    FontMetrics cachedFontMetrics;
    Font oldfont;

    FontMetrics getFontMetrics()
    {
        if (cachedFontMetrics != null)
            return cachedFontMetrics;
        else return getFontMetrics(getPeerFont());

    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        Label target = (Label) this.target;
        label = target.getText();
        if (label == null) {
            label = "";
        }
        alignment = target.getAlignment();
    }

    XLabelPeer(Label target) {
        super(target);
    }

    /**
     * Minimum size.
     */
    public Dimension getMinimumSize() {
        FontMetrics fm = getFontMetrics();
        int w;
        try {
            w = fm.stringWidth(label);
        }
        catch (NullPointerException e) {
            w = 0;
        }
        return new Dimension(w + TEXT_XPAD,
                             fm.getAscent() + fm.getMaxDescent() + TEXT_YPAD);
    }


    /**
     * Paint the label
     */
    // NOTE: This method is called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    @Override
    void paintPeer(final Graphics g) {
        int textX = 0;
        int textY = 0;
        g.setColor(getPeerBackground());
        g.fillRect(0, 0, width, height);

        Font f = getPeerFont();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        if (cachedFontMetrics == null)
        {
            cachedFontMetrics = fm;
        }
        else
        {
            if (oldfont != f)
                cachedFontMetrics = fm;
        }

        switch (alignment) {
          case Label.LEFT:
              textX = 2;
              textY = (height + fm.getMaxAscent() - fm.getMaxDescent()) / 2;
              break;
          case Label.RIGHT:
              textX = width - (fm.stringWidth(label) + 2);
              textY = (height + fm.getMaxAscent() - fm.getMaxDescent()) / 2;
              break;
          case Label.CENTER:
              textX = (width - fm.stringWidth(label)) / 2;
              textY = (height + fm.getMaxAscent() - fm.getMaxDescent()) / 2;
              break;
        }
        if (isEnabled()) {
            g.setColor(getPeerForeground());
            g.drawString(label, textX, textY);
        }
        else {
            g.setColor(getPeerBackground().brighter());
            g.drawString(label, textX, textY);
            g.setColor(getPeerBackground().darker());
            g.drawString(label, textX - 1, textY - 1);
        }
    }

    @Override
    public void setText(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(this.label)) {
            this.label = label;
            repaint();
        }
    }

    @Override
    public void setAlignment(final int alignment) {
        if (this.alignment != alignment) {
            this.alignment = alignment;
            repaint();
        }
    }
}
