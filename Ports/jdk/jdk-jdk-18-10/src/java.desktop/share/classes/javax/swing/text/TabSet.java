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

package javax.swing.text;

import java.io.Serializable;

/**
 * A TabSet is comprised of many TabStops. It offers methods for locating the
 * closest TabStop to a given position and finding all the potential TabStops.
 * It is also immutable.
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
 * @author  Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class TabSet implements Serializable
{
    /** TabStops this TabSet contains. */
    private TabStop[]              tabs;
    /**
     * Since this class is immutable the hash code could be
     * calculated once. MAX_VALUE means that it was not initialized
     * yet. Hash code shouldn't has MAX_VALUE value.
     */
    private int hashCode = Integer.MAX_VALUE;

    /**
     * Creates and returns an instance of TabSet. The array of Tabs
     * passed in must be sorted in ascending order.
     * @param tabs the TabStops to initialize the TabSet
     */
    public TabSet(TabStop[] tabs) {
        // PENDING(sky): If this becomes a problem, make it sort.
        if(tabs != null) {
            int          tabCount = tabs.length;

            this.tabs = new TabStop[tabCount];
            System.arraycopy(tabs, 0, this.tabs, 0, tabCount);
        }
        else
            this.tabs = null;
    }

    /**
     * Returns the number of Tab instances the receiver contains.
     * @return the number of Tab instances the receiver contains
     */
    public int getTabCount() {
        return (tabs == null) ? 0 : tabs.length;
    }

    /**
     * Returns the TabStop at index <code>index</code>. This will throw an
     * IllegalArgumentException if <code>index</code> is outside the range
     * of tabs.
     * @param index which TapStop to return
     * @return the TabStop at index {@code index}
     */
    public TabStop getTab(int index) {
        int          numTabs = getTabCount();

        if(index < 0 || index >= numTabs)
            throw new IllegalArgumentException(index +
                                              " is outside the range of tabs");
        return tabs[index];
    }

    /**
     * Returns the Tab instance after <code>location</code>. This will
     * return null if there are no tabs after <code>location</code>.
     * @param location location to find a Tab after
     * @return the Tab instance after {@code location}
     */
    public TabStop getTabAfter(float location) {
        int     index = getTabIndexAfter(location);

        return (index == -1) ? null : tabs[index];
    }

    /**
     * Returns the index of the TabStop <code>tab</code>, or -1 if
     * <code>tab</code> is not contained in the receiver.
     * @param tab the TabStop to find
     * @return the index of the TabStop <code>tab</code>, or -1 if
     * <code>tab</code> is not contained in the receiver.
     */
    public int getTabIndex(TabStop tab) {
        for(int counter = getTabCount() - 1; counter >= 0; counter--)
            // should this use .equals?
            if(getTab(counter) == tab)
                return counter;
        return -1;
    }

    /**
     * Returns the index of the Tab to be used after <code>location</code>.
     * This will return -1 if there are no tabs after <code>location</code>.
     * @param location location to find a Tab after
     * @return the index of the Tab to be used after <code>location</code>
     */
    public int getTabIndexAfter(float location) {
        int     current, min, max;

        min = 0;
        max = getTabCount();
        while(min != max) {
            current = (max - min) / 2 + min;
            if(location > tabs[current].getPosition()) {
                if(min == current)
                    min = max;
                else
                    min = current;
            }
            else {
                if(current == 0 || location > tabs[current - 1].getPosition())
                    return current;
                max = current;
            }
        }
        // no tabs after the passed in location.
        return -1;
    }

    /**
     * Indicates whether this <code>TabSet</code> is equal to another one.
     * @param o the <code>TabSet</code> instance which this instance
     *  should be compared to.
     * @return <code>true</code> if <code>o</code> is the instance of
     * <code>TabSet</code>, has the same number of <code>TabStop</code>s
     * and they are all equal, <code>false</code> otherwise.
     *
     * @since 1.5
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TabSet) {
            TabSet ts = (TabSet) o;
            int count = getTabCount();
            if (ts.getTabCount() != count) {
                return false;
            }
            for (int i=0; i < count; i++) {
                TabStop ts1 = getTab(i);
                TabStop ts2 = ts.getTab(i);
                if ((ts1 == null && ts2 != null) ||
                        (ts1 != null && !getTab(i).equals(ts.getTab(i)))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns a hashcode for this set of TabStops.
     * @return  a hashcode value for this set of TabStops.
     *
     * @since 1.5
     */
    public int hashCode() {
        if (hashCode == Integer.MAX_VALUE) {
            hashCode = 0;
            int len = getTabCount();
            for (int i = 0; i < len; i++) {
                TabStop ts = getTab(i);
                hashCode ^= ts != null ? getTab(i).hashCode() : 0;
            }
            if (hashCode == Integer.MAX_VALUE) {
                hashCode -= 1;
            }
        }
        return hashCode;
    }

    /**
     * Returns the string representation of the set of tabs.
     */
    public String toString() {
        int            tabCount = getTabCount();
        StringBuilder buffer = new StringBuilder("[ ");

        for(int counter = 0; counter < tabCount; counter++) {
            if(counter > 0)
                buffer.append(" - ");
            buffer.append(getTab(counter).toString());
        }
        buffer.append(" ]");
        return buffer.toString();
    }
}
