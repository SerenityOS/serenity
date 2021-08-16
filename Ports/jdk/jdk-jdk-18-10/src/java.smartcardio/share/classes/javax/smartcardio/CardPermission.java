/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.smartcardio;

import java.io.*;
import java.util.StringJoiner;
import java.security.Permission;

/**
 * A permission for Smart Card operations. A CardPermission consists of the
 * name of the card terminal the permission applies to and a set of actions
 * that are valid for that terminal.
 *
 * <p>A CardPermission with a name of <code>*</code> applies to all
 * card terminals. The actions string is a comma separated list of the actions
 * listed below, or <code>*</code> to signify "all actions."
 *
 * <p>Individual actions are:
 * <dl>
 * <dt>connect
 * <dd>connect to a card using
 * {@linkplain CardTerminal#connect CardTerminal.connect()}
 *
 * <dt>reset
 * <dd>reset the card using {@linkplain Card#disconnect Card.disconnect(true)}
 *
 * <dt>exclusive
 * <dd>establish exclusive access to a card using
 * {@linkplain Card#beginExclusive} and {@linkplain Card#endExclusive
 * endExclusive()}
 *
 * <dt>transmitControl
 * <dd>transmit a control command using
 * {@linkplain Card#transmitControlCommand Card.transmitControlCommand()}
 *
 * <dt>getBasicChannel
 * <dd>obtain the basic logical channel using
 * {@linkplain Card#getBasicChannel}
 *
 * <dt>openLogicalChannel
 * <dd>open a new logical channel using
 * {@linkplain Card#openLogicalChannel}
 *
 * </dl>
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public class CardPermission extends Permission {

    private static final long serialVersionUID = 7146787880530705613L;

    private final static int A_CONNECT              = 0x01;
    private final static int A_EXCLUSIVE            = 0x02;
    private final static int A_GET_BASIC_CHANNEL    = 0x04;
    private final static int A_OPEN_LOGICAL_CHANNEL = 0x08;
    private final static int A_RESET                = 0x10;
    private final static int A_TRANSMIT_CONTROL     = 0x20;

    // sum of all the actions above
    private final static int A_ALL                  = 0x3f;

    private final static int[] ARRAY_MASKS = {
        A_ALL,
        A_CONNECT,
        A_EXCLUSIVE,
        A_GET_BASIC_CHANNEL,
        A_OPEN_LOGICAL_CHANNEL,
        A_RESET,
        A_TRANSMIT_CONTROL,
    };

    private final static String S_CONNECT              = "connect";
    private final static String S_EXCLUSIVE            = "exclusive";
    private final static String S_GET_BASIC_CHANNEL    = "getBasicChannel";
    private final static String S_OPEN_LOGICAL_CHANNEL = "openLogicalChannel";
    private final static String S_RESET                = "reset";
    private final static String S_TRANSMIT_CONTROL     = "transmitControl";

    private final static String S_ALL                  = "*";

    private final static String[] ARRAY_STRINGS = {
        S_ALL,
        S_CONNECT,
        S_EXCLUSIVE,
        S_GET_BASIC_CHANNEL,
        S_OPEN_LOGICAL_CHANNEL,
        S_RESET,
        S_TRANSMIT_CONTROL,
    };

    private transient int mask;

    /**
     * @serial
     */
    private final String actions;

    /**
     * Constructs a new CardPermission with the specified actions.
     * <code>terminalName</code> is the name of a CardTerminal or <code>*</code>
     * if this permission applies to all terminals. <code>actions</code>
     * contains a comma-separated list of the individual actions
     * or <code>*</code> to signify all actions. For more information,
     * see the documentation at the top of this {@linkplain CardPermission
     * class}.
     *
     * @param terminalName the name of the card terminal, or <code>*</code>
     * @param actions the action string (or null if the set of permitted
     *   actions is empty)
     *
     * @throws NullPointerException if terminalName is null
     * @throws IllegalArgumentException if actions is an invalid actions
     *   specification
     */
    public CardPermission(String terminalName, String actions) {
        super(terminalName);
        if (terminalName == null) {
            throw new NullPointerException();
        }
        mask = getMask(actions);
        this.actions = getActions(mask);
    }

    private static int getMask(String actions) {
        if (actions == null) {
            return 0;
        }
        if (actions.length() == 0) {
            throw new IllegalArgumentException("actions must not be empty");
        }

        // try exact matches for simple actions first
        for (int i = 0; i < ARRAY_STRINGS.length; i++) {
            if (actions == ARRAY_STRINGS[i]) {
                return ARRAY_MASKS[i];
            }
        }

        if (actions.endsWith(",")) {
            throw new IllegalArgumentException("Invalid actions: '" + actions + "'");
        }
        int mask = 0;
        String[] split = actions.split(",");
    outer:
        for (String s : split) {
            for (int i = 0; i < ARRAY_STRINGS.length; i++) {
                if (ARRAY_STRINGS[i].equalsIgnoreCase(s)) {
                    mask |= ARRAY_MASKS[i];
                    continue outer;
                }
            }
            throw new IllegalArgumentException("Invalid action: '" + s + "'");
        }

        return mask;
    }

    private static String getActions(int mask) {
        if (mask == 0) {
            return null;
        }
        if (mask == A_ALL) {
            return S_ALL;
        }
        StringJoiner sj = new StringJoiner(",");
        for (int i = 0; i < ARRAY_MASKS.length; i++) {
            final int action = ARRAY_MASKS[i];
            if ((mask & action) == action) {
                sj.add(ARRAY_STRINGS[i]);
            }
        }
        return sj.toString();
    }


    /**
     * Returns the canonical string representation of the actions.
     * It is <code>*</code> to signify all actions defined by this class or
     * the string concatenation of the comma-separated,
     * lexicographically sorted list of individual actions.
     *
     * @return the canonical string representation of the actions.
     */
    public String getActions() {
        return actions;
    }

    /**
     * Checks if this CardPermission object implies the specified permission.
     * That is the case, if and only if
     * <ul>
     * <li><p><code>permission</code> is an instance of CardPermission,</p>
     * <li><p><code>permission</code>'s actions are a proper subset of this
     *   object's actions, and</p>
     * <li><p>this object's <code>getName()</code> method is either
     *   <code>*</code> or equal to <code>permission</code>'s <code>name</code>.
     *   </p>
     * </ul>
     *
     * @param permission the permission to check against
     * @return true if and only if this CardPermission object implies the
     *   specified permission.
     */
    public boolean implies(Permission permission) {
        if (permission instanceof CardPermission == false) {
            return false;
        }
        CardPermission other = (CardPermission)permission;
        if ((this.mask & other.mask) != other.mask) {
            return false;
        }
        String thisName = getName();
        if (thisName.equals("*")) {
            return true;
        }
        if (thisName.equals(other.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Compares the specified object with this CardPermission for equality.
     * This CardPermission is equal to another Object <code>object</code>, if
     * and only if
     * <ul>
     * <li><p><code>object</code> is an instance of CardPermission,</p>
     * <li><p><code>this.getName()</code> is equal to
     * <code>((CardPermission)object).getName()</code>, and</p>
     * <li><p><code>this.getActions()</code> is equal to
     * <code>((CardPermission)object).getActions()</code>.</p>
     * </ul>
     *
     * @param obj the object to be compared for equality with this CardPermission
     * @return true if and only if the specified object is equal to this
     *   CardPermission
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CardPermission == false) {
            return false;
        }
        CardPermission other = (CardPermission)obj;
        return this.getName().equals(other.getName()) && (this.mask == other.mask);
    }

    /**
     * Returns the hash code value for this CardPermission object.
     *
     * @return the hash code value for this CardPermission object.
     */
    public int hashCode() {
        return getName().hashCode() + 31 * mask;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        // Write out the actions. The superclass takes care of the name.
        s.defaultWriteObject();
    }

    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // Read in the actions, then restore the mask.
        s.defaultReadObject();
        mask = getMask(actions);
    }
}
