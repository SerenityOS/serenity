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
  * This interface represents an LDAPv3 control as defined in
  * <A HREF="http://www.ietf.org/rfc/rfc2251.txt">RFC 2251</A>.
  *<p>
  * The LDAPv3 protocol uses controls to send and receive additional data
  * to affect the behavior of predefined operations.
  * Controls can be sent along with any LDAP operation to the server.
  * These are referred to as <em>request controls</em>. For example, a
  * "sort" control can be sent with an LDAP search operation to
  * request that the results be returned in a particular order.
  * Solicited and unsolicited controls can also be returned with
  * responses from the server. Such controls are referred to as
  * <em>response controls</em>. For example, an LDAP server might
  * define a special control to return change notifications.
  *<p>
  * This interface is used to represent both request and response controls.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  *
  * @see ControlFactory
  * @since 1.3
  */
public interface Control extends java.io.Serializable {
    /**
      * Indicates a critical control.
      * The value of this constant is {@code true}.
      */
    public static final boolean CRITICAL = true;

    /**
      * Indicates a non-critical control.
      * The value of this constant is {@code false}.
      */
    public static final boolean NONCRITICAL = false;

    /**
      * Retrieves the object identifier assigned for the LDAP control.
      *
      * @return The non-null object identifier string.
      */
    public String getID();

    /**
      * Determines the criticality of the LDAP control.
      * A critical control must not be ignored by the server.
      * In other words, if the server receives a critical control
      * that it does not support, regardless of whether the control
      * makes sense for the operation, the operation will not be performed
      * and an {@code OperationNotSupportedException} will be thrown.
      * @return true if this control is critical; false otherwise.
      */
    public boolean isCritical();

    /**
      * Retrieves the ASN.1 BER encoded value of the LDAP control.
      * The result is the raw BER bytes including the tag and length of
      * the control's value. It does not include the controls OID or criticality.
      *
      * Null is returned if the value is absent.
      *
      * @return A possibly null byte array representing the ASN.1 BER encoded
      *         value of the LDAP control.
      */
    public byte[] getEncodedValue();

    // static final long serialVersionUID = -591027748900004825L;
}
