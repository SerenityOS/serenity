/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.*;
import java.util.Date;
import java.util.Enumeration;

import sun.security.util.*;

/**
 * This class defines the interval for which the certificate is valid.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see CertAttrSet
 */
public class CertificateValidity implements CertAttrSet<String> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.validity";
    /**
     * Sub attributes name for this CertAttrSet.
     */
    public static final String NAME = "validity";
    public static final String NOT_BEFORE = "notBefore";
    public static final String NOT_AFTER = "notAfter";
    /**
     * YR_2050 date and time set to Jan01 00:00 2050 GMT
     */
    static final long YR_2050 = 2524608000000L;

    // Private data members
    private Date        notBefore;
    private Date        notAfter;

    // Returns the first time the certificate is valid.
    private Date getNotBefore() {
        return (new Date(notBefore.getTime()));
    }

    // Returns the last time the certificate is valid.
    private Date getNotAfter() {
       return (new Date(notAfter.getTime()));
    }

    // Construct the class from the DerValue
    private void construct(DerValue derVal) throws IOException {
        if (derVal.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoded CertificateValidity, " +
                                  "starting sequence tag missing.");
        }
        // check if UTCTime encoded or GeneralizedTime
        if (derVal.data.available() == 0)
            throw new IOException("No data encoded for CertificateValidity");

        DerInputStream derIn = new DerInputStream(derVal.toByteArray());
        DerValue[] seq = derIn.getSequence(2);
        if (seq.length != 2)
            throw new IOException("Invalid encoding for CertificateValidity");

        if (seq[0].tag == DerValue.tag_UtcTime) {
            notBefore = derVal.data.getUTCTime();
        } else if (seq[0].tag == DerValue.tag_GeneralizedTime) {
            notBefore = derVal.data.getGeneralizedTime();
        } else {
            throw new IOException("Invalid encoding for CertificateValidity");
        }

        if (seq[1].tag == DerValue.tag_UtcTime) {
            notAfter = derVal.data.getUTCTime();
        } else if (seq[1].tag == DerValue.tag_GeneralizedTime) {
            notAfter = derVal.data.getGeneralizedTime();
        } else {
            throw new IOException("Invalid encoding for CertificateValidity");
        }
    }

    /**
     * Default constructor for the class.
     */
    public CertificateValidity() { }

    /**
     * The default constructor for this class for the specified interval.
     *
     * @param notBefore the date and time before which the certificate
     *                   is not valid.
     * @param notAfter the date and time after which the certificate is
     *                  not valid.
     */
    public CertificateValidity(Date notBefore, Date notAfter) {
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    /**
     * Create the object, decoding the values from the passed DER stream.
     *
     * @param in the DerInputStream to read the CertificateValidity from.
     * @exception IOException on decoding errors.
     */
    public CertificateValidity(DerInputStream in) throws IOException {
        DerValue derVal = in.getDerValue();
        construct(derVal);
    }

    /**
     * Return the validity period as user readable string.
     */
    public String toString() {
        if (notBefore == null || notAfter == null)
            return "";
        return "Validity: [From: " + notBefore +
               ",\n               To: " + notAfter + ']';
    }

    /**
     * Encode the CertificateValidity period in DER form to the stream.
     *
     * @param out the OutputStream to marshal the contents to.
     * @exception IOException on errors.
     */
    public void encode(OutputStream out) throws IOException {

        // in cases where default constructor is used check for
        // null values
        if (notBefore == null || notAfter == null) {
            throw new IOException("CertAttrSet:CertificateValidity:" +
                                  " null values to encode.\n");
        }
        DerOutputStream pair = new DerOutputStream();

        if (notBefore.getTime() < YR_2050) {
            pair.putUTCTime(notBefore);
        } else
            pair.putGeneralizedTime(notBefore);

        if (notAfter.getTime() < YR_2050) {
            pair.putUTCTime(notAfter);
        } else {
            pair.putGeneralizedTime(notAfter);
        }
        DerOutputStream seq = new DerOutputStream();
        seq.write(DerValue.tag_Sequence, pair);

        out.write(seq.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    public void set(String name, Object obj) throws IOException {
        if (!(obj instanceof Date)) {
            throw new IOException("Attribute must be of type Date.");
        }
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            notBefore = (Date)obj;
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            notAfter = (Date)obj;
        } else {
            throw new IOException("Attribute name not recognized by " +
                            "CertAttrSet: CertificateValidity.");
        }
    }

    /**
     * Get the attribute value.
     */
    public Date get(String name) throws IOException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            return (getNotBefore());
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            return (getNotAfter());
        } else {
            throw new IOException("Attribute name not recognized by " +
                            "CertAttrSet: CertificateValidity.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            notBefore = null;
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            notAfter = null;
        } else {
            throw new IOException("Attribute name not recognized by " +
                            "CertAttrSet: CertificateValidity.");
        }
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(NOT_BEFORE);
        elements.addElement(NOT_AFTER);

        return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return (NAME);
    }

    /**
     * Verify that the current time is within the validity period.
     *
     * @exception CertificateExpiredException if the certificate has expired.
     * @exception CertificateNotYetValidException if the certificate is not
     * yet valid.
     */
    public void valid()
    throws CertificateNotYetValidException, CertificateExpiredException {
        Date now = new Date();
        valid(now);
    }

    /**
     * Verify that the passed time is within the validity period.
     * @param now the Date against which to compare the validity
     * period.
     *
     * @exception CertificateExpiredException if the certificate has expired
     * with respect to the <code>Date</code> supplied.
     * @exception CertificateNotYetValidException if the certificate is not
     * yet valid with respect to the <code>Date</code> supplied.
     *
     */
    public void valid(Date now)
    throws CertificateNotYetValidException, CertificateExpiredException {
        /*
         * we use the internal Dates rather than the passed in Date
         * because someone could override the Date methods after()
         * and before() to do something entirely different.
         */
        if (notBefore.after(now)) {
            throw new CertificateNotYetValidException("NotBefore: " +
                                                      notBefore.toString());
        }
        if (notAfter.before(now)) {
            throw new CertificateExpiredException("NotAfter: " +
                                                  notAfter.toString());
        }
    }
}
