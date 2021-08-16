/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerEncoder;

/**
 * Requests that the results of a search operation be sorted by the LDAP server
 * before being returned.
 * The sort criteria are specified using an ordered list of one or more sort
 * keys, with associated sort parameters.
 * Search results are sorted at the LDAP server according to the parameters
 * supplied in the sort control and then returned to the requestor. If sorting
 * is not supported at the server (and the sort control is marked as critical)
 * then the search operation is not performed and an error is returned.
 * <p>
 * The following code sample shows how the class may be used:
 * <pre>{@code
 *
 *     // Open an LDAP association
 *     LdapContext ctx = new InitialLdapContext();
 *
 *     // Activate sorting
 *     String sortKey = "cn";
 *     ctx.setRequestControls(new Control[]{
 *         new SortControl(sortKey, Control.CRITICAL) });
 *
 *     // Perform a search
 *     NamingEnumeration results =
 *         ctx.search("", "(objectclass=*)", new SearchControls());
 *
 *     // Iterate over search results
 *     while (results != null && results.hasMore()) {
 *         // Display an entry
 *         SearchResult entry = (SearchResult)results.next();
 *         System.out.println(entry.getName());
 *         System.out.println(entry.getAttributes());
 *
 *         // Handle the entry's response controls (if any)
 *         if (entry instanceof HasControls) {
 *             // ((HasControls)entry).getControls();
 *         }
 *     }
 *     // Examine the sort control response
 *     Control[] controls = ctx.getResponseControls();
 *     if (controls != null) {
 *         for (int i = 0; i < controls.length; i++) {
 *             if (controls[i] instanceof SortResponseControl) {
 *                 SortResponseControl src = (SortResponseControl)controls[i];
 *                 if (! src.isSorted()) {
 *                     throw src.getException();
 *                 }
 *             } else {
 *                 // Handle other response controls (if any)
 *             }
 *         }
 *     }
 *
 *     // Close the LDAP association
 *     ctx.close();
 *     ...
 *
 * }</pre>
 * <p>
 * This class implements the LDAPv3 Request Control for server-side sorting
 * as defined in
 * <a href="http://www.ietf.org/rfc/rfc2891.txt">RFC 2891</a>.
 *
 * The control's value has the following ASN.1 definition:
 * <pre>
 *
 *     SortKeyList ::= SEQUENCE OF SEQUENCE {
 *         attributeType     AttributeDescription,
 *         orderingRule  [0] MatchingRuleId OPTIONAL,
 *         reverseOrder  [1] BOOLEAN DEFAULT FALSE }
 *
 * </pre>
 *
 * @since 1.5
 * @see SortKey
 * @see SortResponseControl
 * @author Vincent Ryan
 */
public final class SortControl extends BasicControl {

    /**
     * The server-side sort control's assigned object identifier
     * is 1.2.840.113556.1.4.473.
     */
    public static final String OID = "1.2.840.113556.1.4.473";

    private static final long serialVersionUID = -1965961680233330744L;

    /**
     * Constructs a control to sort on a single attribute in ascending order.
     * Sorting will be performed using the ordering matching rule defined
     * for use with the specified attribute.
     *
     * @param   sortBy  An attribute ID to sort by.
     * @param   criticality     If true then the server must honor the control
     *                          and return the search results sorted as
     *                          requested or refuse to perform the search.
     *                          If false, then the server need not honor the
     *                          control.
     * @throws IOException If an error was encountered while encoding the
     *                     supplied arguments into a control.
     */
    public SortControl(String sortBy, boolean criticality) throws IOException {

        super(OID, criticality, null);
        super.value = setEncodedValue(new SortKey[]{ new SortKey(sortBy) });
    }

    /**
     * Constructs a control to sort on a list of attributes in ascending order.
     * Sorting will be performed using the ordering matching rule defined
     * for use with each of the specified attributes.
     *
     * @param   sortBy  A non-null list of attribute IDs to sort by.
     *                  The list is in order of highest to lowest sort key
     *                  precedence.
     * @param   criticality     If true then the server must honor the control
     *                          and return the search results sorted as
     *                          requested or refuse to perform the search.
     *                          If false, then the server need not honor the
     *                          control.
     * @throws IOException If an error was encountered while encoding the
     *                     supplied arguments into a control.
     */
    public SortControl(String[] sortBy, boolean criticality)
        throws IOException {

        super(OID, criticality, null);
        SortKey[] sortKeys = new SortKey[sortBy.length];
        for (int i = 0; i < sortBy.length; i++) {
            sortKeys[i] = new SortKey(sortBy[i]);
        }
        super.value = setEncodedValue(sortKeys);
    }

    /**
     * Constructs a control to sort on a list of sort keys.
     * Each sort key specifies the sort order and ordering matching rule to use.
     *
     * @param   sortBy      A non-null list of keys to sort by.
     *                      The list is in order of highest to lowest sort key
     *                      precedence.
     * @param   criticality     If true then the server must honor the control
     *                          and return the search results sorted as
     *                          requested or refuse to perform the search.
     *                          If false, then the server need not honor the
     *                          control.
     * @throws IOException If an error was encountered while encoding the
     *                     supplied arguments into a control.
     */
    public SortControl(SortKey[] sortBy, boolean criticality)
        throws IOException {

        super(OID, criticality, null);
        super.value = setEncodedValue(sortBy);
    }

    /**
     * Encodes the sort control's value using ASN.1 BER.
     * The result includes the BER tag and length for the control's value but
     * does not include the control's object identifier and criticality setting.
     *
     * @param   sortKeys    A non-null list of keys to sort by.
     * @return A possibly null byte array representing the ASN.1 BER encoded
     *         value of the sort control.
     * @throws IOException If a BER encoding error occurs.
     */
    private byte[] setEncodedValue(SortKey[] sortKeys) throws IOException {

        // build the ASN.1 BER encoding
        BerEncoder ber = new BerEncoder(30 * sortKeys.length + 10);
        String matchingRule;

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);

        for (int i = 0; i < sortKeys.length; i++) {
            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeString(sortKeys[i].getAttributeID(), true); // v3

            if ((matchingRule = sortKeys[i].getMatchingRuleID()) != null) {
                ber.encodeString(matchingRule, (Ber.ASN_CONTEXT | 0), true);
            }
            if (! sortKeys[i].isAscending()) {
                ber.encodeBoolean(true, (Ber.ASN_CONTEXT | 1));
            }
            ber.endSeq();
        }
        ber.endSeq();

        return ber.getTrimmedBuf();
    }
}
