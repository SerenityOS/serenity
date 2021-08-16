/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

import java.util.Vector;

/**
 * Class {@code AccessibleStateSet} determines a component's state set. The
 * state set of a component is a set of {@code AccessibleState} objects and
 * descriptions. E.G., The current overall state of the object, such as whether
 * it is enabled, has focus, etc.
 *
 * @author Willie Walker
 * @see AccessibleState
 */
public class AccessibleStateSet {

    /**
     * Each entry in the {@code Vector} represents an {@code AccessibleState}.
     *
     * @see #add
     * @see #addAll
     * @see #remove
     * @see #contains
     * @see #toArray
     * @see #clear
     */
    protected Vector<AccessibleState> states = null;

    /**
     * Creates a new empty state set.
     */
    public AccessibleStateSet() {
        states = null;
    }

    /**
     * Creates a new state with the initial set of states contained in the array
     * of states passed in. Duplicate entries are ignored.
     *
     * @param  states an array of {@code AccessibleState} describing the state
     *         set
     */
    public AccessibleStateSet(AccessibleState[] states) {
        if (states.length != 0) {
            this.states = new Vector<>(states.length);
            for (int i = 0; i < states.length; i++) {
                if (!this.states.contains(states[i])) {
                    this.states.addElement(states[i]);
                }
            }
        }
    }

    /**
     * Adds a new state to the current state set if it is not already present.
     * If the state is already in the state set, the state set is unchanged and
     * the return value is {@code false}. Otherwise, the state is added to the
     * state set and the return value is {@code true}.
     *
     * @param  state the state to add to the state set
     * @return {@code true} if state is added to the state set; {@code false} if
     *         the state set is unchanged
     */
    public boolean add(AccessibleState state) {
        // [[[ PENDING:  WDW - the implementation of this does not need
        // to always use a vector of states.  It could be improved by
        // caching the states as a bit set.]]]
        if (states == null) {
            states = new Vector<>();
        }

        if (!states.contains(state)) {
            states.addElement(state);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds all of the states to the existing state set. Duplicate entries are
     * ignored.
     *
     * @param  states {@code AccessibleState} array describing the state set
     */
    public void addAll(AccessibleState[] states) {
        if (states.length != 0) {
            if (this.states == null) {
                this.states = new Vector<>(states.length);
            }
            for (int i = 0; i < states.length; i++) {
                if (!this.states.contains(states[i])) {
                    this.states.addElement(states[i]);
                }
            }
        }
    }

    /**
     * Removes a state from the current state set. If the state is not in the
     * set, the state set will be unchanged and the return value will be
     * {@code false}. If the state is in the state set, it will be removed from
     * the set and the return value will be {@code true}.
     *
     * @param  state the state to remove from the state set
     * @return {@code true} if the state is in the state set; {@code false} if
     *         the state set will be unchanged
     */
    public boolean remove(AccessibleState state) {
        if (states == null) {
            return false;
        } else {
            return states.removeElement(state);
        }
    }

    /**
     * Removes all the states from the current state set.
     */
    public void clear() {
        if (states != null) {
            states.removeAllElements();
        }
    }

    /**
     * Checks if the current state is in the state set.
     *
     * @param  state the state
     * @return {@code true} if the state is in the state set; otherwise
     *         {@code false}
     */
    public boolean contains(AccessibleState state) {
        if (states == null) {
            return false;
        } else {
            return states.contains(state);
        }
    }

    /**
     * Returns the current state set as an array of {@code AccessibleState}.
     *
     * @return {@code AccessibleState} array containing the current state
     */
    public AccessibleState[] toArray() {
        if (states == null) {
            return new AccessibleState[0];
        } else {
            AccessibleState[] stateArray = new AccessibleState[states.size()];
            for (int i = 0; i < stateArray.length; i++) {
                stateArray[i] = states.elementAt(i);
            }
            return stateArray;
        }
    }

    /**
     * Creates a localized string representing all the states in the set using
     * the default locale.
     *
     * @return comma separated localized string
     * @see AccessibleBundle#toDisplayString
     */
    public String toString() {
        String ret = null;
        if ((states != null) && (states.size() > 0)) {
            ret = states.elementAt(0).toDisplayString();
            for (int i = 1; i < states.size(); i++) {
                ret = ret + ","
                        + states.elementAt(i).toDisplayString();
            }
        }
        return ret;
    }
}
