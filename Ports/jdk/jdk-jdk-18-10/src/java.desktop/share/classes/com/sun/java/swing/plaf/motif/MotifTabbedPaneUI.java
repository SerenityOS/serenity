/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * A Motif {@literal L&F} implementation of TabbedPaneUI.
 *
 * @author Amy Fowler
 * @author Philip Milne
 */
public class MotifTabbedPaneUI extends BasicTabbedPaneUI
{

// Instance variables initialized at installation

    protected Color unselectedTabBackground;
    protected Color unselectedTabForeground;
    protected Color unselectedTabShadow;
    protected Color unselectedTabHighlight;


// UI creation

    public static ComponentUI createUI(JComponent tabbedPane) {
        return new MotifTabbedPaneUI();
    }


// UI Installation/De-installation


    protected void installDefaults() {
        super.installDefaults();

        unselectedTabBackground = UIManager.getColor("TabbedPane.unselectedTabBackground");
        unselectedTabForeground = UIManager.getColor("TabbedPane.unselectedTabForeground");
        unselectedTabShadow = UIManager.getColor("TabbedPane.unselectedTabShadow");
        unselectedTabHighlight = UIManager.getColor("TabbedPane.unselectedTabHighlight");
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();

        unselectedTabBackground = null;
        unselectedTabForeground = null;
        unselectedTabShadow = null;
        unselectedTabHighlight = null;
    }

// UI Rendering

   protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
                                            int selectedIndex,
                                            int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);
        g.setColor(lightHighlight);

        // Draw unbroken line if tabs are not on TOP, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != TOP || selectedIndex < 0 ||
            (selRect.x < x || selRect.x > x + w)) {
            g.drawLine(x, y, x+w-2, y);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x, y, selRect.x - 1, y);
            if (selRect.x + selRect.width < x + w - 2) {
                g.drawLine(selRect.x + selRect.width, y,
                           x+w-2, y);
            }
        }
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);
        g.setColor(shadow);

        // Draw unbroken line if tabs are not on BOTTOM, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != BOTTOM || selectedIndex < 0 ||
             (selRect.x < x || selRect.x > x + w)) {
            g.drawLine(x+1, y+h-1, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x+1, y+h-1, selRect.x - 1, y+h-1);
            if (selRect.x + selRect.width < x + w - 2) {
                g.drawLine(selRect.x + selRect.width, y+h-1, x+w-2, y+h-1);
            }
        }
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);
        g.setColor(shadow);
        // Draw unbroken line if tabs are not on RIGHT, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != RIGHT || selectedIndex < 0 ||
             (selRect.y < y || selRect.y > y + h)) {
            g.drawLine(x+w-1, y+1, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x+w-1, y+1, x+w-1, selRect.y - 1);
            if (selRect.y + selRect.height < y + h - 2 ) {
                g.drawLine(x+w-1, selRect.y + selRect.height,
                           x+w-1, y+h-2);
            }
        }
    }

    protected void paintTabBackground(Graphics g,
                                      int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h,
                                      boolean isSelected ) {
        g.setColor(isSelected? tabPane.getBackgroundAt(tabIndex) : unselectedTabBackground);
        switch(tabPlacement) {
          case LEFT:
              g.fillRect(x+1, y+1, w-1, h-2);
              break;
          case RIGHT:
              g.fillRect(x, y+1, w-1, h-2);
              break;
          case BOTTOM:
              g.fillRect(x+1, y, w-2, h-3);
              g.drawLine(x+2, y+h-3, x+w-3, y+h-3);
              g.drawLine(x+3, y+h-2, x+w-4, y+h-2);
              break;
          case TOP:
          default:
              g.fillRect(x+1, y+3, w-2, h-3);
              g.drawLine(x+2, y+2, x+w-3, y+2);
              g.drawLine(x+3, y+1, x+w-4, y+1);
        }

    }

    protected void paintTabBorder(Graphics g,
                                  int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h,
                                  boolean isSelected) {
        g.setColor(isSelected? lightHighlight : unselectedTabHighlight);

        switch(tabPlacement) {
          case LEFT:
              g.drawLine(x, y+2, x, y+h-3);
              g.drawLine(x+1, y+1, x+1, y+2);
              g.drawLine(x+2, y, x+2, y+1);
              g.drawLine(x+3, y, x+w-1, y);
              g.setColor(isSelected? shadow : unselectedTabShadow);
              g.drawLine(x+1, y+h-3, x+1, y+h-2);
              g.drawLine(x+2, y+h-2, x+2, y+h-1);
              g.drawLine(x+3, y+h-1, x+w-1, y+h-1);
              break;
          case RIGHT:
              g.drawLine(x, y, x+w-3, y);
              g.setColor(isSelected? shadow : unselectedTabShadow);
              g.drawLine(x+w-3, y, x+w-3, y+1);
              g.drawLine(x+w-2, y+1, x+w-2, y+2);
              g.drawLine(x+w-1, y+2, x+w-1, y+h-3);
              g.drawLine(x+w-2, y+h-3, x+w-2, y+h-2);
              g.drawLine(x+w-3, y+h-2, x+w-3, y+h-1);
              g.drawLine(x, y+h-1, x+w-3, y+h-1);
              break;
          case BOTTOM:
              g.drawLine(x, y, x, y+h-3);
              g.drawLine(x+1, y+h-3, x+1, y+h-2);
              g.drawLine(x+2, y+h-2, x+2, y+h-1);
              g.setColor(isSelected? shadow : unselectedTabShadow);
              g.drawLine(x+3, y+h-1, x+w-4, y+h-1);
              g.drawLine(x+w-3, y+h-2, x+w-3, y+h-1);
              g.drawLine(x+w-2, y+h-3, x+w-2, y+h-2);
              g.drawLine(x+w-1, y, x+w-1, y+h-3);
              break;
          case TOP:
          default:
              g.drawLine(x, y+2, x, y+h-1);
              g.drawLine(x+1, y+1, x+1, y+2);
              g.drawLine(x+2, y, x+2, y+1);
              g.drawLine(x+3, y, x+w-4, y);
              g.setColor(isSelected? shadow : unselectedTabShadow);
              g.drawLine(x+w-3, y, x+w-3, y+1);
              g.drawLine(x+w-2, y+1, x+w-2, y+2);
              g.drawLine(x+w-1, y+2, x+w-1, y+h-1);
        }

    }

    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        Rectangle tabRect = rects[tabIndex];
        if (tabPane.hasFocus() && isSelected) {
            int x, y, w, h;
            g.setColor(focus);
            switch(tabPlacement) {
              case LEFT:
                  x = tabRect.x + 3;
                  y = tabRect.y + 3;
                  w = tabRect.width - 6;
                  h = tabRect.height - 7;
                  break;
              case RIGHT:
                  x = tabRect.x + 2;
                  y = tabRect.y + 3;
                  w = tabRect.width - 6;
                  h = tabRect.height - 7;
                  break;
              case BOTTOM:
                  x = tabRect.x + 3;
                  y = tabRect.y + 2;
                  w = tabRect.width - 7;
                  h = tabRect.height - 6;
                  break;
              case TOP:
              default:
                  x = tabRect.x + 3;
                  y = tabRect.y + 3;
                  w = tabRect.width - 7;
                  h = tabRect.height - 6;
            }
            g.drawRect(x, y, w, h);
        }
    }

    protected int getTabRunIndent(int tabPlacement, int run) {
        return run*3;
    }

    protected int getTabRunOverlay(int tabPlacement) {
        tabRunOverlay = (tabPlacement == LEFT || tabPlacement == RIGHT)?
            (int)Math.round((float)maxTabWidth * .10) :
            (int)Math.round((float)maxTabHeight * .22);

        // Ensure that runover lay is not more than insets
        // 2 pixel offset is set from insets to each run
        switch(tabPlacement) {
        case LEFT:
                if( tabRunOverlay > tabInsets.right - 2 )
                    tabRunOverlay = tabInsets.right - 2 ;
                break;
        case RIGHT:
                if( tabRunOverlay > tabInsets.left - 2 )
                    tabRunOverlay = tabInsets.left - 2 ;
                break;
        case TOP:
                if( tabRunOverlay > tabInsets.bottom - 2 )
                    tabRunOverlay = tabInsets.bottom - 2 ;
                break;
        case BOTTOM:
                if( tabRunOverlay > tabInsets.top - 2 )
                    tabRunOverlay = tabInsets.top - 2 ;
                break;

        }

        return tabRunOverlay;
    }

}
