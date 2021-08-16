/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap;

/**
 * A sort key and its associated sort parameters.
 * This class implements a sort key which is used by the LDAPv3
 * Control for server-side sorting of search results as defined in
 * <a href="http://www.ietf.org/rfc/rfc2891.txt">RFC 2891</a>.
 *
 * @since 1.5
 * @see SortControl
 * @author Vincent Ryan
 */
public class SortKey {

    /*
     * The ID of the attribute to sort by.
     */
    private String attrID;

    /*
     * The sort order. Ascending order, by default.
     */
    private boolean reverseOrder = false;

    /*
     * The ID of the matching rule to use for ordering attribute values.
     */
    private String matchingRuleID = null;

    /**
     * Creates the default sort key for an attribute. Entries will be sorted
     * according to the specified attribute in ascending order using the
     * ordering matching rule defined for use with that attribute.
     *
     * @param   attrID  The non-null ID of the attribute to be used as a sort
     *          key.
     */
    public SortKey(String attrID) {
        this.attrID = attrID;
    }

    /**
     * Creates a sort key for an attribute. Entries will be sorted according to
     * the specified attribute in the specified sort order and using the
     * specified matching rule, if supplied.
     *
     * @param   attrID          The non-null ID of the attribute to be used as
     *                          a sort key.
     * @param   ascendingOrder  If true then entries are arranged in ascending
     *                          order. Otherwise there are arranged in
     *                          descending order.
     * @param   matchingRuleID  The possibly null ID of the matching rule to
     *                          use to order the attribute values. If not
     *                          specified then the ordering matching rule
     *                          defined for the sort key attribute is used.
     */
    public SortKey(String attrID, boolean ascendingOrder,
                    String matchingRuleID) {

        this.attrID = attrID;
        reverseOrder = (! ascendingOrder);
        this.matchingRuleID = matchingRuleID;
    }

    /**
     * Retrieves the attribute ID of the sort key.
     *
     * @return    The non-null Attribute ID of the sort key.
     */
    public String getAttributeID() {
        return attrID;
    }

    /**
     * Determines the sort order.
     *
     * @return    true if the sort order is ascending, false if descending.
     */
    public boolean isAscending() {
        return (! reverseOrder);
    }

    /**
     * Retrieves the matching rule ID used to order the attribute values.
     *
     * @return    The possibly null matching rule ID. If null then the
     *            ordering matching rule defined for the sort key attribute
     *            is used.
     */
    public String getMatchingRuleID() {
        return matchingRuleID;
    }
}
