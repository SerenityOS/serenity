/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package TVJar;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.StringTokenizer;

public class TVPermission extends Permission {

    /**
     * Watch
     */
    private final static int WATCH = 0x1;

    /**
     * Preview
     */
    private final static int PREVIEW = 0x2;

    /**
     * No actions
     */
    private final static int NONE = 0x0;

    /**
     * All actions
     */
    private final static int ALL = WATCH | PREVIEW;

    // the actions mask
    private int mask;

    // the actions string
    private String actions;

    // the canonical name of the channel
    private String cname;

    // true if the channelname is a wildcard
    private boolean wildcard;

    // num range on channel
    private int[] numrange;

    // various num constants
    private final static int NUM_MIN = 1;
    private final static int NUM_MAX = 128;

    public TVPermission(String channel, String action) {
        this(channel, getMask(action));
    }

    TVPermission(String channel, int mask) {
        super(channel);
        init(channel, mask);
    }

    private synchronized int[] parseNum(String num)
            throws Exception {

        if (num == null || num.equals("") || num.equals("*")) {
            wildcard = true;
            return new int[]{NUM_MIN, NUM_MAX};
        }

        int dash = num.indexOf('-');

        if (dash == -1) {
            int p = 0;
            try {
                p = Integer.parseInt(num);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("invalid input" + num);
            }
            return new int[]{p, p};
        } else {
            String low = num.substring(0, dash);
            String high = num.substring(dash + 1);
            int l, h;

            if (low.equals("")) {
                l = NUM_MIN;
            } else {
                try {
                    l = Integer.parseInt(low);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("invalid input" + num);
                }
            }

            if (high.equals("")) {
                h = NUM_MAX;
            } else {
                try {
                    h = Integer.parseInt(high);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("invalid input" + num);
                }
            }
            if (h < l || l < NUM_MIN || h > NUM_MAX) {
                throw new IllegalArgumentException("invalid num range");
            }

            return new int[]{l, h};
        }
    }

    /**
     * Initialize the TVPermission object.
     */
    private synchronized void init(String channel, int mask) {

        // Parse the channel name.
        int sep = channel.indexOf(':');

        if (sep != -1) {
            String num = channel.substring(sep + 1);
            cname = channel.substring(0, sep);
            try {
                numrange = parseNum(num);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid num range: " + num);
            }
        } else {
            numrange = new int[]{NUM_MIN, NUM_MAX};
        }
    }

    /**
     * Convert an action string to an integer actions mask.
     *
     * @param action the action string
     * @return the action mask
     */
    private synchronized static int getMask(String action) {
        int mask = NONE;

        if (action == null) {
            return mask;
        }

        StringTokenizer st = new StringTokenizer(action.toLowerCase(), ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals("watch")) {
                mask |= WATCH;
            } else if (token.equals("preview")) {
                mask |= PREVIEW;
            } else {
                throw new IllegalArgumentException("invalid TV permission: " + token);
            }
        }
        return mask;
    }

    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof TVPermission)) {
            return false;
        }

        if (this.wildcard) {
            return true;
        }

        TVPermission that = (TVPermission) p;

        if ((this.mask & that.mask) != that.mask) {
            System.out.println("Masks are not ok this = "
                    + this.mask + "THat = " + that.mask);
            return false;
        }

        if ((this.numrange[0] > that.numrange[0])
                || (this.numrange[1] < that.numrange[1])) {

            System.out.println("This 0= " + this.numrange[0]
                    + " 1 = " + this.numrange[1]);
            System.out.println("That 0= " + that.numrange[0]
                    + " 1 = " + that.numrange[1]);
            return false;
        }
        return true;
    }

    /**
     * Checks two TVPermission objects for equality.
     * <p>
     * @param obj the object we are testing for equality.
     * @return true if obj is a TVPermission, and has the same channelname and
     * action mask as this TVPermission object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof TVPermission)) {
            return false;
        }

        TVPermission that = (TVPermission) obj;

        // check the mask first
        if (this.mask != that.mask) {
            return false;
        }

        // now check the num range...
        if ((this.numrange[0] != that.numrange[0])
                || (this.numrange[1] != that.numrange[1])) {
            return false;
        }

        return this.getName().equals(that.getName());
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Return the canonical string representation of the actions. Always returns
     * actions in the following order: watch,preview.
     *
     * @param mask a specific integer action mask to translate into a string
     * @return the canonical string representation of the actions
     */
    private synchronized static String getActions(int mask) {
        StringJoiner sj = new StringJoiner(",");
        if ((mask & WATCH) == WATCH) {
            sj.add("watch");
        }
        if ((mask & PREVIEW) == PREVIEW) {
            sj.add("preview");
        }
        return sj.toString();
    }

    /**
     * Return the canonical string representation of the actions. Always returns
     * actions in the following order: watch,preview.
     *
     * @return the canonical string representation of the actions.
     */
    @Override
    public String getActions() {
        if (actions == null) {
            actions = getActions(this.mask);
        }

        return actions;
    }

    @Override
    public String toString() {
        return super.toString() + "\n"
                + "cname = " + cname + "\n"
                + "wildcard = " + wildcard + "\n"
                + "numrange = " + numrange[0] + "," + numrange[1] + "\n";

    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new TVPermissionCollection();
    }
}

final class TVPermissionCollection extends PermissionCollection {

    /**
     * The TVPermissions for this set.
     */
    private final ArrayList<TVPermission> permissions = new ArrayList<>();

    /**
     * Adds a permission to the TVPermissions. The key for the hash is the name
     * in the case of wildcards, or all the IP addresses.
     *
     * @param permission the Permission object to add.
     */
    @Override
    public void add(Permission permission) {
        if (!(permission instanceof TVPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        permissions.add((TVPermission) permission);
    }

    /**
     * Check and see if this collection of permissions implies the permissions
     * expressed in "permission".
     *
     * @param p the Permission object to compare
     *
     * @return true if "permission" is a proper subset of a permission in the
     * collection, false if not.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof TVPermission)) {
            return false;
        }

        Iterator<TVPermission> i = permissions.iterator();
        while (i.hasNext()) {
            if (((TVPermission) i.next()).implies(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an enumeration of all the TVPermission objects in the container.
     *
     * @return an enumeration of all the TVPermission objects.
     */
    @Override
    public Enumeration elements() {
        return Collections.enumeration(permissions);
    }

}
