/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.naming.ConfigurationException;
import javax.naming.NamingException;
import com.sun.naming.internal.VersionHelper;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;

/**
 * This class implements the LDAPv3 Extended Request for StartTLS as
 * defined in
 * <a href="http://www.ietf.org/rfc/rfc2830.txt">Lightweight Directory
 * Access Protocol (v3): Extension for Transport Layer Security</a>
 *
 * The object identifier for StartTLS is 1.3.6.1.4.1.1466.20037
 * and no extended request value is defined.
 *<p>
 * {@code StartTlsRequest}/{@code StartTlsResponse} are used to establish
 * a TLS connection over the existing LDAP connection associated with
 * the JNDI context on which {@code extendedOperation()} is invoked.
 * Typically, a JNDI program uses these classes as follows.
 * <blockquote><pre>
 * import javax.naming.ldap.*;
 *
 * // Open an LDAP association
 * LdapContext ctx = new InitialLdapContext();
 *
 * // Perform a StartTLS extended operation
 * StartTlsResponse tls =
 *     (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
 *
 * // Open a TLS connection (over the existing LDAP association) and get details
 * // of the negotiated TLS session: cipher suite, peer certificate, etc.
 * SSLSession session = tls.negotiate();
 *
 * // ... use ctx to perform protected LDAP operations
 *
 * // Close the TLS connection (revert back to the underlying LDAP association)
 * tls.close();
 *
 * // ... use ctx to perform unprotected LDAP operations
 *
 * // Close the LDAP association
 * ctx.close;
 * </pre></blockquote>
 *
 * @since 1.4
 * @see StartTlsResponse
 * @author Vincent Ryan
 */
public class StartTlsRequest implements ExtendedRequest {

    // Constant

    /**
     * The StartTLS extended request's assigned object identifier
     * is 1.3.6.1.4.1.1466.20037.
     */
    public static final String OID = "1.3.6.1.4.1.1466.20037";


    // Constructors

    /**
     * Constructs a StartTLS extended request.
     */
    public StartTlsRequest() {
    }


    // ExtendedRequest methods

    /**
     * Retrieves the StartTLS request's object identifier string.
     *
     * @return The object identifier string, "1.3.6.1.4.1.1466.20037".
     */
    public String getID() {
        return OID;
    }

    /**
     * Retrieves the StartTLS request's ASN.1 BER encoded value.
     * Since the request has no defined value, null is always
     * returned.
     *
     * @return The null value.
     */
    public byte[] getEncodedValue() {
        return null;
    }

    /**
     * Creates an extended response object that corresponds to the
     * LDAP StartTLS extended request.
     * <p>
     * The result must be a concrete subclass of StartTlsResponse
     * and must have a public zero-argument constructor.
     * <p>
     * This method locates the implementation class by locating
     * configuration files that have the name:
     * <blockquote>{@code
     *     META-INF/services/javax.naming.ldap.StartTlsResponse
     * }</blockquote>
     * The configuration files and their corresponding implementation classes must
     * be accessible to the calling thread's context class loader.
     * <p>
     * Each configuration file should contain a list of fully-qualified class
     * names, one per line.  Space and tab characters surrounding each name, as
     * well as blank lines, are ignored.  The comment character is {@code '#'}
     * ({@code 0x23}); on each line all characters following the first comment
     * character are ignored.  The file must be encoded in UTF-8.
     * <p>
     * This method will return an instance of the first implementation
     * class that it is able to load and instantiate successfully from
     * the list of class names collected from the configuration files.
     * This method uses the calling thread's context classloader to find the
     * configuration files and to load the implementation class.
     * <p>
     * If no class can be found in this way, this method will use
     * an implementation-specific way to locate an implementation.
     * If none is found, a NamingException is thrown.
     *
     * @param id         The object identifier of the extended response.
     *                   Its value must be "1.3.6.1.4.1.1466.20037" or null.
     *                   Both values are equivalent.
     * @param berValue   The possibly null ASN.1 BER encoded value of the
     *                   extended response. This is the raw BER bytes
     *                   including the tag and length of the response value.
     *                   It does not include the response OID.
     *                   Its value is ignored because a Start TLS response
     *                   is not expected to contain any response value.
     * @param offset     The starting position in berValue of the bytes to use.
     *                   Its value is ignored because a Start TLS response
     *                   is not expected to contain any response value.
     * @param length     The number of bytes in berValue to use.
     *                   Its value is ignored because a Start TLS response
     *                   is not expected to contain any response value.
     * @return           The StartTLS extended response object.
     * @throws           NamingException If a naming exception was encountered
     *                   while creating the StartTLS extended response object.
     */
    public ExtendedResponse createExtendedResponse(String id, byte[] berValue,
        int offset, int length) throws NamingException {

        // Confirm that the object identifier is correct
        if ((id != null) && (!id.equals(OID))) {
            throw new ConfigurationException(
                "Start TLS received the following response instead of " +
                OID + ": " + id);
        }

        StartTlsResponse resp = null;

        ServiceLoader<StartTlsResponse> sl = ServiceLoader.load(
                StartTlsResponse.class, getContextClassLoader());
        Iterator<StartTlsResponse> iter = sl.iterator();

        while (resp == null && privilegedHasNext(iter)) {
            resp = iter.next();
        }
        if (resp != null) {
            return resp;
        }
        try {
            VersionHelper helper = VersionHelper.getVersionHelper();
            @SuppressWarnings("deprecation")
            Object o = helper.loadClass(
                "com.sun.jndi.ldap.ext.StartTlsResponseImpl").newInstance();
            resp = (StartTlsResponse) o;

        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw wrapException(e);
        }

        return resp;
    }

    /*
     * Wrap an exception, thrown while attempting to load the StartTlsResponse
     * class, in a configuration exception.
     */
    private ConfigurationException wrapException(Exception e) {
        ConfigurationException ce = new ConfigurationException(
            "Cannot load implementation of javax.naming.ldap.StartTlsResponse");

        ce.setRootCause(e);
        return ce;
    }

    /*
     * Acquire the class loader associated with this thread.
     */
    @SuppressWarnings("removal")
    private final ClassLoader getContextClassLoader() {
        PrivilegedAction<ClassLoader> pa = Thread.currentThread()::getContextClassLoader;
        return AccessController.doPrivileged(pa);
    }

    @SuppressWarnings("removal")
    private static final boolean privilegedHasNext(final Iterator<StartTlsResponse> iter) {
        PrivilegedAction<Boolean> pa = iter::hasNext;
        return AccessController.doPrivileged(pa);
    }

    private static final long serialVersionUID = 4441679576360753397L;
}
