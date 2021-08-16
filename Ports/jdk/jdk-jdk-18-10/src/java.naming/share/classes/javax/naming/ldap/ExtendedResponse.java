/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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
  * This interface represents an LDAP extended operation response as defined in
  * <A HREF="http://www.ietf.org/rfc/rfc2251.txt">RFC 2251</A>.
  * <pre>
  *     ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
  *          COMPONENTS OF LDAPResult,
  *          responseName     [10] LDAPOID OPTIONAL,
  *          response         [11] OCTET STRING OPTIONAL }
  * </pre>
  * It comprises an optional object identifier and an optional ASN.1 BER
  * encoded value.
  *
  *<p>
  * The methods in this class can be used by the application to get low
  * level information about the extended operation response. However, typically,
  * the application will be using methods specific to the class that
  * implements this interface. Such a class should have decoded the BER buffer
  * in the response and should provide methods that allow the user to
  * access that data in the response in a type-safe and friendly manner.
  *<p>
  * For example, suppose the LDAP server supported a 'get time' extended operation.
  * It would supply GetTimeRequest and GetTimeResponse classes.
  * The GetTimeResponse class might look like:
  *<blockquote><pre>
  * public class GetTimeResponse implements ExtendedResponse {
  *     public java.util.Date getDate() {...};
  *     public long getTime() {...};
  *     ....
  * }
  *</pre></blockquote>
  * A program would use then these classes as follows:
  *<blockquote><pre>
  * GetTimeResponse resp =
  *     (GetTimeResponse) ectx.extendedOperation(new GetTimeRequest());
  * java.util.Date now = resp.getDate();
  *</pre></blockquote>
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  *
  * @see ExtendedRequest
  * @since 1.3
  */

public interface ExtendedResponse extends java.io.Serializable {

    /**
      * Retrieves the object identifier of the response.
      * The LDAP protocol specifies that the response object identifier is optional.
      * If the server does not send it, the response will contain no ID (i.e. null).
      *
      * @return A possibly null object identifier string representing the LDAP
      *         {@code ExtendedResponse.responseName} component.
      */
    public String getID();

    /**
      * Retrieves the ASN.1 BER encoded value of the LDAP extended operation
      * response. Null is returned if the value is absent from the response
      * sent by the LDAP server.
      * The result is the raw BER bytes including the tag and length of
      * the response value. It does not include the response OID.
      *
      * @return A possibly null byte array representing the ASN.1 BER encoded
      *         contents of the LDAP {@code ExtendedResponse.response}
      *         component.
      */
    public byte[] getEncodedValue();

    //static final long serialVersionUID = -3320509678029180273L;
}
