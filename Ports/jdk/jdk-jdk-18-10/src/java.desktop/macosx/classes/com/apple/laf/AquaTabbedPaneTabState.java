/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;

import javax.swing.SwingConstants;

class AquaTabbedPaneTabState {
    static final int FIXED_SCROLL_TAB_LENGTH = 27;

    protected final Rectangle leftScrollTabRect = new Rectangle();
    protected final Rectangle rightScrollTabRect = new Rectangle();

    protected int numberOfVisibleTabs = 0;
    protected int[] visibleTabList = new int[10];
    protected int lastLeftmostTab;
    protected int lastReturnAt;

    private boolean needsScrollers;
    private boolean hasMoreLeftTabs;
    private boolean hasMoreRightTabs;

    private final AquaTabbedPaneUI pane;

    protected AquaTabbedPaneTabState(final AquaTabbedPaneUI pane) {
        this.pane = pane;
    }

    protected int getIndex(final int i) {
        if (i >= visibleTabList.length) return Integer.MIN_VALUE;
        return visibleTabList[i];
    }

    protected void init(final int tabCount) {
        if (tabCount < 1) needsScrollers = false;
        if (tabCount == visibleTabList.length) return;
        final int[] tempVisibleTabs = new int[tabCount];
        System.arraycopy(visibleTabList, 0, tempVisibleTabs, 0, Math.min(visibleTabList.length, tabCount));
        visibleTabList = tempVisibleTabs;
    }

    int getTotal() {
        return numberOfVisibleTabs;
    }

    boolean needsScrollTabs() {
        return needsScrollers;
    }

    void setNeedsScrollers(final boolean needsScrollers) {
        this.needsScrollers = needsScrollers;
    }

    boolean needsLeftScrollTab() {
        return hasMoreLeftTabs;
    }

    boolean needsRightScrollTab() {
        return hasMoreRightTabs;
    }

    Rectangle getLeftScrollTabRect() {
        return leftScrollTabRect;
    }

    Rectangle getRightScrollTabRect() {
        return rightScrollTabRect;
    }

    boolean isBefore(final int i) {
        if (numberOfVisibleTabs == 0) return true;
        if (i < visibleTabList[0]) return true;
        return false;
    }

    boolean isAfter(final int i) {
        if (i > visibleTabList[numberOfVisibleTabs - 1]) return true;
        return false;
    }

    private void addToEnd(final int idToAdd, final int length) {
        visibleTabList[length] = idToAdd;
    }

    private void addToBeginning(final int idToAdd, final int length) {
        System.arraycopy(visibleTabList, 0, visibleTabList, 1, length);
        visibleTabList[0] = idToAdd;
    }


    void relayoutForScrolling(final Rectangle[] rects, final int startX, final int startY, final int returnAt, final int selectedIndex, final boolean verticalTabRuns, final int tabCount, final boolean isLeftToRight) {
        if (!needsScrollers) {
            hasMoreLeftTabs = false;
            hasMoreRightTabs = false;
            return;
        }

        // we don't fit, so we need to figure the space based on the size of the popup
        // tab, then add the tabs, centering the selected tab as much as possible.

        // Tabs on TOP or BOTTOM or LEFT or RIGHT
        // if top or bottom, width is hardocoded
        // if left or right height should be hardcoded.
        if (verticalTabRuns) {
            rightScrollTabRect.height = FIXED_SCROLL_TAB_LENGTH;
            leftScrollTabRect.height = FIXED_SCROLL_TAB_LENGTH;
        } else {
            rightScrollTabRect.width = FIXED_SCROLL_TAB_LENGTH;
            leftScrollTabRect.width = FIXED_SCROLL_TAB_LENGTH;
        }

        // we have all the tab rects, we just need to adjust the x coordinates
        // and populate the visible list

        // sja fix what do we do if remaining width is <0??

        // we could try to center it based on width of tabs, but for now
        // we try to center based on number of tabs on each side, putting the extra
        // on the left (since the first right is the selected tab).
        // if we have 0 selected we will just go right, and if we have

        // the logic here is start with the selected tab, and then fit
        // in as many tabs as possible on each side until we don't fit any more.
        // but if all we did was change selection then we need to try to keep the same
        // tabs on screen so we don't get a jarring tab moving out from under the mouse
        // effect.

        final boolean sizeChanged = returnAt != lastReturnAt;
        // so if we stay the same, make right the first tab and say left done = true
        if (pane.popupSelectionChanged || sizeChanged) {
            pane.popupSelectionChanged = false;
            lastLeftmostTab = -1;
        }

        int right = selectedIndex;
        int left = selectedIndex - 1;

        // if we had a good last leftmost tab then we set left to unused and
        // start at that tab.
        if (lastLeftmostTab >= 0) {
            right = lastLeftmostTab;
            left = -1;
        } else if (selectedIndex < 0) {
            // this is if there is none selected see radar 3138137
            right = 0;
            left = -1;
        }

        int remainingSpace = returnAt - pane.tabAreaInsets.right - pane.tabAreaInsets.left - FIXED_SCROLL_TAB_LENGTH * 2;
        int visibleCount = 0;

        final Rectangle firstRect = rects[right];
        if ((verticalTabRuns ? firstRect.height : firstRect.width) > remainingSpace) {
            // always show at least the selected one!
            addToEnd(right, visibleCount);
            if (verticalTabRuns) {
                firstRect.height = remainingSpace; // force it to fit!
            } else {
                firstRect.width = remainingSpace; // force it to fit!
            }
            visibleCount++;
        } else {
            boolean rightDone = false;
            boolean leftDone = false;

            // at least one if not more will fit
            while ((visibleCount < tabCount) && !(rightDone && leftDone)) {
                if (!rightDone && right >= 0 && right < tabCount) {
                    final Rectangle rightRect = rects[right];
                    if ((verticalTabRuns ? rightRect.height : rightRect.width) > remainingSpace) {
                        rightDone = true;
                    } else {
                        addToEnd(right, visibleCount);
                        visibleCount++;
                        remainingSpace -= (verticalTabRuns ? rightRect.height : rightRect.width);
                        right++;
                        continue; // this gives a bias to "paging forward", and "inching backward"
                    }
                } else {
                    rightDone = true;
                }

                if (!leftDone && left >= 0 && left < tabCount) {
                    final Rectangle leftRect = rects[left];
                    if ((verticalTabRuns ? leftRect.height : leftRect.width) > remainingSpace) {
                        leftDone = true;
                    } else {
                        addToBeginning(left, visibleCount);
                        visibleCount++;
                        remainingSpace -= (verticalTabRuns ? leftRect.height : leftRect.width);
                        left--;
                    }
                } else {
                    leftDone = true;
                }
            }
        }

        if (visibleCount > visibleTabList.length) visibleCount = visibleTabList.length;

        hasMoreLeftTabs = visibleTabList[0] > 0;
        hasMoreRightTabs = visibleTabList[visibleCount - 1] < visibleTabList.length - 1;

        numberOfVisibleTabs = visibleCount;
        // add the scroll tab at the end;
        lastLeftmostTab = getIndex(0);
        lastReturnAt = returnAt;

        final int firstTabIndex = getIndex(0);
        final int lastTabIndex = getIndex(visibleCount - 1);

        // move all "invisible" tabs beyond the edge of known space...
        for (int i = 0; i < tabCount; i++) {
            if (i < firstTabIndex || i > lastTabIndex) {
                final Rectangle rect = rects[i];
                rect.x = Short.MAX_VALUE;
                rect.y = Short.MAX_VALUE;
            }
        }
    }

    protected void alignRectsRunFor(final Rectangle[] rects, final Dimension tabPaneSize, final int tabPlacement, final boolean isRightToLeft) {
        final boolean isVertical = tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT;

        if (isVertical) {
            if (needsScrollers) {
                stretchScrollingVerticalRun(rects, tabPaneSize);
            } else {
                centerVerticalRun(rects, tabPaneSize);
            }
        } else {
            if (needsScrollers) {
                stretchScrollingHorizontalRun(rects, tabPaneSize, isRightToLeft);
            } else {
                centerHorizontalRun(rects, tabPaneSize, isRightToLeft);
            }
        }
    }

    private void centerHorizontalRun(final Rectangle[] rects, final Dimension size, final boolean isRightToLeft) {
        int totalLength = 0;
        for (final Rectangle element : rects) {
            totalLength += element.width;
        }

        int x = size.width / 2 - totalLength / 2;

        if (isRightToLeft) {
            for (final Rectangle rect : rects) {
                rect.x = x;
                x += rect.width;
            }
        } else {
            for (int i = rects.length - 1; i >= 0; i--) {
                final Rectangle rect = rects[i];
                rect.x = x;
                x += rect.width;
            }
        }
    }

    private void centerVerticalRun(final Rectangle[] rects, final Dimension size) {
        int totalLength = 0;
        for (final Rectangle element : rects) {
            totalLength += element.height;
        }

        int y = size.height / 2 - totalLength / 2;

        if (true) {
            for (final Rectangle rect : rects) {
                rect.y = y;
                y += rect.height;
            }
        } else {
            for (int i = rects.length - 1; i >= 0; i--) {
                final Rectangle rect = rects[i];
                rect.y = y;
                y += rect.height;
            }
        }
    }

    private void stretchScrollingHorizontalRun(final Rectangle[] rects, final Dimension size, final boolean isRightToLeft) {
        final int totalTabs = getTotal();
        final int firstTabIndex = getIndex(0);
        final int lastTabIndex = getIndex(totalTabs - 1);

        int totalRunLength = 0;
        for (int i = firstTabIndex; i <= lastTabIndex; i++) {
            totalRunLength += rects[i].width;
        }

        int slack = size.width - totalRunLength - pane.tabAreaInsets.left - pane.tabAreaInsets.right;
        if (needsLeftScrollTab()) {
            slack -= FIXED_SCROLL_TAB_LENGTH;
        }
        if (needsRightScrollTab()) {
            slack -= FIXED_SCROLL_TAB_LENGTH;
        }

        final int minSlack = (int)((float)(slack) / (float)(totalTabs));
        int extraSlack = slack - (minSlack * totalTabs);
        int runningLength = 0;
        final int xOffset = pane.tabAreaInsets.left + (needsLeftScrollTab() ? FIXED_SCROLL_TAB_LENGTH : 0);

        if (isRightToLeft) {
            for (int i = firstTabIndex; i <= lastTabIndex; i++) {
                final Rectangle rect = rects[i];
                int slackToAdd = minSlack;
                if (extraSlack > 0) {
                    slackToAdd++;
                    extraSlack--;
                }
                rect.x = runningLength + xOffset;
                rect.width += slackToAdd;
                runningLength += rect.width;
            }
        } else {
            for (int i = lastTabIndex; i >= firstTabIndex; i--) {
                final Rectangle rect = rects[i];
                int slackToAdd = minSlack;
                if (extraSlack > 0) {
                    slackToAdd++;
                    extraSlack--;
                }
                rect.x = runningLength + xOffset;
                rect.width += slackToAdd;
                runningLength += rect.width;
            }
        }

        if (isRightToLeft) {
            leftScrollTabRect.x = pane.tabAreaInsets.left;
            leftScrollTabRect.y = rects[firstTabIndex].y;
            leftScrollTabRect.height = rects[firstTabIndex].height;

            rightScrollTabRect.x = size.width - pane.tabAreaInsets.right - rightScrollTabRect.width;
            rightScrollTabRect.y = rects[lastTabIndex].y;
            rightScrollTabRect.height = rects[lastTabIndex].height;
        } else {
            rightScrollTabRect.x = pane.tabAreaInsets.left;
            rightScrollTabRect.y = rects[firstTabIndex].y;
            rightScrollTabRect.height = rects[firstTabIndex].height;

            leftScrollTabRect.x = size.width - pane.tabAreaInsets.right - rightScrollTabRect.width;
            leftScrollTabRect.y = rects[lastTabIndex].y;
            leftScrollTabRect.height = rects[lastTabIndex].height;

            if (needsLeftScrollTab()) {
                for (int i = lastTabIndex; i >= firstTabIndex; i--) {
                    final Rectangle rect = rects[i];
                    rect.x -= FIXED_SCROLL_TAB_LENGTH;
                }
            }

            if (needsRightScrollTab()) {
                for (int i = lastTabIndex; i >= firstTabIndex; i--) {
                    final Rectangle rect = rects[i];
                    rect.x += FIXED_SCROLL_TAB_LENGTH;
                }
            }
        }
    }

    private void stretchScrollingVerticalRun(final Rectangle[] rects, final Dimension size) {
        final int totalTabs = getTotal();
        final int firstTabIndex = getIndex(0);
        final int lastTabIndex = getIndex(totalTabs - 1);

        int totalRunLength = 0;
        for (int i = firstTabIndex; i <= lastTabIndex; i++) {
            totalRunLength += rects[i].height;
        }

        int slack = size.height - totalRunLength - pane.tabAreaInsets.top - pane.tabAreaInsets.bottom;
        if (needsLeftScrollTab()) {
            slack -= FIXED_SCROLL_TAB_LENGTH;
        }
        if (needsRightScrollTab()) {
            slack -= FIXED_SCROLL_TAB_LENGTH;
        }

        final int minSlack = (int)((float)(slack) / (float)(totalTabs));
        int extraSlack = slack - (minSlack * totalTabs);
        int runningLength = 0;
        final int yOffset = pane.tabAreaInsets.top + (needsLeftScrollTab() ? FIXED_SCROLL_TAB_LENGTH : 0);

        for (int i = firstTabIndex; i <= lastTabIndex; i++) {
            final Rectangle rect = rects[i];
            int slackToAdd = minSlack;
            if (extraSlack > 0) {
                slackToAdd++;
                extraSlack--;
            }
            rect.y = runningLength + yOffset;
            rect.height += slackToAdd;
            runningLength += rect.height;
        }

        leftScrollTabRect.x = rects[firstTabIndex].x;
        leftScrollTabRect.y = pane.tabAreaInsets.top;
        leftScrollTabRect.width = rects[firstTabIndex].width;

        rightScrollTabRect.x = rects[lastTabIndex].x;
        rightScrollTabRect.y = size.height - pane.tabAreaInsets.bottom - rightScrollTabRect.height;
        rightScrollTabRect.width = rects[lastTabIndex].width;
    }
}
