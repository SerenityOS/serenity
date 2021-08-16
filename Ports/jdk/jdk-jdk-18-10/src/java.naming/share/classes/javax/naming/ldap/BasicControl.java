/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * This class provides a basic implementation of the {@code Control}
 * interface. It represents an LDAPv3 Control as defined in
 * <a href="http://www.ietf.org/rfc/rfc2251.txt">RFC 2251</a>.
 *
 * @since 1.5
 * @author Vincent Ryan
 */
public class BasicControl implements Control {

    /**
     * The control's object identifier string.
     *
     * @serial
     */
    protected String id;

    /**
     * The control's criticality.
     *
     * @serial
     */
    protected boolean criticality = false; // default

    /**
     * The control's ASN.1 BER encoded value.
     *
     * @serial
     */
    protected byte[] value = null;

    private static final long serialVersionUID = -4233907508771791687L;

    /**
     * Constructs a non-critical control.
     *
     * @param   id      The control's object identifier string.
     *
     */
    public BasicControl(String id) {
        this.id = id;
    }

    /**
     * Constructs a control using the supplied arguments.
     *
     * @param   id              The control's object identifier string.
     * @param   criticality     The control's criticality.
     * @param   value           The control's ASN.1 BER encoded value.
     *                          It is not cloned - any changes to value
     *                          will affect the contents of the control.
     *                          It may be null.
     */
    public BasicControl(String id, boolean criticality, byte[] value) {
        this.id = id;
        this.criticality = criticality;
        this.value = value;
    }

    /**
     * Retrieves the control's object identifier string.
     *
     * @return The non-null object identifier string.
     */
    public String getID() {
        return id;
    }

    /**
     * Determines the control's criticality.
     *
     * @return true if the control is critical; false otherwise.
     */
    public boolean isCritical() {
        return criticality;
    }

    /**
     * Retrieves the control's ASN.1 BER encoded value.
     * The result includes the BER tag and length for the control's value but
     * does not include the control's object identifier and criticality setting.
     *
     * @return A possibly null byte array representing the control's
     *          ASN.1 BER encoded value. It is not cloned - any changes to the
     *          returned value will affect the contents of the control.
     */
    public byte[] getEncodedValue() {
        return value;
    }
}
