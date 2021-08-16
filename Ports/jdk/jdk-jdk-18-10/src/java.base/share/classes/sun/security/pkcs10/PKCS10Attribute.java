/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs10;

import java.io.OutputStream;
import java.io.IOException;

import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.*;

/**
 * Represent a PKCS#10 Attribute.
 *
 * <p>Attributes are additonal information which can be inserted in a PKCS#10
 * certificate request. For example a "Driving License Certificate" could have
 * the driving license number as an attribute.
 *
 * <p>Attributes are represented as a sequence of the attribute identifier
 * (Object Identifier) and a set of DER encoded attribute values.
 *
 * ASN.1 definition of Attribute:
 * <pre>
 * Attribute :: SEQUENCE {
 *    type    AttributeType,
 *    values  SET OF AttributeValue
 * }
 * AttributeType  ::= OBJECT IDENTIFIER
 * AttributeValue ::= ANY defined by type
 * </pre>
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class PKCS10Attribute implements DerEncoder {

    protected ObjectIdentifier  attributeId = null;
    protected Object            attributeValue = null;

    /**
     * Constructs an attribute from a DER encoding.
     * This constructor expects the value to be encoded as defined above,
     * i.e. a SEQUENCE of OID and SET OF value(s), not a literal
     * X.509 v3 extension. Only PKCS9 defined attributes are supported
     * currently.
     *
     * @param derVal the der encoded attribute.
     * @exception IOException on parsing errors.
     */
    public PKCS10Attribute(DerValue derVal) throws IOException {
        PKCS9Attribute attr = new PKCS9Attribute(derVal);
        this.attributeId = attr.getOID();
        this.attributeValue = attr.getValue();
    }

    /**
     * Constructs an attribute from individual components of
     * ObjectIdentifier and the value (any java object).
     *
     * @param attributeId the ObjectIdentifier of the attribute.
     * @param attributeValue an instance of a class that implements
     * the attribute identified by the ObjectIdentifier.
     */
    public PKCS10Attribute(ObjectIdentifier attributeId,
                           Object attributeValue) {
        this.attributeId = attributeId;
        this.attributeValue = attributeValue;
    }

    /**
     * Constructs an attribute from PKCS9 attribute.
     *
     * @param attr the PKCS9Attribute to create from.
     */
    public PKCS10Attribute(PKCS9Attribute attr) {
        this.attributeId = attr.getOID();
        this.attributeValue = attr.getValue();
    }

    /**
     * DER encode this object onto an output stream.
     * Implements the <code>DerEncoder</code> interface.
     *
     * @param out
     * the OutputStream on which to write the DER encoding.
     *
     * @exception IOException on encoding errors.
     */
    public void derEncode(OutputStream out) throws IOException {
        PKCS9Attribute attr = new PKCS9Attribute(attributeId, attributeValue);
        attr.derEncode(out);
    }

    /**
     * Returns the ObjectIdentifier of the attribute.
     */
    public ObjectIdentifier getAttributeId() {
        return (attributeId);
    }

    /**
     * Returns the attribute value.
     */
    public Object getAttributeValue() {
        return (attributeValue);
    }

    /**
     * Returns the attribute in user readable form.
     */
    public String toString() {
        return (attributeValue.toString());
    }
}
