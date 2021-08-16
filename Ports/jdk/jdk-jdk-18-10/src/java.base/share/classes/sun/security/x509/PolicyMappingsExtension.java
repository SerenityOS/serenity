/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import sun.security.util.*;

/**
 * Represent the Policy Mappings Extension.
 *
 * This extension, if present, identifies the certificate policies considered
 * identical between the issuing and the subject CA.
 * <p>Extensions are addiitonal attributes which can be inserted in a X509
 * v3 certificate. For example a "Driving License Certificate" could have
 * the driving license number as a extension.
 *
 * <p>Extensions are represented as a sequence of the extension identifier
 * (Object Identifier), a boolean flag stating whether the extension is to
 * be treated as being critical and the extension value itself (this is again
 * a DER encoding of the extension value).
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see Extension
 * @see CertAttrSet
 */
public class PolicyMappingsExtension extends Extension
implements CertAttrSet<String> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.PolicyMappings";
    /**
     * Attribute names.
     */
    public static final String NAME = "PolicyMappings";
    public static final String MAP = "map";

    // Private data members
    private List<CertificatePolicyMap> maps;

    // Encode this extension value
    private void encodeThis() throws IOException {
        if (maps == null || maps.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream os = new DerOutputStream();
        DerOutputStream tmp = new DerOutputStream();

        for (CertificatePolicyMap map : maps) {
            map.encode(tmp);
        }

        os.write(DerValue.tag_Sequence, tmp);
        this.extensionValue = os.toByteArray();
    }

    /**
     * Create a PolicyMappings with the List of CertificatePolicyMap.
     *
     * @param maps the List of CertificatePolicyMap.
     */
    public PolicyMappingsExtension(List<CertificatePolicyMap> maps)
            throws IOException {
        this.maps = maps;
        this.extensionId = PKIXExtensions.PolicyMappings_Id;
        this.critical = true;
        encodeThis();
    }

    /**
     * Create a default PolicyMappingsExtension.
     */
    public PolicyMappingsExtension() {
        extensionId = PKIXExtensions.PolicyMappings_Id;
        critical = true;
        maps = Collections.<CertificatePolicyMap>emptyList();
    }

    /**
     * Create the extension from the passed DER encoded value.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public PolicyMappingsExtension(Boolean critical, Object value)
    throws IOException {
        this.extensionId = PKIXExtensions.PolicyMappings_Id;
        this.critical = critical.booleanValue();

        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding for " +
                                  "PolicyMappingsExtension.");
        }
        maps = new ArrayList<CertificatePolicyMap>();
        while (val.data.available() != 0) {
            DerValue seq = val.data.getDerValue();
            CertificatePolicyMap map = new CertificatePolicyMap(seq);
            maps.add(map);
        }
    }

    /**
     * Returns a printable representation of the policy map.
     */
    public String toString() {
        if (maps == null) return "";
        String s = super.toString() + "PolicyMappings [\n"
                 + maps.toString() + "]\n";

        return (s);
    }

    /**
     * Write the extension to the OutputStream.
     *
     * @param out the OutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (extensionValue == null) {
            extensionId = PKIXExtensions.PolicyMappings_Id;
            critical = true;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    @SuppressWarnings("unchecked") // Checked with instanceof
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(MAP)) {
            if (!(obj instanceof List)) {
              throw new IOException("Attribute value should be of" +
                                    " type List.");
            }
            maps = (List<CertificatePolicyMap>)obj;
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:PolicyMappingsExtension.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public List<CertificatePolicyMap> get(String name) throws IOException {
        if (name.equalsIgnoreCase(MAP)) {
            return (maps);
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:PolicyMappingsExtension.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(MAP)) {
            maps = null;
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:PolicyMappingsExtension.");
        }
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements () {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(MAP);

        return elements.elements();
    }

    /**
     * Return the name of this attribute.
     */
    public String getName () {
        return (NAME);
    }
}
