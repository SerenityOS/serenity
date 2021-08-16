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
 * Requests that the results of a search operation be returned by the LDAP
 * server in batches of a specified size.
 * The requestor controls the rate at which batches are returned by the rate
 * at which it invokes search operations.
 * <p>
 * The following code sample shows how the class may be used:
 * <pre>{@code
 *
 *     // Open an LDAP association
 *     LdapContext ctx = new InitialLdapContext();
 *
 *     // Activate paged results
 *     int pageSize = 20; // 20 entries per page
 *     byte[] cookie = null;
 *     int total;
 *     ctx.setRequestControls(new Control[]{
 *         new PagedResultsControl(pageSize, Control.CRITICAL) });
 *
 *     do {
 *         // Perform the search
 *         NamingEnumeration results =
 *             ctx.search("", "(objectclass=*)", new SearchControls());
 *
 *         // Iterate over a batch of search results
 *         while (results != null && results.hasMore()) {
 *             // Display an entry
 *             SearchResult entry = (SearchResult)results.next();
 *             System.out.println(entry.getName());
 *             System.out.println(entry.getAttributes());
 *
 *             // Handle the entry's response controls (if any)
 *             if (entry instanceof HasControls) {
 *                 // ((HasControls)entry).getControls();
 *             }
 *         }
 *         // Examine the paged results control response
 *         Control[] controls = ctx.getResponseControls();
 *         if (controls != null) {
 *             for (int i = 0; i < controls.length; i++) {
 *                 if (controls[i] instanceof PagedResultsResponseControl) {
 *                     PagedResultsResponseControl prrc =
 *                         (PagedResultsResponseControl)controls[i];
 *                     total = prrc.getResultSize();
 *                     cookie = prrc.getCookie();
 *                 } else {
 *                     // Handle other response controls (if any)
 *                 }
 *             }
 *         }
 *
 *         // Re-activate paged results
 *         ctx.setRequestControls(new Control[]{
 *             new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });
 *     } while (cookie != null);
 *
 *     // Close the LDAP association
 *     ctx.close();
 *     ...
 *
 * } </pre>
 * <p>
 * This class implements the LDAPv3 Control for paged-results as defined in
 * <a href="http://www.ietf.org/rfc/rfc2696.txt">RFC 2696</a>.
 *
 * The control's value has the following ASN.1 definition:
 * <pre>{@code
 *
 *     realSearchControlValue ::= SEQUENCE {
 *         size      INTEGER (0..maxInt),
 *                           -- requested page size from client
 *                           -- result set size estimate from server
 *         cookie    OCTET STRING
 *     }
 *
 * }</pre>
 *
 * @since 1.5
 * @see PagedResultsResponseControl
 * @author Vincent Ryan
 */
public final class PagedResultsControl extends BasicControl {

    /**
     * The paged-results control's assigned object identifier
     * is 1.2.840.113556.1.4.319.
     */
    public static final String OID = "1.2.840.113556.1.4.319";

    private static final byte[] EMPTY_COOKIE = new byte[0];

    private static final long serialVersionUID = 6684806685736844298L;

    /**
     * Constructs a control to set the number of entries to be returned per
     * page of results.
     *
     * @param   pageSize        The number of entries to return in a page.
     * @param   criticality     If true then the server must honor the control
     *                          and return search results as indicated by
     *                          pageSize or refuse to perform the search.
     *                          If false, then the server need not honor the
     *                          control.
     * @throws IOException      If an error was encountered while encoding the
     *                          supplied arguments into a control.
     */
    public PagedResultsControl(int pageSize, boolean criticality)
            throws IOException {

        super(OID, criticality, null);
        value = setEncodedValue(pageSize, EMPTY_COOKIE);
    }

    /**
     * Constructs a control to set the number of entries to be returned per
     * page of results. The cookie is provided by the server and may be
     * obtained from the paged-results response control.
     * <p>
     * A sequence of paged-results can be abandoned by setting the pageSize
     * to zero and setting the cookie to the last cookie received from the
     * server.
     *
     * @param   pageSize        The number of entries to return in a page.
     * @param   cookie          A possibly null server-generated cookie.
     * @param   criticality     If true then the server must honor the control
     *                          and return search results as indicated by
     *                          pageSize or refuse to perform the search.
     *                          If false, then the server need not honor the
     *                          control.
     * @throws IOException      If an error was encountered while encoding the
     *                          supplied arguments into a control.
     */
    public PagedResultsControl(int pageSize, byte[] cookie,
        boolean criticality) throws IOException {

        super(OID, criticality, null);
        if (cookie == null) {
            cookie = EMPTY_COOKIE;
        }
        value = setEncodedValue(pageSize, cookie);
    }

    /**
     * Encodes the paged-results control's value using ASN.1 BER.
     * The result includes the BER tag and length for the control's value but
     * does not include the control's object identifier and criticality setting.
     *
     * @param   pageSize        The number of entries to return in a page.
     * @param   cookie          A non-null server-generated cookie.
     * @return A possibly null byte array representing the ASN.1 BER encoded
     *         value of the LDAP paged-results control.
     * @throws IOException If a BER encoding error occurs.
     */
    private byte[] setEncodedValue(int pageSize, byte[] cookie)
        throws IOException {

        // build the ASN.1 encoding
        BerEncoder ber = new BerEncoder(10 + cookie.length);

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeInt(pageSize);
            ber.encodeOctetString(cookie, Ber.ASN_OCTET_STR);
        ber.endSeq();

        return ber.getTrimmedBuf();
    }
}
