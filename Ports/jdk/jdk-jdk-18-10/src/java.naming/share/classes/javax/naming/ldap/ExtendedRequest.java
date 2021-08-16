/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.NamingException;

/**
  * This interface represents an LDAPv3 extended operation request as defined in
  * <A HREF="http://www.ietf.org/rfc/rfc2251.txt">RFC 2251</A>.
  * <pre>
  *     ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
  *              requestName      [0] LDAPOID,
  *              requestValue     [1] OCTET STRING OPTIONAL }
  * </pre>
  * It comprises an object identifier string and an optional ASN.1 BER
  * encoded value.
  *<p>
  * The methods in this class are used by the service provider to construct
  * the bits to send to the LDAP server. Applications typically only deal with
  * the classes that implement this interface, supplying them with
  * any information required for a particular extended operation request.
  * It would then pass such a class as an argument to the
  * {@code LdapContext.extendedOperation()} method for performing the
  * LDAPv3 extended operation.
  *<p>
  * For example, suppose the LDAP server supported a 'get time' extended operation.
  * It would supply GetTimeRequest and GetTimeResponse classes:
  *<blockquote><pre>
  * public class GetTimeRequest implements ExtendedRequest {
  *     public GetTimeRequest() {... };
  *     public ExtendedResponse createExtendedResponse(String id,
  *         byte[] berValue, int offset, int length)
  *         throws NamingException {
  *         return new GetTimeResponse(id, berValue, offset, length);
  *     }
  *     ...
  * }
  * public class GetTimeResponse implements ExtendedResponse {
  *     long time;
  *     public GetTimeResponse(String id, byte[] berValue, int offset,
  *         int length) throws NamingException {
  *         time =      ... // decode berValue to get time
  *     }
  *     public java.util.Date getDate() { return new java.util.Date(time) };
  *     public long getTime() { return time };
  *     ...
  * }
  *</pre></blockquote>
  * A program would use then these classes as follows:
  *<blockquote><pre>
  * GetTimeResponse resp =
  *     (GetTimeResponse) ectx.extendedOperation(new GetTimeRequest());
  * long time = resp.getTime();
  *</pre></blockquote>
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  *
  * @see ExtendedResponse
  * @see LdapContext#extendedOperation
  * @since 1.3
  */
public interface ExtendedRequest extends java.io.Serializable {

    /**
      * Retrieves the object identifier of the request.
      *
      * @return The non-null object identifier string representing the LDAP
      *         {@code ExtendedRequest.requestName} component.
      */
    public String getID();

    /**
      * Retrieves the ASN.1 BER encoded value of the LDAP extended operation
      * request. Null is returned if the value is absent.
      *
      * The result is the raw BER bytes including the tag and length of
      * the request value. It does not include the request OID.
      * This method is called by the service provider to get the bits to
      * put into the extended operation to be sent to the LDAP server.
      *
      * @return A possibly null byte array representing the ASN.1 BER encoded
      *         contents of the LDAP {@code ExtendedRequest.requestValue}
      *         component.
      * @throws IllegalStateException If the encoded value cannot be retrieved
      * because the request contains insufficient or invalid data/state.
      */
    public byte[] getEncodedValue();

    /**
      * Creates the response object that corresponds to this request.
      *<p>
      * After the service provider has sent the extended operation request
      * to the LDAP server, it will receive a response from the server.
      * If the operation failed, the provider will throw a NamingException.
      * If the operation succeeded, the provider will invoke this method
      * using the data that it got back in the response.
      * It is the job of this method to return a class that implements
      * the ExtendedResponse interface that is appropriate for the
      * extended operation request.
      *<p>
      * For example, a Start TLS extended request class would need to know
      * how to process a Start TLS extended response. It does this by creating
      * a class that implements ExtendedResponse.
      *
      * @param id       The possibly null object identifier of the response
      *                 control.
      * @param berValue The possibly null ASN.1 BER encoded value of the
      *                 response control.
      * This is the raw BER bytes including the tag and length of
      * the response value. It does not include the response OID.
      * @param offset   The starting position in berValue of the bytes to use.
      * @param length   The number of bytes in berValue to use.
      *
      * @return A non-null object.
      * @throws NamingException if cannot create extended response
      *     due to an error.
      * @see ExtendedResponse
      */
    public ExtendedResponse createExtendedResponse(String id,
                byte[] berValue, int offset, int length) throws NamingException;

    // static final long serialVersionUID = -7560110759229059814L;
}
