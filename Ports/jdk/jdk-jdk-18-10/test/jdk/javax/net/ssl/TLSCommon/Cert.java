/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * The certificates and corresponding private keys.
 */
public class Cert {

    public final KeyAlgorithm keyAlgo;
    public final SignatureAlgorithm sigAlgo;
    public final HashAlgorithm hashAlgo;

    public final String certMaterials;
    public final String keyMaterials;

    public Cert(
            KeyAlgorithm keyAlgo,
            SignatureAlgorithm sigAlgo,
            HashAlgorithm hashAlgo,
            String certMaterials,
            String keyMaterials) {
        this.keyAlgo = keyAlgo;
        this.sigAlgo = sigAlgo;
        this.hashAlgo = hashAlgo;

        this.certMaterials = certMaterials;
        this.keyMaterials = keyMaterials;
    }

    public Cert(
            KeyAlgorithm keyAlgo,
            SignatureAlgorithm sigAlgo,
            HashAlgorithm hashAlgo,
            String certMaterials) {
        this(keyAlgo, sigAlgo, hashAlgo, certMaterials, null);
    }

    @Override
    public String toString() {
        return "keyAlgo=" + keyAlgo
                + ",sigAlgo=" + sigAlgo
                + ",hashAlg=" + hashAlgo;
    }
}
