/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.gtk;

import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.synth.*;
import com.sun.java.swing.plaf.gtk.GTKEngine.WidgetType;

/**
 *
 * @author Scott Violet
 */
class GTKStyleFactory extends SynthStyleFactory {

    /**
     * Saves all styles that have been accessed.  In most common cases,
     * the hash key is simply the WidgetType, but in more complex cases
     * it will be a ComplexKey object that contains arguments to help
     * differentiate similar styles.
     */
    private final Map<Object, GTKStyle> stylesCache;

    private Font defaultFont;

    GTKStyleFactory() {
        stylesCache = new HashMap<Object, GTKStyle>();
    }

    /**
     * Returns the <code>GTKStyle</code> to use based on the
     * <code>Region</code> id
     *
     * @param c this parameter isn't used, may be null.
     * @param id of the region to get the style.
     */
    public synchronized SynthStyle getStyle(JComponent c, Region id) {
        WidgetType wt = GTKEngine.getWidgetType(c, id);

        Object key = null;
        if (id == Region.SCROLL_BAR) {
            // The style/insets of a scrollbar can depend on a number of
            // factors (see GTKStyle.getScrollBarInsets()) so use a
            // complex key here.
            if (c != null) {
                JScrollBar sb = (JScrollBar)c;
                boolean sp = (sb.getParent() instanceof JScrollPane);
                boolean horiz = (sb.getOrientation() == JScrollBar.HORIZONTAL);
                boolean ltr = sb.getComponentOrientation().isLeftToRight();
                boolean focusable = sb.isFocusable();
                key = new ComplexKey(wt, sp, horiz, ltr, focusable);
            }
        }
        else if (id == Region.CHECK_BOX || id == Region.RADIO_BUTTON) {
            // The style/insets of a checkbox or radiobutton can depend
            // on the component orientation, so use a complex key here.
            if (c != null) {
                boolean ltr = c.getComponentOrientation().isLeftToRight();
                key = new ComplexKey(wt, ltr);
            }
        }
        else if (id == Region.BUTTON) {
            // The style/insets of a button can depend on whether it is
            // default capable or in a toolbar, so use a complex key here.
            if (c != null) {
                JButton btn = (JButton)c;
                boolean toolButton = (btn.getParent() instanceof JToolBar);
                boolean defaultCapable = btn.isDefaultCapable();
                key = new ComplexKey(wt, toolButton, defaultCapable);
            }
        } else if (id == Region.MENU) {
            if (c instanceof JMenu && ((JMenu) c).isTopLevelMenu() &&
                    UIManager.getBoolean("Menu.useMenuBarForTopLevelMenus")) {
                wt = WidgetType.MENU_BAR;
            }
        }

        if (key == null) {
            // Otherwise, just use the WidgetType as the key.
            key = wt;
        }

        GTKStyle result = stylesCache.get(key);
        if (result == null) {
            result = new GTKStyle(defaultFont, wt);
            stylesCache.put(key, result);
        }

        return result;
    }

    void initStyles(Font defaultFont) {
        this.defaultFont = defaultFont;
        stylesCache.clear();
    }

    /**
     * Represents a hash key used for fetching GTKStyle objects from the
     * cache.  In most cases only the WidgetType is used for lookup, but
     * in some complex cases, other Object arguments can be specified
     * via a ComplexKey to differentiate the various styles.
     */
    private static class ComplexKey {
        private final WidgetType wt;
        private final Object[] args;

        ComplexKey(WidgetType wt, Object... args) {
            this.wt = wt;
            this.args = args;
        }

        @Override
        public int hashCode() {
            int hash = wt.hashCode();
            if (args != null) {
                for (Object arg : args) {
                    hash = hash*29 + (arg == null ? 0 : arg.hashCode());
                }
            }
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComplexKey)) {
                return false;
            }
            ComplexKey that = (ComplexKey)o;
            if (this.wt == that.wt) {
                if (this.args == null && that.args == null) {
                    return true;
                }
                if (this.args != null && that.args != null &&
                    this.args.length == that.args.length)
                {
                    for (int i = 0; i < this.args.length; i++) {
                        Object a1 = this.args[i];
                        Object a2 = that.args[i];
                        if (!(a1==null ? a2==null : a1.equals(a2))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            String str = "ComplexKey[wt=" + wt;
            if (args != null) {
                str += ",args=[";
                for (int i = 0; i < args.length; i++) {
                    str += args[i];
                    if (i < args.length-1) str += ",";
                }
                str += "]";
            }
            str += "]";
            return str;
        }
    }
}
