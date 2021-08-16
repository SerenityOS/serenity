/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import java.io.Serializable;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * The Metal subclass of BasicTabbedPaneUI.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Tom Santos
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalTabbedPaneUI extends BasicTabbedPaneUI {

    /**
     * The minimum width of a pane.
     */
    protected int minTabWidth = 40;
    // Background color for unselected tabs that don't have an explicitly
    // set color.
    private Color unselectedBackground;

    /**
     * The color of tab's background.
     */
    protected Color tabAreaBackground;

    /**
     * The color of the selected pane.
     */
    protected Color selectColor;

    /**
     * The color of the highlight.
     */
    protected Color selectHighlight;
    private boolean tabsOpaque = true;

    // Whether or not we're using ocean. This is cached as it is used
    // extensively during painting.
    private boolean ocean;
    // Selected border color for ocean.
    private Color oceanSelectedBorderColor;

    /**
     * Constructs a {@code MetalTabbedPaneUI}.
     */
    public MetalTabbedPaneUI() {}

    /**
     * Constructs {@code MetalTabbedPaneUI}.
     *
     * @param x a component
     * @return an instance of {@code MetalTabbedPaneUI}
     */
    public static ComponentUI createUI( JComponent x ) {
        return new MetalTabbedPaneUI();
    }

    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            return super.createLayoutManager();
        }
        return new TabbedPaneLayout();
    }

    protected void installDefaults() {
        super.installDefaults();

        tabAreaBackground = UIManager.getColor("TabbedPane.tabAreaBackground");
        selectColor = UIManager.getColor("TabbedPane.selected");
        selectHighlight = UIManager.getColor("TabbedPane.selectHighlight");
        tabsOpaque = UIManager.getBoolean("TabbedPane.tabsOpaque");
        unselectedBackground = UIManager.getColor(
                                         "TabbedPane.unselectedBackground");
        ocean = MetalLookAndFeel.usingOcean();
        if (ocean) {
            oceanSelectedBorderColor = UIManager.getColor(
                         "TabbedPane.borderHightlightColor");
        }
    }


    protected void paintTabBorder( Graphics g, int tabPlacement,
                                   int tabIndex, int x, int y, int w, int h,
                                   boolean isSelected) {
        int bottom = y + (h-1);
        int right = x + (w-1);

        switch ( tabPlacement ) {
        case LEFT:
            paintLeftTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected);
            break;
        case BOTTOM:
            paintBottomTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected);
            break;
        case RIGHT:
            paintRightTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected);
            break;
        case TOP:
        default:
            paintTopTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected);
        }
    }


    /**
     * Paints the top tab border.
     *
     * @param tabIndex a tab index
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param btm bottom
     * @param rght right
     * @param isSelected a selection
     */
    protected void paintTopTabBorder( int tabIndex, Graphics g,
                                      int x, int y, int w, int h,
                                      int btm, int rght,
                                      boolean isSelected ) {
        int currentRun = getRunForTab( tabPane.getTabCount(), tabIndex );
        int lastIndex = lastTabInRun( tabPane.getTabCount(), currentRun );
        int firstIndex = tabRuns[ currentRun ];
        boolean leftToRight = MetalUtils.isLeftToRight(tabPane);
        int selectedIndex = tabPane.getSelectedIndex();
        int bottom = h - 1;
        int right = w - 1;

        //
        // Paint Gap
        //

        if (shouldFillGap( currentRun, tabIndex, x, y ) ) {
            g.translate( x, y );

            if ( leftToRight ) {
                g.setColor( getColorForGap( currentRun, x, y + 1 ) );
                g.fillRect( 1, 0, 5, 3 );
                g.fillRect( 1, 3, 2, 2 );
            } else {
                g.setColor( getColorForGap( currentRun, x + w - 1, y + 1 ) );
                g.fillRect( right - 5, 0, 5, 3 );
                g.fillRect( right - 2, 3, 2, 2 );
            }

            g.translate( -x, -y );
        }

        g.translate( x, y );

        //
        // Paint Border
        //

        if (ocean && isSelected) {
            g.setColor(oceanSelectedBorderColor);
        }
        else {
            g.setColor( darkShadow );
        }

        if ( leftToRight ) {

            // Paint slant
            g.drawLine( 1, 5, 6, 0 );

            // Paint top
            g.drawLine( 6, 0, right, 0 );

            // Paint right
            if ( tabIndex==lastIndex ) {
                // last tab in run
                g.drawLine( right, 1, right, bottom );
            }

            if (ocean && tabIndex - 1 == selectedIndex &&
                                currentRun == getRunForTab(
                                tabPane.getTabCount(), selectedIndex)) {
                g.setColor(oceanSelectedBorderColor);
            }

            // Paint left
            if ( tabIndex != tabRuns[ runCount - 1 ] ) {
                // not the first tab in the last run
                if (ocean && isSelected) {
                    g.drawLine(0, 6, 0, bottom);
                    g.setColor(darkShadow);
                    g.drawLine(0, 0, 0, 5);
                }
                else {
                    g.drawLine( 0, 0, 0, bottom );
                }
            } else {
                // the first tab in the last run
                g.drawLine( 0, 6, 0, bottom );
            }
        } else {

            // Paint slant
            g.drawLine( right - 1, 5, right - 6, 0 );

            // Paint top
            g.drawLine( right - 6, 0, 0, 0 );

            // Paint left
            if ( tabIndex==lastIndex ) {
                // last tab in run
                g.drawLine( 0, 1, 0, bottom );
            }

            // Paint right
            if (ocean && tabIndex - 1 == selectedIndex &&
                                currentRun == getRunForTab(
                                tabPane.getTabCount(), selectedIndex)) {
                g.setColor(oceanSelectedBorderColor);
                g.drawLine(right, 0, right, bottom);
            }
            else if (ocean && isSelected) {
                g.drawLine(right, 6, right, bottom);
                if (tabIndex != 0) {
                    g.setColor(darkShadow);
                    g.drawLine(right, 0, right, 5);
                }
            }
            else {
                if ( tabIndex != tabRuns[ runCount - 1 ] ) {
                    // not the first tab in the last run
                    g.drawLine( right, 0, right, bottom );
                } else {
                    // the first tab in the last run
                    g.drawLine( right, 6, right, bottom );
                }
            }
        }

        //
        // Paint Highlight
        //

        g.setColor( isSelected ? selectHighlight : highlight );

        if ( leftToRight ) {

            // Paint slant
            g.drawLine( 1, 6, 6, 1 );

            // Paint top
            g.drawLine( 6, 1, (tabIndex == lastIndex) ? right - 1 : right, 1 );

            // Paint left
            g.drawLine( 1, 6, 1, bottom );

            // paint highlight in the gap on tab behind this one
            // on the left end (where they all line up)
            if ( tabIndex==firstIndex && tabIndex!=tabRuns[runCount - 1] ) {
                //  first tab in run but not first tab in last run
                if (tabPane.getSelectedIndex()==tabRuns[currentRun+1]) {
                    // tab in front of selected tab
                    g.setColor( selectHighlight );
                }
                else {
                    // tab in front of normal tab
                    g.setColor( highlight );
                }
                g.drawLine( 1, 0, 1, 4 );
            }
        } else {

            // Paint slant
            g.drawLine( right - 1, 6, right - 6, 1 );

            // Paint top
            g.drawLine( right - 6, 1, 1, 1 );

            // Paint left
            if ( tabIndex==lastIndex ) {
                // last tab in run
                g.drawLine( 1, 1, 1, bottom );
            } else {
                g.drawLine( 0, 1, 0, bottom );
            }
        }

        g.translate( -x, -y );
    }

    /**
     * Returns {@code true} if the gap should be filled.
     *
     * @param currentRun the current run
     * @param tabIndex the tab index
     * @param x an X coordinate
     * @param y an Y coordinate
     * @return {@code true} if the gap should be filled
     */
    protected boolean shouldFillGap( int currentRun, int tabIndex, int x, int y ) {
        boolean result = false;

        if (!tabsOpaque) {
            return false;
        }

        if ( currentRun == runCount - 2 ) {  // If it's the second to last row.
            Rectangle lastTabBounds = getTabBounds( tabPane, tabPane.getTabCount() - 1 );
            Rectangle tabBounds = getTabBounds( tabPane, tabIndex );
            if (MetalUtils.isLeftToRight(tabPane)) {
                int lastTabRight = lastTabBounds.x + lastTabBounds.width - 1;

                // is the right edge of the last tab to the right
                // of the left edge of the current tab?
                if ( lastTabRight > tabBounds.x + 2 ) {
                    return true;
                }
            } else {
                int lastTabLeft = lastTabBounds.x;
                int currentTabRight = tabBounds.x + tabBounds.width - 1;

                // is the left edge of the last tab to the left
                // of the right edge of the current tab?
                if ( lastTabLeft < currentTabRight - 2 ) {
                    return true;
                }
            }
        } else {
            // fill in gap for all other rows except last row
            result = currentRun != runCount - 1;
        }

        return result;
    }

    /**
     * Returns the color of the gap.
     *
     * @param currentRun the current run
     * @param x an X coordinate
     * @param y an Y coordinate
     * @return the color of the gap
     */
    protected Color getColorForGap( int currentRun, int x, int y ) {
        final int shadowWidth = 4;
        int selectedIndex = tabPane.getSelectedIndex();
        int startIndex = tabRuns[ currentRun + 1 ];
        int endIndex = lastTabInRun( tabPane.getTabCount(), currentRun + 1 );
        int tabOverGap = -1;
        // Check each tab in the row that is 'on top' of this row
        for ( int i = startIndex; i <= endIndex; ++i ) {
            Rectangle tabBounds = getTabBounds( tabPane, i );
            int tabLeft = tabBounds.x;
            int tabRight = (tabBounds.x + tabBounds.width) - 1;
            // Check to see if this tab is over the gap
            if ( MetalUtils.isLeftToRight(tabPane) ) {
                if ( tabLeft <= x && tabRight - shadowWidth > x ) {
                    return selectedIndex == i ? selectColor : getUnselectedBackgroundAt( i );
                }
            }
            else {
                if ( tabLeft + shadowWidth < x && tabRight >= x ) {
                    return selectedIndex == i ? selectColor : getUnselectedBackgroundAt( i );
                }
            }
        }

        return tabPane.getBackground();
    }

    /**
     * Paints the left tab border.
     *
     * @param tabIndex a tab index
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param btm bottom
     * @param rght right
     * @param isSelected a selection
     */
    protected void paintLeftTabBorder( int tabIndex, Graphics g,
                                       int x, int y, int w, int h,
                                       int btm, int rght,
                                       boolean isSelected ) {
        int tabCount = tabPane.getTabCount();
        int currentRun = getRunForTab( tabCount, tabIndex );
        int lastIndex = lastTabInRun( tabCount, currentRun );
        int firstIndex = tabRuns[ currentRun ];

        g.translate( x, y );

        int bottom = h - 1;
        int right = w - 1;

        //
        // Paint part of the tab above
        //

        if ( tabIndex != firstIndex && tabsOpaque ) {
            g.setColor( tabPane.getSelectedIndex() == tabIndex - 1 ?
                        selectColor :
                        getUnselectedBackgroundAt( tabIndex - 1 ) );
            g.fillRect( 2, 0, 4, 3 );
            g.drawLine( 2, 3, 2, 3 );
        }


        //
        // Paint Highlight
        //

        if (ocean) {
            g.setColor(isSelected ? selectHighlight :
                       MetalLookAndFeel.getWhite());
        }
        else {
            g.setColor( isSelected ? selectHighlight : highlight );
        }

        // Paint slant
        g.drawLine( 1, 6, 6, 1 );

        // Paint left
        g.drawLine( 1, 6, 1, bottom );

        // Paint top
        g.drawLine( 6, 1, right, 1 );

        if ( tabIndex != firstIndex ) {
            if (tabPane.getSelectedIndex() == tabIndex - 1) {
                g.setColor(selectHighlight);
            } else {
                g.setColor(ocean ? MetalLookAndFeel.getWhite() : highlight);
            }

            g.drawLine( 1, 0, 1, 4 );
        }

        //
        // Paint Border
        //

        if (ocean) {
            if (isSelected) {
                g.setColor(oceanSelectedBorderColor);
            }
            else {
                g.setColor( darkShadow );
            }
        }
        else {
            g.setColor( darkShadow );
        }

        // Paint slant
        g.drawLine( 1, 5, 6, 0 );

        // Paint top
        g.drawLine( 6, 0, right, 0 );

        // Paint bottom
        if ( tabIndex == lastIndex ) {
            g.drawLine( 0, bottom, right, bottom );
        }

        // Paint left
        if (ocean) {
            if (tabPane.getSelectedIndex() == tabIndex - 1) {
                g.drawLine(0, 5, 0, bottom);
                g.setColor(oceanSelectedBorderColor);
                g.drawLine(0, 0, 0, 5);
            }
            else if (isSelected) {
                g.drawLine( 0, 6, 0, bottom );
                if (tabIndex != 0) {
                    g.setColor(darkShadow);
                    g.drawLine(0, 0, 0, 5);
                }
            }
            else if ( tabIndex != firstIndex ) {
                g.drawLine( 0, 0, 0, bottom );
            } else {
                g.drawLine( 0, 6, 0, bottom );
            }
        }
        else { // metal
            if ( tabIndex != firstIndex ) {
                g.drawLine( 0, 0, 0, bottom );
            } else {
                g.drawLine( 0, 6, 0, bottom );
            }
        }

        g.translate( -x, -y );
    }


    /**
     * Paints the bottom tab border.
     *
     * @param tabIndex a tab index
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param btm bottom
     * @param rght right
     * @param isSelected a selection
     */
    protected void paintBottomTabBorder( int tabIndex, Graphics g,
                                         int x, int y, int w, int h,
                                         int btm, int rght,
                                         boolean isSelected ) {
        int tabCount = tabPane.getTabCount();
        int currentRun = getRunForTab( tabCount, tabIndex );
        int lastIndex = lastTabInRun( tabCount, currentRun );
        int firstIndex = tabRuns[ currentRun ];
        boolean leftToRight = MetalUtils.isLeftToRight(tabPane);

        int bottom = h - 1;
        int right = w - 1;

        //
        // Paint Gap
        //

        if ( shouldFillGap( currentRun, tabIndex, x, y ) ) {
            g.translate( x, y );

            if ( leftToRight ) {
                g.setColor( getColorForGap( currentRun, x, y ) );
                g.fillRect( 1, bottom - 4, 3, 5 );
                g.fillRect( 4, bottom - 1, 2, 2 );
            } else {
                g.setColor( getColorForGap( currentRun, x + w - 1, y ) );
                g.fillRect( right - 3, bottom - 3, 3, 4 );
                g.fillRect( right - 5, bottom - 1, 2, 2 );
                g.drawLine( right - 1, bottom - 4, right - 1, bottom - 4 );
            }

            g.translate( -x, -y );
        }

        g.translate( x, y );


        //
        // Paint Border
        //

        if (ocean && isSelected) {
            g.setColor(oceanSelectedBorderColor);
        }
        else {
            g.setColor( darkShadow );
        }

        if ( leftToRight ) {

            // Paint slant
            g.drawLine( 1, bottom - 5, 6, bottom );

            // Paint bottom
            g.drawLine( 6, bottom, right, bottom );

            // Paint right
            if ( tabIndex == lastIndex ) {
                g.drawLine( right, 0, right, bottom );
            }

            // Paint left
            if (ocean && isSelected) {
                g.drawLine(0, 0, 0, bottom - 6);
                if ((currentRun == 0 && tabIndex != 0) ||
                    (currentRun > 0 && tabIndex != tabRuns[currentRun - 1])) {
                    g.setColor(darkShadow);
                    g.drawLine(0, bottom - 5, 0, bottom);
                }
            }
            else {
                if (ocean && tabIndex == tabPane.getSelectedIndex() + 1) {
                    g.setColor(oceanSelectedBorderColor);
                }
                if ( tabIndex != tabRuns[ runCount - 1 ] ) {
                    g.drawLine( 0, 0, 0, bottom );
                } else {
                    g.drawLine( 0, 0, 0, bottom - 6 );
                }
            }
        } else {

            // Paint slant
            g.drawLine( right - 1, bottom - 5, right - 6, bottom );

            // Paint bottom
            g.drawLine( right - 6, bottom, 0, bottom );

            // Paint left
            if ( tabIndex==lastIndex ) {
                // last tab in run
                g.drawLine( 0, 0, 0, bottom );
            }

            // Paint right
            if (ocean && tabIndex == tabPane.getSelectedIndex() + 1) {
                g.setColor(oceanSelectedBorderColor);
                g.drawLine(right, 0, right, bottom);
            }
            else if (ocean && isSelected) {
                g.drawLine(right, 0, right, bottom - 6);
                if (tabIndex != firstIndex) {
                    g.setColor(darkShadow);
                    g.drawLine(right, bottom - 5, right, bottom);
                }
            }
            else if ( tabIndex != tabRuns[ runCount - 1 ] ) {
                // not the first tab in the last run
                g.drawLine( right, 0, right, bottom );
            } else {
                // the first tab in the last run
                g.drawLine( right, 0, right, bottom - 6 );
            }
        }

        //
        // Paint Highlight
        //

        g.setColor( isSelected ? selectHighlight : highlight );

        if ( leftToRight ) {

            // Paint slant
            g.drawLine( 1, bottom - 6, 6, bottom - 1 );

            // Paint left
            g.drawLine( 1, 0, 1, bottom - 6 );

            // paint highlight in the gap on tab behind this one
            // on the left end (where they all line up)
            if ( tabIndex==firstIndex && tabIndex!=tabRuns[runCount - 1] ) {
                //  first tab in run but not first tab in last run
                if (tabPane.getSelectedIndex()==tabRuns[currentRun+1]) {
                    // tab in front of selected tab
                    g.setColor( selectHighlight );
                }
                else {
                    // tab in front of normal tab
                    g.setColor( highlight );
                }
                g.drawLine( 1, bottom - 4, 1, bottom );
            }
        } else {

            // Paint left
            if ( tabIndex==lastIndex ) {
                // last tab in run
                g.drawLine( 1, 0, 1, bottom - 1 );
            } else {
                g.drawLine( 0, 0, 0, bottom - 1 );
            }
        }

        g.translate( -x, -y );
    }

    /**
     * Paints the right tab border.
     *
     * @param tabIndex a tab index
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param btm bottom
     * @param rght right
     * @param isSelected a selection
     */
    protected void paintRightTabBorder( int tabIndex, Graphics g,
                                        int x, int y, int w, int h,
                                        int btm, int rght,
                                        boolean isSelected ) {
        int tabCount = tabPane.getTabCount();
        int currentRun = getRunForTab( tabCount, tabIndex );
        int lastIndex = lastTabInRun( tabCount, currentRun );
        int firstIndex = tabRuns[ currentRun ];

        g.translate( x, y );

        int bottom = h - 1;
        int right = w - 1;

        //
        // Paint part of the tab above
        //

        if ( tabIndex != firstIndex && tabsOpaque ) {
            g.setColor( tabPane.getSelectedIndex() == tabIndex - 1 ?
                        selectColor :
                        getUnselectedBackgroundAt( tabIndex - 1 ) );
            g.fillRect( right - 5, 0, 5, 3 );
            g.fillRect( right - 2, 3, 2, 2 );
        }


        //
        // Paint Highlight
        //

        g.setColor( isSelected ? selectHighlight : highlight );

        // Paint slant
        g.drawLine( right - 6, 1, right - 1, 6 );

        // Paint top
        g.drawLine( 0, 1, right - 6, 1 );

        // Paint left
        if ( !isSelected ) {
            g.drawLine( 0, 1, 0, bottom );
        }


        //
        // Paint Border
        //

        if (ocean && isSelected) {
            g.setColor(oceanSelectedBorderColor);
        }
        else {
            g.setColor( darkShadow );
        }

        // Paint bottom
        if ( tabIndex == lastIndex ) {
            g.drawLine( 0, bottom, right, bottom );
        }

        // Paint slant
        if (ocean && tabPane.getSelectedIndex() == tabIndex - 1) {
            g.setColor(oceanSelectedBorderColor);
        }
        g.drawLine( right - 6, 0, right, 6 );

        // Paint top
        g.drawLine( 0, 0, right - 6, 0 );

        // Paint right
        if (ocean && isSelected) {
            g.drawLine(right, 6, right, bottom);
            if (tabIndex != firstIndex) {
                g.setColor(darkShadow);
                g.drawLine(right, 0, right, 5);
            }
        }
        else if (ocean && tabPane.getSelectedIndex() == tabIndex - 1) {
            g.setColor(oceanSelectedBorderColor);
            g.drawLine(right, 0, right, 6);
            g.setColor(darkShadow);
            g.drawLine(right, 6, right, bottom);
        }
        else if ( tabIndex != firstIndex ) {
            g.drawLine( right, 0, right, bottom );
        } else {
            g.drawLine( right, 6, right, bottom );
        }

        g.translate( -x, -y );
    }

    public void update( Graphics g, JComponent c ) {
        if ( c.isOpaque() ) {
            g.setColor( tabAreaBackground );
            g.fillRect( 0, 0, c.getWidth(),c.getHeight() );
        }
        paint( g, c );
    }

    protected void paintTabBackground( Graphics g, int tabPlacement,
                                       int tabIndex, int x, int y, int w, int h, boolean isSelected ) {
        int slantWidth = h / 2;
        if ( isSelected ) {
            g.setColor( selectColor );
        } else {
            g.setColor( getUnselectedBackgroundAt( tabIndex ) );
        }

        if (MetalUtils.isLeftToRight(tabPane)) {
            switch ( tabPlacement ) {
                case LEFT:
                    g.fillRect( x + 5, y + 1, w - 5, h - 1);
                    g.fillRect( x + 2, y + 4, 3, h - 4 );
                    break;
                case BOTTOM:
                    g.fillRect( x + 2, y, w - 2, h - 4 );
                    g.fillRect( x + 5, y + (h - 1) - 3, w - 5, 3 );
                    break;
                case RIGHT:
                    g.fillRect( x, y + 2, w - 4, h - 2);
                    g.fillRect( x + (w - 1) - 3, y + 5, 3, h - 5 );
                    break;
                case TOP:
                default:
                    g.fillRect( x + 4, y + 2, (w - 1) - 3, (h - 1) - 1 );
                    g.fillRect( x + 2, y + 5, 2, h - 5 );
            }
        } else {
            switch ( tabPlacement ) {
                case LEFT:
                    g.fillRect( x + 5, y + 1, w - 5, h - 1);
                    g.fillRect( x + 2, y + 4, 3, h - 4 );
                    break;
                case BOTTOM:
                    g.fillRect( x, y, w - 5, h - 1 );
                    g.fillRect( x + (w - 1) - 4, y, 4, h - 5);
                    g.fillRect( x + (w - 1) - 4, y + (h - 1) - 4, 2, 2);
                    break;
                case RIGHT:
                    g.fillRect( x + 1, y + 1, w - 5, h - 1);
                    g.fillRect( x + (w - 1) - 3, y + 5, 3, h - 5 );
                    break;
                case TOP:
                default:
                    g.fillRect( x, y + 2, (w - 1) - 3, (h - 1) - 1 );
                    g.fillRect( x + (w - 1) - 3, y + 5, 3, h - 3 );
            }
        }
    }

    /**
     * Overridden to do nothing for the Java L&amp;F.
     */
    protected int getTabLabelShiftX( int tabPlacement, int tabIndex, boolean isSelected ) {
        return 0;
    }


    /**
     * Overridden to do nothing for the Java L&amp;F.
     */
    protected int getTabLabelShiftY( int tabPlacement, int tabIndex, boolean isSelected ) {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.6
     */
    protected int getBaselineOffset() {
        return 0;
    }

    public void paint( Graphics g, JComponent c ) {
        int tabPlacement = tabPane.getTabPlacement();

        Insets insets = c.getInsets(); Dimension size = c.getSize();

        // Paint the background for the tab area
        if ( tabPane.isOpaque() ) {
            Color background = c.getBackground();
            if (background instanceof UIResource && tabAreaBackground != null) {
                g.setColor(tabAreaBackground);
            }
            else {
                g.setColor(background);
            }
            switch ( tabPlacement ) {
            case LEFT:
                g.fillRect( insets.left, insets.top,
                            calculateTabAreaWidth( tabPlacement, runCount, maxTabWidth ),
                            size.height - insets.bottom - insets.top );
                break;
            case BOTTOM:
                int totalTabHeight = calculateTabAreaHeight( tabPlacement, runCount, maxTabHeight );
                g.fillRect( insets.left, size.height - insets.bottom - totalTabHeight,
                            size.width - insets.left - insets.right,
                            totalTabHeight );
                break;
            case RIGHT:
                int totalTabWidth = calculateTabAreaWidth( tabPlacement, runCount, maxTabWidth );
                g.fillRect( size.width - insets.right - totalTabWidth,
                            insets.top, totalTabWidth,
                            size.height - insets.top - insets.bottom );
                break;
            case TOP:
            default:
                g.fillRect( insets.left, insets.top,
                            size.width - insets.right - insets.left,
                            calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) );
                paintHighlightBelowTab();
            }
        }

        super.paint( g, c );
    }

    /**
     * Paints highlights below tab.
     */
    protected void paintHighlightBelowTab( ) {

    }


    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        if ( tabPane.hasFocus() && isSelected ) {
            Rectangle tabRect = rects[tabIndex];
            boolean lastInRun = isLastInRun( tabIndex );
            g.setColor( focus );
            g.translate( tabRect.x, tabRect.y );
            int right = tabRect.width - 1;
            int bottom = tabRect.height - 1;
            boolean leftToRight = MetalUtils.isLeftToRight(tabPane);
            switch ( tabPlacement ) {
            case RIGHT:
                g.drawLine( right - 6,2 , right - 2,6 );         // slant
                g.drawLine( 1,2 , right - 6,2 );                 // top
                g.drawLine( right - 2,6 , right - 2,bottom );    // right
                g.drawLine( 1,2 , 1,bottom );                    // left
                g.drawLine( 1,bottom , right - 2,bottom );       // bottom
                break;
            case BOTTOM:
                if ( leftToRight ) {
                    g.drawLine( 2, bottom - 6, 6, bottom - 2 );   // slant
                    g.drawLine( 6, bottom - 2,
                                right, bottom - 2 );              // bottom
                    g.drawLine( 2, 0, 2, bottom - 6 );            // left
                    g.drawLine( 2, 0, right, 0 );                 // top
                    g.drawLine( right, 0, right, bottom - 2 );    // right
                } else {
                    g.drawLine( right - 2, bottom - 6,
                                right - 6, bottom - 2 );          // slant
                    g.drawLine( right - 2, 0,
                                right - 2, bottom - 6 );          // right
                    if ( lastInRun ) {
                        // last tab in run
                        g.drawLine( 2, bottom - 2,
                                    right - 6, bottom - 2 );      // bottom
                        g.drawLine( 2, 0, right - 2, 0 );         // top
                        g.drawLine( 2, 0, 2, bottom - 2 );        // left
                    } else {
                        g.drawLine( 1, bottom - 2,
                                    right - 6, bottom - 2 );      // bottom
                        g.drawLine( 1, 0, right - 2, 0 );         // top
                        g.drawLine( 1, 0, 1, bottom - 2 );        // left
                    }
                }
                break;
            case LEFT:
                g.drawLine( 2, 6, 6, 2 );                         // slant
                g.drawLine( 2, 6, 2, bottom - 1);                 // left
                g.drawLine( 6, 2, right, 2 );                     // top
                g.drawLine( right, 2, right, bottom - 1 );        // right
                g.drawLine( 2, bottom - 1,
                            right, bottom - 1 );                  // bottom
                break;
            case TOP:
             default:
                    if ( leftToRight ) {
                        g.drawLine( 2, 6, 6, 2 );                     // slant
                        g.drawLine( 2, 6, 2, bottom - 1);             // left
                        g.drawLine( 6, 2, right, 2 );                 // top
                        g.drawLine( right, 2, right, bottom - 1 );    // right
                        g.drawLine( 2, bottom - 1,
                                    right, bottom - 1 );              // bottom
                    }
                    else {
                        g.drawLine( right - 2, 6, right - 6, 2 );     // slant
                        g.drawLine( right - 2, 6,
                                    right - 2, bottom - 1);           // right
                        if ( lastInRun ) {
                            // last tab in run
                            g.drawLine( right - 6, 2, 2, 2 );         // top
                            g.drawLine( 2, 2, 2, bottom - 1 );        // left
                            g.drawLine( right - 2, bottom - 1,
                                        2, bottom - 1 );              // bottom
                        }
                        else {
                            g.drawLine( right - 6, 2, 1, 2 );         // top
                            g.drawLine( 1, 2, 1, bottom - 1 );        // left
                            g.drawLine( right - 2, bottom - 1,
                                        1, bottom - 1 );              // bottom
                        }
                    }
            }
            g.translate( -tabRect.x, -tabRect.y );
        }
    }

    protected void paintContentBorderTopEdge( Graphics g, int tabPlacement,
                                              int selectedIndex,
                                              int x, int y, int w, int h ) {
        boolean leftToRight = MetalUtils.isLeftToRight(tabPane);
        int right = x + w - 1;
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);
        if (ocean) {
            g.setColor(oceanSelectedBorderColor);
        }
        else {
            g.setColor(selectHighlight);
        }

        // Draw unbroken line if tabs are not on TOP, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
         if (tabPlacement != TOP || selectedIndex < 0 ||
            (selRect.y + selRect.height + 1 < y) ||
            (selRect.x < x || selRect.x > x + w)) {
            g.drawLine(x, y, x+w-2, y);
            if (ocean && tabPlacement == TOP) {
                g.setColor(MetalLookAndFeel.getWhite());
                g.drawLine(x, y + 1, x+w-2, y + 1);
            }
        } else {
            // Break line to show visual connection to selected tab
            boolean lastInRun = isLastInRun(selectedIndex);

            if ( leftToRight || lastInRun ) {
                g.drawLine(x, y, selRect.x + 1, y);
            } else {
                g.drawLine(x, y, selRect.x, y);
            }

            if (selRect.x + selRect.width < right - 1) {
                if ( leftToRight && !lastInRun ) {
                    g.drawLine(selRect.x + selRect.width, y, right - 1, y);
                } else {
                    g.drawLine(selRect.x + selRect.width - 1, y, right - 1, y);
                }
            } else {
                g.setColor(shadow);
                g.drawLine(x+w-2, y, x+w-2, y);
            }

            if (ocean) {
                g.setColor(MetalLookAndFeel.getWhite());

                if ( leftToRight || lastInRun ) {
                    g.drawLine(x, y + 1, selRect.x + 1, y + 1);
                } else {
                    g.drawLine(x, y + 1, selRect.x, y + 1);
                }

                if (selRect.x + selRect.width < right - 1) {
                    if ( leftToRight && !lastInRun ) {
                        g.drawLine(selRect.x + selRect.width, y + 1,
                                   right - 1, y + 1);
                    } else {
                        g.drawLine(selRect.x + selRect.width - 1, y + 1,
                                   right - 1, y + 1);
                    }
                } else {
                    g.setColor(shadow);
                    g.drawLine(x+w-2, y + 1, x+w-2, y + 1);
                }
            }
        }
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                                int selectedIndex,
                                                int x, int y, int w, int h) {
        boolean leftToRight = MetalUtils.isLeftToRight(tabPane);
        int bottom = y + h - 1;
        int right = x + w - 1;
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(darkShadow);

        // Draw unbroken line if tabs are not on BOTTOM, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != BOTTOM || selectedIndex < 0 ||
             (selRect.y - 1 > h) ||
             (selRect.x < x || selRect.x > x + w)) {
            if (ocean && tabPlacement == BOTTOM) {
                g.setColor(oceanSelectedBorderColor);
            }
            g.drawLine(x, y+h-1, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            boolean lastInRun = isLastInRun(selectedIndex);

            if (ocean) {
                g.setColor(oceanSelectedBorderColor);
            }

            if ( leftToRight || lastInRun ) {
                g.drawLine(x, bottom, selRect.x, bottom);
            } else {
                g.drawLine(x, bottom, selRect.x - 1, bottom);
            }

            if (selRect.x + selRect.width < x + w - 2) {
                if ( leftToRight && !lastInRun ) {
                    g.drawLine(selRect.x + selRect.width, bottom,
                                                   right, bottom);
                } else {
                    g.drawLine(selRect.x + selRect.width - 1, bottom,
                                                       right, bottom);
                }
            }
        }
    }

    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
                                              int selectedIndex,
                                              int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);
        if (ocean) {
            g.setColor(oceanSelectedBorderColor);
        }
        else {
            g.setColor(selectHighlight);
        }

        // Draw unbroken line if tabs are not on LEFT, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != LEFT || selectedIndex < 0 ||
            (selRect.x + selRect.width + 1 < x) ||
            (selRect.y < y || selRect.y > y + h)) {
            g.drawLine(x, y + 1, x, y+h-2);
            if (ocean && tabPlacement == LEFT) {
                g.setColor(MetalLookAndFeel.getWhite());
                g.drawLine(x + 1, y, x + 1, y + h - 2);
            }
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x, y, x, selRect.y + 1);
            if (selRect.y + selRect.height < y + h - 2) {
              g.drawLine(x, selRect.y + selRect.height + 1,
                         x, y+h+2);
            }
            if (ocean) {
                g.setColor(MetalLookAndFeel.getWhite());
                g.drawLine(x + 1, y + 1, x + 1, selRect.y + 1);
                if (selRect.y + selRect.height < y + h - 2) {
                    g.drawLine(x + 1, selRect.y + selRect.height + 1,
                               x + 1, y+h+2);
                }
            }
        }
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(darkShadow);
        // Draw unbroken line if tabs are not on RIGHT, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != RIGHT || selectedIndex < 0 ||
             (selRect.x - 1 > w) ||
             (selRect.y < y || selRect.y > y + h)) {
            if (ocean && tabPlacement == RIGHT) {
                g.setColor(oceanSelectedBorderColor);
            }
            g.drawLine(x+w-1, y, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            if (ocean) {
                g.setColor(oceanSelectedBorderColor);
            }
            g.drawLine(x+w-1, y, x+w-1, selRect.y);

            if (selRect.y + selRect.height < y + h - 2) {
                g.drawLine(x+w-1, selRect.y + selRect.height,
                           x+w-1, y+h-2);
            }
        }
    }

    protected int calculateMaxTabHeight( int tabPlacement ) {
        FontMetrics metrics = getFontMetrics();
        int height = metrics.getHeight();
        boolean tallerIcons = false;

        for ( int i = 0; i < tabPane.getTabCount(); ++i ) {
            Icon icon = tabPane.getIconAt( i );
            if ( icon != null ) {
                if ( icon.getIconHeight() > height ) {
                    tallerIcons = true;
                    break;
                }
            }
        }
        return super.calculateMaxTabHeight( tabPlacement ) -
                  (tallerIcons ? (tabInsets.top + tabInsets.bottom) : 0);
    }


    protected int getTabRunOverlay( int tabPlacement ) {
        // Tab runs laid out vertically should overlap
        // at least as much as the largest slant
        if ( tabPlacement == LEFT || tabPlacement == RIGHT ) {
            int maxTabHeight = calculateMaxTabHeight(tabPlacement);
            return maxTabHeight / 2;
        }
        return 0;
    }

    /**
     * Returns {@code true} if tab runs should be rotated.
     *
     * @param tabPlacement a tab placement
     * @param selectedRun a selected run
     * @return {@code true} if tab runs should be rotated.
     */
    protected boolean shouldRotateTabRuns( int tabPlacement, int selectedRun ) {
        return false;
    }

    // Don't pad last run
    protected boolean shouldPadTabRun( int tabPlacement, int run ) {
        return runCount > 1 && run < runCount - 1;
    }

    private boolean isLastInRun( int tabIndex ) {
        int run = getRunForTab( tabPane.getTabCount(), tabIndex );
        int lastIndex = lastTabInRun( tabPane.getTabCount(), run );
        return tabIndex == lastIndex;
    }

    /**
     * Returns the color to use for the specified tab.
     */
    private Color getUnselectedBackgroundAt(int index) {
        Color color = tabPane.getBackgroundAt(index);
        if (color instanceof UIResource) {
            if (unselectedBackground != null) {
                return unselectedBackground;
            }
        }
        return color;
    }

    /**
     * Returns the tab index of JTabbedPane the mouse is currently over
     */
    int getRolloverTabIndex() {
        return getRolloverTab();
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of {@code MetalTabbedPaneUI}.
     */
    public class TabbedPaneLayout extends BasicTabbedPaneUI.TabbedPaneLayout {

        /**
         * Constructs {@code TabbedPaneLayout}.
         */
        public TabbedPaneLayout() {
            MetalTabbedPaneUI.this.super();
        }

        protected void normalizeTabRuns( int tabPlacement, int tabCount,
                                     int start, int max ) {
            // Only normalize the runs for top & bottom;  normalizing
            // doesn't look right for Metal's vertical tabs
            // because the last run isn't padded and it looks odd to have
            // fat tabs in the first vertical runs, but slimmer ones in the
            // last (this effect isn't noticeable for horizontal tabs).
            if ( tabPlacement == TOP || tabPlacement == BOTTOM ) {
                super.normalizeTabRuns( tabPlacement, tabCount, start, max );
            }
        }

        // Don't rotate runs!
        protected void rotateTabRuns( int tabPlacement, int selectedRun ) {
        }

        // Don't pad selected tab
        protected void padSelectedTab( int tabPlacement, int selectedIndex ) {
        }
    }

}
