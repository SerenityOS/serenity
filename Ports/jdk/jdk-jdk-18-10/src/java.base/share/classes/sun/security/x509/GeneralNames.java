/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.io.IOException;

import sun.security.util.*;

/**
 * This object class represents the GeneralNames type required in
 * X509 certificates.
 * <p>The ASN.1 syntax for this is:
 * <pre>
 * GeneralNames ::= SEQUENCE SIZE (1..MAX) OF GeneralName
 * </pre>
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 *
 */
public class GeneralNames {

    private final List<GeneralName> names;

    /**
     * Create the GeneralNames, decoding from the passed DerValue.
     *
     * @param derVal the DerValue to construct the GeneralNames from.
     * @exception IOException on error.
     */
    public GeneralNames(DerValue derVal) throws IOException {
        this();
        if (derVal.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding for GeneralNames.");
        }
        if (derVal.data.available() == 0) {
            throw new IOException("No data available in "
                                      + "passed DER encoded value.");
        }
        // Decode all the GeneralName's
        while (derVal.data.available() != 0) {
            DerValue encName = derVal.data.getDerValue();

            GeneralName name = new GeneralName(encName);
            add(name);
        }
    }

    /**
     * The default constructor for this class.
     */
    public GeneralNames() {
        names = new ArrayList<GeneralName>();
    }

    public GeneralNames add(GeneralName name) {
        if (name == null) {
            throw new NullPointerException();
        }
        names.add(name);
        return this;
    }

    public GeneralName get(int index) {
        return names.get(index);
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    public int size() {
        return names.size();
    }

    public Iterator<GeneralName> iterator() {
        return names.iterator();
    }

    public List<GeneralName> names() {
        return names;
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on error.
     */
    public void encode(DerOutputStream out) throws IOException {
        if (isEmpty()) {
            return;
        }

        DerOutputStream temp = new DerOutputStream();
        for (GeneralName gn : names) {
            gn.encode(temp);
        }
        out.write(DerValue.tag_Sequence, temp);
    }

    /**
     * compare this GeneralNames to other object for equality
     *
     * @return true if this equals obj
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GeneralNames == false) {
            return false;
        }
        GeneralNames other = (GeneralNames)obj;
        return this.names.equals(other.names);
    }

    public int hashCode() {
        return names.hashCode();
    }

    public String toString() {
        return names.toString();
    }

}
